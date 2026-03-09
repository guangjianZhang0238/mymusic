@echo off
chcp 65001 > nul
echo 正在启动音乐服务器...
echo 当前代码页: 65001 (UTF-8)

REM 检查8080端口是否被占用
echo 检查8080端口占用情况...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr /R ":8080 " ^| findstr LISTENING') do (
    if not "%%a"=="" (
        echo 发现8080端口被进程 %%a 占用
        echo 正在终止进程 %%a ...
        taskkill /f /pid %%a
        timeout /t 2 /nobreak > nul
    )
)

echo 检查完成，准备启动服务...

REM 设置JVM参数（通过MAVEN_OPTS传递给spring-boot:run）
set MAVEN_OPTS=-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Dconsole.encoding=UTF-8

REM 启动Spring Boot应用
cd /d "%~dp0"
mvn spring-boot:run

pause