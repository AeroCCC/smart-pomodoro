# Smart PomoTodo - 智能个人时间管理系统

基于 Spring Boot 3.x 与 Vue 3 的全栈智能时间管理系统，结合待办事项管理、番茄工作法、团队协作和 AI 智能任务拆解。

## ✨ 最新更新 (v3.0)

### 🎉 新增团队协作功能!
- 👥 **团队管理** - 创建团队、邀请成员、角色管理(OWNER/ADMIN/MEMBER)
- 📋 **团队任务看板** - 拖拽式 Kanban 看板 (TODO → IN_PROGRESS → DONE)
- 🎯 **任务分配** - 将任务指派给团队成员
- 🔐 **权限控制** - 基于角色的细粒度权限管理
- 💥 **解散团队** - 所有者可以解散团队（软删除）
- 🔗 **完整邀请链接** - 生成可分享的完整 URL，一键复制邀请
- 📋 **任务详情弹窗** - 查看任务完整信息（创建人、负责人、截止日期等）

### 🤖 AI 智能任务拆解
- ✨ 集成通义千问大模型
- 🎯 自动拆解复杂任务为可执行步骤
- ✏️ 支持任务编辑和自定义
- 📋 一键批量添加到任务列表

### 🔔 推送通知系统
- 📱 Web Push 浏览器通知
- ⏰ 番茄钟完成提醒
- 📅 任务截止提醒
- ⚙️ 可配置的订阅管理

---

## 📋 功能特性

### 🎯 核心功能

#### 1. 用户认证系统
- 🔐 JWT Token 身份认证
- 👤 用户注册/登录
- 🔑 密码加密存储 (BCrypt)
- ⏱️ Token 自动续期

#### 2. 个人任务管理
- ✅ 任务的增删改查 (CRUD)
- 🏷️ 优先级设置 (High/Medium/Low)
- 📅 截止日期设置
- ✔️ 任务完成状态追踪
- 📊 完成率统计

#### 3. 番茄钟专注计时
- ⏱️ 25分钟专注 + 5分钟休息
- 🎵 完成音效提醒
- 📈 专注时长统计
- 📅 历史记录追踪

#### 4. AI 智能拆解
- 🤖 集成通义千问 AI
- 📝 输入目标自动生成子任务
- ✏️ 交互式编辑和审核
- ➕ 支持手动添加任务

#### 5. 团队协作 (新增)
- 👥 **团队管理**
  - 创建团队，自动生成邀请码
  - 通过邀请码加入团队
  - 成员角色管理 (OWNER/ADMIN/MEMBER)
  - 团队信息查看和编辑

- 📋 **团队任务看板**
  - 拖拽式 Kanban 三列布局
  - 支持 TODO → IN_PROGRESS → DONE 状态流转
  - 实时状态同步

- 🎯 **任务分配**
  - 指派任务给团队成员
  - 负责人头像显示
  - 逾期提醒

- 🔐 **权限控制**
  - OWNER: 全部权限（解散团队、管理成员、管理任务）
  - ADMIN: 管理团队和任务
  - MEMBER: 查看和完成任务

- 💥 **解散团队**
  - 只有团队所有者可以解散
  - 软删除（设置非活跃状态）
  - 解散后所有数据保留但不可访问
  - 二次确认防止误操作

- 🔗 **邀请链接**
  - 自动生成完整的邀请URL
  - 支持通过链接直接加入团队
  - 一键复制分享
  - 同时支持邀请码方式

- 📋 **任务详情**
  - 点击任务卡片查看完整信息
  - 显示创建人、负责人、状态
  - 显示创建时间和截止日期
  - 支持编辑和删除操作

#### 6. 推送通知
- 🔔 浏览器 Web Push 通知
- ⏰ 番茄钟完成提醒
- 📅 任务截止前提醒
- ⚙️ 订阅管理和配置

#### 7. 数据仪表板
- 📊 任务完成统计图表
- 📈 专注时长趋势图
- 🔥 连续专注天数
- 📅 周活动热力图

---

## 🛠️ 技术栈

