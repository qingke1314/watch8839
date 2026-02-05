package come.watch.common;

import come.watch.repository.RumPo;

import java.util.Arrays;

public class RumValidator {

    private static final int MAX_ROUTE_KEY_LEN = 200;
    private static final int MAX_RELEASE_VER_LEN = 50;
    private static final int MAX_ENV_LEN = 20;

    /**
     * 校验 RumPo 是否合法
     * @return 错误消息，null 表示校验通过
     */
    public static String validate(RumPo dto) {
        if (dto == null) {
            return "Invalid body";
        }
        // metric 校验：只能是 1(LCP)/2(INP)/3(CL)
        boolean validMetric = Arrays.stream(Metric.values()).map(Metric::getCode).anyMatch(code -> code.equals(dto.getMetric()));
        if (dto.getMetric() == null || !validMetric) {
            return "Invalid metric";
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
