@echo off
chcp 65001 >nul
echo ========================================
echo   AI配置验证脚本
echo ========================================
echo.

REM 检查环境变量
echo [1/3] 检查环境变量...
if defined DASHSCOPE_API_KEY (
    echo ✓ DASHSCOPE_API_KEY 已设置
    echo   值: %DASHSCOPE_API_KEY:~0,10%***（已隐藏部分）
) else (
    echo ✗ DASHSCOPE_API_KEY 未设置！
    echo.
    echo 请按照以下步骤设置环境变量：
    echo 1. 右键"此电脑" - "属性" - "高级系统设置"
    echo 2. 点击"环境变量"
    echo 3. 新建用户变量：
    echo    变量名：DASHSCOPE_API_KEY
    echo    变量值：你的通义千问API Key
    echo 4. 重启命令行窗口和IDE
    echo.
    pause
    exit /b 1
)

echo.
echo [2/3] 检查配置文件...
if exist "backend\src\main\resources\application.yml" (
    echo ✓ application.yml 存在
    findstr /C:"DASHSCOPE_API_KEY" backend\src\main\resources\application.yml >nul
    if %errorlevel%==0 (
        echo ✓ API Key 配置已更新
    ) else (
        echo ✗ application.yml 中未找到 DASHSCOPE_API_KEY
    )
) else (
    echo ✗ application.yml 不存在
)

echo.
echo [3/3] 检查控制器代码...
if exist "backend\src\main\java\com\pomotodo\controller\AiController.java" (
    echo ✓ AiController.java 存在
    findstr /C:"qwen-plus" backend\src\main\java\com\pomotodo\controller\AiController.java >nul
    if %errorlevel%==0 (
        echo ✓ 已配置通义千问模型
    ) else (
        echo ⚠ 未找到 qwen-plus 模型配置
    )
) else (
    echo ✗ AiController.java 不存在
)

echo.
echo ========================================
echo   配置验证完成！
echo ========================================
echo.
echo 下一步操作：
echo 1. 重启后端服务以应用配置
echo 2. 访问 http://localhost:3000/tasks
echo 3. 点击右侧AI助手测试任务拆解功能
echo.
pause
