package come.watch.repository;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("rum_event")
@Data
public class RumPo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

    private Long clientTs;

    private Integer metric;     // 1/2/3
    private Double value;
    private Double delta;
    private String metricId;
    private Integer rating;
    private Integer navType;

    private String routeKey;
    private String releaseVer;
    private String env;

    private Integer device;
    private String network;

    private String ua;
    private byte[] ip;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object ext; // 支持对象或字符串

    private String sessionId;
    private String pageId;
}
