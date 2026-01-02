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

@Slf4j
@Component
@RequiredArgsConstructor
public class RumDailyAggJob {

    private final RumPoMapper eventMapper;
    private final RumDailyAggMapper aggMapper;


    // 每天 00:10 跑昨天的数据（你也可以改时间）
    @Scheduled(cron = "0 10 0 * * ?")
    public void run() {
        LocalDate day = LocalDate.now().minusDays(1);


        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = day.plusDays(1).atStartOfDay();

        // 1) 拉取明细（Phase1 简化：一次性拉；量大再做分页）
        List<RumPo> rows = eventMapper.selectList(new LambdaQueryWrapper<RumPo>()
                .ge(RumPo::getCreateTime, start)
                .lt(RumPo::getCreateTime, end)
                .eq(RumPo::getDeleted, 0)
                .isNotNull(RumPo::getMetric)
                .isNotNull(RumPo::getMetricId)
                .isNotNull(RumPo::getRouteKey)
                .isNotNull(RumPo::getReleaseVer)
                .isNotNull(RumPo::getEnv)
        );

        if (rows.isEmpty()) {
            log.info("RUM agg: no data for {}", day);
            return;
        }

        // 2) 先按 (metric, metricId) 去重取 max(value)
        Map<String, RumPo> dedup = new HashMap<>(rows.size() * 2);
        for (RumPo e : rows) {
            String k = e.getMetric() + "|" + e.getMetricId();
            RumPo old = dedup.get(k);
            if (old == null || (e.getValue() != null && old.getValue() != null && e.getValue() > old.getValue())) {
                dedup.put(k, e);
            }
        }

        // 3) 按 (env, metric, routeKey, releaseVer) 分组，收集 values + rating
        Map<GroupKey, List<Double>> valuesMap = new HashMap<>();
        Map<GroupKey, int[]> ratingCntMap = new HashMap<>(); // [good,total]

        for (RumPo e : dedup.values()) {
            if (e.getValue() == null) continue;

            GroupKey g = new GroupKey(e.getEnv(), e.getMetric(), e.getRouteKey(), e.getReleaseVer());

            valuesMap.computeIfAbsent(g, x -> new ArrayList<>()).add(e.getValue());

            int[] rt = ratingCntMap.computeIfAbsent(g, x -> new int[2]);
            rt[1]++; // total
            if (e.getRating() != null && e.getRating() == 0) rt[0]++; // good
        }

        // 4) 计算 p50/p75/p95，写入聚合表（upsert）
        int written = 0;
        for (Map.Entry<GroupKey, List<Double>> entry : valuesMap.entrySet()) {
            GroupKey g = entry.getKey();
            List<Double> vals = entry.getValue();
            vals.sort(Double::compareTo);

            int n = vals.size();
            double p50 = percentile(vals, 0.50);
            double p75 = percentile(vals, 0.75);
            double p95 = percentile(vals, 0.95);

            int[] rt = ratingCntMap.getOrDefault(g, new int[]{0, n});
            BigDecimal goodRate = (rt[1] == 0) ? null :
                    BigDecimal.valueOf((double) rt[0] / rt[1]).setScale(4, RoundingMode.HALF_UP);

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

            aggMapper.upsert(agg);
            written++;
        }

        log.info("RUM agg done. day={}, groups={}, rawRows={}, dedupRows={}", day, written, rows.size(), dedup.size());
    }

    // p 取值 0~1，vals 必须已排序
    private double percentile(List<Double> vals, double p) {
        int n = vals.size();
        if (n == 0) return Double.NaN;
        int idx = (int) Math.ceil(p * n) - 1;
        if (idx < 0) idx = 0;
        if (idx >= n) idx = n - 1;
        return vals.get(idx);
    }

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
