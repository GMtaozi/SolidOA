# SolidOA 部署文档

## 环境要求

### 硬件要求
- CPU: 4核+
- 内存: 8GB+
- 磁盘: 100GB+

### 软件要求
| 软件 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | 必须 |
| Maven | 3.9+ | 编译构建 |
| MySQL | 8.0+ | 主数据库 |
| Redis | 7.x | 缓存/会话 |
| RabbitMQ | 3.x | 消息队列 |
| MinIO | 最新 | 对象存储(可选) |

---

## 方式一：Docker Compose 部署（推荐）

### 1. 启动基础设施

```bash
cd solidoa/docker

# 启动 MySQL、Redis、RabbitMQ、MinIO
docker-compose up -d
```

### 2. 初始化数据库

```bash
# 创建数据库
mysql -h localhost -u root -p < ../sql/init/00_init_databases.sql

# 导入表结构和数据
mysql -h localhost -u root -p oa_system < ../sql/init/01_system.sql
mysql -h localhost -u root -p oa_workflow < ../sql/init/02_workflow.sql
mysql -h localhost -u root -p oa_collaboration < ../sql/init/03_collaboration.sql
mysql -h localhost -u root -p oa_finance < ../sql/init/04_finance.sql
mysql -h localhost -u root -p oa_attendance < ../sql/init/05_attendance.sql
mysql -h localhost -u root -p oa_common < ../sql/init/06_common.sql
mysql -h localhost -u root -p oa_system < ../sql/init/07_init_data.sql
```

### 3. 修改配置

编辑各服务的 `application.yml`，配置数据库连接：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/oa_system?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
```

### 4. 编译项目

```bash
cd solidoa
mvn clean package -DskipTests
```

### 5. 启动服务

```bash
cd scripts

# Linux/Mac
chmod +x deploy.sh
./deploy.sh

# Windows
start-services.bat
```

---

## 方式二：手动部署

### 1. 安装依赖服务

#### MySQL 8.0

```bash
# Ubuntu
sudo apt update
sudo apt install mysql-server

# 启动服务
sudo systemctl start mysql
sudo systemctl enable mysql

# 配置远程访问
sudo mysql
CREATE USER 'solidoa'@'%' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON *.* TO 'solidoa'@'%';
FLUSH PRIVILEGES;
```

#### Redis 7.x

```bash
# Ubuntu
sudo apt install redis-server

# 启动服务
sudo systemctl start redis
sudo systemctl enable redis
```

#### RabbitMQ 3.x

```bash
# Ubuntu
echo 'deb http://www.rabbitmq.com/debian/ bullseye main' | sudo tee /etc/apt/sources.list.d/rabbitmq.list
wget -O- https://www.rabbitmq.com/rabbitmq-signing-key-public.asc | sudo apt-key add -
sudo apt update
sudo apt install rabbitmq-server

# 启动服务
sudo systemctl start rabbitmq-server
sudo systemctl enable rabbitmq-server

# 创建用户和虚拟主机
sudo rabbitmqctl add_user solidoa your_password
sudo rabbitmqctl set_permissions -p / solidoa ".*" ".*" ".*"
```

#### MinIO（可选）

```bash
# 下载
wget https://dl.min.io/server/minio/release/linux-amd64/minio
chmod +x minio

# 启动
MINIO_ROOT_USER=solidoa MINIO_ROOT_PASSWORD=minioadmin123 ./minio server /data &
```

### 2. 创建数据库

```bash
mysql -u root -p < sql/init/00_init_databases.sql
mysql -u root -p oa_system < sql/init/01_system.sql
mysql -u root -p oa_workflow < sql/init/02_workflow.sql
mysql -u root -p oa_collaboration < sql/init/03_collaboration.sql
mysql -u root -p oa_finance < sql/init/04_finance.sql
mysql -u root -p oa_attendance < sql/init/05_attendance.sql
mysql -u root -p oa_common < sql/init/06_common.sql
mysql -u root -p oa_system < sql/init/07_init_data.sql
```

### 3. 编译项目

```bash
cd solidoa
mvn clean package -DskipTests -pl solidoa-parent -am
```

### 4. 配置服务

在各服务的 `application.yml` 中配置：

```yaml
server:
  port: 8081  # 各服务端口不同

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/oa_system?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
  
  redis:
    host: localhost
    port: 6379
    password: your_password

  rabbitmq:
    host: localhost
    port: 5672
    username: solidoa
    password: your_password
