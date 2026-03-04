@echo off
echo ==========================================
echo    Smart PomoTodo 数据库备份脚本
echo ==========================================
echo.

REM 设置备份目录
set BACKUP_DIR=C:\Backups\PomoTodo
set DATE=%date:~0,4%%date:~5,2%%date:~8,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set DATE=%DATE: =0%

REM 创建备份目录
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

echo [1/2] 正在备份数据库 pomotodo...
mysqldump -u pomotodo -p040317 pomotodo > "%BACKUP_DIR%\pomotodo_backup_%DATE%.sql"

if %ERRORLEVEL% == 0 (
    echo     ✓ 备份成功: pomotodo_backup_%DATE%.sql
    echo     备份位置: %BACKUP_DIR%
) else (
    echo     ✗ 备份失败，请检查数据库连接
    pause
    exit /b 1
)

echo.
echo [2/2] 清理30天前的备份...
forfiles /P "%BACKUP_DIR%" /M "*.sql" /D -30 /C "cmd /c del @path" 2>nul
echo     ✓ 清理完成

echo.
echo ==========================================
echo 备份完成！
echo ==========================================
pause
