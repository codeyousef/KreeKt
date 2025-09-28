#!/usr/bin/env powershell
<#
.SYNOPSIS
KreeKt Examples Runner for Windows PowerShell

.DESCRIPTION
This script provides easy access to run KreeKt examples and manage the project.

.PARAMETER Command
The command to execute (help, list, simple, jvm, web, build, test, clean)

.EXAMPLE
.\run-examples.ps1 help
.\run-examples.ps1 simple
.\run-examples.ps1 jvm
#>

param(
    [string]$Command = "help"
)

Write-Host ""
Write-Host "🚀 KreeKt Examples Runner" -ForegroundColor Cyan
Write-Host "========================" -ForegroundColor Cyan

switch ($Command.ToLower()) {
    "help" {
        Write-Host ""
        Write-Host "Available commands:" -ForegroundColor Yellow
        Write-Host "  .\run-examples.ps1 help     - Show this help" -ForegroundColor White
        Write-Host "  .\run-examples.ps1 list     - List all available examples" -ForegroundColor White
        Write-Host "  .\run-examples.ps1 simple   - Run simple launcher (no complex setup)" -ForegroundColor White
        Write-Host "  .\run-examples.ps1 jvm      - Run JVM example with LWJGL" -ForegroundColor White
        Write-Host "  .\run-examples.ps1 web      - Run web example in browser" -ForegroundColor White
        Write-Host "  .\run-examples.ps1 build    - Build entire project" -ForegroundColor White
        Write-Host "  .\run-examples.ps1 test     - Run all tests" -ForegroundColor White
        Write-Host "  .\run-examples.ps1 clean    - Clean build artifacts" -ForegroundColor White
        Write-Host ""
        Write-Host "Quick Start:" -ForegroundColor Green
        Write-Host "  1. .\run-examples.ps1 build" -ForegroundColor White
        Write-Host "  2. .\run-examples.ps1 simple" -ForegroundColor White
        Write-Host ""
    }

    "list" {
        Write-Host ""
        Write-Host "📝 Available KreeKt Examples:" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Simple Examples:" -ForegroundColor Green
        Write-Host "  simple   - Simple core functionality demo" -ForegroundColor White
        Write-Host "  jvm      - Interactive 3D scene with LWJGL" -ForegroundColor White
        Write-Host "  web      - Browser-based WebGPU example" -ForegroundColor White
        Write-Host ""
        Write-Host "Build Tasks:" -ForegroundColor Green
        Write-Host "  build    - Build entire project" -ForegroundColor White
        Write-Host "  test     - Run all tests" -ForegroundColor White
        Write-Host "  clean    - Clean build artifacts" -ForegroundColor White
        Write-Host ""
    }

    "simple" {
        Write-Host ""
        Write-Host "🚀 Running Simple KreeKt Demo..." -ForegroundColor Green
        Write-Host ""
        & gradle ":examples:basic-scene:runSimple"
    }

    "jvm" {
        Write-Host ""
        Write-Host "🎮 Running JVM Example with LWJGL..." -ForegroundColor Green
        Write-Host ""
        & gradle ":examples:basic-scene:runJvm"
    }

    "web" {
        Write-Host ""
        Write-Host "🌐 Running Web Example in Browser..." -ForegroundColor Green
        Write-Host ""
        & gradle ":examples:basic-scene:runJs"
    }

    "build" {
        Write-Host ""
        Write-Host "🔨 Building KreeKt Project..." -ForegroundColor Green
        Write-Host ""
        & gradle "build"
    }

    "test" {
        Write-Host ""
        Write-Host "🧪 Running Tests..." -ForegroundColor Green
        Write-Host ""
        & gradle "test"
    }

    "clean" {
        Write-Host ""
        Write-Host "🧹 Cleaning Build Artifacts..." -ForegroundColor Green
        Write-Host ""
        & gradle "clean"
    }

    default {
        Write-Host ""
        Write-Host "Usage: .\run-examples.ps1 [command]" -ForegroundColor Yellow
        Write-Host "Run '.\run-examples.ps1 help' for available commands" -ForegroundColor White
        Write-Host ""
    }
}