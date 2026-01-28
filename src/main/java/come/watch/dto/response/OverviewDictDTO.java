package come.watch.dto.response;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class OverviewDictDTO {
    private List<Map<String, Object>> metricList;

    private List<Map<String, Object>> releaseVerList;

    private List<Map<String, Object>> environmentList;
}
