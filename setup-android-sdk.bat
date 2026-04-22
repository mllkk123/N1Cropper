@echo off
chcp 65001 >nul
echo =========================================
echo   Android SDK 自动安装脚本
echo =========================================
echo.

set "SDK_ROOT=C:\Users\%USERNAME%\AppData\Local\Android\Sdk"
set "ZIP_URL=https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
set "ZIP_PATH=%TEMP%\commandlinetools.zip"

REM 检查 Java
java -version >nul 2>&1
if errorlevel 1 (
    echo [错误] 找不到 Java。请先运行 setup-java-env.ps1
    pause
    exit /b 1
)
echo [OK] Java 已安装

REM 创建 SDK 目录
if not exist "%SDK_ROOT%" (
    mkdir "%SDK_ROOT%"
    echo [OK] 创建 SDK 目录: %SDK_ROOT%
)

set "CMDLINE_PATH=%SDK_ROOT%\cmdline-tools\latest"

REM 下载 Command Line Tools
if not exist "%CMDLINE_PATH%\bin\sdkmanager.bat" (
    echo [1/5] 正在下载 Android SDK Command Line Tools...
    echo        约 150MB，请耐心等待...
    echo.

    powershell -Command "Invoke-WebRequest -Uri '%ZIP_URL%' -OutFile '%ZIP_PATH%'"

    if not exist "%ZIP_PATH%" (
        echo [错误] 下载失败，请检查网络连接
        pause
        exit /b 1
    )
    echo [OK] 下载完成

    echo [2/5] 正在解压...
    powershell -Command "Expand-Archive -Path '%ZIP_PATH%' -DestinationPath '%SDK_ROOT%\cmdline-tools-temp' -Force"

    if exist "%CMDLINE_PATH%" rmdir /s /q "%CMDLINE_PATH%"
    mkdir "%SDK_ROOT%\cmdline-tools"
    move /y "%SDK_ROOT%\cmdline-tools-temp\cmdline-tools" "%CMDLINE_PATH%"
    rmdir /s /q "%SDK_ROOT%\cmdline-tools-temp"
    del /f "%ZIP_PATH%"

    echo [OK] 解压完成
) else (
    echo [跳过] Command Line Tools 已存在
)

echo.
echo [3/5] 正在安装 Android SDK 组件...
echo        - platforms;android-34
echo        - build-tools;34.0.0
echo        - platform-tools
echo        这可能需要几分钟...
echo.

set "SDKMANAGER=%CMDLINE_PATH%\bin\sdkmanager.bat"

REM 接受 license
echo        正在接受 license...
echo y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y y | "%SDKMANAGER%" --sdk_root="%SDK_ROOT%" --licenses >nul 2>&1

REM 安装组件
echo        安装 platforms;android-34...
"%SDKMANAGER%" --sdk_root="%SDK_ROOT%" "platforms;android-34" >nul 2>&1

echo        安装 build-tools;34.0.0...
"%SDKMANAGER%" --sdk_root="%SDK_ROOT%" "build-tools;34.0.0" >nul 2>&1

echo        安装 platform-tools...
"%SDKMANAGER%" --sdk_root="%SDK_ROOT%" "platform-tools" >nul 2>&1

echo [OK] SDK 组件安装完成
echo.

echo [4/5] 正在设置环境变量...
echo        ANDROID_HOME = %SDK_ROOT%
setx ANDROID_HOME "%SDK_ROOT%" >nul 2>&1

echo [OK] 环境变量设置完成
echo.

echo [5/5] 验证安装...
set "ALL_OK=1"

if exist "%SDK_ROOT%\platform-tools\adb.exe" (
    echo        [OK] adb
) else (
    echo        [X] adb 未找到
    set "ALL_OK=0"
)

if exist "%SDK_ROOT%\platforms\android-34" (
    echo        [OK] platforms;android-34
) else (
    echo        [X] platforms;android-34 未找到
    set "ALL_OK=0"
)

if exist "%SDK_ROOT%\build-tools\34.0.0" (
    echo        [OK] build-tools;34.0.0
) else (
    echo        [X] build-tools;34.0.0 未找到
    set "ALL_OK=0"
)

echo.
if "%ALL_OK%"=="1" (
    echo =========================================
    echo   Android SDK 安装完成！
    echo =========================================
) else (
    echo =========================================
    echo   部分组件可能安装失败
    echo =========================================
)
echo.
echo 注意：请关闭当前终端，重新打开一个新的，
echo 然后就可以运行 build-apk.bat 打包了。
echo.
pause