```

### 5. 启动服务

```bash
# 启动顺序：common -> gateway -> system -> 其他服务

# 后台启动各服务
nohup java -jar solidoa-parent/solidoa-common/target/solidoa-common-1.0.0-SNAPSHOT.jar &
nohup java -jar solidoa-parent/solidoa-gateway/target/solidoa-gateway-1.0.0-SNAPSHOT.jar &
nohup java -jar solidoa-parent/solidoa-system/target/solidoa-system-1.0.0-SNAPSHOT.jar &
nohup java -jar solidoa-parent/solidoa-workflow/target/solidoa-workflow-1.0.0-SNAPSHOT.jar &
nohup java -jar solidoa-parent/solidoa-collaboration/target/solidoa-collaboration-1.0.0-SNAPSHOT.jar &
nohup java -jar solidoa-parent/solidoa-finance/target/solidoa-finance-1.0.0-SNAPSHOT.jar &
nohup java -jar solidoa-parent/solidoa-attendance/target/solidoa-attendance-1.0.0-SNAPSHOT.jar &
nohup java -jar solidoa-parent/solidoa-file/target/solidoa-file-1.0.0-SNAPSHOT.jar &
```

---

## 服务列表与端口

| 服务 | 端口 | 健康检查 |
|------|------|----------|
| solidoa-gateway | 8080 | http://localhost:8080/actuator/health |
| solidoa-system | 8081 | http://localhost:8081/actuator/health |
| solidoa-workflow | 8082 | http://localhost:8082/actuator/health |
| solidoa-collaboration | 8083 | http://localhost:8083/actuator/health |
| solidoa-finance | 8084 | http://localhost:8084/actuator/health |
| solidoa-attendance | 8085 | http://localhost:8085/actuator/health |
| solidoa-file | 8086 | http://localhost:8086/actuator/health |
| solidoa-dingtalk | 8087 | http://localhost:8087/actuator/health |

---

## 验证部署

### 1. 检查服务状态

```bash
# Linux/Mac
./scripts/health-check.sh

# Windows
test-api.bat
```

### 2. API 测试

```bash
# 用户服务
curl http://localhost:8081/api/v1/system/users/1

# 工作流服务
curl http://localhost:8082/api/v1/workflow/leave

# 财务服务
curl http://localhost:8084/api/v1/finance/budget
```

### 3. 日志位置

```
solidoa-parent/logs/
├── system-service.log
├── workflow-service.log
├── collaboration-service.log
├── finance-service.log
├── attendance-service.log
├── file-service.log
└── gateway.log
```

---

## 生产环境注意事项

### 1. 安全配置

- 修改数据库密码
- 配置 Redis 密码
- 配置 RabbitMQ 用户权限
- 开启防火墙，仅开放必要端口

### 2. JVM 调优

```bash
java -Xms512m -Xmx1024m -XX:+UseG1GC \
     -jar solidoa-system-1.0.0-SNAPSHOT.jar
```

### 3. 负载均衡

使用 Nginx 配置多实例负载均衡：

```nginx
upstream solidoa-backend {
    server 127.0.0.1:8081;
    server 127.0.0.1:8081_backup;
}

server {
    listen 80;
    location / {
        proxy_pass http://solidoa-backend;
    }
}
```

### 4. 监控告警

配置 Prometheus + Grafana 监控：

- JVM 内存使用
- 数据库连接池
- RabbitMQ 队列深度
- 接口响应时间

### 5. 定时备份

```bash
# 数据库备份
mysqldump -u root -p oa_system > backup_$(date +%Y%m%d).sql

# 配置文件备份
tar -czf config_backup.tar.gz solidoa-parent/*/src/main/resources/
```

---

## 常见问题

### 1. 服务启动失败

检查日志：
```bash
tail -f logs/system-service.log
```

常见原因：
- 端口被占用
- 数据库连接失败
- Redis/MQ 连接失败

### 2. 前端无法访问后端

检查网关配置：
```bash
curl http://localhost:8080/actuator/routes
```

### 3. 文件上传失败

检查 MinIO 配置或本地存储目录权限

---

## 卸载

```bash
# 停止所有服务
./scripts/stop.sh

# 删除数据库
mysql -u root -p -e "DROP DATABASE oa_system;DROP DATABASE oa_workflow;DROP DATABASE oa_collaboration;DROP DATABASE oa_finance;DROP DATABASE oa_attendance;DROP DATABASE oa_common;"

# 删除镜像（Docker部署）
docker-compose down --rmi all
```
