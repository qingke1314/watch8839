package come.watch.common;

public enum Environment {
    DEVELOPMENT("development", "开发环境"),
    PRODUCTION("production", "生产环境");

    private final String code;
    private final String description;

    Environment(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
