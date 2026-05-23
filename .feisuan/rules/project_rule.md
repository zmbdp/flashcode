
# 开发规范指南

本项目基于 **Spring Cloud Alibaba 微服务架构**，采用 **Maven 多模块** 管理。为保证代码质量、可维护性、安全性与可扩展性，请在开发过程中严格遵循以下规范。

## 一、项目基础信息

- **工作目录**：`D:\flashcode`
- **操作系统**：Windows 11
- **构建工具**：Maven 3.x
- **主框架**：Spring Boot 3.3.3 + Spring Cloud 2023.0.3 + Spring Cloud Alibaba 2023.0.1.2
- **语言版本**：JDK 21.0.11 (编译配置为 Java 17 兼容模式，建议实际开发使用 JDK 21 特性)
- **代码作者**：Win11
- **注释语言**：中文 (First Language)

### 1.1 目录结构规范

项目采用标准的 Maven 多模块结构，根目录为 `flashcode`。各业务模块（admin, file, portal, mstemplate）统一遵循 `api` 与 `service` 分离的结构。公共模块位于 `zmbdp-common`。

```text
flashcode/
├── deploy/                  # 部署相关配置 (Docker, Nacos, MySQL, SkyWalking等)
├── docs/                    # 项目文档
├── javapro/javadoc/         # JavaDoc 输出目录
├── zmbdp-common/            # 公共模块集合
│   ├── zmbdp-common-core/   # 核心工具类、常量、基础DTO
│   ├── zmbdp-common-redis/  # Redis & Redisson 封装
│   ├── zmbdp-common-cache/  # Caffeine 本地缓存封装
│   ├── zmbdp-common-security/# 安全认证、JWT处理
│   ├── zmbdp-common-log/    # 日志切面、异步日志
│   ├── zmbdp-common-rabbitmq/# RabbitMQ 配置与工具
│   ├── zmbdp-common-xxljob/ # XXL-JOB 分布式任务调度
│   ├── zmbdp-common-idempotent/# 幂等性控制
│   ├── zmbdp-common-ratelimit/# 限流控制
│   ├── zmbdp-common-datapermission/# 数据权限控制
│   ├── zmbdp-common-message/# 消息通知 (短信等)
│   ├── zmbdp-common-excel/  # Excel 导入导出工具
│   └── zmbdp-common-domain/# 通用领域对象 (DO/VO/DTO定义)
├── zmbdp-admin/             # 后台管理服务
│   ├── zmbdp-admin-api/     # Feign 接口定义、DTO/VO
│   └── zmbdp-admin-service/ # 业务实现、Controller、Mapper
├── zmbdp-file/              # 文件服务
│   ├── zmbdp-file-api/
│   └── zmbdp-file-service/
├── zmbdp-portal/            # 前台门户服务
│   ├── zmbdp-portal-api/
│   └── zmbdp-portal-service/
├── zmbdp-mstemplate/        # 模板服务
│   ├── zmbdp-mstemplate-api/
│   └── zmbdp-mstemplate-service/
└── zmbdp-gateway/           # 网关服务
```

### 1.2 模块内部包结构规范

所有 `*-service` 模块内部遵循以下包结构：

```text
com.zmbdp.[module].service
├── config/                  # 配置类 (Nacos, Web, Redis等)
├── controller/              # REST 接口层
├── domain/                  # 领域模型
│   ├── dto/                 # 数据传输对象
│   ├── entity/              # 数据库实体 (MyBatis-Plus Entity)
│   └── vo/                  # 视图对象
├── mapper/                  # MyBatis Mapper 接口
└── service/                 # 业务逻辑层
    ├── impl/                # 业务实现类
    └── [optional]/          # 其他子业务逻辑
```

所有 `*-api` 模块内部遵循以下包结构：

```text
com.zmbdp.[module].api
├── domain/                  # DTO/VO 定义
│   ├── dto/
│   └── vo/
└── feign/                   # Feign Client 接口定义
```

## 二、技术栈与依赖规范

### 2.1 核心依赖版本管理

所有依赖版本必须在根 `pom.xml` 的 `<dependencyManagement>` 中统一管理，子模块引用时**不指定版本号**。