### 后端技术
| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.0 | 核心框架 |
| Spring Security | 6.x | 安全认证 |
| Spring Data JPA | 3.x | 数据访问 |
| Spring Data Redis | 3.x | 缓存支持 |
| JWT | 0.12.3 | Token认证 |
| H2 Database | 2.x | 开发环境 |
| MySQL | 8.x | 生产环境 |
| Web Push | 5.1.1 | 推送通知 |
| Lombok | 1.18.x | 代码简化 |

### 前端技术
| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.4.0 | 前端框架 |
| Pinia | 2.1.7 | 状态管理 |
| Vue Router | 4.6.4 | 路由管理 |
| Axios | 1.6.0 | HTTP客户端 |
| ECharts | 5.4.3 | 数据可视化 |
| Vite | 5.0.0 | 构建工具 |
| Element Plus | 2.13.1 | UI组件库 |

---

## 🚀 快速开始

### 前置要求
- **Java**: JDK 17+
- **Node.js**: 16+
- **Maven**: 3.8+

### 方式 1: 使用启动脚本 (推荐 Windows)

```bash
# 启动后端
双击 start-backend.bat

# 启动前端
双击 start-frontend.bat
```

### 方式 2: 命令行启动

#### 启动后端

```bash
cd backend
mvn spring-boot:run
```
后端将运行在: **http://localhost:8080**

#### 启动前端

```bash
cd frontend
npm install    # 首次运行需要安装依赖
npm run dev
```
前端将运行在: **http://localhost:3000**

---

## ⚙️ 环境配置

### 1. AI API Key 配置 (可选)

项目使用**通义千问**作为 AI 服务商：

```bash
# Windows PowerShell
$env:DASHSCOPE_API_KEY="your-dashscope-api-key"

# Windows CMD
set DASHSCOPE_API_KEY=your-dashscope-api-key

# Linux/Mac
export DASHSCOPE_API_KEY=your-dashscope-api-key
```

获取 API Key: https://dashscope.aliyun.com/

### 2. JWT Secret 配置

```bash
# 设置 JWT 密钥
export JWT_SECRET=your-secret-key-change-in-production
```

### 3. 数据库配置

开发环境使用 H2 内存数据库（无需配置），生产环境切换到 MySQL：

```yaml
# application-prod.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/pomotodo
    username: root
    password: your-password
  jpa:
    hibernate:
      ddl-auto: update
```

### 4. Web Push 配置 (可选)

生成 VAPID 密钥对：
```bash
# 访问 https://web-push-codelab.glitch.me/ 生成密钥
# 或本地使用工具生成
```

配置环境变量：
```bash
export VAPID_PUBLIC_KEY=your-public-key
export VAPID_PRIVATE_KEY=your-private-key
```

---

## 📚 API 接口文档

### 认证接口

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| POST | `/api/auth/register` | 用户注册 | 否 |
| POST | `/api/auth/login` | 用户登录 | 否 |

### 任务管理接口

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| GET | `/api/tasks` | 获取所有个人任务 | 是 |
| POST | `/api/tasks` | 创建个人任务 | 是 |
| PUT | `/api/tasks/{id}` | 更新个人任务 | 是 |
| DELETE | `/api/tasks/{id}` | 删除个人任务 | 是 |
| GET | `/api/tasks/stats` | 获取任务统计 | 是 |

### 团队接口

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| GET | `/api/teams` | 获取我的团队列表 | 是 |
| POST | `/api/teams` | 创建团队 | 是 |
| GET | `/api/teams/{id}` | 获取团队详情 | 是 |
| POST | `/api/teams/join` | 加入团队 | 是 |
| GET | `/api/teams/{id}/members` | 获取团队成员 | 是 |
| PUT | `/api/teams/{id}/members/{userId}/role` | 更新成员角色 | 是 |
| DELETE | `/api/teams/{id}/members/{userId}` | 移除成员 | 是 |
| DELETE | `/api/teams/{id}` | 解散团队(所有者) | 是 |
| GET | `/api/teams/{id}/invite-link` | 获取邀请链接 | 是 |

### 团队任务接口

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| GET | `/api/tasks/team/{teamId}` | 获取团队任务列表 | 是 |
| POST | `/api/tasks/team/{teamId}` | 创建团队任务 | 是 |
| PUT | `/api/tasks/team/{teamId}/{taskId}` | 更新团队任务 | 是 |
| DELETE | `/api/tasks/team/{teamId}/{taskId}` | 删除团队任务 | 是 |
| PUT | `/api/tasks/team/{teamId}/{taskId}/status` | 更新任务状态(拖拽) | 是 |
| PUT | `/api/tasks/team/{teamId}/{taskId}/assign` | 分配任务 | 是 |

