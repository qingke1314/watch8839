package come.watch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import come.watch.dto.response.DayAggResponseDTO;
import come.watch.repository.RumDailyTopNPo;
import come.watch.repository.RumPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface RumPoMapper extends BaseMapper<RumPo> {
        /**
         * 根据某条日聚合数据检索匹配的明细数据
         */
        IPage<RumPo> selectPageByParam(Page<RumPo> page, @Param("query") DayAggResponseDTO query);

        /**
         * 根据某条topN数据查询明细数据
         */
        IPage<RumPo> selectPageByTopNParam(Page<RumPo> page, @Param("query") RumDailyTopNPo query);

        /**
         * 按资源类型查询资源摘要事件
         */
        IPage<RumPo> selectResourceSummaryEvents(Page<RumPo> page, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("metric") Integer metric);
}
