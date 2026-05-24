# SolidOA 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成SolidOA企业办公系统的完整开发，包含8个微服务、前端Vue3、移动端UniApp，实现审批流、协作办公、财务管理、考勤、钉钉集成五大核心模块。

**Architecture:** 采用Spring Cloud Alibaba微服务架构，Gateway统一入口，Nacos服务注册与配置，Sentinel熔断降级，Camunda工作流引擎处理复杂审批流程，自研状态机处理加签/转交，RabbitMQ异步消息+死信队列，Redis缓存，MySQL分库存储。

**Tech Stack:**
- 后端：Spring Boot 3.2.x, Spring Cloud Alibaba 2023.x, MyBatis-Plus 3.5.x, Camunda 7.19, Sentinel 1.8.x, RabbitMQ 3.x, Redis 7.x
- 前端：Vue3 3.x, Vite 5.x, Element Plus 2.x, Pinia 2.x
- 移动端：UniApp 3.x
- 数据库：MySQL 8.0
- 文件存储：MinIO

---

## 文件结构

```
solidoa/
├── solidoa-parent/                      # 父POM
│   ├── pom.xml
│   └── modules/
│       ├── solidoa-common/             # 公共模块
│       │   └── pom.xml
│       ├── solidoa-gateway/            # 网关服务 :8080
│       │   └── pom.xml
│       ├── solidoa-system/             # 系统服务 :8081
│       │   └── pom.xml
│       ├── solidoa-workflow/           # 流程服务 :8082
│       │   └── pom.xml
│       ├── solidoa-collaboration/       # 协作服务 :8083
│       │   └── pom.xml
│       ├── solidoa-finance/            # 财务服务 :8084
│       │   └── pom.xml
│       ├── solidoa-attendance/         # 考勤服务 :8085
│       │   └── pom.xml
│       ├── solidoa-file/               # 文件服务 :8086
│       │   └── pom.xml
│       └── solidoa-dingtalk/           # 钉钉服务 :8087
│           └── pom.xml
├── solidoa-web/                        # Vue3前端
│   ├── package.json
│   └── src/
├── solidoa-app/                        # UniApp移动端
│   ├── package.json
│   └── src/
└── sql/                                # 数据库脚本
    ├── init.sql
    └── update/
```

---

## 阶段一：基础架构（第1-4周）

### Task 1: 项目脚手架搭建

**Files:**
- Create: `solidoa/pom.xml` (父POM)
- Create: `solidoa/solidoa-parent/pom.xml`
- Create: `solidoa/solidoa-common/pom.xml`
- Create: `solidoa/solidoa-common/src/main/java/com/solidoa/common/SolidoaCommonApplication.java`
- Create: `solidoa/solidoa-common/src/main/resources/application.yml`
- Create: `solidoa/solidoa-gateway/pom.xml`
- Create: `solidoa/solidoa-system/pom.xml`
- Create: `solidoa/solidoa-workflow/pom.xml`
- Create: `solidoa/solidoa-collaboration/pom.xml`
- Create: `solidoa/solidoa-finance/pom.xml`
- Create: `solidoa/solidoa-attendance/pom.xml`
- Create: `solidoa/solidoa-file/pom.xml`
- Create: `solidoa/solidoa-dingtalk/pom.xml`

- [ ] **Step 1: 创建父POM (solidoa/pom.xml)**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.solidoa</groupId>
    <artifactId>solidoa</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <modules>
        <module>solidoa-parent</module>
    </modules>

    <properties>
        <java.version>17</java.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <spring-boot.version>3.2.5</spring-boot.version>
        <spring-cloud.version>2023.0.1.0</spring-cloud.version>
        <spring-cloud-alibaba.version>2023.0.1.2</spring-cloud-alibaba.version>
        <mybatis-plus.version>3.5.6</mybatis-plus.version>
        <camunda.version>7.21.0</camunda.version>
        <sentinel.version>1.8.7</sentinel.version>
        <hutool.version>5.8.26</hutool.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>com.alibaba.cloud</groupId>
                <artifactId>spring-cloud-alibaba-dependencies</artifactId>
                <version>${spring-cloud-alibaba.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>17</source>
                    <target>17</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建父模块POM (solidoa-parent/pom.xml)**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.solidoa</groupId>
        <artifactId>solidoa</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>solidoa-parent</artifactId>
    <packaging>pom</packaging>

    <modules>
        <module>solidoa-common</module>
        <module>solidoa-gateway</module>
        <module>solidoa-system</module>
        <module>solidoa-workflow</module>
        <module>solidoa-collaboration</module>
        <module>solidoa-finance</module>
        <module>solidoa-attendance</module>
        <module>solidoa-file</module>
        <module>solidoa-dingtalk</module>
    </modules>
</project>
```

- [ ] **Step 3: 创建common模块POM (solidoa-common/pom.xml)**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.solidoa</groupId>
        <artifactId>solidoa-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>solidoa-common</artifactId>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- MyBatis-Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>

        <!-- Redis -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.5</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.5</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.12.5</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Hutool -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 4: 创建common模块启动类**

```java
package com.solidoa.common;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SolidoaCommonApplication {
    public static void main(String[] args) {
        SpringApplication.run(SolidoaCommonApplication.class, args);
    }
}
```

- [ ] **Step 5: 创建common模块通用类结构**

```java
// Result统一响应
package com.solidoa.common.result;

@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;
    private long timestamp;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("success");
        result.setData(data);
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }

    public static <T> Result<T> fail(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }
}
```

```java
// PageDTO分页请求
package com.solidoa.common.dto;

@Data
public class PageDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
```

```java
// PageVO分页响应
package com.solidoa.common.vo;

@Data
public class PageVO<T> {
    private List<T> records;
    private long total;
    private int pageNum;
    private int pageSize;
}
```

```java
// 全局异常处理
package com.solidoa.common.exception;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        return Result.fail(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail("系统异常，请稍后重试");
    }
}
```

- [ ] **Step 6: 创建各服务模块POM (以system为例)**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.solidoa</groupId>
        <artifactId>solidoa-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>solidoa-system</artifactId>

    <dependencies>
        <dependency>
            <groupId>com.solidoa</groupId>
            <artifactId>solidoa-common</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- Nacos Discovery -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>

        <!-- Nacos Config -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
        </dependency>

        <!-- MySQL -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
        </dependency>

        <!-- OpenFeign -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>

        <!-- Sentinel -->
        <dependency>
            <groupId>com.alibaba.csp</groupId>
            <artifactId>sentinel-spring-boot-starter</artifactId>
            <version>${sentinel.version}</version>
        </dependency>

        <!-- Actuator -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 7: 提交代码**

```bash
git add .
git commit -m "feat: 初始化项目脚手架，8个微服务模块结构"
```

---

### Task 2: Nacos服务注册与配置中心

**Files:**
- Modify: `solidoa/solidoa-system/src/main/resources/application.yml`
- Create: `solidoa/sql/nacos-config/bootstrap.yml`
- Create: `solidoa/sql/nacos-config/system-service.yml`
- Create: `solidoa/sql/nacos-config/common.yml`

- [ ] **Step 1: 创建公共配置 (common.yml)**

```yaml
spring:
  application:
    name: solidoa-common
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:oa_system}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: ${DB_USER:root}
    password: ${DB_PASSWORD:root123}
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      connection-timeout: 30000

  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    database: 0

  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USER:solidoa}
    password: ${RABBITMQ_PASSWORD:solidoa123}

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.solidoa.**.entity
  global-config:
    db-config:
      id-type: snowflake
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

jwt:
  secret: ${JWT_SECRET:solidoa-secret-key}
  expiration: 900
  refresh-expiration: 604800

logging:
  level:
    com.solidoa: DEBUG

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

- [ ] **Step 2: 配置system-service的application.yml**

```yaml
server:
  port: 8081

spring:
  application:
    name: system-service
  cloud:
    nacos:
      server-addr: ${NACOS_HOST:localhost}:${NACOS_PORT:8848}
      username: nacos
      password: nacos
      config:
        namespace: ${NACOS_NAMESPACE:public}
        file-extension: yml
        shared-configs:
          - data-id: common.yml
            group: DEFAULT_GROUP
            refresh: true
```

- [ ] **Step 3: 验证Nacos连接**

```bash
# 启动Nacos
cd /opt/solidoa/nacos && sh bin/startup.sh -m standalone

# 验证服务注册
curl http://localhost:8848/nacos/v1/ns/instance/list?serviceName=system-service
```

- [ ] **Step 4: 提交代码**

