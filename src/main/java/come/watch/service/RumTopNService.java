package come.watch.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import come.watch.dto.request.TopNQueryDTO;
import come.watch.repository.RumDailyTopNPo;

import java.time.LocalDate;

public interface RumTopNService {
    void rebuildTopn(LocalDate day);

    IPage<RumDailyTopNPo> getTopN(TopNQueryDTO query, Long pageNo, Long pageSize);
}
