package come.watch.common;

public enum UmiEnv {
    IAT("iat", "投顾端"),
    IMP("imp", "投资端"),
    GD("gd", "国都"),
    FRONTWATCH("frontwatch", "前端监控"),
    ALL("all", "公司测试环境8839");

    private final String code;
    private final String description;

     UmiEnv(String code, String description) {
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