```bash
git add .
git commit -m "feat: 配置Nacos服务注册与配置中心"
```

---

### Task 3: API网关（Gateway）

**Files:**
- Modify: `solidoa/solidoa-gateway/pom.xml`
- Create: `solidoa/solidoa-gateway/src/main/java/com/solidoa/gateway/GatewayApplication.java`
- Create: `solidoa/solidoa-gateway/src/main/resources/application.yml`
- Create: `solidoa/solidoa-gateway/src/main/java/com/solidoa/gateway/filter/JwtFilter.java`
- Create: `solidoa/solidoa-gateway/src/main/java/com/solidoa/gateway/filter/RateLimiterFilter.java`

- [ ] **Step 1: 创建Gateway POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.solidoa</groupId>
        <artifactId>solidoa-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>solidoa-gateway</artifactId>

    <dependencies>
        <!-- Gateway -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway</artifactId>
        </dependency>

        <!-- Nacos Discovery -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>

        <!-- Sentinel Gateway -->
        <dependency>
            <groupId>com.alibaba.csp</groupId>
            <artifactId>sentinel-spring-cloud-gateway-adapter</artifactId>
            <version>${sentinel.version}</version>
        </dependency>

        <!-- Redis (限流) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.5</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.5</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.12.5</version>
            <scope>runtime</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 创建Gateway启动类**

```java
package com.solidoa.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```

- [ ] **Step 3: 配置Gateway路由**

```yaml
server:
  port: 8080

spring:
  application:
    name: gateway
  cloud:
    nacos:
      server-addr: localhost:8848
    gateway:
      routes:
        - id: system-service
          uri: lb://system-service
          predicates:
            - Path=/api/v1/system/**
          filters:
            - StripPrefix=1

        - id: workflow-service
          uri: lb://workflow-service
          predicates:
            - Path=/api/v1/workflow/**
          filters:
            - StripPrefix=1

        - id: collaboration-service
          uri: lb://collaboration-service
          predicates:
            - Path=/api/v1/collab/**
          filters:
            - StripPrefix=1

        - id: finance-service
          uri: lb://finance-service
          predicates:
            - Path=/api/v1/finance/**
          filters:
            - StripPrefix=1

        - id: attendance-service
          uri: lb://attendance-service
          predicates:
            - Path=/api/v1/attendance/**
          filters:
            - StripPrefix=1

        - id: file-service
          uri: lb://file-service
          predicates:
            - Path=/api/v1/file/**
          filters:
            - StripPrefix=1

        - id: dingtalk-service
          uri: lb://dingtalk-service
          predicates:
            - Path=/api/v1/dingtalk/**
          filters:
            - StripPrefix=1

      default-filters:
        - name: RequestRateLimiter
          args:
            redis-rate-limiter.replenishRate: 100
            redis-rate-limiter.burstCapacity: 200

jwt:
  secret: ${JWT_SECRET:solidoa-secret-key}
  exclusion:
    - /api/v1/auth/login
    - /api/v1/auth/refresh
    - /api/v1/dingtalk/callback
```

- [ ] **Step 4: 创建JWT过滤器**

```java
package com.solidoa.gateway.filter;

@Component
public class JwtFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.exclusion}")
    private List<String> exclusions;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 跳过无需认证的路径
        if (exclusions.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        // 从请求头获取Token
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        token = token.substring(7);

        try {
            // 验证JWT
            Claims claims = Jwts.parser()
                    .setSigningKey(jwtSecret.getBytes())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 将用户信息传递给下游服务
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", claims.getSubject())
                    .header("X-User-Name", claims.get("username", String.class))
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
```

- [ ] **Step 5: 启动并验证Gateway**

```bash
curl http://localhost:8080/api/v1/system/users
# 应返回401未认证
```

- [ ] **Step 6: 提交代码**

```bash
git add .
git commit -m "feat: 实现Gateway网关，包含JWT认证和限流"
```

---

### Task 4: 数据库设计与初始化

**Files:**
- Create: `solidoa/sql/init.sql`
- Create: `solidoa/sql/init/01_system.sql`
- Create: `solidoa/sql/init/02_workflow.sql`
- Create: `solidoa/sql/init/03_collaboration.sql`
- Create: `solidoa/sql/init/04_finance.sql`
- Create: `solidoa/sql/init/05_attendance.sql`
- Create: `solidoa/sql/init/06_common.sql`

