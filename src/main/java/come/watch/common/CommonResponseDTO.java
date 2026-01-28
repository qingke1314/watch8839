package come.watch.common;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * @author li.dongquan
 * @date 2020/12/7
 *
 * 支持列表和对对象
 */
@Data
@Accessors(chain = true)
public class CommonResponseDTO<T> implements Serializable {

    /**
     * Field  状态码
     * <p>
     * >0 ,成功
     * <0 ,失败
     */
    private Integer code;

    /**
     * Field  响应说明
     */
    private String note;

    /**
     * Field  总页数
     */
    private Long totalPage = -1L;

    /**
     * Field  总记录数
     */
    private Long totalRecord = -1L;

    /**
     * Field  结果数组
     */
    private List<T> records;
    private T data;

    private Long duration;

    public static <T> CommonResponseDTO<T> ok() {
        CommonResponseDTO<T> response = new CommonResponseDTO<>();
        response.code = RequestConstants.CODE_SUCCESS;

        return response;
    }

    public static <T> CommonResponseDTO<T> ok(T data) {
        CommonResponseDTO<T> response = new CommonResponseDTO<>();
        response.code = RequestConstants.CODE_SUCCESS;
        response.data = data;

        return response;
    }

    public static <T> CommonResponseDTO<T> badRequest(String note) {
        CommonResponseDTO<T> response = new CommonResponseDTO<>();
        response.code = RequestConstants.CODE_FAIL;
        response.note = note;

        return response;
    }

    public static <T> CommonResponseDTO<T> records(List<T> records) {
        CommonResponseDTO<T> response = new CommonResponseDTO<>();
        response.code = RequestConstants.CODE_SUCCESS;
        response.records = records;

        return response;
    }

    public static <T> CommonResponseDTO<T> page(List<T> records, long totalPage, long totalRecord) {
        CommonResponseDTO<T> response = new CommonResponseDTO<>();
        response.code = RequestConstants.CODE_SUCCESS;
        response.records = records;
        response.totalPage = totalPage;
        response.totalRecord = totalRecord;

        return response;
    }
}
