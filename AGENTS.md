# [](https://)AGENTS.md - RUM 监控系统项目说明

## 项目概述

**项目名称**: Watch RUM 监控系统
**项目类型**: Spring Boot Web 应用
**主要功能**: 实时用户监控（Real User Monitoring）数据采集、聚合与查询
这是一个用于收集和分析前端性能指标的后端服务系统，支持 Web Vitals 核心指标监控（LCP、INP、CLS），提供数据采集接口、定时聚合任务和查询 API。

## 技术栈

### 核心框架

- **Spring Boot**: 2.7.18
- **Java**: 1.8
- **MyBatis Plus**: 3.5.3.2
- **MySQL**: 8.0.31

### 主要依赖

- `spring-boot-starter-web` - Web 应用基础
- `spring-boot-starter-validation` - 参数校验
- `mybatis-plus-boot-starter` - ORM 框架
- `mysql-connector-j` - MySQL 驱动
- `lombok` - 简化 Java 代码
- `@EnableScheduling` - 定时任务支持

## 项目结构

```
come.watch/
├── Main.java                        # Spring Boot 启动类
├── common/                          # 通用组件
│   ├── BusinessException.java       # 业务异常类
│   ├── CommonResponse.java          # 统一响应格式
│   ├── CommonResponseDTO.java       # 响应 DTO
│   ├── Environment.java             # 环境枚举
│   ├── GlobalExceptionHandler.java  # 全局异常处理器
│   ├── Metric.java                  # 指标枚举（LCP/INP/CLS）
│   ├── MetricType.java              # 指标类型定义
│   ├── RequestConstants.java        # 请求常量
│   ├── RumDailyAggJob.java          # 日聚合定时任务
│   ├── RumValidator.java            # RUM 数据校验器
│   ├── UmiEnv.java                  # Umi 环境枚举
│   └── validation/                  # 自定义校验器
│       ├── EnumValue.java           # 枚举值注解
│       └── EnumValueValidator.java  # 枚举值校验器
├── config/                          # 配置类
│   └── MyBatisPlusConfig.java       # MyBatis Plus 配置
├── controller/                      # 控制器层
│   └── RumController.java           # RUM 数据接收与查询接口
├── dto/                             # 数据传输对象
│   ├── request/
│   │   └── DayAggQueryDTO.java      # 日聚合查询请求
│   └── response/
│       ├── DayAggResponseDTO.java   # 日聚合查询响应
│       └── OverviewDictDTO.java     # 概览字典响应
├── mapper/                          # MyBatis Mapper
│   ├── RumDailyAggMapper.java       # 日聚合数据 Mapper
│   └── RumPoMapper.java             # RUM 事件数据 Mapper
├── repository/                      # 持久层实体
│   ├── RumDailyPo.java              # 日聚合表实体
│   └── RumPo.java                   # RUM 事件表实体
└── service/                         # 服务层
    ├── RumService.java              # RUM 服务接口
    └── impl/
        └── RumServiceImpl.java      # RUM 服务实现
```

## 核心功能模块

### 1. 数据采集模块（RumController）

**功能**: 接收前端上报的 RUM 监控数据
**API 端点**:

- `POST /rum/rum` - 单条数据上报（支持 `text/plain` 和 `application/json`）
- `POST /rum/rumBatch` - 批量数据上报
- `GET /rum/dayAgg` - 查询日聚合数据
- `GET /rum/overview/dict` - 获取概览字典数据
  **数据处理流程**:

1. 接收 JSON 格式的性能数据
2. 解析并校验字段（metricId、环境、版本等）
3. 白名单校验（API Key 验证）
4. 解析 User-Agent 和 IP 信息
5. 数据入库到 `rum_event` 表

### 2. 数据聚合模块（RumDailyAggJob）

**功能**: 每日定时聚合前一天的性能数据
**执行时间**: 每天凌晨 00:10（`cron = "0 10 0 * * ?"`)
**聚合逻辑**:

1. **去重**: 按 `(sessionId, pageId, metric, routeKey)` 去重，同一页面会话只保留 value 最大的记录
2. **分组**: 按 `(env, metric, routeKey, releaseVer)` 分组
3. **统计指标**:
   - 样本量（cnt）
   - P50 中位数
   - P75 分位数（核心指标）
   - P95 分位数
   - Good Rate（良好体验占比）
4. **结果存储**: 写入 `rum_daily_agg` 表
   **聚合维度说明**:

- `metric`: 指标类型（1=LCP, 2=INP, 3=CLS）
- `env`: 环境（prod/staging/dev）
- `routeKey`: 归一化路由
- `releaseVer`: 发布版本

### 3. 数据查询模块（RumService）

**功能**: 提供聚合数据查询和字典数据
**主要方法**:

- `collect()` - 处理单条数据采集
- `collectBatch()` - 处理批量数据采集
- `dayAgg()` - 分页查询日聚合数据
- `getDict()` - 获取概览字典（环境、版本、路由等枚举值）

## 数据模型

### RumPo（rum_event 表）

存储原始 RUM 事件数据
**关键字段**:

- `clientTs` - 客户端时间戳
- `metric` - 指标类型（1/2/3）
- `value` - 指标值
- `delta` - 增量值
- `metricId` - 指标唯一标识
- `rating` - 性能评级
- `navType` - 导航类型
- `routeKey` - 路由键
- `releaseVer` - 发布版本
- `env` - 环境
- `device` - 设备类型
- `network` - 网络类型
- `ua` - User-Agent
- `ip` - IP 地址（byte 数组）
- `ext` - 扩展字段（JSON）
- `sessionId` - 会话 ID
- `pageId` - 页面 ID

