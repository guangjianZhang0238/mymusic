@echo off

rem 设置系统编码为UTF-8
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8
set LANG=en_US.UTF-8

rem 启动应用程序
java -Dfile.encoding=UTF-8 -jar target\music-server-integrated-1.0.0.jar
