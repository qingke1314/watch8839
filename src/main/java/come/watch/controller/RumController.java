package come.watch.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import come.watch.common.CommonResponseDTO;
import come.watch.common.Metric;
import come.watch.common.RumDailyTopNJob;
import come.watch.common.RumValidator;
import come.watch.dto.request.DayAggQueryDTO;
import come.watch.dto.request.TopNQueryDTO;
import come.watch.dto.response.DayAggResponseDTO;
import come.watch.dto.response.OverviewDictDTO;
import come.watch.repository.RumDailyTopNPo;
import come.watch.repository.RumPo;
import come.watch.service.RumService;
import come.watch.service.RumTopNService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/rum")
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

  private final RumTopNService topService;


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
    List<RumPo> validDtos = new ArrayList<>();
    for (RumPo dto : dtos) {
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

    /**
     * 获取日聚合 数据
     */
  @PostMapping("/dayAgg")
  public CommonResponseDTO<DayAggResponseDTO> dayAgg(@Valid @RequestBody DayAggQueryDTO query,
                                                     @RequestParam Long current, @RequestParam Long pageSize) {
      IPage<DayAggResponseDTO> page = service.dayAgg(query, current, pageSize);
      return CommonResponseDTO.page(page.getRecords(), page.getPages(), page.getTotal());
  }

  @PostMapping("/getDict")
  public CommonResponseDTO<OverviewDictDTO> metricList() {
      OverviewDictDTO dict = service.getDict();
      return CommonResponseDTO.ok(dict);
  }

    /**
     * 根据某条日聚合数据检索匹配的明细数据
     */
    @PostMapping("/getDetail")
    public CommonResponseDTO<RumPo> getDetail(@RequestBody DayAggResponseDTO query) {
        IPage<RumPo> page = service.getDetail(query);
        return CommonResponseDTO.page(page.getRecords(), page.getPages(), page.getTotal());
    }

    /**
     * 根据某条topN数据查询明细数据
     */
     @PostMapping("/getDetailByTopN")
    public CommonResponseDTO<RumPo> getDetailByTopN(@RequestBody RumDailyTopNPo query) {
        IPage<RumPo> page = service.getDetailByTopN(query);
        return CommonResponseDTO.page(page.getRecords(), page.getPages(), page.getTotal());
    }

    /**
     * 获取topN数据
     */
    @PostMapping("/getTopN")
    public CommonResponseDTO<RumDailyTopNPo> getTopN(@Valid @RequestBody TopNQueryDTO query, @RequestParam Long current, @RequestParam Long pageSize) {
        IPage<RumDailyTopNPo> page = topService.getTopN(query, current, pageSize);
        return CommonResponseDTO.page(page.getRecords(), page.getPages(), page.getTotal());
    }
}
