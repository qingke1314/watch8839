package come.watch.common;

import come.watch.repository.RumPo;

public class RumValidator {

    private static final int MAX_ROUTE_KEY_LEN = 200;
    private static final int MAX_RELEASE_VER_LEN = 50;
    private static final int MAX_ENV_LEN = 20;

    /**
     * 校验 RumPo 是否合法
     * @return 错误消息，null 表示校验通过
     */
    public static String validate(RumPo dto) {
        // metric 校验：只能是 1(LCP)/2(INP)/3(CL)
        if (dto.getMetric() == null || !MetricType.fromValue(dto.getMetric()).equals(MetricType.LCP) && !MetricType.fromValue(dto.getMetric()).equals(MetricType.INP) && !MetricType.fromValue(dto.getMetric()).equals(MetricType.CL)) {
            return "Invalid metric";
        }

        // value 校验：LCP/INP 0~120000ms，CL 0~10
        if (dto.getValue() != null) {
            MetricType type = MetricType.fromValue(dto.getMetric());
            if (type == MetricType.CL) {
                // CLS 范围 0~10
                if (dto.getValue() < 0 || dto.getValue() > 10) {
                    return "Invalid cls value";
                }
            } else {
                // LCP/INP 范围 0~120000ms
                if (dto.getValue() < 0 || dto.getValue() > 120000) {
                    return "Invalid performance value";
                }
            }
        }

        // route_key 校验：非空且长度合理
        if (dto.getRouteKey() == null || dto.getRouteKey().isEmpty()) {
            return "Invalid routeKey";
        }
        if (dto.getRouteKey().length() > MAX_ROUTE_KEY_LEN) {
            return "routeKey too long";
        }

        // release_ver 校验：长度
        if (dto.getReleaseVer() != null && dto.getReleaseVer().length() > MAX_RELEASE_VER_LEN) {
            return "releaseVer too long";
        }

        // env 校验：长度
        if (dto.getEnv() != null && dto.getEnv().length() > MAX_ENV_LEN) {
            return "env too long";
        }

        return null;
    }
}
