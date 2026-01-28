package come.watch.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import come.watch.mapper.RumDailyAggMapper;
import come.watch.mapper.RumPoMapper;
import come.watch.repository.RumDailyPo;
import come.watch.repository.RumPo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RUM 每日聚合定时任务
 * <p>
 * 功能说明：
 * 1. 每天凌晨 00:10 执行，聚合昨天的明细数据
 * 2. 先按 (pageId, sessionId) 去重，同一页面会话只保留 value 最大的记录
 * 3. 再按 (env, metric, routeKey, releaseVer) 分组，计算 P50/P75/P95 和 good 占比
 * 4. 结果写入 rum_daily_agg 表，用于榜单和版本对比展示
 * <p>
 * 聚合维度说明：
 * - metric: 指标类型 (1=LCP, 2=INP, 3=CLS)
 * - env: 环境 (prod/staging/dev)
 * - routeKey: 归一化路由
 * - releaseVer: 发布版本
 * - pageId: 页面标识（用于去重）
 * - sessionId: 会话标识（用于去重）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RumDailyAggJob {

    private final RumPoMapper eventMapper;
    private final RumDailyAggMapper aggMapper;


    /**
     * 每天 00:10 跑昨天的数据聚合
     * <p>
     * Cron 表达式说明：秒 分 时 日 月 周
     * - 0 10 0 * * ? = 每天凌晨 0 点 10 分 0 秒执行
     */
    @Scheduled(cron = "0 10 0 * * ?")
    public void run() {
        // 1. 确定统计日期：昨天
        LocalDate day = LocalDate.now().minusDays(1);

        // 确定时间范围：[昨天 00:00:00, 今天 00:00:00)
        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = day.plusDays(1).atStartOfDay();

        // 2) 查询明细数据（Phase1 简化：一次性拉取；数据量大时需改成分页）
        // 过滤条件：
        //   - create_time >= start AND create_time < end 限定日期
        //   - deleted = 0 排除软删数据
        //   - metric/routeKey/releaseVer/env 非空
        List<RumPo> rows = eventMapper.selectList(new LambdaQueryWrapper<RumPo>()
                .ge(RumPo::getCreateTime, start)
                .lt(RumPo::getCreateTime, end)
                .eq(RumPo::getDeleted, 0)
                .isNotNull(RumPo::getMetric)
                .isNotNull(RumPo::getRouteKey)
                .isNotNull(RumPo::getReleaseVer)
                .isNotNull(RumPo::getEnv)
        );

        if (rows.isEmpty()) {
            log.info("RUM agg: no data for {}", day);
            return;
        }

        // 3) 明细去重：同一会话/同一页面视图/同一指标/同一路由 只保留一条
        // 说明：
        // - 前端一个 pageId 代表“单次页面加载”，期间可能多次 flush 上报
        // - 同一 pageId 下不同 routeKey（SPA 路由切换）不能互相覆盖
        // - LCP/INP/CLS 都会在生命周期内多次上报，因此去重维度必须包含 metric
        // 去重维度：(sessionId, pageId, metric, routeKey)
        // 选择策略：优先取 createTime 最新的一条；若时间相同/缺失，取 value 更大的（更贴近最终/最差体验）
        Map<String, RumPo> dedup = new HashMap<>(rows.size() * 2);
        for (RumPo e : rows) {
            if (e.getPageId() == null || e.getSessionId() == null || e.getMetric() == null || e.getRouteKey() == null) {
                continue;
            }
            String k = e.getSessionId() + "|" + e.getPageId() + "|" + e.getMetric() + "|" + e.getRouteKey();
            RumPo old = dedup.get(k);
            if (old == null) {
                dedup.put(k, e);
                continue;
            }

            // 先比时间（越晚越优先）
            LocalDateTime nt = e.getCreateTime();
            LocalDateTime ot = old.getCreateTime();
            if (nt != null && ot != null) {
                if (nt.isAfter(ot)) {
                    dedup.put(k, e);
                }
                continue;
            }
            if (nt != null && ot == null) {
                dedup.put(k, e);
                continue;
            }

            // 再比 value（越大越优先）
            Double nv = e.getValue();
            Double ov = old.getValue();
            if (nv != null && ov != null && nv > ov) {
                dedup.put(k, e);
            }
        }

        // 4) 按 (env, metric, routeKey, releaseVer) 分组统计
        // - valuesMap: 收集每个分组的 value 列表，用于计算百分位
        // - ratingCntMap: 统计 good 次数和总次数，用于计算 goodRate
        Map<GroupKey, List<Double>> valuesMap = new HashMap<>();
        Map<GroupKey, int[]> ratingCntMap = new HashMap<>(); // int[0]=good次数, int[1]=总次数

        for (RumPo e : dedup.values()) {
            if (e.getValue() == null) continue;

            // 生成分组 key
            GroupKey g = new GroupKey(e.getEnv(), e.getMetric(), e.getRouteKey(), e.getReleaseVer());

            // 收集 value
            valuesMap.computeIfAbsent(g, x -> new ArrayList<>()).add(e.getValue());

            // 统计 rating
            int[] rt = ratingCntMap.computeIfAbsent(g, x -> new int[2]);
            rt[1]++; // total++
            // rating=0 表示 good（如 INP <= 200ms），这里做简单判断
            if (e.getRating() != null && e.getRating() == 0) rt[0]++; // good++
        }

        // 5) 计算百分位并写入聚合表（upsert）
        int written = 0;
        for (Map.Entry<GroupKey, List<Double>> entry : valuesMap.entrySet()) {
            GroupKey g = entry.getKey();
            List<Double> vals = entry.getValue();

            // 排序后才能计算百分位
            vals.sort(Double::compareTo);

            int n = vals.size();
            double p50 = percentile(vals, 0.50);
            double p75 = percentile(vals, 0.75);
            double p95 = percentile(vals, 0.95);

            // 计算 good 占比
            int[] rt = ratingCntMap.getOrDefault(g, new int[]{0, n});
            BigDecimal goodRate = (rt[1] == 0) ? null :
                    BigDecimal.valueOf((double) rt[0] / rt[1]).setScale(4, RoundingMode.HALF_UP);

            // 构建聚合记录
            RumDailyPo agg = new RumDailyPo();
            agg.setDay(day);
            agg.setEnv(g.env);
            agg.setMetric(g.metric);
            agg.setRouteKey(g.routeKey);
            agg.setReleaseVer(g.releaseVer);
            agg.setCnt(n);
            agg.setP50(p50);
            agg.setP75(p75);
            agg.setP95(p95);
            agg.setGoodRate(goodRate);
            agg.setDeleted(0);

            // 写入聚合表（存在则更新，不存在则插入）
            aggMapper.upsert(agg);
            written++;
        }

        log.info("RUM agg done. day={}, groups={}, rawRows={}, dedupRows={}", day, written, rows.size(), dedup.size());
    }

    /**
     * 计算百分位值
     * <p>
     * 注意：vals 必须已升序排序
     * <p>
     * 百分位计算方式（简化版）：
     * - p=0.50 (P50) 取中间值
     * - p=0.75 (P75) 取 75% 位置的值
     * - p=0.95 (P95) 取 95% 位置的值
     *
     * @param vals 已排序的 value 列表
     * @param p    百分位比例 (0~1)
     * @return 百分位值
     */
    private double percentile(List<Double> vals, double p) {
        int n = vals.size();
        if (n == 0) return Double.NaN;
        // 计算索引位置（向上取整后减1得到0-based索引）
        int idx = (int) Math.ceil(p * n) - 1;
        if (idx < 0) idx = 0;
        if (idx >= n) idx = n - 1;
        return vals.get(idx);
    }

    /**
     * 聚合分组 Key
     * <p>
     * 用于按 [env, metric, routeKey, releaseVer] 维度聚合数据
     */
    @AllArgsConstructor
    @EqualsAndHashCode
    @Data
    static class GroupKey {
        String env;
        Integer metric;
        String routeKey;
        String releaseVer;
    }
}
