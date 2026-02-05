package come.watch.repository;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * RUM 日聚合表（用于榜单/对比发布）
 * @TableName rum_daily_agg
 */
@TableName(value ="rum_daily_agg")
@Data
public class RumDailyPo implements Serializable {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 软删标记：0否1是
     */
    private Integer deleted;

    /**
     * 统计日期（按 create_time 落到哪天）
     */
    private LocalDate day;

    /**
     * 环境：prod/staging/dev
     */
    private String env;

    /**
     * 指标类型：1=LCP 2=INP 3=CLS
     */
    private Integer metric;

    /**
     * 归一化路由
     */
    private String routeKey;

    /**
     * 版本标识
     */
    private String releaseVer;

    /**
     * 样本量
     */
    private Integer cnt;

    /**
     * p50
     */
    private Double p50;

    /**
     * p75（Phase1核心）
     */
    private Double p75;

    /**
     * p95
     */
    private Double p95;

    /**
     * good占比(0~1)
     */
    private BigDecimal goodRate;

     /**
     * 维度键 metric为123时为空，为4时为apiKey，为567后面再看
     */
    private String dimKey;

}
