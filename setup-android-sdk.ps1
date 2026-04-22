# 需要以管理员身份运行
$ErrorActionPreference = "Stop"

$sdkRoot = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk"
$cmdlineToolsUrl = "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
$zipPath = "$env:TEMP\commandlinetools.zip"

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  Android SDK 自动安装脚本" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# 检查 Java
$javaPath = (Get-Command java -ErrorAction SilentlyContinue).Source
if (-not $javaPath) {
    Write-Host "[错误] 找不到 Java。请先运行 setup-java-env.ps1" -ForegroundColor Red
    pause
    exit 1
}
Write-Host "[OK] Java 路径: $javaPath" -ForegroundColor Green

# 创建 SDK 目录
if (-not (Test-Path $sdkRoot)) {
    New-Item -ItemType Directory -Path $sdkRoot -Force | Out-Null
    Write-Host "[OK] 创建 SDK 目录: $sdkRoot" -ForegroundColor Green
}

# 下载 Command Line Tools
$cmdlineToolsPath = "$sdkRoot\cmdline-tools\latest"
if (-not (Test-Path "$cmdlineToolsPath\bin\sdkmanager.bat")) {
    Write-Host "[1/5] 正在下载 Android SDK Command Line Tools..." -ForegroundColor Yellow

    # 使用 WebClient 下载
    $wc = New-Object System.Net.WebClient
    $wc.DownloadFile($cmdlineToolsUrl, $zipPath)

    Write-Host "[OK] 下载完成" -ForegroundColor Green

    # 解压
    Write-Host "[2/5] 正在解压..." -ForegroundColor Yellow
    Expand-Archive -Path $zipPath -DestinationPath "$sdkRoot\cmdline-tools-temp" -Force

    # 移动到正确位置
    if (Test-Path $cmdlineToolsPath) {
        Remove-Item -Path $cmdlineToolsPath -Recurse -Force
    }
    Move-Item -Path "$sdkRoot\cmdline-tools-temp\cmdline-tools" -Destination $cmdlineToolsPath -Force
    Remove-Item -Path "$sdkRoot\cmdline-tools-temp" -Recurse -Force
    Remove-Item -Path $zipPath -Force

    Write-Host "[OK] 解压完成" -ForegroundColor Green
} else {
    Write-Host "[跳过] Command Line Tools 已存在" -ForegroundColor Yellow
}

# 安装必要组件
Write-Host "[3/5] 正在安装 Android SDK 组件（需要几分钟，请耐心等待）..." -ForegroundColor Yellow
Write-Host "       - platforms;android-34" -ForegroundColor Gray
Write-Host "       - build-tools;34.0.0" -ForegroundColor Gray
Write-Host "       - platform-tools" -ForegroundColor Gray
Write-Host ""

$sdkmanager = "$cmdlineToolsPath\bin\sdkmanager.bat"

# 接受所有 license
Write-Host "       正在接受 license..." -ForegroundColor Gray
$proc = Start-Process -FilePath $sdkmanager -ArgumentList "--sdk_root=$sdkRoot", "--licenses" -NoNewWindow -Wait -PassThru

# 安装组件
$components = @(
    "platforms;android-34",
    "build-tools;34.0.0",
    "platform-tools"
)

foreach ($component in $components) {
    Write-Host "       正在安装 $component ..." -ForegroundColor Gray
    $proc = Start-Process -FilePath $sdkmanager -ArgumentList "--sdk_root=$sdkRoot", $component -NoNewWindow -Wait -PassThru
    if ($proc.ExitCode -ne 0) {
        Write-Host "[警告] $component 安装可能有问题，但会继续" -ForegroundColor Yellow
    }
}

Write-Host "[OK] SDK 组件安装完成" -ForegroundColor Green

# 设置环境变量
Write-Host "[4/5] 正在设置环境变量..." -ForegroundColor Yellow

[Environment]::SetEnvironmentVariable("ANDROID_HOME", $sdkRoot, "Machine")
Write-Host "       ANDROID_HOME = $sdkRoot" -ForegroundColor Gray

$currentPath = [Environment]::GetEnvironmentVariable("PATH", "Machine")
$pathsToAdd = @(
    "%ANDROID_HOME%\platform-tools",
    "%ANDROID_HOME%\cmdline-tools\latest\bin"
)

foreach ($p in $pathsToAdd) {
    if ($currentPath -notlike "*$p*") {
        $currentPath += ";$p"
    }
}
[Environment]::SetEnvironmentVariable("PATH", $currentPath, "Machine")
Write-Host "[OK] 环境变量设置完成" -ForegroundColor Green

# 验证
Write-Host "[5/5] 验证安装..." -ForegroundColor Yellow

$adbPath = "$sdkRoot\platform-tools\adb.exe"
$platformPath = "$sdkRoot\platforms\android-34"
$buildToolsPath = "$sdkRoot\build-tools\34.0.0"

$allOk = $true
if (Test-Path $adbPath) {
    Write-Host "       [OK] adb" -ForegroundColor Green
} else {
    Write-Host "       [X] adb 未找到" -ForegroundColor Red
    $allOk = $false
}
if (Test-Path $platformPath) {
    Write-Host "       [OK] platforms;android-34" -ForegroundColor Green
} else {
    Write-Host "       [X] platforms;android-34 未找到" -ForegroundColor Red
    $allOk = $false
}
if (Test-Path $buildToolsPath) {
    Write-Host "       [OK] build-tools;34.0.0" -ForegroundColor Green
} else {
    Write-Host "       [X] build-tools;34.0.0 未找到" -ForegroundColor Red
    $allOk = $false
}

Write-Host ""
if ($allOk) {
    Write-Host "=========================================" -ForegroundColor Cyan
    Write-Host "  Android SDK 安装完成！" -ForegroundColor Cyan
    Write-Host "=========================================" -ForegroundColor Cyan
} else {
    Write-Host "=========================================" -ForegroundColor Red
    Write-Host "  部分组件可能安装失败" -ForegroundColor Red
    Write-Host "=========================================" -ForegroundColor Red
}
Write-Host ""
Write-Host "注意：请关闭当前终端，重新打开一个新的，" -ForegroundColor Yellow
Write-Host "然后就可以运行 build-apk.bat 打包了。" -ForegroundColor Yellow
Write-Host ""
pause
