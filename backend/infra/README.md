# 本地依赖服务（Docker Compose）

本目录是代驾项目的本地依赖服务编排，**只依赖本目录和仓库根目录的已有文件**，不修改 `daijia-parent` 下任何源码。

## 启动 / 停止 / 查看

```powershell
# 启动（后台常驻）
docker compose -f infra\docker-compose.yml up -d

# 查看状态（全部 Up/healthy）
docker compose -f infra\docker-compose.yml ps

# 停止（保留数据卷）
docker compose -f infra\docker-compose.yml stop

# 重新初始化数据库（⚠️ 会删除所有数据卷）
docker compose -f infra\docker-compose.yml down -v
```

也可以直接用封装脚本：`infra\scripts\start.ps1`、`infra\scripts\stop.ps1`、`infra\scripts\verify.ps1`（验收）。

## 连接信息（与 daijia-parent/DEFAULT_GROUP 下配置一致）

| 服务 | 容器名 | 宿主机地址 | 账号 / 密码 | 备注 |
| --- | --- | --- | --- | --- |
| MySQL | `roadrescue-mysql` | `127.0.0.1:3306` | `root` / `shangguigu@QY123` | 8 个业务库 + `xxl_job` |
| Redis | `roadrescue-redis` | `127.0.0.1:6379` | 无密码，db 0 | appendonly 开启 |
| RabbitMQ | `roadrescue-rabbitmq` | `127.0.0.1:5672`，管理页 `http://localhost:15672` | `admin` / `admin123`，vhost `/` | 已启用 `rabbitmq_delayed_message_exchange` |
| MongoDB | `roadrescue-mongo` | `127.0.0.1:27017` | 无认证，业务库 `daijia` | |
| Nacos | `roadrescue-nacos` | `http://127.0.0.1:8848/nacos`，客户端 gRPC `127.0.0.1:9848` | standalone、无认证 | 2.3.0 |
| XXL-JOB admin | `roadrescue-xxl-job-admin` | `http://127.0.0.1:8080/xxl-job-admin` | `admin` / `123456`，accessToken `default_token` | 2.3.0，库 `xxl_job` |

## 首次初始化

MySQL 数据卷为空时，官方镜像会自动按文件名顺序执行 `/docker-entrypoint-initdb.d/` 下的脚本：

1. `01-daijia.sql` ← 仓库根目录 `daijia.sql`（8 个业务库，整包执行，含 `CREATE DATABASE`/`USE`）
2. `02-xxl-job.sql` ← `xxl-job-master/doc/db/tables_xxl_job.sql`（v2.3.0 表结构）

脚本只执行一次；如果初始化失败或想重新灌数据，先 `down -v` 清卷再 `up -d`。

验证结果（首次启动实测）：`daijia_coupon` 3 表、`daijia_customer` 3 表、`daijia_dispatch` 3 表、`daijia_driver` 7 表、`daijia_order` 9 表、`daijia_payment` 3 表、`daijia_rule` 5 表、`daijia_system` 10 表，`xxl_job` 8 表。

## 关键细节 / 踩坑记录

- **MySQL 8 认证**：启动参数固定 `--default-authentication-plugin=mysql_native_password`。因为服务端 JDBC URL（如 `jdbc:mysql://127.0.0.1:3306/daijia_order?characterEncoding=utf-8&useSSL=false`）没有 `allowPublicKeyRetrieval=true`，用默认的 `caching_sha2_password` 会报 `Public Key Retrieval is not allowed`。
- **MySQL 时区**：`--default-time-zone=+08:00`，避免 `serverTimezone` 缺失时的时差问题；容器 TZ 也设为 `Asia/Shanghai`。
- **RabbitMQ 延迟插件**：`.ez` 挂载到 `/opt/rabbitmq/plugins/`（镜像里 `/plugins` 是它的软链接），并用 `infra/rabbitmq/enabled_plugins` 覆盖 `/etc/rabbitmq/enabled_plugins`，显式启用 `rabbitmq_management` + `rabbitmq_delayed_message_exchange`，偏移插件版本 3.9.0 在 RabbitMQ 3.9.29 上实测可正常加载。
- **RabbitMQ 凭据**：必须用 `RABBITMQ_DEFAULT_USER/PASS` 创建 `admin/admin123`；内置 `guest` 只能从 `localhost`（容器内）登录，所以不能用 guest。
- **Nacos 2.x**：除 8848 外必须映射 **9848（gRPC）**，否则 Java 客户端启动失败；`MODE=standalone` 使用内嵌 Derby 存储，无需 MySQL。
- **XXL-JOB admin 2.3.0**：通过环境变量 `PARAMS` 传 `--spring.datasource.url/username/password --xxl.job.accessToken`；必须等 MySQL healthy 后再启动（`depends_on: condition: service_healthy`）。镜像里没有 curl/wget，健康检查改用 `infra/scripts/xxl-job-admin-healthcheck.sh`（bash `/dev/tcp` 请求 `/xxl-job-admin/toLogin`，该路径返回 200，而 `/xxl-job-admin/` 是 302 跳转）。
- **数据持久化**：MySQL/Redis/RabbitMQ/Mongo/Nacos 用 Docker 命名卷；Nacos 与 XXL-JOB 日志写 `infra/data/`。
