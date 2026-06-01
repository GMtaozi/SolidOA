# Multi-stage build for Spring Boot application
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# 复制根 POM
COPY solidoa/pom.xml solidoa/

# 复制各模块 POM
COPY solidoa/solidoa-parent/pom.xml solidoa/solidoa-parent/
COPY solidoa/solidoa-parent/solidoa-common/pom.xml solidoa/solidoa-parent/solidoa-common/
COPY solidoa/solidoa-parent/solidoa-gateway/pom.xml solidoa/solidoa-parent/solidoa-gateway/
COPY solidoa/solidoa-parent/solidoa-system/pom.xml solidoa/solidoa-parent/solidoa-system/
COPY solidoa/solidoa-parent/solidoa-workflow/pom.xml solidoa/solidoa-parent/solidoa-workflow/
COPY solidoa/solidoa-parent/solidoa-hr/pom.xml solidoa/solidoa-parent/solidoa-hr/
COPY solidoa/solidoa-parent/solidoa-file/pom.xml solidoa/solidoa-parent/solidoa-file/
COPY solidoa/solidoa-parent/solidoa-dingtalk/pom.xml solidoa/solidoa-parent/solidoa-dingtalk/

# 复制源代码
COPY solidoa/solidoa-parent/solidoa-common/src solidoa/solidoa-parent/solidoa-common/src
COPY solidoa/solidoa-parent/solidoa-gateway/src solidoa/solidoa-parent/solidoa-gateway/src
COPY solidoa/solidoa-parent/solidoa-system/src solidoa/solidoa-parent/solidoa-system/src
COPY solidoa/solidoa-parent/solidoa-workflow/src solidoa/solidoa-parent/solidoa-workflow/src
COPY solidoa/solidoa-parent/solidoa-hr/src solidoa/solidoa-parent/solidoa-hr/src
COPY solidoa/solidoa-parent/solidoa-file/src solidoa/solidoa-parent/solidoa-file/src
COPY solidoa/solidoa-parent/solidoa-dingtalk/src solidoa/solidoa-parent/solidoa-dingtalk/src

# 编译
RUN cd solidoa && mvn package -DskipTests -pl solidoa-parent -am
