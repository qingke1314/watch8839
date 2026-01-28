package come.watch.common;

public enum Metric {
    LCP(1, "最大内容渲染时间LCP"),
    INP(2, "交互到下一次渲染时间INP"),
    CLS(3, "累积布局偏移CLS"),
    API_TIMING(4, "接口请求时间");

    private final int code;
    private final String description;

    Metric(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static Metric fromCode (Integer code) {
        for (Metric metric : Metric.values()) {
            if (metric.getCode() == code) {
                return metric;
            }
        }
        return null;
    }
}
