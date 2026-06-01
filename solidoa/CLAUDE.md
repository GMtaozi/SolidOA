# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Full build (skip tests)
mvn clean package -DskipTests

# Build specific module
mvn clean package -DskipTests -pl solidoa-parent/solidoa-system -am

# Run single service (from solidoa-parent directory)
java -jar solidoa-system/target/solidoa-system-1.0.0-SNAPSHOT.jar

# Docker compose up
cd docker && docker-compose up -d
```

## Architecture

### Multi-Database Architecture
Each microservice has its own database:
- `oa_system` - System service (用户、部门、角色、通讯录、日程)
- `oa_workflow` - Workflow service (审批流、请假、用印、采购)
- `oa_hr` - HR service (考勤、财务、工资、加班、出差、补卡)
- `oa_file` - File service
- `oa_dingtalk` - DingTalk integration

### Service Ports
| Service | Port | Database | Description |
|---------|------|----------|-------------|
| solidoa-gateway | 8080 | - | API 网关 |
| solidoa-system | 8081 | oa_system | 系统管理 + 协作 |
| solidoa-workflow | 8082 | oa_workflow | 工作流审批 |
| solidoa-hr | 8085 | oa_hr | 考勤 + 财务 |
| solidoa-file | 8086 | - | 文件服务 |
| solidoa-dingtalk | 8087 | oa_dingtalk | 钉钉集成 |

### Key Configuration Pattern
Each service has `application.yml` with database connection using environment variables:
```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/oa_system?...
    password: ${DB_PASSWORD:749958714}
```

## Code Patterns

### Layer Structure (per service)
```
controller/   → @RestController with @RequestMapping
service/      → Interface + Impl (business logic)
mapper/       → MyBatis-Plus BaseMapper interfaces
entity/      → @TableName annotated JPA entities
form/        → Request DTOs (input)
vo/          → Response DTOs (output)
```

### Response Format
Use `Result<T>` from `com.solidoa.common.result.Result`:
```java
return Result.success(data);
return Result.fail(400, "error message");
```

### Exception Handling
`GlobalExceptionHandler` in `solidoa-common` handles all exceptions centrally:
- `BusinessException` → returns code + message
- `MethodArgumentNotValidException` → returns 400 with validation message
- `Exception` → returns 500 system error

### MyBatis-Plus Notes
- Use `@TableField(exist = false)` for non-persistent calculated fields
- Calculated columns (MySQL GENERATED) must be marked as exist=false
- Entity fields map to DB columns with snake_case → camelCase conversion

### Authentication
Services use `X-User-Id` header for user identification (no JWT in current implementation)

## Database Initialization
SQL scripts in `sql/init/` must be run in order:
1. `00_init_databases.sql` - Create databases
2. `01_system.sql` through `07_init_data.sql` - Table structures and seed data

## Common Issues

### Service Configuration Protection
禁止无理由修改以下关键配置，但配置本身有误时应主动修正并同步更新关联配置：
- `application.yml` 中的 `server.port`、`spring.datasource.url`、`spring.application.name`
- `pom.xml` 中的模块依赖关系
- `gateway` 路由配置（路径 → 端口映射）
- Feign 客户端中的服务名
- 前端 `vite.config.js` 中的 API 代理配置

错误配置的判定标准：路径与后端 Controller 不匹配、服务名在 application.yml 中不存在、端口与实际服务不一致等。

### Cross-Database JOINs
MySQL does not support JOIN across different databases on same connection. Each service must only query its own database. If JOIN is needed, use separate queries or Feign calls.

### Calculated Columns
If using MySQL generated/calculated columns (e.g., `remaining_amount`), ensure:
1. Entity field has `@TableField(exist = false)`
2. Service code does NOT call `setRemainingAmount()` (let DB calculate)

### CORS Configuration
When adding new APIs that will be called from frontend, ensure `SecurityConfig` has CORS configuration with proper `CorsConfigurationSource` bean.

## Scripts
- `scripts/deploy.sh` - Linux/Mac deployment script
- `scripts/start-services.bat` - Windows startup
- `scripts/test-api.bat` - API testing script
- `scripts/health-check.sh` - Service health check
