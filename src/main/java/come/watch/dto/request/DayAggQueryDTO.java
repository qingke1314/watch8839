package come.watch.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import come.watch.common.Environment;
import come.watch.common.Metric;
import come.watch.common.UmiEnv;
import come.watch.common.validation.EnumValue;
import lombok.Data;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
public class DayAggQueryDTO {

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Beijing")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Beijing")
    private LocalDate endDate;

    @EnumValue(enumClass = Environment.class, method = "getCode", message = "env非法")
    private String env;

    @EnumValue(enumClass = Metric.class, method = "getCode", message = "metric非法")
    private Integer metric;

    @EnumValue(enumClass = UmiEnv.class, method = "getCode", message = "releaseVer非法")
    private String releaseVer;

    private String routeKey;

    @AssertTrue(message = "startDate必须小于等于endDate")
    public boolean isDateRangeValid() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !startDate.isAfter(endDate);
    }

}
