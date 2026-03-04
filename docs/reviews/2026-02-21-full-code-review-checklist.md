# Smart PomoTodo 全量代码审查整改清单（2026-02-21）

## 审查范围
- 业务代码：`backend/src`、`frontend/src`
- 配置与运行：`backend/src/main/resources`、前后端构建
- 依赖与供应链：Maven 依赖树、`npm audit`（根目录与 `frontend`）

## 执行基线
- 后端：`mvn test` 通过，但未发现 `surefire-reports`（当前几乎无自动化测试）
- 前端：`npm run build` 通过，但 `DashboardView` chunk 超 500kB
- 依赖扫描：
  - `frontend`：1 个 high（axios）、2 个 moderate（vite/esbuild）
  - 根目录：2 个 moderate（lodash/lodash-es）
  - OWASP Dependency-Check：本机超时，未产出完整后端 CVE 清单

---

## P0（立即修复）

### 1) 敏感信息暴露风险：直接返回 JPA Entity（Task/FocusLog）
- 严重级别：Critical
- 证据：
  - `backend/src/main/java/com/pomotodo/controller/TaskController.java:77`
  - `backend/src/main/java/com/pomotodo/controller/TaskController.java:114`
  - `backend/src/main/java/com/pomotodo/controller/FocusController.java:66`
  - `backend/src/main/java/com/pomotodo/controller/FocusController.java:113`
  - `backend/src/main/java/com/pomotodo/entity/Task.java:36`
  - `backend/src/main/java/com/pomotodo/entity/Task.java:46`
  - `backend/src/main/java/com/pomotodo/entity/FocusLog.java:24`
  - `backend/src/main/java/com/pomotodo/entity/User.java:32`
- 影响：
  - 可能通过关联对象序列化泄露 `User.password`
  - 可能触发懒加载序列化异常，导致线上不稳定
- 修复方案：
  - 所有对外返回统一改为 DTO，不直接返回 Entity
  - `User.password` 增加 `@JsonIgnore` 作为防线兜底
  - Controller 层只返回字段白名单
- 验收标准：
  - 任意任务/专注相关接口响应中不存在 `password`
  - 回归测试覆盖 DTO 映射字段完整性和空值场景

### 2) 团队任务分配权限边界缺失（可分配给非团队成员）
- 严重级别：Critical
- 证据：
  - `backend/src/main/java/com/pomotodo/controller/TaskController.java:212`
  - `backend/src/main/java/com/pomotodo/controller/TaskController.java:213`
  - `backend/src/main/java/com/pomotodo/controller/TaskController.java:215`
  - `backend/src/main/java/com/pomotodo/controller/TaskController.java:254`
  - `backend/src/main/java/com/pomotodo/controller/TaskController.java:255`
  - `backend/src/main/java/com/pomotodo/controller/TaskController.java:257`
- 影响：
  - 可把团队任务指派给任意系统用户，越权写入业务关系
- 修复方案：
  - `createTeamTask` / `updateTeamTask` 里对 `assignedToId` 强制执行 `isTeamMember(teamId, assignedToId)`
  - 非成员返回 400（或 403）
- 验收标准：
  - 非团队成员 ID 分配请求稳定失败
  - 团队成员分配请求成功且审计日志记录完整

### 3) 异常信息透传给前端（内部实现细节泄露）
- 严重级别：High
- 证据：
  - `backend/src/main/java/com/pomotodo/controller/TaskController.java:117`
  - `backend/src/main/java/com/pomotodo/controller/TaskController.java:155`
  - `backend/src/main/java/com/pomotodo/controller/TeamController.java:84`
  - `backend/src/main/java/com/pomotodo/controller/FocusController.java:68`
  - `backend/src/main/java/com/pomotodo/controller/AiController.java:66`
- 影响：
  - 暴露内部类名、SQL/HTTP 下游错误等细节，提升攻击面
- 修复方案：
  - 引入全局异常处理器（`@RestControllerAdvice`）
  - 统一错误结构：`code/message/details/timestamp/path`
  - 默认隐藏底层异常消息，仅日志保留详细栈
- 验收标准：
  - 所有错误响应结构一致
  - 无 `e.getMessage()` 直接对外输出

