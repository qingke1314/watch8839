package come.watch.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import come.watch.common.Environment;
import come.watch.common.Metric;
import come.watch.common.UmiEnv;
import come.watch.dto.request.DayAggQueryDTO;
import come.watch.dto.response.DayAggResponseDTO;
import come.watch.dto.response.OverviewDictDTO;
import come.watch.mapper.RumDailyAggMapper;
import come.watch.mapper.RumPoMapper;
import come.watch.repository.RumDailyPo;
import come.watch.repository.RumPo;
import come.watch.service.RumService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class RumServiceImpl extends ServiceImpl<RumPoMapper, RumPo> implements RumService {
    private final RumPoMapper rumPoMapper;
    private final RumDailyAggMapper rumDailyAggMapper;

    @Override
    public void collect(RumPo dto, String userAgent, HttpServletRequest req) {
        rumPoMapper.insert(createMapperRumPo(dto, userAgent, getClientIp(req)));
    }

    /**
     * 从请求中获取用户真实IP
     * 考虑代理情况，依次尝试以下header:
     * - X-Forwarded-For
     * - Proxy-Client-IP
     * - WL-Proxy-Client-IP
     * - HTTP_X_FORWARDED_FOR
     * - HTTP_X_FORWARDED
     * - HTTP_FORWARDED_FOR
     * - HTTP_FORWARDED
     * - HTTP_CLIENT_IP
     * 最后使用 request.getRemoteAddr()
     */
    private byte[] getClientIp(HttpServletRequest req) {
        String ip = null;
        String[] headers = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_CLIENT_IP"
        };

        for (String header : headers) {
            ip = req.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                break;
            }
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = req.getRemoteAddr();
        }

        // X-Forwarded-For 可能包含多个IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ipToBytes(ip);
    }

    /**
     * 将IP字符串转换为byte数组
     */
    private byte[] ipToBytes(String ip) {
        if (ip == null || ip.isEmpty()) {
            return null;
        }
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address.getAddress();
        } catch (UnknownHostException e) {
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void collectBatch(RumPo[] dtos, String userAgent, HttpServletRequest req) {
        byte[] ipBytes = getClientIp(req);
        List<RumPo> entities = new ArrayList<>();
        for (RumPo dto : dtos) {
            if (dto.getMetric() == null || dto.getRouteKey() == null) {
                log.warn("skip invalid dto: metric={}, routeKey={}", dto.getMetric(), dto.getRouteKey());
                continue;
            }
            entities.add(createMapperRumPo(dto, userAgent, ipBytes));
        }
        if (!entities.isEmpty()) {
            this.saveBatch(entities);
        }
    }

    @Override
    public IPage<DayAggResponseDTO> dayAgg(DayAggQueryDTO query, Long pageNo, Long pageSize) {
        // Phase1: 先不加任何 query 过滤条件，直接把 rum_daily_agg 全量分页返回。
        // 后续你再根据 query 补充 env/metric/routeKey/releaseVer/day 等过滤逻辑。
        Page<RumDailyPo> page = new Page<>(pageNo, pageSize);
        IPage<RumDailyPo> poPage = rumDailyAggMapper.selectPageByParam(page, query);
        return poPage.convert(po -> {
            DayAggResponseDTO dto = new DayAggResponseDTO();
            dto.setId(po.getId());
            dto.setCreateTime(po.getCreateTime());
            dto.setUpdateTime(po.getUpdateTime());
            dto.setDeleted(po.getDeleted());
            dto.setDay(po.getDay());
            dto.setEnv(po.getEnv());
            dto.setMetric(po.getMetric());
            dto.setRouteKey(po.getRouteKey());
            dto.setReleaseVer(po.getReleaseVer());
            dto.setCnt(po.getCnt());
            dto.setP50(po.getP50());
            dto.setP75(po.getP75());
            dto.setP95(po.getP95());
            dto.setGoodRate(po.getGoodRate());
            dto.setDimKey(po.getDimKey());
            return dto;
        });
    }

    @Override
    public OverviewDictDTO getDict() {
        OverviewDictDTO dict = new OverviewDictDTO();
        dict.setMetricList(Arrays.stream(Metric.values()).map(metric -> {
            Map<String, Object> item = new HashMap<>();
            item.put("label", metric.getDescription());
            item.put("value", metric.getCode());
            return item;
        }).collect(Collectors.toList()));
        dict.setReleaseVerList(Arrays.stream(UmiEnv.values()).map(ver -> {
            Map<String, Object> item = new HashMap<>();
            item.put("label", ver.getDescription());
            item.put("value", ver.getCode());
            return item;
        }).collect(Collectors.toList()));
        dict.setEnvironmentList(Arrays.stream(Environment.values()).map(env -> {
            Map<String, Object> item = new HashMap<>();
            item.put("label", env.getDescription());
            item.put("value", env.getCode());
            return item;
        }).collect(Collectors.toList()));

        return dict;
    }

    public RumPo createMapperRumPo(RumPo dto, String userAgent, byte[] ipBytes) {
      RumPo entity = new RumPo();
      entity.setClientTs(dto.getClientTs());
      entity.setMetric(dto.getMetric());
      entity.setValue(dto.getValue());
      entity.setDelta(dto.getDelta());
      entity.setMetricId(dto.getMetricId());
      entity.setRating(dto.getRating());
      entity.setNavType(dto.getNavType());
      entity.setRouteKey(dto.getRouteKey());
      entity.setReleaseVer(dto.getReleaseVer());
      entity.setEnv(dto.getEnv());
      entity.setDevice(dto.getDevice());
      entity.setNetwork(dto.getNetwork());
      entity.setUa(userAgent);
      entity.setIp(ipBytes);
      entity.setDeleted(0);
      entity.setSessionId(dto.getSessionId());
      entity.setPageId(dto.getPageId());
      entity.setExt(dto.getExt());
      entity.setDimKey(dto.getDimKey());
      return entity;
    }
}
