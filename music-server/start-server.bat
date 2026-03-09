@echo off

rem 设置编码参数
set MAVEN_OPTS=-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8

rem 切换到音乐服务器主模块目录
cd music-server-main

rem 启动音乐服务器
mvn spring-boot:run

pause