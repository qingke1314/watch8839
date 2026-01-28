package come.watch.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import come.watch.dto.request.DayAggQueryDTO;
import come.watch.dto.response.DayAggResponseDTO;
import come.watch.dto.response.OverviewDictDTO;
import come.watch.repository.RumPo;

import javax.servlet.http.HttpServletRequest;

public interface RumService {
    void collect(RumPo body, String userAgent, HttpServletRequest req);

    void collectBatch(RumPo[] dtos, String userAgent, HttpServletRequest req);

    IPage<DayAggResponseDTO> dayAgg(DayAggQueryDTO query, Long pageNo, Long pageSize);

     OverviewDictDTO getDict();
}