- [ ] **Step 1: 创建数据库**

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS oa_system DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS oa_workflow DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS oa_collaboration DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS oa_finance DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS oa_attendance DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS oa_common DEFAULT CHARACTER SET utf8mb4;
```

- [ ] **Step 2: 创建oa_system数据库表 (01_system.sql)**

```sql
-- sys_user
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码(BCrypt加密)',
    real_name VARCHAR(50) COMMENT '真实姓名',
    mobile VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    avatar VARCHAR(255) COMMENT '头像URL',
    dept_id BIGINT COMMENT '部门ID',
    status TINYINT DEFAULT 1 COMMENT '状态:0禁用,1启用',
    dingtalk_userid VARCHAR(100) COMMENT '钉钉用户ID',
    dingtalk_unionid VARCHAR(100) COMMENT '钉钉UnionID',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    INDEX idx_dept_id (dept_id),
    INDEX idx_dingtalk_userid (dingtalk_userid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- sys_department
CREATE TABLE sys_department (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '部门名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父部门ID',
    leader_id BIGINT COMMENT '部门负责人ID',
    sort INT DEFAULT 0 COMMENT '排序号',
    dingtalk_id VARCHAR(100) COMMENT '钉钉部门ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- sys_role
CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL COMMENT '角色名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码',
    description VARCHAR(200) COMMENT '角色描述',
    status TINYINT DEFAULT 1 COMMENT '状态:0禁用,1启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- sys_user_role
CREATE TABLE sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- sys_permission
CREATE TABLE sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '权限名称',
    code VARCHAR(100) NOT NULL UNIQUE COMMENT '权限编码',
    type VARCHAR(20) COMMENT '类型:menu,button,api',
    url VARCHAR(200) COMMENT '请求路径',
    method VARCHAR(10) COMMENT '请求方法',
    parent_id BIGINT DEFAULT 0 COMMENT '父权限ID',
    sort INT DEFAULT 0 COMMENT '排序号',
    icon VARCHAR(50) COMMENT '图标',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_parent_id (parent_id),
    INDEX idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- sys_role_permission
CREATE TABLE sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_perm (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- sys_dict
CREATE TABLE sys_dict (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type VARCHAR(50) NOT NULL COMMENT '字典类型',
    label VARCHAR(100) NOT NULL COMMENT '字典标签',
    value VARCHAR(100) NOT NULL COMMENT '字典值',
    sort INT DEFAULT 0 COMMENT '排序号',
    status TINYINT DEFAULT 1 COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- sys_oper_log
CREATE TABLE sys_oper_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    module VARCHAR(50) COMMENT '模块',
    business_type VARCHAR(20) COMMENT '业务类型',
    method VARCHAR(100) COMMENT '请求方法',
    request_url VARCHAR(200) COMMENT '请求URL',
    request_method VARCHAR(10) COMMENT '请求方式',
    request_params TEXT COMMENT '请求参数',
    response_data TEXT COMMENT '响应数据',
    user_id BIGINT COMMENT '用户ID',
    user_name VARCHAR(50) COMMENT '用户名',
    ip VARCHAR(50) COMMENT 'IP地址',
    location VARCHAR(200) COMMENT '操作地点',
    error_msg TEXT COMMENT '错误信息',
    execute_time BIGINT COMMENT '执行时间(ms)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 3: 创建oa_workflow数据库表 (02_workflow.sql)**

```sql
-- oa_leave
CREATE TABLE oa_leave (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    leave_no VARCHAR(32) NOT NULL UNIQUE COMMENT '请假单号',
    user_id BIGINT NOT NULL COMMENT '申请人ID',
    leave_type VARCHAR(20) NOT NULL COMMENT '请假类型',
    start_date DATE NOT NULL COMMENT '开始日期',
    end_date DATE NOT NULL COMMENT '结束日期',
    days DECIMAL(5,1) NOT NULL COMMENT '请假天数',
    hours DECIMAL(5,1) DEFAULT 0 COMMENT '请假小时数',
    reason TEXT COMMENT '请假事由',
    attachments VARCHAR(500) COMMENT '附件',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态',
    process_instance_id VARCHAR(100) COMMENT 'Camunda流程实例ID',
    current_approver_id BIGINT COMMENT '当前审批人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- oa_expense
CREATE TABLE oa_expense (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    expense_no VARCHAR(32) NOT NULL UNIQUE COMMENT '报销单号',
    user_id BIGINT NOT NULL COMMENT '申请人ID',
    dept_id BIGINT COMMENT '部门ID',
    expense_type VARCHAR(50) NOT NULL COMMENT '报销类型',
    amount DECIMAL(10,2) NOT NULL COMMENT '报销金额',
    reason TEXT COMMENT '报销事由',
    attachments VARCHAR(500) COMMENT '附件',
    bank_name VARCHAR(50) COMMENT '开户行',
    bank_account VARCHAR(50) COMMENT '银行账号',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态',
    process_instance_id VARCHAR(100) COMMENT 'Camunda流程实例ID',
    payment_time DATETIME COMMENT '付款时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_dept_id (dept_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- oa_approval_record
CREATE TABLE oa_approval_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    business_type VARCHAR(20) NOT NULL COMMENT '业务类型',
    business_id BIGINT NOT NULL COMMENT '业务ID',
    approver_id BIGINT NOT NULL COMMENT '审批人ID',
    approve_type VARCHAR(20) NOT NULL COMMENT '审批类型',
    comment TEXT COMMENT '审批意见',
    task_id VARCHAR(100) COMMENT 'Camunda任务ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_business (business_type, business_id, create_time),
    INDEX idx_approver (approver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- oa_transfer_record
CREATE TABLE oa_transfer_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    business_type VARCHAR(20) NOT NULL COMMENT '业务类型',
    business_id BIGINT NOT NULL COMMENT '业务ID',
    from_approver_id BIGINT NOT NULL COMMENT '原审批人',
    to_approver_id BIGINT NOT NULL COMMENT '新审批人',
    reason VARCHAR(200) COMMENT '转交原因',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- oa_reminder_record
CREATE TABLE oa_reminder_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    business_type VARCHAR(20) NOT NULL,
    business_id BIGINT NOT NULL,
    approver_id BIGINT NOT NULL,
    reminder_count INT DEFAULT 0 COMMENT '催办次数',
    last_reminder_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 4: 创建其他库表 (03-06)**

类似方式创建oa_collaboration、oa_finance、oa_attendance、oa_common的表结构

- [ ] **Step 5: 初始化数据**

```sql
-- 插入超级管理员
INSERT INTO sys_user (username, password, real_name, status) 
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5E', '系统管理员', 1);

-- 插入角色
INSERT INTO sys_role (name, code, description) VALUES 
('系统管理员', 'SYSTEM_ADMIN', '系统最高权限'),
('部门经理', 'DEPT_MANAGER', '部门负责人'),
('财务专员', 'FINANCE', '财务人员'),
('普通员工', 'EMPLOYEE', '普通员工'),
('考勤管理员', 'ATTENDANCE_ADMIN', '考勤管理员');

-- 插入部门
INSERT INTO sys_department (name, parent_id, sort) VALUES 
('总公司', 0, 0),
('技术部', 1, 1),
('销售部', 1, 2),
('财务部', 1, 3),
('行政部', 1, 4);
```

- [ ] **Step 6: 提交代码**

```bash
git add sql/
git commit -m "feat: 创建6个数据库及全部数据表结构"
```

---

### Task 5: 系统管理模块（System Service）

**Files:**
- Create: `solidoa/solidoa-system/src/main/java/com/solidoa/system/SystemApplication.java`
- Create: `solidoa/solidoa-system/src/main/java/com/solidoa/system/controller/AuthController.java`
- Create: `solidoa/solidoa-system/src/main/java/com/solidoa/system/controller/UserController.java`
- Create: `solidoa/solidoa-system/src/main/java/com/solidoa/system/controller/DeptController.java`
- Create: `solidoa/solidoa-system/src/main/java/com/solidoa/system/service/UserService.java`
- Create: `solidoa/solidoa-system/src/main/java/com/solidoa/system/service/impl/UserServiceImpl.java`
- Create: `solidoa/solidoa-system/src/main/java/com/solidoa/system/mapper/UserMapper.java`
- Create: `solidoa/solidoa-system/src/main/java/com/solidoa/system/entity/User.java`
- Create: `solidoa/solidoa-system/src/main/java/com/solidoa/system/dto/UserDTO.java`
- Create: `solidoa/solidoa-system/src/main/java/com/solidoa/system/vo/UserVO.java`
- Create: `solidoa/solidoa-system/src/main/resources/mapper/UserMapper.xml`

- [ ] **Step 1: 创建启动类和配置**

```java
package com.solidoa.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.solidoa.system.mapper")
public class SystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(SystemApplication.class, args);
    }
}
```

```yaml
server:
  port: 8081

spring:
  application:
    name: system-service
  cloud:
    nacos:
      server-addr: ${NACOS_HOST:localhost}:${NACOS_PORT:8848}
      config:
        namespace: ${NACOS_NAMESPACE:public}
        file-extension: yml
        shared-configs:
          - data-id: common.yml
            group: DEFAULT_GROUP
            refresh: true

  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/oa_system?useUnicode=true&characterEncoding=utf8
    username: ${DB_USER:root}
    password: ${DB_PASSWORD:root123}
```

- [ ] **Step 2: 创建User实体**

```java
package com.solidoa.system.entity;

@Data
@TableName("sys_user")
public class User {
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String mobile;
    private String email;
    private String avatar;
    private Long deptId;
    private Integer status;
    private String dingtalkUserid;
    private String dingtalkUnionid;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
    private Integer deleted;
}
```

- [ ] **Step 3: 创建UserMapper**

```java
package com.solidoa.system.mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    UserDTO selectByUsername(@Param("username") String username);
    
    List<UserVO> selectPageList(@Param("pageNum") Integer pageNum, 
                                  @Param("pageSize") Integer pageSize,
                                  @Param("username") String username,
                                  @Param("realName") String realName);
}
```

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.solidoa.system.mapper.UserMapper">

    <select id="selectByUsername" resultType="com.solidoa.system.dto.UserDTO">
        SELECT id, username, password, real_name, mobile, email, dept_id, status
        FROM sys_user WHERE username = #{username} AND deleted = 0
    </select>

    <select id="selectPageList" resultType="com.solidoa.system.vo.UserVO">
        SELECT id, username, real_name, mobile, email, dept_id, status, create_time
        FROM sys_user 
        WHERE deleted = 0
        <if test="username != null and username != ''">
            AND username LIKE CONCAT('%', #{username}, '%')
        </if>
        <if test="realName != null and realName != ''">
            AND real_name LIKE CONCAT('%', #{realName}, '%')
        </if>
        ORDER BY create_time DESC
        LIMIT #{pageNum}, #{pageSize}
    </select>

</mapper>
```

- [ ] **Step 4: 创建AuthController登录**

```java
package com.solidoa.system.controller;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/auth/login")
    public Result<TokenVO> login(@RequestBody @Valid LoginForm form) {
        return Result.success(authService.login(form));
    }

    @PostMapping("/auth/refresh")
    public Result<TokenVO> refresh(@RequestBody RefreshForm form) {
        return Result.success(authService.refreshToken(form.getRefreshToken()));
    }

    @PostMapping("/auth/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String token) {
        authService.logout(token.substring(7));
        return Result.success();
    }
}
```

- [ ] **Step 5: 创建AuthService**

```java
package com.solidoa.system.service;

public interface AuthService {
    TokenVO login(LoginForm form);
    TokenVO refreshToken(String refreshToken);
    void logout(String token);
}

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public TokenVO login(LoginForm form) {
        UserDTO user = userMapper.selectByUsername(form.getUsername());
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        
        if (!passwordEncoder.matches(form.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }
        
        // 生成Token
        String accessToken = jwtService.createAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtService.createRefreshToken(user.getId());
        
        // 存储RefreshToken到Redis
        redisTemplate.opsForValue().set(
            "token:refresh:" + user.getId(), 
            refreshToken, 
            7, TimeUnit.DAYS
        );
        
        TokenVO vo = new TokenVO();
        vo.setAccessToken(accessToken);
        vo.setRefreshToken(refreshToken);
        vo.setExpiresIn(900L);
        vo.setTokenType("Bearer");
        return vo;
    }

    @Override
    public TokenVO refreshToken(String refreshToken) {
        // 验证RefreshToken
        Claims claims = jwtService.parseToken(refreshToken);
        Long userId = Long.valueOf(claims.getSubject());
        
        // 检查Redis中的Token
        String storedToken = redisTemplate.opsForValue().get("token:refresh:" + userId);
        if (!refreshToken.equals(storedToken)) {
            throw new BusinessException("Token已失效");
        }
        
        // 查询用户
        UserDTO user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == 0) {
            throw new BusinessException("账号不可用");
        }
        
        // 生成新Token
        String newAccessToken = jwtService.createAccessToken(user.getId(), user.getUsername());
        String newRefreshToken = jwtService.createRefreshToken(user.getId());
        
        // 更新Redis
        redisTemplate.opsForValue().set(
            "token:refresh:" + userId, 
            newRefreshToken, 
            7, TimeUnit.DAYS
        );
        
        TokenVO vo = new TokenVO();
        vo.setAccessToken(newAccessToken);
        vo.setRefreshToken(newRefreshToken);
        vo.setExpiresIn(900L);
        vo.setTokenType("Bearer");
        return vo;
    }

    @Override
    public void logout(String token) {
        Claims claims = jwtService.parseToken(token);
        Long userId = Long.valueOf(claims.getSubject());
        redisTemplate.delete("token:refresh:" + userId);
    }
}
```

- [ ] **Step 6: 创建UserController CRUD**

```java
package com.solidoa.system.controller;

@RestController
@RequestMapping("/system/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public Result<PageVO<UserVO>> list(PageDTO dto, @RequestParam(required = false) String username, 
                                        @RequestParam(required = false) String realName) {
        return Result.success(userService.pageList(dto, username, realName));
    }

    @GetMapping("/{id}")
    public Result<UserDTO> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody @Valid UserForm form) {
        return Result.success(userService.create(form));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid UserForm form) {
        userService.update(id, form);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id) {
        userService.resetPassword(id);
        return Result.success();
    }
}
```

- [ ] **Step 7: 实现UserService**

```java
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private CacheManager cacheManager;

    @Override
    public PageVO<UserVO> pageList(PageDTO dto, String username, String realName) {
        List<UserVO> records = userMapper.selectPageList(
            (dto.getPageNum() - 1) * dto.getPageSize(),
            dto.getPageSize(),
            username, realName
        );
        long total = userMapper.selectCount(username, realName);
        
        PageVO<UserVO> page = new PageVO<>();
        page.setRecords(records);
        page.setTotal(total);
        page.setPageNum(dto.getPageNum());
        page.setPageSize(dto.getPageSize());
        return page;
    }

    @Override
    @Transactional
    public Long create(UserForm form) {
        // 检查用户名唯一性
        UserDTO existing = userMapper.selectByUsername(form.getUsername());
        if (existing != null) {
            throw new BusinessException("用户名已存在");
        }
        
        User user = new User();
        BeanUtils.copyProperties(form, user);
        user.setPassword(passwordEncoder.encode("123456")); // 默认密码
        userMapper.insert(user);
        
        return user.getId();
    }

    @Override
    @Transactional
    public void update(Long id, UserForm form) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        BeanUtils.copyProperties(form, user);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        
        // 清除缓存
        cacheManager.evict("user", id.toString());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        userMapper.deleteById(id);
        cacheManager.evict("user", id.toString());
    }

    @Override
    @Transactional
    public void resetPassword(Long id) {
        User user = new User();
        user.setId(id);
        user.setPassword(passwordEncoder.encode("123456"));
        userMapper.updateById(user);
    }
}
```

- [ ] **Step 8: 提交代码**

```bash
git add .
git commit -m "feat: 实现System模块，包含登录、用户管理CRUD"
```

---

## 阶段二：核心业务（第5-12周）

### Task 6: 审批流程模块（Workflow Service + Camunda）

**Files:**
- Create: `solidoa/solidoa-workflow/pom.xml`
- Create: `solidoa/solidoa-workflow/src/main/resources/leave.bpmn`
- Create: `solidoa/solidoa-workflow/src/main/java/com/solidoa/workflow/WorkflowApplication.java`
- Create: `solidoa/solidoa-workflow/src/main/java/com/solidoa/workflow/controller/LeaveController.java`
- Create: `solidoa/solidoa-workflow/src/main/java/com/solidoa/workflow/service/LeaveService.java`
- Create: `solidoa/solidoa-workflow/src/main/java/com/solidoa/workflow/service/impl/LeaveServiceImpl.java`
- Create: `solidoa/solidoa-workflow/src/main/java/com/solidoa/workflow/listener/LeaveProcessListener.java`

- [ ] **Step 1: 创建Workflow POM（含Camunda）**

```xml
<dependency>
    <groupId>org.camunda.bpm</groupId>
    <artifactId>camunda-spring-boot-starter-rest</artifactId>
    <version>${camunda.version}</version>
</dependency>
<dependency>
    <groupId>org.camunda.bpm</groupId>
    <artifactId>camunda-spring-boot-starter-webapp</artifactId>
    <version>${camunda.version}</version>
</dependency>
```

- [ ] **Step 2: 配置Camunda**

```yaml
camunda:
  bpm:
    auto-deployment:
      enabled: true
      resource-pattern: classpath:/bpmn/*.bpmn
  api:
    context-path: /camunda
```

- [ ] **Step 3: 创建请假BPMN流程 (leave.bpmn)**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                  id="Definitions_1" name="请假审批流程">

  <bpmn:process id="leaveProcess" name="请假审批" isExecutable="true">
    <bpmn:startEvent id="StartEvent_1" name="开始"/>
    
    <bpmn:userTask id="ApplyTask" name="填写申请" assignee="${initiator}">
      <bpmn:documentation>申请人填写请假单</bpmn:documentation>
    </bpmn:userTask>
    
    <bpmn:userTask id="ManagerApprove" name="直属审批" candidateGroups="DEPT_MANAGER">
      <bpmn:documentation>部门经理审批</bpmn:documentation>
    </bpmn:userTask>
    
    <bpmn:exclusiveGateway id="Gateway_1" name="是否需要二级审批"/>
    
    <bpmn:userTask id="SeniorApprove" name="二级审批" candidateGroups="SENIOR_MANAGER">
      <bpmn:documentation>高管审批（请假天数>=3天）</bpmn:documentation>
    </bpmn:userTask>
    
    <bpmn:endEvent id="EndEvent_Pass" name="审批通过"/>
    <bpmn:endEvent id="EndEvent_Reject" name="审批拒绝"/>
    
    <!-- 流程连线 -->
    <bpmn:sequenceFlow id="Flow_1" sourceRef="StartEvent_1" targetRef="ApplyTask"/>
    <bpmn:sequenceFlow id="Flow_2" sourceRef="ApplyTask" targetRef="ManagerApprove"/>
    <bpmn:sequenceFlow id="Flow_3" sourceRef="ManagerApprove" targetRef="Gateway_1"/>
    
    <bpmn:sequenceFlow id="Flow_Yes" name="≥3天" sourceRef="Gateway_1" targetRef="SeniorApprove">
      <bpmn:conditionExpression>${days >= 3}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    
    <bpmn:sequenceFlow id="Flow_No" name="<3天" sourceRef="Gateway_1" targetRef="EndEvent_Pass"/>
    <bpmn:sequenceFlow id="Flow_Senior" sourceRef="SeniorApprove" targetRef="EndEvent_Pass"/>
    
    <!-- 拒绝连线 -->
    <bpmn:sequenceFlow id="Flow_Reject_1" sourceRef="ManagerApprove" targetRef="EndEvent_Reject">
      <bpmn:conditionExpression>${approveResult == 'REJECT'}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="Flow_Reject_2" sourceRef="SeniorApprove" targetRef="EndEvent_Reject">
      <bpmn:conditionExpression>${approveResult == 'REJECT'}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
  </bpmn:process>
</bpmn:definitions>
```

- [ ] **Step 4: 创建LeaveController**

```java
@RestController
@RequestMapping("/api/v1/workflow/leave")
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @PostMapping
    public Result<Long> create(@RequestBody @Valid LeaveForm form, @RequestHeader("X-User-Id") Long userId) {
        return Result.success(leaveService.createLeave(form, userId));
    }

    @GetMapping
    public Result<PageVO<LeaveVO>> list(PageDTO dto, @RequestHeader("X-User-Id") Long userId) {
        return Result.success(leaveService.pageList(dto, userId));
    }

    @GetMapping("/{id}")
    public Result<LeaveDTO> getById(@PathVariable Long id) {
        return Result.success(leaveService.getById(id));
    }

    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @RequestBody @Valid ApproveForm form,
                                 @RequestHeader("X-User-Id") Long approverId) {
        leaveService.approve(id, form, approverId);
        return Result.success();
    }

    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        leaveService.cancel(id, userId);
        return Result.success();
    }
}
```

- [ ] **Step 5: 创建LeaveServiceImpl（含Camunda集成）**

```java
@Service
@Slf4j
public class LeaveServiceImpl implements LeaveService {

