package come.watch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import come.watch.common.RumValidator;
import come.watch.repository.RumPo;
import come.watch.service.RumService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Slf4j
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
   * <p>
   * 支持的 Content-Type：
   * - application/json: 标准 JSON 格式
   * - text/plain: 兼容某些前端库的发送格式
   *
   * @param body 请求体字符串（JSON 数据）
   * @param req  HTTP 请求对象，用于获取 User-Agent 等请求头
   */
  @PostMapping(value = "/rum", consumes = MediaType.TEXT_PLAIN_VALUE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void collect(@RequestBody String body, HttpServletRequest req) throws Exception {
    // 1. 空值校验
    if (body == null || body.trim().isEmpty()) {
      log.warn("rum: empty body, ip={}", req.getRemoteAddr());
      return;
    }

    // 2. JSON 解析
    RumPo dto = null;
    try {
      dto = om.readValue(body, RumPo.class);
    } catch (Exception e) {
      log.warn("rum: json parse error, ip={}, error={}", req.getRemoteAddr(), e.getMessage());
      return;
    }

    // 3. 业务字段校验
    if (dto.getMetricId() == null || dto.getMetricId().isEmpty()) {
      log.warn("rum: missing metricId, ip={}", req.getRemoteAddr());
      return;
    }

    // 白名单校验
    String error = RumValidator.validate(dto);
    if (error != null) {
      log.warn("rum: validate failed, ip={}, error={}", req.getRemoteAddr(), error);
      return;
    }

    // 4. 数据入库
    service.collect(dto, req.getHeader("User-Agent"), req);
    log.info("rum: success, ip={}, metricId={}", req.getRemoteAddr(), dto.getMetricId());
  }

  /**
   * 批量接收 RUM 监控数据上报
   *
   * @param body 请求体字符串（JSON 数组格式，如：[{},{},...]）
   * @param req  HTTP 请求对象，用于获取 User-Agent 和 IP
   */
  @PostMapping(value = "/rumBatch", consumes = MediaType.TEXT_PLAIN_VALUE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void collectBatch(@RequestBody String body, HttpServletRequest req) throws Exception {
    if (body == null || body.trim().isEmpty()) {
      log.warn("rumBatch: empty body, ip={}", req.getRemoteAddr());
      return;
    }

    // 解析为对象数组
    RumPo[] dtos = null;
    try {
      dtos = om.readValue(body, RumPo[].class);
    } catch (Exception e) {
      log.warn("rumBatch: json parse error, ip={}, error={}", req.getRemoteAddr(), e.getMessage());
      return;
    }

    if (dtos == null || dtos.length == 0) {
      log.warn("rumBatch: empty array, ip={}", req.getRemoteAddr());
      return;
    }

    // 批量校验：过滤无效数据
    java.util.List<RumPo> validDtos = new java.util.ArrayList<>();
    for (RumPo dto : dtos) {
      if (dto.getMetricId() == null || dto.getMetricId().isEmpty()) {
        continue;
      }
      String error = RumValidator.validate(dto);
      if (error == null) {
        validDtos.add(dto);
      }
    }

    if (validDtos.isEmpty()) {
      log.warn("rumBatch: no valid data, ip={}, total={}", req.getRemoteAddr(), dtos.length);
      return;
    }

    service.collectBatch(validDtos.toArray(new RumPo[0]), req.getHeader("User-Agent"), req);
    log.info("rumBatch: success, ip={}, total={}, valid={}", req.getRemoteAddr(), dtos.length, validDtos.size());
  }
}