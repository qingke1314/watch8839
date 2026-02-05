package come.watch.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import come.watch.dto.request.DayAggQueryDTO;
import come.watch.dto.response.DayAggResponseDTO;
import come.watch.dto.response.OverviewDictDTO;
import come.watch.repository.RumDailyTopNPo;
import come.watch.repository.RumPo;

import javax.servlet.http.HttpServletRequest;

public interface RumService {
    void collect(RumPo body, String userAgent, HttpServletRequest req);

    void collectBatch(RumPo[] dtos, String userAgent, HttpServletRequest req);

    IPage<DayAggResponseDTO> dayAgg(DayAggQueryDTO query, Long pageNo, Long pageSize);

     /**
      * 根据某条日聚合数据检索匹配的明细数据
      */
     IPage<RumPo> getDetail(DayAggResponseDTO query);

     /**
      * 根据某条topN数据查询明细数据
      */
     IPage<RumPo> getDetailByTopN(RumDailyTopNPo query);

     OverviewDictDTO getDict();
}