### 专注记录接口

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| POST | `/api/focus` | 保存专注记录 | 是 |
| GET | `/api/focus/today` | 获取今日专注时长 | 是 |
| GET | `/api/focus/stats` | 获取周统计数据 | 是 |

### AI 接口

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| POST | `/api/ai/decompose` | AI任务拆解 | 是 |

### 推送通知接口

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| POST | `/api/notifications/subscribe` | 订阅推送 | 是 |
| DELETE | `/api/notifications/unsubscribe` | 取消订阅 | 是 |

### 仪表板接口

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| GET | `/api/dashboard/stats` | 获取仪表板统计数据 | 是 |

---

## 📁 项目结构

```
smart-pomotodo/
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/com/pomotodo/
│   │   ├── controller/        # REST API 控制器
│   │   │   ├── AuthController.java
│   │   │   ├── TaskController.java
│   │   │   ├── TeamController.java       # 👥 团队管理
│   │   │   ├── FocusController.java
│   │   │   ├── AiController.java         # 🤖 AI 拆解
│   │   │   ├── DashboardController.java
│   │   │   └── PushNotificationController.java
│   │   ├── service/           # 业务逻辑
│   │   │   ├── AuthService.java
│   │   │   ├── PushNotificationService.java
│   │   │   └── NotificationScheduler.java
│   │   ├── entity/            # JPA 实体
│   │   │   ├── User.java
│   │   │   ├── Task.java
│   │   │   ├── Team.java                   # 👥 团队
│   │   │   ├── TeamMember.java             # 👥 团队成员
│   │   │   ├── FocusLog.java
│   │   │   └── PushSubscription.java
│   │   ├── repository/        # 数据仓库
│   │   ├── security/          # 安全配置
│   │   │   ├── SecurityConfig.java
│   │   │   ├── JwtTokenProvider.java
│   │   │   └── JwtAuthenticationFilter.java
│   │   └── dto/               # 数据传输对象
│   │       ├── TeamTaskRequest.java        # 👥 团队任务请求
│   │       └── TeamTaskResponse.java       # 👥 团队任务响应
│   └── src/main/resources/
│       ├── application.yml
│       ├── application-dev.yml
│       └── application-prod.yml
│
├── frontend/                   # Vue 3 前端
│   ├── src/
│   │   ├── views/             # 页面视图
│   │   │   ├── LoginView.vue
│   │   │   ├── RegisterView.vue
│   │   │   ├── TaskListView.vue
│   │   │   ├── FocusView.vue
│   │   │   ├── DashboardView.vue
│   │   │   ├── TeamListView.vue           # 👥 团队列表
│   │   │   └── TeamDetailView.vue         # 👥 团队详情
│   │   ├── components/        # 可复用组件
│   │   │   ├── Sidebar.vue
│   │   │   ├── AiSidebar.vue              # 🤖 AI 侧边栏
│   │   │   ├── CircularProgress.vue
│   │   │   ├── TeamCard.vue               # 👥 团队卡片
│   │   │   ├── TeamTaskCard.vue           # 👥 团队任务卡片
│   │   │   └── NotificationSettings.vue
│   │   ├── stores/            # Pinia 状态管理
│   │   │   ├── authStore.js
│   │   │   ├── taskStore.js
│   │   │   ├── teamStore.js               # 👥 团队状态
│   │   │   ├── focusStore.js
│   │   │   └── notificationStore.js
│   │   ├── App.vue
│   │   └── main.js
│   └── package.json
│
├── START_GUIDE.md             # 📖 详细启动指南
├── PREVIEW.md                 # 🖼️ 界面预览
└── README.md                  # 📖 项目说明
```

---

## 💡 使用示例

### 示例 1: 团队协作开发项目管理

1. **创建团队**: 点击 "Create Team"，输入团队名称和描述
2. **邀请成员**: 点击 "Invite"，复制完整邀请链接或邀请码分享给团队成员
   - 支持两种邀请方式：完整链接（直接点击加入）或邀请码（手动输入）
