package come.watch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import come.watch.dto.request.TopNQueryDTO;
import come.watch.repository.RumDailyTopNPo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface RumDailyTopNMapper extends BaseMapper<RumDailyTopNPo> {
    /**
     * 根据日期删除数据
     * @param day 日期
     */
    @Delete("delete from rum_daily_topn where day = #{day}")
    void deleteByDay(LocalDate day);

     /**
     * 批量插入数据
     * @param list 数据列表
     */
    void batchInsert(List<RumDailyTopNPo> list);

    IPage<RumDailyTopNPo> selectByParams(Page<RumDailyTopNPo> page, @Param("query") TopNQueryDTO params);
}
