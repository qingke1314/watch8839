package come.watch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import come.watch.common.CommonResponse;
import come.watch.repository.RumPo;
import come.watch.service.RumService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * RUM (Real User Monitoring) 数据采集接口
 *
 * 功能：接收前端上报的页面性能/体验数据，存入数据库供后续分析
 * 典型场景：页面加载耗时、接口响应时间、用户交互延迟等
 */
@RestController
@RequiredArgsConstructor
public class RumController {

    /**
     * JSON 序列化/反序列化工具
     * 用于将请求体字符串解析为 RumPo 对象
     */
    private final ObjectMapper om;

    /**
     * RUM 数据处理服务
     * 负责数据入库、IP 解析、UA 解析等业务逻辑
     */
    private final RumService service;


    /**
     * 接收 RUM 监控数据上报
     *
     * 支持的 Content-Type：
     * - application/json: 标准 JSON 格式
     * - text/plain: 兼容某些前端库的发送格式
     *
     * @param body 请求体字符串（JSON 数据）
     * @param req  HTTP 请求对象，用于获取 User-Agent 等请求头
     * @return 统一响应包装
     */
    @PostMapping(value = "/rum", consumes = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)  // 204 No Content，接口本身不返回业务数据
    public CommonResponse<String> collect(@RequestBody String body, HttpServletRequest req) throws Exception {

        // 1. 空值校验：防止空请求或纯空白请求
        if (body == null || body.trim().isEmpty()) {
            return CommonResponse.fail("Invalid body");
        }

        // 2. JSON 解析：将字符串转换为 RumPo 对象
        //    支持宽松的 JSON 格式（键可以无引号、单引号等）
        RumPo dto = om.readValue(body, RumPo.class);

        // 3. 业务字段校验：metric、metricId、routeKey 为必填字段
        //    - metric: 指标类型（如 1=FCP, 2=LCP, 3=INP）
        //    - metricId: 指标唯一标识（用于去重）
        //    - routeKey: 页面/路由标识（用于按页面维度聚合）
        if (dto.getMetric() == null || dto.getMetricId() == null || dto.getRouteKey() == null) {
            return CommonResponse.fail("Invalid body");
        }

        // 4. 数据入库：service 层负责写入数据库、解析 IP 和 UA
        service.collect(dto, req.getHeader("User-Agent"));

        // 5. 返回成功响应（HTTP 状态码为 204，Body 仅为状态标识）
        return CommonResponse.ok("OK");
    }

}