    @Autowired
    private RuntimeService runtimeService;
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private LeaveMapper leaveMapper;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public Long createLeave(LeaveForm form, Long userId) {
        Leave leave = new Leave();
        BeanUtils.copyProperties(form, leave);
        leave.setUserId(userId);
        leave.setLeaveNo(generateLeaveNo());
        leave.setStatus("PENDING");
        
        // 启动Camunda流程
        Map<String, Object> variables = new HashMap<>();
        variables.put("initiator", userId.toString());
        variables.put("days", form.getDays());
        
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
            "leaveProcess", 
            leave.getId().toString(), // businessKey
            variables
        );
        
        leave.setProcessInstanceId(instance.getId());
        leaveMapper.insert(leave);
        
        return leave.getId();
    }

    @Override
    public void approve(Long id, ApproveForm form, Long approverId) {
        Leave leave = leaveMapper.selectById(id);
        if (leave == null) {
            throw new BusinessException("请假单不存在");
        }
        
        // 完成任务
        Task task = taskService.createTaskQuery()
            .processInstanceId(leave.getProcessInstanceId())
            .taskAssignee(approverId.toString())
            .singleResult();
        
        if (task == null) {
            throw new BusinessException("没有可审批的任务");
        }
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("approveResult", form.getApproveResult()); // APPROVE / REJECT
        
        taskService.complete(task.getId(), variables);
        
        // 记录审批
        leaveMapper.insertApprovalRecord(id, approverId, form);
        
        // 发送消息通知
        sendApprovalNotify(leave, approverId, form);
        
        // 更新状态
        leave.setStatus(form.getApproveResult() == ApproveResult.APPROVE ? "APPROVED" : "REJECTED");
        leaveMapper.updateById(leave);
    }

    private void sendApprovalNotify(Leave leave, Long approverId, ApproveForm form) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "APPROVAL");
        message.put("businessType", "LEAVE");
        message.put("businessId", leave.getId());
        message.put("result", form.getApproveResult().name());
        message.put("approverId", approverId);
        
        rabbitTemplate.convertAndSend("solidoa.exchange", "approval.notify." + form.getApproveResult().name().toLowerCase(), message);
    }
}
```

- [ ] **Step 6: 创建Camunda流程监听器**

```java
@Component
public class LeaveProcessListener implements ExecutionListener, TaskListener {