### 4) 生产默认配置风险（默认 prod + 弱默认密钥 + schema 自动变更）
- 严重级别：High
- 证据：
  - `backend/src/main/resources/application.yml:7`
  - `backend/src/main/resources/application.yml:28`
  - `backend/src/main/resources/application-prod.yml:23`
- 影响：
  - 本地/测试可能误连生产行为路径
  - JWT 默认密钥存在误用风险
  - `ddl-auto: update` 在生产有不可控 schema 变更风险
- 修复方案：
  - 默认 profile 改为 `dev` 或不设置
  - `JWT_SECRET` 改为必填启动项（缺失即失败）
  - 生产改为 `validate`/`none`，配合迁移工具
- 验收标准：
  - 未注入关键密钥时应用拒绝启动
  - 生产环境不发生自动 schema 写操作

---

## P1（本周内）

### 5) CORS 策略冲突与行为不一致
- 严重级别：High
- 证据：
  - `backend/src/main/java/com/pomotodo/controller/AuthController.java:15`
  - `backend/src/main/java/com/pomotodo/controller/TaskController.java:31`
  - `backend/src/main/java/com/pomotodo/security/SecurityConfig.java:79`
  - `backend/src/main/java/com/pomotodo/security/SecurityConfig.java:83`
- 影响：
  - 控制器 `@CrossOrigin("*")` 与全局 `allowCredentials=true` 配置语义冲突
  - 跨域在不同端点表现不一致，排障困难
- 修复方案：
  - 删除控制器级 `@CrossOrigin`
  - 仅保留 `SecurityConfig` 统一 CORS 策略，并按环境白名单配置
- 验收标准：
  - 前端所有 API 跨域行为一致
  - 预检请求规则与生产域名策略一致

### 6) H2 Console 放行策略过宽
- 严重级别：High
- 证据：
  - `backend/src/main/java/com/pomotodo/security/SecurityConfig.java:44`
  - `backend/src/main/resources/application-dev.yml:19`
- 影响：
  - 安全规则层面对 `/h2-console/**` 永久放行，不应出现在生产配置路径
- 修复方案：
  - 仅在 `dev` profile 启用并放行 H2 Console
  - 生产 profile 明确禁用
- 验收标准：
  - 生产环境访问 `/h2-console` 返回 404/403

### 7) Focus 统计双源状态导致计数不一致
- 严重级别：High
- 证据：
  - `frontend/src/stores/focusStore.js:354`
  - `frontend/src/stores/focusStore.js:355`
  - `frontend/src/views/FocusView.vue:417`
  - `frontend/src/views/FocusView.vue:421`
  - `frontend/src/views/FocusView.vue:422`
- 影响：
  - Store 与页面本地状态同时增量，数值可能重复或偏差（本地固定 +25 分钟）
- 修复方案：
  - 只保留 store 作为单一统计源
  - 页面仅展示 store 派生数据，不做独立增量
- 验收标准：
  - 每完成一次会话，统计只变化一次且与后端一致

### 8) Setup Store 中使用生命周期钩子（行为不可预期）
- 严重级别：Medium
- 证据：
  - `frontend/src/stores/focusStore.js:531`
  - `frontend/src/stores/focusStore.js:535`
- 影响：
  - Pinia Setup Store 非组件上下文，`onMounted/onUnmounted` 语义不可靠
- 修复方案：
  - 将 DOM 监听注册/销毁迁移到页面组件（如 `FocusView.vue`）
- 验收标准：
  - 切页后无遗留监听器
  - 全屏状态在多次进入退出后稳定

### 9) 团队邀请码生成使用 `Random`（弱随机）
- 严重级别：Medium
- 证据：
  - `backend/src/main/java/com/pomotodo/controller/TeamController.java:41`
  - `backend/src/main/java/com/pomotodo/controller/TeamController.java:42`
- 影响：
  - 邀请码可被穷举风险高于加密安全随机
- 修复方案：
  - 改用 `SecureRandom`，并提升长度/复杂度
  - 增加失败次数限制与节流
- 验收标准：
  - 码空间与熵满足安全要求
  - 穷举尝试触发限流

