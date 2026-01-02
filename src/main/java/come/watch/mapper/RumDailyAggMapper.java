
package come.watch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import come.watch.repository.RumDailyPo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RumDailyAggMapper extends BaseMapper<RumDailyPo> {
    @Insert("INSERT INTO rum_daily_agg\n" +
            "  (create_time, update_time, deleted,\n" +
            "   day, env, metric, route_key, release_ver,\n" +
            "   cnt, p50, p75, p95, good_rate)\n" +
            "VALUES\n" +
            "  (NOW(3), NOW(3), 0,\n" +
            "   #{day}, #{env}, #{metric}, #{routeKey}, #{releaseVer},\n" +
            "   #{cnt}, #{p50}, #{p75}, #{p95}, #{goodRate})\n" +
            "ON DUPLICATE KEY UPDATE\n" +
            "  update_time = NOW(3),\n" +
            "  deleted = 0,\n" +
            "  cnt = VALUES(cnt),\n" +
            "  p50 = VALUES(p50),\n" +
            "  p75 = VALUES(p75),\n" +
            "  p95 = VALUES(p95),\n" +
            "  good_rate = VALUES(good_rate)")
    int upsert(RumDailyPo agg);
}