# AGENTS.md - Watch RUM Agent Guide

## Purpose
- This file helps agentic coding tools work effectively in this repo.
- Focus: build/lint/test commands and coding style conventions.

## Repo Facts (quick)
- Language: Java 8
- Framework: Spring Boot 2.7.18
- ORM: MyBatis-Plus 3.5.3.2
- Build tool: Maven
- DB: MySQL 8 (see `src/main/resources/application.yml`)

## Build, Run, Lint, Test

### Build
- Full build: `mvn clean package`
- Skip tests: `mvn clean package -DskipTests`

### Run (local)
- Spring Boot run: `mvn spring-boot:run`
- Run from IDE: `come.watch.Main`

### Tests
- All tests (if any): `mvn test`
- Single test class: `mvn -Dtest=MyTestClass test`
- Single test method: `mvn -Dtest=MyTestClass#myTestMethod test`
- Notes: there is no `src/test` directory in the current repo.

### Lint / Format
- No lint or formatter is configured in `pom.xml`.
- Do not introduce new formatting tools unless requested.

## Cursor / Copilot Rules
- `.cursor/rules/`: not found
- `.cursorrules`: not found
- `.github/copilot-instructions.md`: not found

## Code Style Guidelines

### Language + Formatting
- Use 4 spaces per indent; braces on the same line.
- Keep lines concise; wrap long argument lists onto new lines.
- Prefer explicit types when it improves clarity; generics are common.
- Use standard Java naming: camelCase for fields/methods, PascalCase for types.
- Constants use `UPPER_SNAKE_CASE` (see `RumValidator` constants).

### Imports
- Follow the existing import grouping/order seen in controllers/services:
  - External libs (`com.*`, third-party)
  - Project packages (`come.watch.*`)
  - Lombok (`lombok.*`)
  - Spring / other framework (`org.*`)
  - Javax (`javax.*`)
  - JDK (`java.*`)
- Keep imports sorted within each group and remove unused imports.

### Lombok
- Lombok is used for DTOs/entities (`@Data`, `@Builder`, etc.).
- Prefer Lombok for simple data carriers; avoid complex logic in Lombok classes.
- Use constructor injection via `@RequiredArgsConstructor` in services/controllers.

### Package Structure
- Main packages follow `come.watch.*`.
- Layers are conventional:
  - `controller` for HTTP endpoints
  - `service` for business logic
  - `mapper` for MyBatis-Plus mappers
  - `repository` for entity/PO objects
  - `dto` for request/response objects
  - `common` for cross-cutting utilities and enums

### Naming Conventions
- Entities end with `Po` (e.g., `RumPo`, `RumDailyPo`).
- DTOs end with `DTO` (e.g., `DayAggQueryDTO`).
- Services use interface + `Impl` naming (`RumService` / `RumServiceImpl`).
- Enums use clear labels and `getCode()` / `getDescription()` methods.

### Validation
- Use `javax.validation` annotations for request DTOs.
- Custom enum validation uses `@EnumValue` (see `dto/request/DayAggQueryDTO`).
- Additional runtime validation is done in `RumValidator` before persistence.

### Error Handling
- Use `BusinessException` for domain/business errors.
- Leverage `GlobalExceptionHandler` for HTTP-friendly errors.
- Controllers typically log validation failures and return early.
- Return standardized responses using `CommonResponse` or `CommonResponseDTO`.

### Logging
- Use `@Slf4j` and structured logs (`log.warn("key={}, ...", value)`).
- Log validation failures and bad inputs at warn level.
- Log unexpected failures at error level with stack trace.

### API Layer
- Controllers use `@RestController` and map under `/rum`.
- Accept `text/plain` for JSON payloads in collection endpoints.
- Prefer `@RequestBody` DTOs and `@RequestParam` for pagination.

### Transactions
- Services use `@Transactional(rollbackFor = Exception.class)` when needed.
- Batch operations should be transaction-wrapped (see `collectBatch`).

### Persistence (MyBatis-Plus)
- Entities use `@TableName` and `@TableId`.
- JSON columns use `@TableField(typeHandler = JacksonTypeHandler.class)`.
- Mapper XML lives in `src/main/resources/mapper`.
- Use `Page<>` for pagination and `IPage` for results.

### Dates and Time
- DTOs use `@JsonFormat` with `Asia/Beijing` timezone.
- Use `LocalDate` for day-level filters and `Date`/`LocalDateTime` for timestamps.
- Database timezone is set to UTC in `application.yml`.

### API Responses
- `CommonResponse` and `CommonResponseDTO` provide success/fail patterns.
- Use `CommonResponseDTO.page(...)` for pagination responses.
- For `NO_CONTENT` endpoints, return `void` and log outcomes.

### Defensive Checks
- Validate request body presence and JSON parsing errors explicitly.
- Check required fields (metric, routeKey) before persistence.
- Avoid null pointer risks; guard for empty arrays/lists.

## Files to Know
- `pom.xml` - Maven build config and versions
- `src/main/java/come/watch/Main.java` - Spring Boot entry
- `src/main/java/come/watch/common/GlobalExceptionHandler.java`
- `src/main/java/come/watch/controller/RumController.java`
- `src/main/java/come/watch/service/impl/RumServiceImpl.java`
- `src/main/resources/application.yml` - local DB config

## Contribution Tips for Agents
- Keep edits minimal and consistent with the existing patterns.
- Avoid introducing new frameworks or style tools without user request.
- If adding new endpoints, follow current response and logging patterns.
- If adding new fields, update DTOs, entities, mapper XML, and validators.

## Security and Config Notes
- API keys are read from `rum.security.api-keys`.
- Local DB credentials exist in `application.yml`; do not log secrets.

## When in Doubt
- Prefer existing patterns and class structure over new abstractions.
- Keep behavior unchanged unless the task explicitly requests changes.
