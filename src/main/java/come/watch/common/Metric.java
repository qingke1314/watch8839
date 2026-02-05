package come.watch.common;

import lombok.Getter;

@Getter
public enum Metric {
    LCP(1, "最大内容渲染时间LCP"),
    INP(2, "交互到下一次渲染时间INP"),
    CLS(3, "累积布局偏移CLS"),
    API_TIMING(4, "接口请求时间"),
    RES_DUR_SUM(5, "资源加载时间总和"),
    RES_SIZE_SUM(6, "资源加载大小总和"),
    RES_CNT(7, "资源加载数量"),
    FCP(8, "首次内容绘制FCP");


    private final int code;
    private final String description;

    Metric(int code, String description) {
        this.code = code;
        this.description = description;
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
