# Smart PomoTodo 从 0 启动指南（Windows）

## 1. 环境准备

1. 安装：
- JDK 17
- Maven 3.9+
- Node.js 20+（建议 20 LTS）
- npm 10+
2. 进入项目根目录：

```powershell
cd d:\用户目录\桌面\实验报告\毕设\smart-pomotodo
```

## 2. 安装前端依赖

```powershell
cd .\frontend
cmd /c "npm.cmd install"
cd ..
```

## 3. 启动后端（推荐先用 dev）

### 3.1 dev 模式（H2 内存库，最快）

```powershell
cd .\backend
mvn spring-boot:run
```

说明：
- `application.yml` 默认 `SPRING_PROFILES_ACTIVE=dev`
- dev 模式不依赖 MySQL/Redis

### 3.2 prod 模式（MySQL + Redis）

先确保：
- MySQL 已启动，库 `pomotodo` 已创建
- Redis 已启动

然后执行：

```powershell
$env:MYSQL_PASSWORD="040317"
$env:JWT_SECRET="local-dev-jwt-secret-min-32-characters!!"
.\start-backend-prod.bat
```

## 4. 启动前端

新开一个终端执行：

```powershell
cd d:\用户目录\桌面\实验报告\毕设\smart-pomotodo\frontend
cmd /c "npm.cmd run dev -- --host 127.0.0.1 --port 3000"
```

访问地址：
- 前端：`http://127.0.0.1:3000`
- 后端：`http://127.0.0.1:8080`

## 5. 注册与登录验证（可直接复制）

```powershell
$u='user'+(Get-Date -Format 'yyyyMMddHHmmssfff')
$p='Passw0rd!123'

# 注册
$registerBody=@{username=$u; email="$u@test.local"; password=$p} | ConvertTo-Json
$register=Invoke-RestMethod -Uri 'http://127.0.0.1:8080/api/auth/register' -Method Post -ContentType 'application/json' -Body $registerBody

# 登录（字段必须是 usernameOrEmail）
$loginBody=@{usernameOrEmail=$u; password=$p} | ConvertTo-Json
$login=Invoke-RestMethod -Uri 'http://127.0.0.1:8080/api/auth/login' -Method Post -ContentType 'application/json' -Body $loginBody

# 鉴权用户信息
$me=Invoke-RestMethod -Uri 'http://127.0.0.1:8080/api/auth/me' -Method Get -Headers @{Authorization="Bearer $($login.accessToken)"}

"REGISTER_OK=" + [bool]($register.accessToken)
"LOGIN_OK=" + [bool]($login.accessToken)
"ME_OK=" + [bool]($me.username)
"USERNAME=" + $u
```

期望输出：
- `REGISTER_OK=True`
- `LOGIN_OK=True`
- `ME_OK=True`

## 6. 常见问题

1. `npm : 无法加载 npm.ps1`
- 原因：PowerShell 执行策略限制
- 处理：使用 `cmd /c "npm.cmd ..."`

2. 登录接口返回 400
- 原因：登录参数名写成了 `username`
- 处理：改为 `usernameOrEmail`

3. 跨域报错
- 处理：前端固定使用 `http://127.0.0.1:3000` 或 `http://localhost:3000`，并确保后端 `CORS_ALLOWED_ORIGINS` 覆盖该地址

4. prod 启动失败
- 排查：先检查 `MYSQL_PASSWORD`、`JWT_SECRET` 是否已设置，以及 MySQL/Redis 是否已启动
