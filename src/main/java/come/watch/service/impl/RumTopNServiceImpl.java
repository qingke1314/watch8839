package come.watch.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import come.watch.common.Metric;
import come.watch.common.RumDailyTopNJob;
import come.watch.dto.common.ResourceSummaryExt;
import come.watch.dto.common.TopnResourceItem;
import come.watch.dto.request.TopNQueryDTO;
import come.watch.mapper.RumDailyTopNMapper;
import come.watch.mapper.RumPoMapper;
import come.watch.repository.RumDailyTopNPo;
import come.watch.repository.RumPo;
import come.watch.service.RumTopNService;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class RumTopNServiceImpl implements RumTopNService {
    private static final int TOP_N = 5;
    private static final int RES_DUR_SUM  = Metric.RES_DUR_SUM.getCode();
    // private static int RES_SIZE_SUM = Metric.RES_SIZE_SUM.getCode();
    private final RumDailyTopNMapper rumDailyTopNMapper;
    private final RumPoMapper rumPoMapper;
    private final ObjectMapper objectMapper;

    @Data
    public static class GroupKey {
        LocalDate day;
        String env;
        String routeKey;
        String releaseVer;

        GroupKey(LocalDate day, String env, String routeKey, String releaseVer) {
            this.day = day;
            this.env = env;
            this.routeKey = routeKey;
            this.releaseVer = releaseVer;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof GroupKey &&
                    ((GroupKey) obj).day.equals(day) &&
                    ((GroupKey) obj).env.equals(env) &&
                    ((GroupKey) obj).routeKey.equals(routeKey) &&
                    ((GroupKey) obj).releaseVer.equals(releaseVer);
        }

        @Override
        public int hashCode() {
            return day.hashCode() ^ env.hashCode() ^ routeKey.hashCode() ^ releaseVer.hashCode();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rebuildTopn(LocalDate day) {
        log.info("Rebuild topn. day={}", day);
        rumDailyTopNMapper.deleteByDay(day);
        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = day.plusDays(1).atStartOfDay();

        int current = 1;
        int pageSize = 2000; // 分页获取，避免数据太大拉爆内存

        Map<GroupKey, GroupAgg> groups = new HashMap<>();
        while(true) {
            IPage<RumPo> rows = rumPoMapper.selectResourceSummaryEvents(
                    new Page<>(current, pageSize),
                    start,
            end,RES_DUR_SUM);

            if(rows.getRecords().isEmpty()) {
                break;
            }
            current++;
            /*
             * 对明细进行遍历，以day+env+routeKey+releaseVer去重，然后累加top5
             * 根据明细的top长度进行occCnt的累加，并同时计算最大值和总值（平均值可以在最后计算）
             */
            for(RumPo po:rows.getRecords()) {
                GroupKey key = new GroupKey(day, po.getEnv(), po.getRouteKey(), po.getReleaseVer());
                GroupAgg agg = groups.computeIfAbsent(key, k -> new GroupAgg());
                // sample_cnt：按 session 去重计数（page_id + session_id）
                String sessionKey = (po.getPageId() == null ? "" : po.getPageId())
                        + "|" + (po.getSessionId() == null ? "" : po.getSessionId());
                if (agg.sessions.add(sessionKey)) {
                    agg.sampleCnt++;
                }
                ResourceSummaryExt ext = parseExt(po.getExt());
                if (ext == null) continue;

                // dur 榜
                mergeList(agg, "dur", ext.getTd());
                // size 榜
                mergeList(agg, "size", ext.getTs());
            }
        }
        List<RumDailyTopNPo> inserts = new ArrayList<>(groups.size() * 2 * TOP_N);
        for(
                Map.Entry<GroupKey, GroupAgg> entry: groups.entrySet()
        ) {
            GroupKey k = entry.getKey();
            GroupAgg g = entry.getValue();
            buildTop(inserts, k, g, "dur"); // 对聚合起来的数据进行排序取top
            buildTop(inserts, k, g, "size");
        }
        // 4) 批量写入
        if (!inserts.isEmpty()) {
            rumDailyTopNMapper.batchInsert(inserts);
        }

        log.info("Topn rebuilt. day={}, groups={}, rows={}", day, groups.size(), inserts.size());
    }

    private void buildTop(List<RumDailyTopNPo> out, GroupKey k, GroupAgg g, String kind) {
        List<ItemAgg> list = g.items.entrySet().stream()
                .filter(en -> en.getKey().startsWith(kind + "|"))
                .map(Map.Entry::getValue)
                .sorted(Comparator.comparingInt(ItemAgg::getMetricMax).reversed())
                .limit(TOP_N)
                .collect(Collectors.toList());

        int rank = 1;
        for (ItemAgg ia : list) {
            RumDailyTopNPo row = new RumDailyTopNPo();
            row.setDay(k.getDay());
            row.setEnv(k.getEnv());
            row.setRouteKey(k.getRouteKey());
            row.setReleaseVer(k.getReleaseVer());

            row.setKind(kind); // 'dur' / 'size' -> 对应 ENUM
            row.setRankNo(rank++);

            row.setItemKey(ia.base.getK());
            row.setInitiator(ia.base.getT());
            row.setHost(ia.base.getHost());

            row.setMetricMax(ia.metricMax);
            row.setMetricAvg(ia.occCnt == 0 ? null : (int) Math.round((double) ia.metricSum / ia.occCnt));
            row.setOccCnt(ia.occCnt);
            row.setSampleCnt(g.sampleCnt);
            out.add(row);
        }
    }

    private void mergeList(GroupAgg g, String kind, List<TopnResourceItem> items) {
        if (items == null || items.isEmpty()) return;

        // 让 occ_cnt 按“会话”计数：同一次事件内同 itemKey 只计一次
        Set<String> seen = new HashSet<>();

        for (TopnResourceItem it : items) {
            if (it == null || it.getK() == null) continue;
            String itemKey = it.getK();

            String seenKey = kind + "|" + itemKey;
            if (!seen.add(seenKey)) continue;

            ItemAgg agg = g.items.computeIfAbsent(seenKey, x -> new ItemAgg(it));
            agg.occCnt += 1;
            agg.metricMax = Math.max(agg.metricMax, kind.equals("dur") ? it.getD() : it.getS());
            agg.metricSum += kind.equals("dur") ? it.getD() : it.getS();
        }
    }

    @Getter
    private static class GroupAgg {
        int sampleCnt = 0;
        Set<String> sessions = new HashSet<>();
        Map<String, ItemAgg> items = new HashMap<>();
    }

    @Getter
    private static class ItemAgg {
        TopnResourceItem base;
        int metricMax = 0;
        long metricSum = 0;
        int occCnt = 0;

        ItemAgg(TopnResourceItem base) { this.base = base; }
    }

    ResourceSummaryExt parseExt(Object ext) {
        if (ext == null) return null;
        try {
            Map<String, Object> extMap;
            if (ext instanceof String) {
                extMap = objectMapper.readValue((String) ext, Map.class);
            } else {
                extMap = objectMapper.convertValue(ext, Map.class);
            }
            List<TopnResourceItem> top5Dur = parseItemList(extMap.get("top5Dur"));
            List<TopnResourceItem> top5Size = parseItemList(extMap.get("top5Size"));
            ResourceSummaryExt result = new ResourceSummaryExt();
            result.setTd(top5Dur);
            result.setTs(top5Size);
            return result;
        } catch (Exception e) {
            log.warn("parseExt failed, ext={}", ext, e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<TopnResourceItem> parseItemList(Object rawList) {
        if (rawList == null) return Collections.emptyList();
        if (!(rawList instanceof List)) return Collections.emptyList();

        List<Map<String, Object>> list = (List<Map<String, Object>>) rawList;
        List<TopnResourceItem> result = new ArrayList<>();

        for (Map<String, Object> itemMap : list) {
            TopnResourceItem item = new TopnResourceItem();

            Object d = itemMap.get("d");
            if (d instanceof Number) item.setD(((Number) d).intValue());

            Object s = itemMap.get("s");
            if (s instanceof Number) item.setS(((Number) s).intValue());

            Object k = itemMap.get("k");
            Object host = itemMap.get("h");
            if (k instanceof String) {
                String url = (String) k;
                item.setK(url);
                item.setHost(host != null ? host.toString() : null);
            }

            Object t = itemMap.get("t");
            if (t instanceof String) item.setT((String) t);

            result.add(item);
        }

        return result;
    }

    private String extractHost(String url) {
        if (url == null || url.isEmpty()) return null;
        try {
            if (url.startsWith("http://")) {
                int start = 7;
                int end = url.indexOf('/', start);
                if (end == -1) end = url.indexOf('?', start);
                if (end == -1) end = url.length();
                return url.substring(start, end);
            } else if (url.startsWith("https://")) {
                int start = 8;
                int end = url.indexOf('/', start);
                if (end == -1) end = url.indexOf('?', start);
                if (end == -1) end = url.length();
                return url.substring(start, end);
            }
        } catch (Exception e) {
            log.warn("extractHost failed, url={}", url);
        }
        return null;
    }

    private String normalizeUrl(String url) {
        if (url == null || url.isEmpty()) return url;

        String normalized = url;

        int queryIndex = normalized.indexOf('?');
        if (queryIndex != -1) {
            normalized = normalized.substring(0, queryIndex);
        }

        int hashIndex = normalized.indexOf('#');
        if (hashIndex != -1) {
            normalized = normalized.substring(0, hashIndex);
        }

        if (normalized.startsWith("http://")) {
            normalized = normalized.substring(7);
        } else if (normalized.startsWith("https://")) {
            normalized = normalized.substring(8);
        }

        int slashIndex = normalized.indexOf('/');
        if (slashIndex != -1) {
            normalized = normalized.substring(slashIndex);
        }

        return normalized;
    }

    public IPage<RumDailyTopNPo> getTopN(TopNQueryDTO query, Long current, Long size) {
        Page<RumDailyTopNPo> page = new Page<>(current, size);
        return rumDailyTopNMapper.selectByParams(page, query);
    }
}
