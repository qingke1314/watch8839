package come.watch.common;

/**
 * @author shenyehui
 * @date 2023/4/18 10:36
 */
public interface RequestConstants {
    /**
     * 请求成功
     */
    Integer CODE_SUCCESS = 1;

    /**
     * 请求失败
     */
    Integer CODE_FAIL = -1;

    /**
     * 未登录或者会话过期
     */
    Integer CODE_NOT_TOKEN_IN = -2;

    /**
     * 用户无相关权限
     */
    Integer CODE_NOT_AUTHORITY_IN = -3;

    /**
     * ContextPath领导驾驶舱路径前缀
     */
    String LCP_CONTEXT_PATH = "/lcp";

    /**
     * ContextPath系统管理路径前缀
     */
    String FICP_CONTEXT_PATH = "/ficp";

    /**
     * ContextPath资产360路径前缀
     */
    String ASSETS_CONTEXT_PATH = "/assets360";

    /**
     * ContextPath投顾端估值路径前缀
     */
    String VALUATION_CONTEXT_PATH = "/valuation";
}

