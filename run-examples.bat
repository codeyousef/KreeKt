@echo off
REM KreeKt Examples Runner for Windows
REM This script provides easy access to run KreeKt examples

echo.
echo 🚀 KreeKt Examples Runner
echo ========================

if "%1"=="help" goto :help
if "%1"=="list" goto :list
if "%1"=="simple" goto :simple
if "%1"=="jvm" goto :jvm
if "%1"=="web" goto :web
if "%1"=="build" goto :build
if "%1"=="test" goto :test
if "%1"=="clean" goto :clean

:help
echo.
echo Available commands:
echo   run-examples help     - Show this help
echo   run-examples list     - List all available examples
echo   run-examples simple   - Run simple launcher (no complex setup)
echo   run-examples jvm      - Run JVM example with LWJGL
echo   run-examples web      - Run web example in browser
echo   run-examples build    - Build entire project
echo   run-examples test     - Run all tests
echo   run-examples clean    - Clean build artifacts
echo.
echo Quick Start:
echo   1. run-examples build
echo   2. run-examples simple
echo.
goto :end

:list
echo.
echo 📝 Available KreeKt Examples:
echo.
echo Simple Examples:
echo   run-examples simple   - Simple core functionality demo
echo   run-examples jvm      - Interactive 3D scene with LWJGL
echo   run-examples web      - Browser-based WebGPU example
echo.
echo Build Tasks:
echo   run-examples build    - Build entire project
echo   run-examples test     - Run all tests
echo   run-examples clean    - Clean build artifacts
echo.
goto :end

:simple
echo.
echo 🚀 Running Simple KreeKt Demo...
echo.
call gradle :examples:basic-scene:runSimple
goto :end

:jvm
echo.
echo 🎮 Running JVM Example with LWJGL...
echo.
call gradle :examples:basic-scene:runJvm
goto :end

:web
echo.
echo 🌐 Running Web Example in Browser...
echo.
call gradle :examples:basic-scene:runJs
goto :end

:build
echo.
echo 🔨 Building KreeKt Project...
echo.
call gradle build
goto :end

:test
echo.
echo 🧪 Running Tests...
echo.
call gradle test
goto :end

:clean
echo.
echo 🧹 Cleaning Build Artifacts...
echo.
call gradle clean
goto :end

REM Default action if no parameter provided
echo.
echo Usage: run-examples [command]
echo Run "run-examples help" for available commands
echo.

:end