3. **成员加入**: 
   - 方式一：点击邀请链接直接加入
   - 方式二：在 "Join Team" 中输入邀请码加入
4. **创建任务**: 在看板中点击 "Add Task" 创建团队任务
5. **分配任务**: 在任务编辑中选择负责人
6. **拖拽更新**: 将任务卡片从 TODO 拖拽到 IN_PROGRESS 或 DONE
7. **查看详情**: 点击任务卡片查看完整信息
8. **解散团队**: 团队所有者点击 "Dissolve Team" 解散团队（需二次确认）

### 示例 2: 使用 AI 拆解学习计划

1. 打开 AI Sidebar (左侧边栏的 "✨ AI" 按钮)
2. 输入目标: "学习 Vue3 框架并制作个人博客"
3. AI 生成任务:
   - 学习 Vue3 基础语法和 Composition API
   - 掌握 Vite 构建工具配置
   - 设计博客页面组件结构
   - 实现文章列表和详情页
   - 添加评论和分类功能
   - 部署到 GitHub Pages
4. 审核并批量添加到任务列表
5. 使用番茄钟专注完成每个任务

---

## 🗄️ 数据库访问

开发环境使用 H2 内存数据库：

**H2 控制台**: http://localhost:8080/h2-console

**连接信息**:
- JDBC URL: `jdbc:h2:mem:pomotodo`
- Username: `sa`
- Password: (留空)

⚠️ **注意**: 内存数据库在服务重启后数据会丢失，生产环境请使用 MySQL。

---

## 🐛 常见问题

### 1. 端口被占用
**问题**: `Port 8080 already in use`

**解决**:
```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID <进程ID> /F

# Linux/Mac
lsof -ti:8080 | xargs kill -9
```

### 2. AI 拆解失败
**可能原因**:
- API Key 未配置或无效
- 网络连接问题
- API 配额用尽

**解决**: 
- 检查 `DASHSCOPE_API_KEY` 环境变量
- 确认 API Key 有效且有可用额度

### 3. Web Push 通知不生效
**检查**:
- 浏览器是否允许通知权限
- VAPID 密钥是否正确配置
- Service Worker 是否正常注册

### 4. 团队协作功能无法使用
**检查**:
- 是否已登录
- 是否是团队成员
- 是否有相应权限

---

## 🔐 安全说明

1. **JWT Secret**: 生产环境必须更换默认的 JWT Secret
2. **密码加密**: 所有密码使用 BCrypt 加密存储
3. **CORS 配置**: 生产环境需要配置正确的跨域来源
4. **HTTPS**: 生产环境建议使用 HTTPS

---

## 📝 开发提示

### 修改 AI 模型
编辑 `AiController.java`:
```java
// 当前使用通义千问
"model", "qwen-turbo"  // 可选: qwen-plus, qwen-max
```

### 调整番茄钟时长
编辑 `frontend/src/views/FocusView.vue`:
```javascript
const workDuration = 25 * 60    // 工作时长(秒)
const shortBreakDuration = 5 * 60   // 短休息(秒)
const longBreakDuration = 15 * 60   // 长休息(秒)
```

### 添加新的团队权限
编辑 `TeamMember.java`:
```java
public enum Role {
    OWNER,
    ADMIN,
    MEMBER,
    VIEWER  // 新增角色
}
```

---

## 🗺️ 路线图

- [x] ✅ 用户认证系统
- [x] ✅ 个人任务管理
- [x] ✅ 番茄钟计时器
- [x] ✅ AI 任务拆解
- [x] ✅ 推送通知
- [x] ✅ 数据仪表板
- [x] ✅ 团队协作功能
- [ ] 📅 任务评论功能
- [ ] 📊 团队统计报表
- [ ] 🔔 邮件通知
- [ ] 📱 PWA 支持
- [ ] 🌐 国际化支持

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request!

**贡献步骤**:
1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

---

## 📄 许可

MIT License

Copyright (c) 2024 Smart PomoTodo

---

## 📧 技术支持

如有问题，请：
1. 查看 [START_GUIDE.md](./START_GUIDE.md) 详细启动指南
2. 检查浏览器控制台日志 (F12)
3. 查看后端日志输出
4. 提交 Issue 描述问题

---

**享受高效的时间管理! 🚀**