### RumDailyPo（rum_daily_agg 表）

存储日聚合数据，用于榜单和版本对比
**关键字段**:

- `day` - 统计日期
- `env` - 环境
- `metric` - 指标类型
- `routeKey` - 归一化路由
- `releaseVer` - 版本标识
- `cnt` - 样本量
- `p50` - P50 分位数
- `p75` - P75 分位数（核心）
- `p95` - P95 分位数
- `goodRate` - 良好体验占比

## 配置说明

### 数据库配置（application.yml）

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/watch?...
    username: root
    password: taotao123.
```

### RUM 安全配置

```yaml
rum:
  security:
    api-keys: ${RUM_API_KEYS:${RUM_API_KEY}}
```

### 服务端口

```yaml
server:
  port: 8888
```

## 核心业务流程

### 数据采集流程

```
前端上报 → RumController 接收 
→ JSON 解析 
→ 字段校验（metricId、env、版本等）
→ API Key 白名单校验
→ UA/IP 解析
→ RumService.collect()
→ 数据入库（rum_event）
```

### 数据聚合流程

```
定时任务触发（每天 00:10）
→ 查询昨天的明细数据
→ 按 (sessionId, pageId, metric, routeKey) 去重
→ 按 (env, metric, routeKey, releaseVer) 分组
→ 计算 P50/P75/P95 和 good 占比
→ 写入聚合表（rum_daily_agg）
```

### 数据查询流程

```
前端请求 → RumController.dayAgg()
→ 参数校验（DayAggQueryDTO）
→ RumService.dayAgg()
→ MyBatis Plus 分页查询
→ 返回聚合数据（DayAggResponseDTO）
```

## Web Vitals 指标说明

### 1. LCP (Largest Contentful Paint)

- **指标 ID**: 1
- **含义**: 最大内容绘制时间
- **良好标准**: < 2500ms
- **用途**: 衡量页面加载性能

### 2. INP (Interaction to Next Paint)

- **指标 ID**: 2
- **含义**: 交互到下一次绘制时间
- **良好标准**: < 200ms
- **用途**: 衡量页面交互响应性

### 3. CLS (Cumulative Layout Shift)

- **指标 ID**: 3
- **含义**: 累积布局偏移
- **良好标准**: < 0.1
- **用途**: 衡量视觉稳定性

## 开发指南

### 启动项目

1. 确保 MySQL 服务运行，数据库 `watch` 已创建
2. 配置 `application.yml` 中的数据库连接信息
3. 运行 `Main.java` 启动 Spring Boot 应用
4. 服务将在 `http://localhost:8888` 启动

### 添加新的监控指标

1. 在 `Metric.java` 或 `MetricType.java` 中添加新指标枚举
2. 更新 `RumValidator.java` 校验规则
3. 如需聚合，修改 `RumDailyAggJob.java` 聚合逻辑
4. 更新前端上报格式

### 自定义聚合规则

修改 `RumDailyAggJob.java`:

- 调整去重维度（GroupKey）
- 修改分位数计算逻辑
- 添加新的统计指标

## 常见问题

### Q1: 如何修改定时任务执行时间？

修改 `RumDailyAggJob.java` 中的 `@Scheduled(cron = "0 10 0 * * ?")` 注解。

### Q2: 数据采集失败如何排查？

1. 检查日志中的 `rum: validate failed` 警告
2. 确认 API Key 配置正确
3. 验证 JSON 格式是否符合 `RumPo` 结构
4. 检查 metricId 是否在白名单中

### Q3: 聚合数据不准确？

1. 检查去重逻辑是否符合业务需求
2. 确认时区配置（当前使用 UTC）
3. 验证分位数计算算法

### Q4: 如何扩展存储容量？

1. 实现数据分表（按日期或其他维度）
2. 添加数据归档策略
3. 考虑使用时序数据库（InfluxDB、TimescaleDB）

## 性能优化建议

1. **数据采集优化**:
   - 批量上报减少请求次数
   - 异步入库提升吞吐量
   - 添加消息队列缓冲（Kafka/RabbitMQ）
2. **聚合任务优化**:
   - 数据量大时改为分页查询
   - 使用数据库聚合函数（GROUP BY + 百分位函数）
   - 考虑增量聚合而非全量计算
3. **查询优化**:
   - 添加复合索引（env, metric, routeKey, day）
   - 使用 Redis 缓存热点数据
   - 实现查询结果预计算

## 安全注意事项

1. **API Key 验证**:
   - 生产环境务必配置 `RUM_API_KEYS`
   - 定期轮换 API Key
   - 实现 IP 白名单机制
2. **数据脱敏**:
   - IP 地址仅存储，不对外暴露
   - User-Agent 信息需谨慎处理
   - ext 扩展字段避免存储敏感信息
3. **SQL 注入防护**:
   - 使用 MyBatis Plus 参数化查询
   - 禁止动态拼接 SQL

## 监控与告警

建议监控以下指标：

- 数据采集失败率
- 定时任务执行状态
- 数据库连接池状态
- 接口响应时间
- 磁盘空间使用率

## 版本历史

- **v0.0.1-SNAPSHOT**: 初始版本
  - 支持 LCP/INP/CLS 三大指标采集
  - 实现每日聚合定时任务
  - 提供基础查询 API

## 联系与贡献

如有问题或建议，请联系项目维护者或提交 Issue。
----------------------------------------------

**最后更新**: 2026-01-23