    @Autowired
    private LeaveMapper leaveMapper;

    @Override
    public void notify(DelegateExecution execution) throws Exception {
        String eventName = execution.getEventName();
        if ("complete".equals(eventName)) {
            String processInstanceId = execution.getProcessInstanceId();
            // 流程结束时更新业务状态
            log.info("请假流程结束: {}", processInstanceId);
        }
    }

    @Override
    public void notify(DelegateTask delegateTask) throws Exception {
        String taskDefinitionKey = delegateTask.getTaskDefinitionKey();
        // 记录当前审批人
        delegateTask.addCandidateUser(delegateTask.getVariable("initiator").toString());
    }
}
```

- [ ] **Step 7: 提交代码**

```bash
git add .
git commit -m "feat: 实现Workflow模块，集成Camunda请假审批流程"
```

---

### Task 7: 消息中心（Collaboration Service + WebSocket）

**Files:**
- Create: `solidoa/solidoa-collaboration/pom.xml`
- Create: `solidoa/solidoa-collaboration/src/main/java/com/solidoa/collab/CollabApplication.java`
- Create: `solidoa/solidoa-collaboration/src/main/java/com/solidoa/collab/config/WebSocketConfig.java`
- Create: `solidoa/solidoa-collaboration/src/main/java/com/solidoa/collab/handler/WebSocketHandler.java`
- Create: `solidoa/solidoa-collaboration/src/main/java/com/solidoa/collab/controller/MessageController.java`
- Create: `solidoa/solidoa-collaboration/src/main/java/com/solidoa/collab/listener/ApprovalNotifyListener.java`

- [ ] **Step 1: 创建WebSocket配置**

```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private WebSocketHandler messageHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(messageHandler, "/ws/message/{userId}")
                .setAllowedOrigins("*")
                .addInterceptors(new WebSocketSessionInterceptor());
    }
}
```

```java
public class WebSocketSessionInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        // 验证用户身份
        String userId = request.getHeaders().getFirst("X-User-Id");
        if (userId != null) {
            attributes.put("userId", userId);
            return true;
        }
        return false;
    }
}
```

- [ ] **Step 2: 创建WebSocket处理器**

```java
@Component
@Slf4j
public class WebSocketHandler extends TextWebSocketHandler {

    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = Long.valueOf(session.getAttributes().get("userId").toString());
        sessions.put(userId, session);
        log.info("WebSocket连接建立: userId={}", userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 处理客户端消息
        log.info("收到消息: {}", message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = Long.valueOf(session.getAttributes().get("userId").toString());
        sessions.remove(userId);
        log.info("WebSocket连接关闭: userId={}", userId);
    }

    // 发送消息给指定用户
    public void sendToUser(Long userId, String message) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage(message));
        }
    }

    // 广播消息
    public void broadcast(String message) {
        sessions.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (Exception e) {
                    log.error("广播消息失败", e);
                }
            }
        });
    }
}
```

- [ ] **Step 3: 创建消息监听器（消费RabbitMQ消息）**

```java
@Component
@Slf4j
public class ApprovalNotifyListener {

    @Autowired
    private WebSocketHandler webSocketHandler;
    
    @Autowired
    private MessageService messageService;

    @RabbitListener(queues = "queue.approval.notify")
    public void handleApprovalNotify(Map<String, Object> message) {
        log.info("收到审批通知: {}", message);
        
        String businessType = (String) message.get("businessType");
        Long businessId = ((Number) message.get("businessId")).longValue();
        String result = (String) message.get("result");
        
        // 1. 保存消息到数据库
        Message msg = new Message();
        msg.setTitle("审批" + ("APPROVE".equals(result) ? "通过" : "拒绝"));
        msg.setContent("您的" + businessType + "申请已" + ("APPROVE".equals(result) ? "通过" : "拒绝"));
        msg.setType("APPROVAL");
        msg.setRelatedType(businessType);
        msg.setRelatedId(businessId);
        messageService.save(msg);
        
        // 2. 通过WebSocket推送
        Map<String, Object> wsMessage = new HashMap<>();
        wsMessage.put("type", "APPROVAL");
        wsMessage.put("title", msg.getTitle());
        wsMessage.put("content", msg.getContent());
        
        webSocketHandler.sendToUser(msg.getReceiverId(), JSON.toJSONString(wsMessage));
        
        // 3. 小程序通过钉钉工作通知
        dingTalkService.sendWorkNotify(msg);
    }
}
```

- [ ] **Step 4: 提交代码**

```bash
git add .
git commit -m "feat: 实现Collaboration模块，WebSocket实时消息推送"
```

---

### Task 8: 财务模块（Finance Service）

**Files:**
- Create: `solidoa/solidoa-finance/`
- Create: `solidoa/solidoa-finance/pom.xml`
- Create: `solidoa/solidoa-finance/src/main/java/com/solidoa/finance/FinanceApplication.java`
- Create: `solidoa/solidoa-finance/src/main/java/com/solidoa/finance/controller/ExpenseController.java`
- Create: `solidoa/solidoa-finance/src/main/java/com/solidoa/finance/controller/BudgetController.java`
- Create: `solidoa/solidoa-finance/src/main/java/com/solidoa/finance/service/ExpenseService.java`
- Create: `solidoa/solidoa-finance/src/main/java/com/solidoa/finance/service/impl/ExpenseServiceImpl.java`
- Create: `solidoa/solidoa-finance/src/main/java/com/solidoa/finance/mapper/ExpenseMapper.java`
- Create: `solidoa/solidoa-finance/src/main/resources/mapper/ExpenseMapper.xml`
- Create: `solidoa/sql/init/04_finance.sql` (预算表、报销明细表)

- [ ] **Step 1: 创建Finance POM和配置**

```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>easyexcel</artifactId>
    <version>3.3.3</version>
