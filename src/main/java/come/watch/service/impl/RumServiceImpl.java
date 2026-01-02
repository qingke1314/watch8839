package come.watch.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import come.watch.mapper.RumPoMapper;
import come.watch.repository.RumPo;
import come.watch.service.RumService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class RumServiceImpl extends ServiceImpl<RumPoMapper, RumPo> implements RumService {
    private final RumPoMapper rumPoMapper;

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
        java.util.List<RumPo> entities = new java.util.ArrayList<>();
        for (RumPo dto : dtos) {
            if (dto.getMetric() == null || dto.getMetricId() == null || dto.getRouteKey() == null) {
                log.warn("skip invalid dto: metricId={}", dto.getMetricId());
                continue;
            }
            entities.add(createMapperRumPo(dto, userAgent, ipBytes));
        }
        if (!entities.isEmpty()) {
            this.saveBatch(entities);
        }
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
      return entity;
    }
}
