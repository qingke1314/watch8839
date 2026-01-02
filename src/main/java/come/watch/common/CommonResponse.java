package come.watch.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommonResponse<T> {

    /** 1 表示成功，其它表示失败（你也可以换成 20000/40000 这类业务码） */
    private int code;

    /** 给前端展示/提示的消息 */
    private String message;

    /** 真实业务数据：对象/列表/分页列表都放这里 */
    private T data;

    /** 可选：分页、traceId、耗时等 */
    private Meta meta;

    public static <T> CommonResponse<T> ok(T data) {
        return CommonResponse.<T>builder()
                .code(1)
                .message("ok")
                .data(data)
                .build();
    }

    public static <T> CommonResponse<T> ok(T data, Meta meta) {
        return CommonResponse.<T>builder()
                .code(1)
                .message("ok")
                .data(data)
                .meta(meta)
                .build();
    }

    public static <T> CommonResponse<T> fail(int code, String message) {
        return CommonResponse.<T>builder()
                .code(code)
                .message(message)
                .build();
    }

    public static <T> CommonResponse<T> fail(String message) {
        return CommonResponse.<T>builder()
                .code(0)
                .message(message)
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Meta {
        /** 分页：总条数 */
        private Long total;
        /** 分页：总页数 */
        private Long pages;
        /** 分页：当前页 */
        private Long pageNo;
        /** 分页：每页大小 */
        private Long pageSize;

        /** 链路追踪（可从日志 MDC 或网关传入） */
        private String traceId;

        /** 本次请求耗时（毫秒） */
        private Long durationMs;
    }
}