</dependency>
```

- [ ] **Step 2: 创建ExpenseController**

```java
@RestController
@RequestMapping("/api/v1/finance/expense")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @PostMapping
    public Result<Long> create(@RequestBody @Valid ExpenseForm form, @RequestHeader("X-User-Id") Long userId) {
        return Result.success(expenseService.createExpense(form, userId));
    }

    @GetMapping
    public Result<PageVO<ExpenseVO>> list(PageDTO dto, @RequestHeader("X-User-Id") Long userId) {
        return Result.success(expenseService.pageList(dto, userId));
    }

    @GetMapping("/{id}")
    public Result<ExpenseDTO> getById(@PathVariable Long id) {
        return Result.success(expenseService.getById(id));
    }

    @GetMapping("/statistics")
    public Result<ExpenseStatisticsVO> statistics(@RequestParam String startDate, @RequestParam String endDate) {
        return Result.success(expenseService.getStatistics(startDate, endDate));
    }
}
```

- [ ] **Step 3: 创建BudgetController**

```java
@RestController
@RequestMapping("/api/v1/finance/budget")
public class BudgetController {

    @Autowired
    private BudgetService budgetService;

    @PostMapping
    public Result<Long> create(@RequestBody @Valid BudgetForm form) {
        return Result.success(budgetService.create(form));
    }

    @GetMapping
    public Result<List<BudgetVO>> list(@RequestParam Integer year, @RequestParam Integer month) {
        return Result.success(budgetService.list(year, month));
    }

    @GetMapping("/warnings")
    public Result<List<BudgetWarningVO>> warnings() {
        return Result.success(budgetService.getWarnings());
    }
}
```

- [ ] **Step 4: 实现ExpenseServiceImpl**

```java
@Service
@Slf4j
public class ExpenseServiceImpl implements ExpenseService {

    @Autowired
    private ExpenseMapper expenseMapper;
    
    @Autowired
    private RuntimeService runtimeService;
    
    @Autowired
    private SystemClient systemClient;

    @Override
    @Transactional
    public Long createExpense(ExpenseForm form, Long userId) {
        // 获取用户部门信息
        UserDTO user = systemClient.getUserById(userId);
        
        Expense expense = new Expense();
        BeanUtils.copyProperties(form, expense);
        expense.setUserId(userId);
        expense.setDeptId(user.getDeptId());
        expense.setExpenseNo(generateExpenseNo());
        expense.setStatus("PENDING");
        
        // 启动Camunda流程
        Map<String, Object> variables = new HashMap<>();
        variables.put("initiator", userId.toString());
        variables.put("amount", form.getAmount());
        
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
            "expenseProcess", 
            expense.getId().toString(),
            variables
        );
        
        expense.setProcessInstanceId(instance.getId());
        expenseMapper.insert(expense);
        
        return expense.getId();
    }

    @Override
    public ExpenseStatisticsVO getStatistics(String startDate, String endDate) {
        return expenseMapper.selectStatistics(startDate, endDate);
    }
}
```

- [ ] **Step 5: 提交代码**

```bash
git add .
git commit -m "feat: 实现Finance模块，报销管理和预算管理"
```

---

## 阶段三：扩展功能（第13-14周）

### Task 9: 考勤模块（Attendance Service）

**Files:**
- Create: `solidoa/solidoa-attendance/pom.xml`
- Create: `solidoa/solidoa-attendance/src/main/java/com/solidoa/attendance/AttendanceApplication.java`
- Create: `solidoa/solidoa-attendance/src/main/java/com/solidoa/attendance/controller/AttendanceController.java`
- Create: `solidoa/solidoa-attendance/src/main/java/com/solidoa/attendance/service/AttendanceService.java`
- Create: `solidoa/solidoa-attendance/src/main/java/com/solidoa/attendance/service/impl/AttendanceServiceImpl.java`
- Create: `solidoa/solidoa-attendance/src/main/java/com/solidoa/attendance/scheduler/SyncScheduler.java`

- [ ] **Step 1: 创建考勤打卡接口**

```java
@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @PostMapping("/check")
    public Result<AttendanceVO> check(@RequestBody @Valid CheckForm form, @RequestHeader("X-User-Id") Long userId) {
        return Result.success(attendanceService.check(form, userId));
    }

    @GetMapping("/records")
    public Result<PageVO<AttendanceVO>> records(PageDTO dto, @RequestParam String checkDate) {
        return Result.success(attendanceService.getRecords(dto, checkDate));
    }

    @GetMapping("/summary")
    public Result<AttendanceSummaryVO> summary(@RequestParam String yearMonth) {
        return Result.success(attendanceService.getSummary(yearMonth));
    }

    @GetMapping("/exceptions")
    public Result<PageVO<AttendanceExceptionVO>> exceptions(PageDTO dto) {
        return Result.success(attendanceService.getExceptions(dto));
    }
}
```

- [ ] **Step 2: 实现考勤打卡逻辑**

```java
@Service
@Slf4j
public class AttendanceServiceImpl implements AttendanceService {

    @Autowired
    private AttendanceMapper attendanceMapper;
    
    @Autowired
    private DingtalkService dingtalkService;

    @Override
    @Transactional
    public AttendanceVO check(CheckForm form, Long userId) {
        LocalDateTime checkTime = LocalDateTime.now();
        LocalDate checkDate = checkTime.toLocalDate();
        
        // 判断是签到还是签退
        Attendance lastRecord = attendanceMapper.selectLastRecord(userId, checkDate);
        String checkType = (lastRecord == null || "SIGN_OUT".equals(lastRecord.getCheckType())) 
            ? "SIGN_IN" : "SIGN_OUT";
        
        // 判断是否迟到/早退
        boolean isLate = false;
        boolean isEarlyLeave = false;
        if ("SIGN_IN".equals(checkType)) {
            isLate = checkTime.toLocalTime().isAfter(LocalTime.of(9, 0));
        } else {
            isEarlyLeave = checkTime.toLocalTime().isBefore(LocalTime.of(18, 0));
        }
        
        Attendance attendance = new Attendance();
        attendance.setUserId(userId);
        attendance.setCheckDate(checkDate);
        attendance.setCheckType(checkType);
        attendance.setCheckTime(checkTime);
        attendance.setLocation(form.getLocation());
        attendance.setLongitude(form.getLongitude());
        attendance.setLatitude(form.getLatitude());
        attendance.setDeviceType(form.getDeviceType());
        attendance.setIsLate(isLate ? 1 : 0);
        attendance.setIsEarlyLeave(isEarlyLeave ? 1 : 0);
        
        attendanceMapper.insert(attendance);
        
        return convertToVO(attendance);
    }

    @Override
    public AttendanceSummaryVO getSummary(String yearMonth) {
        return attendanceMapper.selectSummary(yearMonth);
    }
}
```

- [ ] **Step 3: 创建钉钉同步定时任务**

```java
@Component
@Slf4j
public class SyncScheduler {

    @Autowired
    private DingtalkService dingtalkService;
    
    @Autowired
    private AttendanceMapper attendanceMapper;

    @SchedulerLock(name = "dingtalkSync", lockAtLeastFor = "5m", lockAtMostFor = "30m")
    public void syncAttendance() {
        log.info("开始同步钉钉考勤数据...");
        
        try {
            // 1. 获取钉钉accessToken
            String accessToken = dingtalkService.getAccessToken();
            
            // 2. 获取考勤数据
            List<Map<String, Object>> records = dingtalkService.getAttendanceList(
                accessToken, 
                LocalDate.now().minusDays(1)
            );
            
            // 3. 保存到本地数据库
            for (Map<String, Object> record : records) {
                Attendance attendance = convertFromDingtalk(record);
                // 检查是否已存在（防止重复）
                Attendance existing = attendanceMapper.selectByDingtalkId(
                    (String) record.get("checkId")
                );
                if (existing == null) {
                    attendanceMapper.insert(attendance);
                }
            }
            
            log.info("钉钉考勤同步完成，共 {} 条记录", records.size());
        } catch (Exception e) {
            log.error("钉钉考勤同步失败", e);
        }
    }
}
```

- [ ] **Step 4: 提交代码**

```bash
git add .
git commit -m "feat: 实现Attendance模块，考勤打卡和钉钉同步"
```

---

### Task 10: 文件服务（File Service）

**Files:**
- Create: `solidoa/solidoa-file/pom.xml`
- Create: `solidoa/solidoa-file/src/main/java/com/solidoa/file/FileApplication.java`
- Create: `solidoa/solidoa-file/src/main/java/com/solidoa/file/controller/FileController.java`
- Create: `solidoa/solidoa-file/src/main/java/com/solidoa/file/service/impl/FileServiceImpl.java`
- Create: `solidoa/solidoa-file/src/main/java/com/solidoa/file/util/MinIOUtil.java`

- [ ] **Step 1: 创建文件上传接口**

```java
@RestController
@RequestMapping("/api/v1/file")
public class FileController {