- **Spring Boot**: `3.3.3`
- **Spring Cloud**: `2023.0.3`
- **Spring Cloud Alibaba**: `2023.0.1.2`
- **MyBatis-Plus**: `3.5.7` (Spring Boot 3 Starter)
- **Redisson**: `3.29.0`
- **Hutool**: `5.8.25`
- **JWT**: `0.11.5`
- **Alibaba EasyExcel**: `3.2.1`
- **XXL-JOB**: `2.4.2`
- **SkyWalking**: `9.0.0`
- **Spring AI Alibaba**: `1.1.0.0-RC2`

### 2.2 数据库访问规范

- **ORM 框架**：使用 `MyBatis-Plus` (Spring Boot 3 Starter)。
- **禁止**：禁止使用原生 JDBC 或 MyBatis 原生 XML 进行复杂查询，优先使用 MyBatis-Plus 的 `BaseMapper` 和 `IService`。
- **实体映射**：
  - Entity 包名统一为 `entity`。
  - 使用 `@TableName` 指定表名。
  - 使用 `@TableId` 指定主键策略（推荐使用雪花算法 `IdType.ASSIGN_ID`，依赖 `zmbdp-common-snowflake`）。
- **N+1 查询**：严禁在循环中查询数据库，必须使用 `@Select` 的 `@Results` 或 `@Mapper` 中的关联查询，或使用 MyBatis-Plus 的 `lambdaQuery().in()` 批量查询。

### 2.3 缓存规范

- **多级缓存**：
  - **L1 本地缓存**：使用 `Caffeine` (通过 `zmbdp-common-cache`) 存储高频、小数据、低敏感数据。
  - **L2 分布式缓存**：使用 `Redis` (通过 `zmbdp-common-redis`) 存储共享数据。
- **Redisson**：对于分布式锁、分布式集合等高级特性，使用 `Redisson`。
- **缓存穿透/击穿/雪崩**：必须实现相应的防护策略（如布隆过滤器、互斥锁、随机过期时间）。

### 2.4 消息队列与异步

- **RabbitMQ**：使用 `zmbdp-common-rabbitmq` 进行消息发送与接收。
- **异步处理**：使用 `@Async` 或线程池处理非核心业务逻辑，避免阻塞主线程。

## 三、分层架构与开发约束

### 3.1 分层职责

| 层级 | 职责说明 | 开发约束 |
| :--- | :--- | :--- |
| **Controller** | 处理 HTTP 请求，参数校验，返回统一结果 | 1. 必须返回 `Result<T>` 统一响应对象。<br>2. 使用 `@Validated` 进行参数校验。<br>3. 不包含业务逻辑。 |
| **Service** | 业务逻辑编排，事务管理，调用 Mapper/Feign | 1. 接口与实现分离，实现类包名 `impl`。<br>2. 复杂事务使用 `@Transactional`。<br>3. 调用远程服务使用 Feign Client。 |
| **Mapper** | 数据持久化操作 | 1. 继承 `BaseMapper<T>`。<br>2. 复杂 SQL 写在 XML 中，命名空间对应 Mapper 接口。<br>3. 禁止直接返回 Entity 给 Controller，需转换为 DTO/VO。 |
| **Domain** | 数据载体 | 1. `DTO`: 用于服务间传输或参数接收。<br>2. `VO`: 用于前端展示。<br>3. `Entity`: 仅用于数据库映射。 |

### 3.2 接口与实现分离

- **Service 层**：所有 Service 接口定义在 `service` 包下，实现类放在 `service.impl` 包下。
- **Feign 层**：所有 Feign Client 定义在 `api.feign` 包下。

## 四、安全与性能规范

### 4.1 输入校验

- 使用 `jakarta.validation` (Spring Boot 3.x)。
- Controller 方法参数前添加 `@Validated` 或 `@Valid`。
- 自定义校验注解需实现 `ConstraintValidator`。

### 4.2 幂等性控制

- 涉及资金、订单创建等关键操作，必须使用 `@Idempotent` 注解（来自 `zmbdp-common-idempotent`）防止重复提交。
- 基于 Redis 实现，支持自定义 Key 生成策略。

### 4.3 限流控制

- 使用 `@RateLimit` 注解（来自 `zmbdp-common-ratelimit`）对接口进行限流。
- 支持基于 IP、用户 ID 或接口路径的限流策略。

### 4.4 数据权限