### 10) 路由鉴权初始化存在竞态窗口
- 严重级别：Medium
- 证据：
  - `frontend/src/router/index.js:67`
  - `frontend/src/router/index.js:71`
  - `frontend/src/stores/authStore.js:107`
  - `frontend/src/stores/authStore.js:112`
- 影响：
  - 导航守卫中调用 `initAuth()` 但未等待异步用户校验，可能产生短暂错误跳转
- 修复方案：
  - 启动期做一次性会话恢复（Promise 缓存）
  - 守卫读取“已初始化”状态后再判定路由跳转
- 验收标准：
  - 刷新受保护页面不出现登录页闪断

---

## P2（计划迭代）

### 11) Dashboard 体积过大（整包引入 ECharts）
- 严重级别：Medium
- 证据：
  - `frontend/src/views/DashboardView.vue:184`
  - `frontend/src/views/DashboardView.vue:311`
  - 构建告警：`DashboardView` chunk > 500kB
- 影响：
  - 首屏与路由切换耗时增加，移动端更明显
- 修复方案：
  - 改为按需引入 ECharts 模块
  - 页面级懒加载 + 手动拆包（`manualChunks`）
  - 卸载时移除 `resize` 监听
- 验收标准：
  - Dashboard 相关 chunk 显著下降（目标 < 500kB）
  - 无新增性能告警

### 12) AudioContext/ObjectURL 创建频繁，存在资源泄漏风险
- 严重级别：Low
- 证据：
  - `frontend/src/stores/focusStore.js:51`
  - `frontend/src/stores/focusStore.js:88`
  - `frontend/src/stores/focusStore.js:95`
  - `frontend/src/stores/focusStore.js:128`
- 影响：
  - 长时间使用可能导致内存与音频资源累积
- 修复方案：
  - 复用单例 `AudioContext`
  - 对 `createObjectURL` 调用对应 `revokeObjectURL`
- 验收标准：
  - 长时会话内存曲线稳定，无持续增长

### 13) AI 接口允许客户端透传 API Key
- 严重级别：Medium
- 证据：
  - `backend/src/main/java/com/pomotodo/controller/AiController.java:23`
  - `backend/src/main/java/com/pomotodo/controller/AiController.java:25`
- 影响：
  - 密钥管理边界不清晰，日志/抓包场景下提升泄漏风险
- 修复方案：
  - 禁止客户端传 key，后端统一读服务端安全配置
  - 增加调用限频与审计
- 验收标准：
  - 请求体不再接受 `apiKey`
  - 服务端凭据仅来自环境变量或密钥管理系统

---

## 依赖与供应链基线（本次结果）

### 前端（`frontend/package.json`）
- `axios` high（`<=1.13.4`）：建议升级到修复版本
- `vite`/`esbuild` moderate：建议升级到修复版本（可能跨 major，需回归）

### 根目录（`package.json`）
- `lodash`/`lodash-es` moderate：建议升级并验证受影响链路

### 后端（Maven）
- 依赖树已导出：`backend/target/dependency-tree.txt`
- OWASP 扫描本机超时，需在 CI/CD 环境补跑并归档报告

---

## 最小风险修复顺序
1. P0 全部（实体脱敏 DTO 化 + 团队分配权限 + 错误结构统一 + 配置硬化）
2. P1 全部（CORS 收口 + H2 放行收口 + Focus 单源状态 + 鉴权初始化 + SecureRandom）
3. P2（性能与体验优化、AI key 策略收敛、音频资源治理）

---

## 回归测试矩阵

### 安全/权限
- 未登录访问受保护接口：401
- 非团队成员分配任务：403/400
- 非资源拥有者修改/删除任务：403/404
- 响应体敏感字段扫描：无 `password`

### 接口一致性
- 异常响应统一结构字段完整
- 非法状态值（如 `INVALID_STATUS`）返回 400
- DTO 返回字段稳定，无懒加载异常

### 前端状态正确性
- 一次专注完成仅增量一次
- 切页后无重复监听器/定时器
- 登录态刷新不闪退登录页

### 性能与构建
- Dashboard chunk 体积下降
- `npm run build` 无新增 warning

### 供应链
- `npm audit` high 收敛为 0
- 后端 OWASP 报告归档并闭环高危项