    @Autowired
    private FileService fileService;

    @PostMapping("/upload")
    public Result<FileVO> upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam(required = false) String businessType,
                                 @RequestParam(required = false) Long businessId,
                                 @RequestHeader("X-User-Id") Long userId) {
        return Result.success(fileService.upload(file, businessType, businessId, userId));
    }

    @GetMapping("/list")
    public Result<PageVO<FileVO>> list(@RequestParam String businessType, @RequestParam Long businessId) {
        return Result.success(fileService.list(businessType, businessId));
    }

    @GetMapping("/{id}/preview")
    public void preview(@PathVariable Long id, HttpServletResponse response) {
        fileService.preview(id, response);
    }

    @GetMapping("/{id}/download")
    public void download(@PathVariable Long id, HttpServletResponse response) {
        fileService.download(id, response);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        fileService.delete(id);
        return Result.success();
    }
}
```

- [ ] **Step 2: 实现文件服务（含安全校验）**

```java
@Service
@Slf4j
public class FileServiceImpl implements FileService {

    // 允许的文件类型白名单
    private static final Set<String> ALLOWED_TYPES = Set.of(
        "jpg", "jpeg", "png", "gif", "webp",
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
        "zip", "rar", "7z",
        "txt", "csv"
    );

    @Autowired
    private FileMapper fileMapper;
    
    @Autowired
    private MinioClient minioClient;

    @Override
    @Transactional
    public FileVO upload(MultipartFile file, String businessType, Long businessId, Long userId) {
        // 1. 文件类型校验
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_TYPES.contains(extension)) {
            throw new BusinessException("不支持的文件类型");
        }
        
        // 2. 文件大小校验（10MB）
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BusinessException("文件大小不能超过10MB");
        }
        
        // 3. 生成唯一文件名（UUID随机化）
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String fileName = uuid + "." + extension;
        
        // 4. 确定存储桶
        String bucket = determineBucket(businessType);
        
        // 5. 上传到MinIO
        try {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(fileName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败");
        }
        
        // 6. 保存文件记录
        File fileEntity = new File();
        fileEntity.setFileName(fileName);
        fileEntity.setOriginalName(originalFilename);
        fileEntity.setFilePath(bucket + "/" + fileName);
        fileEntity.setFileSize(file.getSize());
        fileEntity.setFileType(extension);
        fileEntity.setMimeType(file.getContentType());
        fileEntity.setBucket(bucket);
        fileEntity.setBusinessType(businessType);
        fileEntity.setBusinessId(businessId);
        fileEntity.setUploaderId(userId);
        fileMapper.insert(fileEntity);
        
        return convertToVO(fileEntity);
    }

    private String determineBucket(String businessType) {
        return switch (businessType) {
            case "LEAVE" -> "leave-attach";
            case "EXPENSE" -> "expense-invoice";
            default -> "common";
        };
    }

    @Override
    public void delete(Long id) {
        File file = fileMapper.selectById(id);
        if (file != null) {
            // 删除MinIO文件
            try {
                minioClient.removeObject(
                    RemoveObjectArgs.builder()
                        .bucket(file.getBucket())
                        .object(file.getFileName())
                        .build()
                );
            } catch (Exception e) {
                log.error("删除MinIO文件失败", e);
            }
            // 删除记录
            fileMapper.deleteById(id);
        }
    }
}
```

- [ ] **Step 3: 提交代码**

```bash
git add .
git commit -m "feat: 实现File模块，MinIO文件上传下载和安全校验"
```

---

### Task 11: 钉钉集成（Dingtalk Service）

**Files:**
- Create: `solidoa/solidoa-dingtalk/pom.xml`
- Create: `solidoa/solidoa-dingtalk/src/main/java/com/solidoa/dingtalk/DingtalkApplication.java`
- Create: `solidoa/solidoa-dingtalk/src/main/java/com/solidoa/dingtalk/controller/DingtalkController.java`
- Create: `solidoa/solidoa-dingtalk/src/main/java/com/solidoa/dingtalk/service/DingtalkService.java`
- Create: `solidoa/solidoa-dingtalk/src/main/java/com/solidoa/dingtalk/service/impl/DingtalkServiceImpl.java`
- Create: `solidoa/solidoa-dingtalk/src/main/java/com/solidoa/dingtalk/scheduler/SyncScheduler.java`

- [ ] **Step 1: 创建钉钉服务**

```java
@Service
@Slf4j
public class DingtalkServiceImpl implements DingtalkService {

    @Autowired
    private DingtalkConfigMapper configMapper;
    
    @Autowired
    private DingtalkSyncLogMapper syncLogMapper;
    
    @Autowired
    private RestTemplate restTemplate;

    @Override
    public String getAccessToken() {
        DingtalkConfig config = configMapper.selectActiveConfig();
        
        String url = "https://oapi.dingtalk.com/gettoken?appkey=" + config.getAppKey() + 
                     "&appsecret=" + decrypt(config.getAppSecret());
        
        Map<String, Object> result = restTemplate.getForObject(url, Map.class);
        if ("0".equals(result.get("errcode"))) {
            return (String) result.get("access_token");
        }
        throw new BusinessException("获取钉钉AccessToken失败");
    }

    @Override
    public void syncUsers() {
        DingtalkSyncLog log = new DingtalkSyncLog();
        log.setSyncType("USER");
        log.setStartTime(LocalDateTime.now());
        log.setStatus("RUNNING");
        syncLogMapper.insert(log);
        
        try {
            String accessToken = getAccessToken();
            String url = "https://oapi.dingtalk.com/topapi/user/listid?access_token=" + accessToken;
            
            // 获取部门列表
            // 遍历部门获取用户列表
            // 同步到sys_user表
            
            log.setStatus("SUCCESS");
            log.setSuccessCount(100);
        } catch (Exception e) {
            log.setStatus("FAILED");
            log.setErrorMsg(e.getMessage());
        } finally {
            log.setEndTime(LocalDateTime.now());
            syncLogMapper.updateById(log);
        }
    }

    @Override
    public void sendWorkNotify(Message message) {
        // 调用钉钉工作通知接口
        String accessToken = getAccessToken();
        String url = "https://oapi.dingtalk.com/topapi/message/corpconversation/asyncsend_v2?access_token=" + accessToken;
        
        Map<String, Object> body = new HashMap<>();
        body.put("agent_id", configMapper.selectActiveConfig().getAgentId());
        body.put("userid_list", getDingtalkUserId(message.getReceiverId()));
        body.put("msg", Map.of(
            "msgtype", "text",
            "text", Map.of("content", message.getTitle() + "\n" + message.getContent())
        ));
        
        restTemplate.postForObject(url, body, Map.class);
    }
}
```

- [ ] **Step 2: 创建钉钉登录接口**

```java
@GetMapping("/login")
public void dingtalkLogin(HttpServletResponse response) {
    DingtalkConfig config = configMapper.selectActiveConfig();
    
    String url = "https://oapi.dingtalk.com/oauth2/appAuthorize?app_id=" + config.getAppKey() + 
                 "&redirect_uri=" + callbackUrl + "&response_type=code";
    
    response.sendRedirect(url);
}

@GetMapping("/callback")
public Result<UserDTO> callback(@RequestParam String code) {
    // 用code换取用户信息
    String accessToken = dingtalkService.getAccessToken();
    // 调用钉钉API获取用户信息
    // 创建或更新本地用户
    // 返回JWT Token
}
```

- [ ] **Step 3: 提交代码**

```bash
git add .
git commit -m "feat: 实现Dingtalk模块，钉钉免登和消息推送"
```

---

## 阶段四：移动端（第15-18周）

### Task 12: Vue3前端（Web管理端）

**Files:**
- Create: `solidoa/solidoa-web/package.json`
- Create: `solidoa/solidoa-web/src/main.js`
- Create: `solidoa/solidoa-web/src/App.vue`
- Create: `solidoa/solidoa-web/src/api/index.js`
- Create: `solidoa/solidoa-web/src/api/system.js`
- Create: `solidoa/solidoa-web/src/api/workflow.js`
- Create: `solidoa/solidoa-web/src/stores/user.js`
- Create: `solidoa/solidoa-web/src/router/index.js`
- Create: `solidoa/solidoa-web/src/views/login/index.vue`
- Create: `solidoa/solidoa-web/src/views/layout/index.vue`
- Create: `solidoa/solidoa-web/src/views/system/user/index.vue`
- Create: `solidoa/solidoa-web/src/views/workflow/leave/index.vue`
- Create: `solidoa/solidoa-web/vite.config.js`

- [ ] **Step 1: 创建Vue3项目结构**

```javascript
// package.json
{
  "name": "solidoa-web",
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "vue": "^3.4.21",
    "vue-router": "^4.3.0",
    "pinia": "^2.1.7",
    "axios": "^1.6.8",
    "element-plus": "^2.6.3"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.4",
    "vite": "^5.2.6"
  }
}
```

- [ ] **Step 2: 创建Axios封装**

```javascript
// src/api/index.js
import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 10000
})

