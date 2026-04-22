@echo off
chcp 65001 >nul
echo =========================================
echo   N1Cropper APK 打包工具
echo =========================================
echo.

REM 检查 Java
java -version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未检测到 Java。请先安装 JDK 17：
    echo   https://adoptium.net/temurin/releases/?version=17
    pause
    exit /b 1
)

REM 检查 ANDROID_HOME
if "%ANDROID_HOME%"=="" (
    echo [警告] ANDROID_HOME 环境变量未设置
    echo.
    echo 如果打包失败，请先安装 Android SDK：
    echo   方法1：安装 Android Studio ^(推荐^)
    echo   方法2：使用命令行工具
    echo     https://developer.android.com/studio#command-tools
    echo.
    echo 然后设置环境变量 ANDROID_HOME 为 SDK 路径
    echo 例如：setx ANDROID_HOME "C:\Users\%USERNAME%\AppData\Local\Android\Sdk"
    echo.
    pause
)

echo [1/3] 开始打包 Debug APK...
echo.

REM 执行 Gradle 打包
call gradlew.bat assembleDebug --no-daemon

if errorlevel 1 (
    echo.
    echo [错误] 打包失败！
    echo 常见原因：
    echo   - 网络问题导致依赖下载失败（多试几次）
    echo   - Android SDK 未安装或路径错误
    echo   - Java 版本不对（需要 JDK 17）
    pause
    exit /b 1
)

echo.
echo [2/3] 打包成功！
echo.

REM 复制 APK 到项目根目录，方便找到
set APK_SOURCE=app\build\outputs\apk\debug\app-debug.apk
set APK_TARGET=N1Cropper_v2.0_debug.apk

copy /Y "%APK_SOURCE%" "%APK_TARGET%" >nul

echo [3/3] APK 已生成：
echo   %CD%\%APK_TARGET%
echo.
echo =========================================
echo   安装到设备：adb install %APK_TARGET%
echo =========================================
pause
