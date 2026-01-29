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
 * 1. 每天凌晨 00:10 执行，聚合近 1 天明细数据
 * 2. 先按 (day, env, metric, routeKey, releaseVer, dimKey, pageId, sessionId) 去重，同一分组只保留 value 最大的记录
 * 3. 再按 (day, env, metric, routeKey, releaseVer, dimKey) 分组，计算 P50/P75/P95 和 good 占比
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
        // 1) 统计窗口：昨天 00:00:00 ~ 今天 00:00:00 (左闭右开)
        LocalDate day = LocalDate.now(); //.minusDays(1);
        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = day.plusDays(1).atStartOfDay();

        // 2) 拉取原始明细
        // 过滤条件对齐 SQL：
        // - create_time 在统计窗口内
        // - deleted = 0
        // - value/page_id/session_id 不为空
        List<RumPo> rows = eventMapper.selectList(new LambdaQueryWrapper<RumPo>()
                .ge(RumPo::getCreateTime, start)
                .lt(RumPo::getCreateTime, end)
                .eq(RumPo::getDeleted, 0)
                .isNotNull(RumPo::getValue)
                .isNotNull(RumPo::getPageId)
                .isNotNull(RumPo::getSessionId)
        );

        if (rows.isEmpty()) {
            log.info("RUM agg: no data for {}", day);
            return;
        }

        // 3) 去重：同一 (day, env, metric, routeKey, releaseVer, dimKey, pageId, sessionId)
        // 保留 value 最大的一条，同时保留 rating 最大值（与 SQL 的 MAX(value)/MAX(rating) 对齐）
        Map<DedupKey, DedupValue> dedup = new HashMap<>(rows.size() * 2);
        for (RumPo e : rows) {
            // 缺少 createTime 无法落日，直接跳过
            if (e.getCreateTime() == null) {
                continue;
            }
            LocalDate rowDay = e.getCreateTime().toLocalDate();
            String dimKey = e.getDimKey() == null ? "" : e.getDimKey();
            // 注意 dimKey 空值归一化为 ""，保持分组一致
            DedupKey key = new DedupKey(rowDay, e.getEnv(), e.getMetric(), e.getRouteKey(), e.getReleaseVer(), dimKey, e.getPageId(), e.getSessionId());
            DedupValue old = dedup.get(key);
            if (old == null) {
                dedup.put(key, new DedupValue(e.getValue(), e.getRating()));
                continue;
            }
            // value 取最大值
            if (e.getValue() != null && (old.value == null || e.getValue() > old.value)) {
                old.value = e.getValue();
            }
            // rating 取最大值
            if (e.getRating() != null && (old.rating == null || e.getRating() > old.rating)) {
                old.rating = e.getRating();
            }
        }

        // 4) 聚合分组：(day, env, metric, routeKey, releaseVer, dimKey)
        // valuesMap: 收集每组 value 列表
        // ratingCntMap: 统计 good/total
        Map<GroupKey, List<Double>> valuesMap = new HashMap<>();
        Map<GroupKey, int[]> ratingCntMap = new HashMap<>();

        for (Map.Entry<DedupKey, DedupValue> entry : dedup.entrySet()) {
            DedupKey k = entry.getKey();
            DedupValue v = entry.getValue();
            if (v.value == null) {
                continue;
            }

            GroupKey g = new GroupKey(k.day, k.env, k.metric, k.routeKey, k.releaseVer, k.dimKey);
            // 收集 value 以计算百分位
            valuesMap.computeIfAbsent(g, x -> new ArrayList<>()).add(v.value);

            int[] rt = ratingCntMap.computeIfAbsent(g, x -> new int[2]);
            rt[1]++;
            // rating=0 代表 good
            if (v.rating != null && v.rating == 0) {
                rt[0]++;
            }
        }

        // 5) 百分位/占比计算并写入聚合表
        int written = 0;
        for (Map.Entry<GroupKey, List<Double>> entry : valuesMap.entrySet()) {
            GroupKey g = entry.getKey();
            List<Double> vals = entry.getValue();
            // 百分位依赖排序
            vals.sort(Double::compareTo);

            int n = vals.size();
            double p50 = percentile(vals, 0.50);
            double p75 = percentile(vals, 0.75);
            double p95 = percentile(vals, 0.95);

            int[] rt = ratingCntMap.getOrDefault(g, new int[]{0, n});
            BigDecimal goodRate = (rt[1] == 0) ? null :
                    BigDecimal.valueOf((double) rt[0] / rt[1]).setScale(4, RoundingMode.HALF_UP);

            RumDailyPo agg = new RumDailyPo();
            agg.setDay(g.day);
            agg.setEnv(g.env);
            agg.setMetric(g.metric);
            agg.setRouteKey(g.routeKey);
            agg.setReleaseVer(g.releaseVer);
            agg.setDimKey(g.dimKey);
            agg.setCnt(n);
            agg.setP50(p50);
            agg.setP75(p75);
            agg.setP95(p95);
            agg.setGoodRate(goodRate);
            agg.setDeleted(0);

            // upsert: 主键/唯一键冲突时更新统计值
            aggMapper.upsert(agg);
            written++;
        }

        log.info("RUM agg done. start={}, end={}, groups={}, rawRows={}, dedupRows={}", start, end, written, rows.size(), dedup.size());
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
     * 去重 Key
     * <p>
     * 与 SQL dedup 分组一致：
     * (day, env, metric, routeKey, releaseVer, dimKey, pageId, sessionId)
     */
    @AllArgsConstructor
    @EqualsAndHashCode
    @Data
    static class DedupKey {
        LocalDate day;
        String env;
        Integer metric;
        String routeKey;
        String releaseVer;
        String dimKey;
        String pageId;
        String sessionId;
    }

    /**
     * 去重后的保留字段
     * value/rating 均取 MAX 结果
     */
    @AllArgsConstructor
    @Data
    static class DedupValue {
        Double value;
        Integer rating;
    }

    /**
     * 聚合分组 Key
     * <p>
     * 与 SQL picked 分组一致：
     * (day, env, metric, routeKey, releaseVer, dimKey)
     */
    @AllArgsConstructor
    @EqualsAndHashCode
    @Data
    static class GroupKey {
        LocalDate day;
        String env;
        Integer metric;
        String routeKey;
        String releaseVer;
        String dimKey;
    }
}
