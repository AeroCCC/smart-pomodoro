# 🚀 Smart PomoTodo 启动指南

## 项目简介
智能番茄工作法待办事项管理系统,集成 AI 任务拆解功能。

---

## 📋 前置要求
- **Java**: JDK 17+
- **Node.js**: 16+ 
- **Maven**: 3.8+
- **AI API Key**: DeepSeek API Key (可选,用于 AI 任务拆解)

---

## 🔧 启动步骤

### 1️⃣ 启动后端 (Spring Boot)

```bash
cd backend
mvn spring-boot:run
```

后端将运行在: **http://localhost:8080**

📌 **可选**: 配置 AI API Key
```bash
# Windows PowerShell
$env:AI_API_KEY="your-deepseek-api-key"

# Windows CMD
set AI_API_KEY=your-deepseek-api-key

# Linux/Mac
export AI_API_KEY=your-deepseek-api-key
```

### 2️⃣ 启动前端 (Vue 3 + Vite)

```bash
cd frontend
npm install       # 首次运行需要安装依赖
npm run dev
```

前端将运行在: **http://localhost:3000**

---

## ✨ 新功能: AI 任务拆解

### 功能说明
点击 "AI Task Breakdown" 按钮,输入复杂任务目标,AI 会智能拆解成 3-6 个可执行的小任务。

### 使用步骤
1. 在任务列表底部点击 **✨ AI Task Breakdown** 按钮
2. 在弹出的对话框中输入你的目标,例如:
   - "学习 Vue3 框架并制作一个个人博客"
   - "准备毕业论文答辩"
   - "健身计划"
3. 点击 **Generate Tasks** 生成任务列表
4. 审核 AI 生成的任务:
   - ✅ 勾选/取消勾选任务
   - ✏️ 双击编辑任务文本
   - 🗑️ 删除不需要的任务
   - ➕ 手动添加新步骤
5. 点击 **Confirm and Add to List** 批量添加到任务列表

### AI API Key 获取
1. 访问 [DeepSeek 官网](https://platform.deepseek.com/)
2. 注册并获取 API Key
3. 配置环境变量或在对话框中输入

---

## 📊 功能特性

### ✅ 已实现
- 任务增删改查
- 任务优先级 (Low/Medium/High)
- 番茄钟计时器 (25分钟工作 + 5分钟休息)
- 每日完成进度展示
- AI 智能任务拆解
- 任务完成状态管理

### 🎯 核心功能
1. **任务管理**: 创建、完成、删除任务
2. **番茄工作法**: 专注计时,劳逸结合
3. **AI 拆解**: 复杂任务智能拆解
4. **进度追踪**: 实时查看完成率

---

## 🗄️ 数据库访问

项目使用 H2 内存数据库,可通过以下方式访问:

**H2 控制台**: http://localhost:8080/h2-console

**连接信息**:
- JDBC URL: `jdbc:h2:mem:pomotodo`
- Username: `sa`
- Password: (留空)

⚠️ **注意**: 内存数据库在服务重启后数据会丢失

---

## 📁 项目结构

```
smart-pomotodo/
├── backend/                # Spring Boot 后端
│   ├── src/
│   │   └── main/
│   │       ├── java/com/pomotodo/
│   │       │   ├── controller/      # REST API 控制器
│   │       │   │   ├── TaskController.java
│   │       │   │   ├── FocusController.java
│   │       │   │   └── AiController.java    # ✨ AI 拆解 API
│   │       │   ├── entity/          # JPA 实体
│   │       │   └── repository/      # 数据仓库
│   │       └── resources/
│   │           └── application.yml  # 配置文件
│   └── pom.xml
│
└── frontend/               # Vue 3 前端
    ├── src/
    │   ├── App.vue        # 主组件 (包含 AI 对话框)
    │   ├── main.js
    │   └── stores/
    │       └── taskStore.js
    ├── package.json
    └── vite.config.js
```

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

**解决**: 检查环境变量和网络连接

### 3. 前端无法连接后端
**检查**:
- 后端是否在 8080 端口运行
- Vite 代理配置 (vite.config.js)

---

## 📖 API 文档

### 任务管理 API
- `GET /api/tasks` - 获取所有任务
- `POST /api/tasks` - 创建任务
- `PUT /api/tasks/{id}` - 更新任务
- `DELETE /api/tasks/{id}` - 删除任务

### AI 拆解 API
- `POST /api/ai/decompose`
  ```json
  {
    "goal": "学习Vue3",
    "apiKey": "sk-xxx"  // 可选
  }
  ```

### 专注记录 API
- `POST /api/focus` - 保存专注记录
- `GET /api/focus/today` - 获取今日专注时长
- `GET /api/focus/stats` - 获取近7天统计

---

## 🎨 UI 设计特性

- **现代渐变背景**: 紫色主题
- **毛玻璃效果**: 背景模糊
- **流畅动画**: 按钮悬停、加载动画
- **响应式设计**: 支持移动端

---

## 📝 开发提示

### 修改 AI 模型
编辑 `AiController.java` 第 46 行:
```java
"model", "deepseek-chat"  // 修改为其他模型
```

### 调整番茄钟时长
编辑 `App.vue`:
```javascript
const workDuration = 25 * 60    // 工作时长(秒)
const breakDuration = 5 * 60    // 休息时长(秒)
```

---

## 📧 技术支持

如有问题,请检查:
1. 控制台日志 (F12 开发者工具)
2. 后端日志输出
3. 网络请求状态

---

**祝您使用愉快! 🎉**
