# 数据库配置指南

## 当前配置：H2（开发环境）

项目默认使用 **H2内存数据库**，方便开发和测试。

### H2特性
- 无需安装，开箱即用
- 数据存储在内存中，重启后清空
- 提供Web控制台访问

### 访问H2控制台
- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:pomotodo`
- Username: `sa`
- Password: (留空)

---

## 切换到MySQL（生产环境）

### 1. 启动参数切换
```bash
java -jar your-app.jar --spring.profiles.active=prod
```

或在IDE中设置VM参数：
```
-Dspring.profiles.active=prod
```

### 2. 环境变量配置
生产环境需要配置以下环境变量：

```bash
# MySQL配置
export MYSQL_HOST=localhost
export MYSQL_PORT=3306
export MYSQL_DB=pomotodo
export MYSQL_USER=your_username
export MYSQL_PASSWORD=your_password

# Redis配置
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=your_redis_password

# JWT密钥（生产环境必须修改）
export JWT_SECRET=your-256-bit-secret-key-here
```

### 3. 创建MySQL数据库
```sql
CREATE DATABASE pomotodo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 4. 数据库连接配置
生产环境使用连接池（HikariCP），配置包括：
- 最小空闲连接：5
- 最大连接数：20
- 连接超时：20秒
- 最大生命周期：20分钟

---

## Redis缓存配置

### 启用Redis（生产环境自动启用）
当切换到 `prod` profile时，Redis自动启用。

### 缓存策略
- 默认TTL：1小时
- 不缓存null值
- 使用Lettuce客户端

### 常用缓存配置
```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 3600000
```

---

## 数据迁移建议

### 从H2迁移到MySQL

#### 方式1：JPA自动迁移
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update  # 自动创建表结构
```

#### 方式2：使用Flyway（推荐生产环境）
1. 添加依赖：
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

2. 创建迁移脚本：`src/main/resources/db/migration/V1__init.sql`

3. 配置：
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
```

---

## 环境切换检查清单

### 开发环境（H2）
- [ ] application.yml 中 `spring.profiles.active=dev`
- [ ] 或启动参数 `--spring.profiles.active=dev`
- [ ] H2控制台可访问

### 生产环境（MySQL + Redis）
- [ ] MySQL数据库已创建
- [ ] MySQL用户权限已配置
- [ ] Redis服务器已启动
- [ ] 环境变量已设置
- [ ] JWT密钥已修改（不能使用默认值）
- [ ] 数据库连接测试通过
- [ ] Redis连接测试通过

---

## 常见问题

### Q: H2数据如何导出到MySQL？
A: 使用JPA的 `ddl-auto: create` 首次运行时自动创建表，然后手动迁移数据。

### Q: 如何验证Redis连接？
A: 启动应用后查看日志，或访问 `/actuator/health` 端点。

### Q: 生产环境使用什么JPA策略？
A: 推荐使用 `validate` 或 `none`，配合Flyway/Liquibase管理数据库版本。

### Q: 连接池参数如何调优？
A: 根据实际并发量调整：
- `maximum-pool-size`: 并发请求数 / 单请求处理时间
- `minimum-idle`: 保持的最小连接数
- `connection-timeout`: 等待连接的最大时间