- 使用 `@DataPermission` 注解（来自 `zmbdp-common-datapermission`）实现数据行级权限控制。
- 自动拦截 SQL 并追加权限条件。

### 4.5 安全规范

- **SQL 注入**：严禁手动拼接 SQL，必须使用 MyBatis-Plus 的参数绑定机制。
- **XSS 防护**：对前端传入的文本内容进行过滤或转义。
- **敏感信息**：日志中禁止打印密码、身份证、银行卡等敏感信息。
- **JWT**：使用 `zmbdp-common-security` 中的工具类解析 Token，获取当前用户信息。

## 五、代码风格规范

### 5.1 命名规范

| 类型 | 命名方式 | 示例 |
| :--- | :--- | :--- |
| 类名 | UpperCamelCase | `UserServiceImpl`, `OrderController` |
| 方法/变量 | lowerCamelCase | `getUserById()`, `userName` |
| 常量 | UPPER_SNAKE_CASE | `MAX_LOGIN_ATTEMPTS`, `DEFAULT_PAGE_SIZE` |
| 包名 | 全小写 | `com.zmbdp.admin.service.impl` |

### 5.2 注释规范

- **强制要求**：所有类、公共方法、复杂逻辑块必须添加 **中文 Javadoc** 注释。
- **注释内容**：
  - 类/方法：说明功能、参数含义、返回值、异常信息。
  - 字段：说明业务含义。
  - 业务逻辑：解释“为什么”这样做，特别是涉及复杂算法或业务规则时。
- **示例**：
  ```java
  /**
   * 创建新用户
   *
   * @param userDTO 用户数据传输对象
   * @return 创建成功后的用户ID
   * @throws BusinessException 当用户名已存在时抛出
   */
  public Long createUser(UserDTO userDTO) {
      // ...
  }
  ```

### 5.3 Lombok 使用规范

- 优先使用 Lombok 简化代码。
- **Entity/DTO/VO**：使用 `@Data`。
- **构造方法**：如有必要，使用 `@NoArgsConstructor` 和 `@AllArgsConstructor`。
- **日志**：使用 `@Slf4j`。
- **注意**：在 `@EqualsAndHashCode` 中排除 `serialVersionUID` 或不需要的字段，避免equals/hashCode问题。

## 六、扩展性与日志规范

### 6.1 接口优先原则

- 所有业务逻辑通过接口定义。
- 新增功能时，优先扩展接口而非修改现有实现。

### 6.2 日志记录

- 使用 `@Slf4j` 注解注入 Logger。
- **日志级别**：
  - `ERROR`：系统错误、异常、不可恢复的错误。
  - `WARN`：警告信息、非关键异常、潜在风险。
  - `INFO`：关键业务流程节点、用户操作记录。
  - `DEBUG`：调试信息，生产环境默认关闭。
- **日志格式**：确保日志中包含 `TraceId` (SkyWalking)，便于链路追踪。
- **禁止**：禁止使用 `System.out.println` 或 `e.printStackTrace()`。

### 6.3 监控与追踪

- **SkyWalking**：集成 SkyWalking 进行分布式链路追踪。
- **Micrometer**：集成 Micrometer + Prometheus 进行指标监控。
- **Grafana**：通过 Grafana 展示监控大盘。

## 七、编码原则总结

| 原则 | 说明 |
| :--- | :--- |
| **SOLID** | 高内聚、低耦合，单一职责，依赖倒置 |
| **DRY** | 避免重复代码，提取公共方法或工具类 |
| **KISS** | 保持代码简洁易懂，避免过度设计 |
| **YAGNI** | 不实现当前不需要的功能 |
| **OWASP** | 防范常见安全漏洞 (SQL注入, XSS, CSRF) |
| **Fail-Fast** | 尽早失败，快速反馈错误 |

## 八、部署与构建规范

- **构建命令**：`mvn clean package -DskipTests`
- **Docker 部署**：使用 `fabric8io/docker-maven-plugin` 自动构建镜像并推送至阿里云镜像仓库。
- **环境配置**：
  - 开发环境：`dev`
  - 测试环境：`test`
  - 生产环境：`prd`
  - 配置文件通过 Nacos 动态加载，本地 `bootstrap.yml` 仅指定 Nacos 地址。

---
*最后更新时间：2026-05-23*
*维护者：Win11*
