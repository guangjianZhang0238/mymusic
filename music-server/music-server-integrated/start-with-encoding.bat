@echo off
chcp 65001 > nul
echo 正在启动音乐服务器...
echo 当前代码页: %CODEPAGE%

REM 检查8080端口是否被占用
echo 检查8080端口占用情况...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8080') do (
    echo 发现8080端口被进程 %%a 占用
    echo 正在终止进程 %%a ...
    taskkill /f /pid %%a
    timeout /t 2 /nobreak > nul
)

echo 检查完成，准备启动服务...

REM 设置Java系统属性
set JAVA_OPTS=-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Dconsole.encoding=UTF-8

REM 启动Spring Boot应用
cd /d "%~dp0"
mvn spring-boot:run %JAVA_OPTS%

pause