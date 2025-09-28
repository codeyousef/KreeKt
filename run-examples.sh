#!/bin/bash
# KreeKt Examples Runner for Unix/Linux/macOS
# This script provides easy access to run KreeKt examples

echo ""
echo "🚀 KreeKt Examples Runner"
echo "========================"

case "${1:-help}" in
    "help")
        echo ""
        echo "Available commands:"
        echo "  ./run-examples.sh help     - Show this help"
        echo "  ./run-examples.sh list     - List all available examples"
        echo "  ./run-examples.sh simple   - Run simple launcher (no complex setup)"
        echo "  ./run-examples.sh jvm      - Run JVM example with LWJGL"
        echo "  ./run-examples.sh web      - Run web example in browser"
        echo "  ./run-examples.sh build    - Build entire project"
        echo "  ./run-examples.sh test     - Run all tests"
        echo "  ./run-examples.sh clean    - Clean build artifacts"
        echo ""
        echo "Quick Start:"
        echo "  1. ./run-examples.sh build"
        echo "  2. ./run-examples.sh simple"
        echo ""
        ;;

    "list")
        echo ""
        echo "📝 Available KreeKt Examples:"
        echo ""
        echo "Simple Examples:"
        echo "  simple   - Simple core functionality demo"
        echo "  jvm      - Interactive 3D scene with LWJGL"
        echo "  web      - Browser-based WebGPU example"
        echo ""
        echo "Build Tasks:"
        echo "  build    - Build entire project"
        echo "  test     - Run all tests"
        echo "  clean    - Clean build artifacts"
        echo ""
        ;;

    "simple")
        echo ""
        echo "🚀 Running Simple KreeKt Demo..."
        echo ""
        ./gradlew :examples:basic-scene:runSimple
        ;;

    "jvm")
        echo ""
        echo "🎮 Running JVM Example with LWJGL..."
        echo ""
        ./gradlew :examples:basic-scene:runJvm
        ;;

    "web")
        echo ""
        echo "🌐 Running Web Example in Browser..."
        echo ""
        ./gradlew :examples:basic-scene:runJs
        ;;

    "build")
        echo ""
        echo "🔨 Building KreeKt Project..."
        echo ""
        ./gradlew build
        ;;

    "test")
        echo ""
        echo "🧪 Running Tests..."
        echo ""
        ./gradlew test
        ;;

    "clean")
        echo ""
        echo "🧹 Cleaning Build Artifacts..."
        echo ""
        ./gradlew clean
        ;;

    *)
        echo ""
        echo "Usage: ./run-examples.sh [command]"
        echo "Run './run-examples.sh help' for available commands"
        echo ""
        ;;
esac