package come.watch.common;

import lombok.Getter;

@Getter
public enum MetricType {
    LCP(1),
    INP(2),
    CL(3);

    private final int value;

    MetricType(int value) {
        this.value = value;
    }

    public static MetricType fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (MetricType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        return null;
    }
}
