# 设置控制台编码为UTF-8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::InputEncoding = [System.Text.Encoding]::UTF8

# 设置环境变量
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Dconsole.encoding=UTF-8"

Write-Host "正在启动音乐服务器..." -ForegroundColor Green
Write-Host "控制台编码已设置为UTF-8" -ForegroundColor Yellow

# 进入项目目录并启动
Set-Location "$PSScriptRoot"
mvn spring-boot:run