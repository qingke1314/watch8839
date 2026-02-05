package come.watch.repository;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * RUM 日采样表
 * @TableName rum_daily_sample
 */
@TableName(value = "rum_daily_sample")
@Data
public class RumDailySamplePo implements Serializable {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 统计日期
     */
    private LocalDate day;

    /**
     * 环境：prod/staging/dev
     */
    private String env;

    /**
     * 归一化路由
     */
    private String routeKey;

    /**
     * 版本标识
     */
    private String releaseVer;

    /**
     * 指标类型：1=LCP 2=INP 3=CLS
     */
    private Integer metric;

    /**
     * 维度键
     */
    private String dimKey;

    /**
     * 评分：1=good 2=needs-improvement 3=poor
     */
    private Integer rating;

    /**
     * 采样类型：p95/max/bad/...
     */
    private String sampleType;

    /**
     * 页面ID
     */
    private String pageId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 指标值
     */
    private Double value;

    /**
     * 扩展信息（JSON）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object ext;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 记录创建时间
     */
    private LocalDateTime createdAt;

}
