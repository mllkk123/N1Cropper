# 需要以管理员身份运行
$jdkPath = "C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot"

# 检查 JDK 是否存在
if (-not (Test-Path "$jdkPath\bin\java.exe")) {
    Write-Host "[错误] 找不到 JDK，路径: $jdkPath" -ForegroundColor Red
    pause
    exit 1
}

# 设置 JAVA_HOME
[Environment]::SetEnvironmentVariable("JAVA_HOME", $jdkPath, "Machine")
Write-Host "[OK] JAVA_HOME 已设置为: $jdkPath" -ForegroundColor Green

# 获取当前系统 PATH
$currentPath = [Environment]::GetEnvironmentVariable("PATH", "Machine")

# 检查是否已存在
if ($currentPath -notlike "*%JAVA_HOME%\bin*") {
    $newPath = $currentPath + ";%JAVA_HOME%\bin"
    [Environment]::SetEnvironmentVariable("PATH", $newPath, "Machine")
    Write-Host "[OK] PATH 已添加 %JAVA_HOME%\bin" -ForegroundColor Green
} else {
    Write-Host "[跳过] PATH 中已存在 JDK bin 路径" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  配置完成！" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "注意：环境变量已更新，但你当前打开的" -ForegroundColor Yellow
Write-Host "命令行/终端窗口不会立即生效。" -ForegroundColor Yellow
Write-Host ""
Write-Host "请关闭当前终端，重新打开一个新的，" -ForegroundColor Yellow
Write-Host "然后运行: java -version" -ForegroundColor Yellow
Write-Host ""
pause
