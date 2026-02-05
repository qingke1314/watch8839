package come.watch.repository;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("rum_daily_topn")
public class RumDailyTopNPo {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 上报时间
    private LocalDate day;

    // 路由key
    private String routeKey;

    // 现场环境key iat/imp/...
    private String releaseVer;

    // 环境key  development/production
    private String env;

    // 指标类型  dur时间/size大小
    private String kind;

    // 排名 1-5
    private int rankNo;

    // 归一化key
    private String itemKey;

    // 资源维度 css javascript img等
    private String initiator;

    private String host;

    // 最大值
    private int metricMax;

    // 平均值
    private int metricAvg;

    // 统计总数
    private int occCnt;
    private int sampleCnt;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object ext;
}