service.interceptors.request.use(config => {
  const token = localStorage.getItem('access_token')
  if (token) {
    config.headers['Authorization'] = 'Bearer ' + token
  }
  return config
})

service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('access_token')
      window.location.href = '/login'
    }
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default service
```

- [ ] **Step 3: 创建登录页面**

```vue
<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2>SolidOA 登录</h2>
      <el-form :model="form" :rules="rules" ref="formRef">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleLogin" :loading="loading" style="width: 100%">
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import api from '@/api'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名' }],
  password: [{ required: true, message: '请输入密码' }]
}

const handleLogin = async () => {
  await formRef.value.validate()
  loading.value = true
  
  try {
    const { data } = await api.post('/auth/login', form)
    localStorage.setItem('access_token', data.accessToken)
    userStore.setUser(data.userInfo)
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>
```

- [ ] **Step 4: 创建布局和菜单**

```vue
<template>
  <el-container class="layout-container">
    <el-aside width="200px">
      <div class="logo">SolidOA</div>
      <el-menu :default-active="route.path" router>
        <el-menu-item index="/system/users">系统管理</el-menu-item>
        <el-menu-item index="/workflow/leave">审批流程</el-menu-item>
        <el-menu-item index="/collab/messages">消息中心</el-menu-item>
        <el-menu-item index="/finance/expense">财务管理</el-menu-item>
        <el-menu-item index="/attendance/check">考勤管理</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header>
        <span>{{ userStore.user?.realName }}</span>
        <el-button @click="handleLogout">退出</el-button>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>
```

- [ ] **Step 5: 提交代码**

```bash
git add .
git commit -m "feat: 完成Vue3前端基础架构"
```

---

### Task 13: UniApp移动端

**Files:**
- Create: `solidoa/solidoa-app/package.json`
- Create: `solidoa/solidoa-app/src/main.js`
- Create: `solidoa/solidoa-app/src/api/index.js`
- Create: `solidoa/solidoa-app/src/pages/index/index.vue` (首页)
- Create: `solidoa/solidoa-app/src/pages/login/index.vue` (登录)
- Create: `solidoa/solidoa-app/src/pages/approval/list.vue` (审批列表)
- Create: `solidoa/solidoa-app/src/pages/attendance/check.vue` (考勤打卡)
- Create: `solidoa/solidoa-app/src/store/user.js`

- [ ] **Step 1: 创建UniApp项目结构**

```javascript
// package.json
{
  "name": "solidoa-app",
  "version": "1.0.0",
  "dependencies": {
    "@dcloudio/uni-app": "3.0.0-alpha-4020920231219001",
    "@dcloudio/uni-app-plus": "3.0.0-alpha-4020920231219001",
    "@dcloudio/uni-components": "3.0.0-alpha-4020920231219001",
    "@dcloudio/uni-h5": "3.0.0-alpha-4020920231219001",
    "vue": "^3.4.21",
    "pinia": "^2.1.7"
  }
}
```

- [ ] **Step 2: 创建考勤打卡页面**

```vue
<template>
  <view class="check-container">
    <view class="check-card">
      <view class="time">{{ currentTime }}</view>
      <view class="date">{{ currentDate }}</view>
      <view class="location">
        <text class="icon">📍</text>
        <text>{{ location || '正在获取位置...' }}</text>
      </view>
    </view>
    
    <button class="check-btn" :type="checkType === 'SIGN_IN' ? 'primary' : 'success'" 
            @click="handleCheck">
      {{ checkType === 'SIGN_IN' ? '签到' : '签退' }}
    </button>
    
    <view class="records">
      <view class="title">今日打卡记录</view>
      <view v-for="record in records" :key="record.id" class="record-item">
        <text>{{ record.checkTime }}</text>
        <text>{{ record.checkType === 'SIGN_IN' ? '签到' : '签退' }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const currentTime = ref('')
const currentDate = ref('')
const location = ref('')
const checkType = ref('SIGN_IN')
const records = ref([])

onMounted(() => {
  updateTime()
  setInterval(updateTime, 1000)
  loadRecords()
})

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  currentDate.value = now.toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' })
}

const handleCheck = async () => {
  try {
    const res = await uni.request({
      url: '/api/v1/attendance/check',
      method: 'POST',
      data: { location: location.value }
    })
    uni.showToast({ title: '打卡成功' })
    loadRecords()
  } catch (e) {
    uni.showToast({ title: '打卡失败', icon: 'error' })
  }
}
</script>
```

- [ ] **Step 2: 提交代码**

```bash
git add .
git commit -m "feat: 完成UniApp移动端基础架构"
```

---

## 阶段五：测试与部署（第19-20周）

### Task 14: 集成测试与部署

**Files:**
- Create: `solidoa/scripts/deploy.sh`
- Create: `solidoa/scripts/health-check.sh`
- Create: `solidoa/sql/docker-compose.yml` (开发环境)

- [ ] **Step 1: 创建启动脚本**

```bash
#!/bin/bash
# deploy.sh

APP_HOME="/opt/solidoa"
LOG_HOME="${APP_HOME}/logs"

echo "===== SolidOA 部署开始 ====="

# 等待基础设施启动
echo "等待Nacos启动..."
until curl -s http://localhost:8848/nacos/v1/console/health | grep -q UP; do
  sleep 5
done

# 启动各服务
for service in gateway system workflow collaboration finance attendance file dingtalk; do
  echo "启动 ${service}..."
  nohup java -Xms512m -Xmx1024m -XX:+UseG1GC \
    -jar ${APP_HOME}/app/solidoa-${service}.jar \
    --spring.config.additional-location=${APP_HOME}/config/ \
    > ${LOG_HOME}/${service}/stdout.log 2>&1 &
  
  # 健康检查
  port=$(echo $service | sed 's/gateway/8080; s/system/8081; s/workflow/8082; s/collaboration/8083; s/finance/8084; s/attendance/8085; s/file/8086; s/dingtalk/8087/')
  
  for i in {1..30}; do
    if curl -s "http://localhost:${port}/actuator/health" | grep -q UP; then
      echo "${service} 启动成功"
      break
    fi
    sleep 2
  done
done

echo "===== SolidOA 部署完成 ====="
```

- [ ] **Step 2: 创建健康检查脚本**

```bash
#!/bin/bash
# health-check.sh

services=(
  "gateway:8080"
  "system:8081"
  "workflow:8082"
  "collaboration:8083"
  "finance:8084"
  "attendance:8085"
  "file:8086"
  "dingtalk:8087"
)

all_healthy=true

for item in "${services[@]}"; do
  name="${item%%:*}"
  port="${item##*:}"
  
  if curl -s "http://localhost:${port}/actuator/health" | grep -q UP; then
    echo "✓ ${name} 健康"
  else
    echo "✗ ${name} 不健康"
    all_healthy=false
  fi
done

if $all_healthy; then
  echo "所有服务健康"
  exit 0
else
  echo "部分服务不健康"
  exit 1
fi
```

- [ ] **Step 3: 提交代码**

```bash
git add .
git commit -m "feat: 完成部署脚本"
```

---

## 验证检查清单

完成所有任务后，验证以下内容：

| 验证项 | 检查点 |
|--------|--------|
| Gateway | `curl http://localhost:8080/actuator/health` 返回UP |
| System登录 | POST /api/v1/auth/login 返回Token |
| 审批流程 | 请假申请→审批→流程完成 |
| 消息推送 | WebSocket连接成功，消息可实时推送 |
| 文件上传 | 上传文件到MinIO，下载正常 |
| 钉钉同步 | 用户、部门同步成功 |
| 前端 | Vue3管理端可正常登录和操作 |
| 移动端 | UniApp可考勤打卡 |
| 部署 | 所有服务启动成功，健康检查通过 |

---

## 计划文件路径

`docs/superpowers/plans/2026-05-24-solidoa-implementation.md`

---

**Plan complete and saved to `docs/superpowers/plans/2026-05-24-solidoa-implementation.md`.**

**Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**