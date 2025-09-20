#!/bin/bash
# KreeKt Tools - Linux Desktop Packaging Script
# Creates AppImage, DEB, RPM, and Flatpak packages

set -e

echo "========================================"
echo "KreeKt Tools - Linux Packaging"
echo "========================================"

# Configuration
APP_NAME="KreeKt Tools"
APP_ID="dev.kreekt.tools"
APP_VERSION="1.0.0"
JAVA_VERSION="17"
BUILD_DIR="$(pwd)/build"
DIST_DIR="$(pwd)/dist"
RESOURCES_DIR="$(pwd)/resources"

# Architecture detection
ARCH=$(uname -m)
case $ARCH in
    x86_64) DEB_ARCH="amd64"; RPM_ARCH="x86_64" ;;
    aarch64) DEB_ARCH="arm64"; RPM_ARCH="aarch64" ;;
    *) echo "Unsupported architecture: $ARCH"; exit 1 ;;
esac

# Clean previous builds
rm -rf "$BUILD_DIR"
rm -rf "$DIST_DIR"
mkdir -p "$BUILD_DIR"
mkdir -p "$DIST_DIR"

echo "Building Kotlin application..."

# Build the Kotlin application
cd ../../../
./gradlew :tools:editor:desktopJar
./gradlew :tools:profiler:desktopJar
./gradlew :tools:api-server:shadowJar

echo "Build completed successfully."

# Copy application JARs
cp tools/editor/desktop/build/libs/*.jar "$BUILD_DIR/"
cp tools/profiler/desktop/build/libs/*.jar "$BUILD_DIR/"
cp tools/api-server/build/libs/*-all.jar "$BUILD_DIR/kreekt-api-server.jar"

echo "Creating custom JRE with jlink..."

# Create custom JRE
$JAVA_HOME/bin/jlink \
    --module-path $JAVA_HOME/jmods \
    --add-modules java.base,java.desktop,java.logging,java.management,java.naming,java.security.jgss,java.security.sasl,java.sql,jdk.crypto.ec,jdk.unsupported \
    --output "$BUILD_DIR/jre" \
    --compress=2 \
    --no-header-files \
    --no-man-pages

echo "Creating application structure..."

# Create application directory structure
APP_DIR="$BUILD_DIR/kreekt-tools"
mkdir -p "$APP_DIR/bin"
mkdir -p "$APP_DIR/lib"
mkdir -p "$APP_DIR/resources"
mkdir -p "$APP_DIR/share/applications"
mkdir -p "$APP_DIR/share/icons/hicolor/256x256/apps"

# Copy JRE
cp -R "$BUILD_DIR/jre" "$APP_DIR/"

# Copy JARs
cp "$BUILD_DIR"/*.jar "$APP_DIR/lib/"

# Create launcher scripts
cat > "$APP_DIR/bin/kreekt-tools" << 'EOF'
#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_ROOT="$(dirname "$SCRIPT_DIR")"
JAVA_HOME="$APP_ROOT/jre"

cd "$APP_ROOT"

exec "$JAVA_HOME/bin/java" \
    -Xmx2g \
    -Djava.awt.headless=false \
    -Dsun.java2d.opengl=true \
    -jar "$APP_ROOT/lib/kreekt-api-server.jar" \
    "$@"
EOF

cat > "$APP_DIR/bin/scene-editor" << 'EOF'
#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_ROOT="$(dirname "$SCRIPT_DIR")"
JAVA_HOME="$APP_ROOT/jre"

exec "$JAVA_HOME/bin/java" \
    -Xmx1g \
    -Djava.awt.headless=false \
    -jar "$APP_ROOT/lib/editor-desktop.jar" \
    "$@"
EOF

cat > "$APP_DIR/bin/profiler" << 'EOF'
#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_ROOT="$(dirname "$SCRIPT_DIR")"
JAVA_HOME="$APP_ROOT/jre"

exec "$JAVA_HOME/bin/java" \
    -Xmx1g \
    -Djava.awt.headless=false \
    -jar "$APP_ROOT/lib/profiler-desktop.jar" \
    "$@"
EOF

chmod +x "$APP_DIR/bin/"*

# Create desktop entry
cat > "$APP_DIR/share/applications/kreekt-tools.desktop" << EOF
[Desktop Entry]
Type=Application
Name=$APP_NAME
Comment=3D graphics development tools for Kotlin Multiplatform
Icon=kreekt-tools
Exec=kreekt-tools
Categories=Development;Graphics;3DGraphics;
StartupNotify=true
StartupWMClass=KreeKt Tools
MimeType=application/x-kreekt-project;
Keywords=3D;graphics;development;kotlin;webgpu;vulkan;
EOF

# Copy icon if available
if [ -f "$RESOURCES_DIR/app-icon.png" ]; then
    cp "$RESOURCES_DIR/app-icon.png" "$APP_DIR/share/icons/hicolor/256x256/apps/kreekt-tools.png"
fi

# Copy resources
if [ -d "$RESOURCES_DIR" ]; then
    cp -R "$RESOURCES_DIR"/* "$APP_DIR/resources/" 2>/dev/null || true
fi

echo "Creating AppImage..."

# Create AppImage if appimagetool is available
if command -v appimagetool >/dev/null 2>&1; then
    APPDIR="$BUILD_DIR/AppDir"
    mkdir -p "$APPDIR"

    # Copy application
    cp -R "$APP_DIR"/* "$APPDIR/"

    # Create AppRun
    cat > "$APPDIR/AppRun" << 'EOF'
#!/bin/bash
APPDIR="$(dirname "$(readlink -f "${0}")")"
exec "$APPDIR/bin/kreekt-tools" "$@"
EOF
    chmod +x "$APPDIR/AppRun"

    # Copy desktop file and icon to AppDir root
    cp "$APP_DIR/share/applications/kreekt-tools.desktop" "$APPDIR/"
    if [ -f "$APP_DIR/share/icons/hicolor/256x256/apps/kreekt-tools.png" ]; then
        cp "$APP_DIR/share/icons/hicolor/256x256/apps/kreekt-tools.png" "$APPDIR/"
    fi

    # Build AppImage
    appimagetool "$APPDIR" "$DIST_DIR/KreeKt-Tools-$ARCH.AppImage"
    chmod +x "$DIST_DIR/KreeKt-Tools-$ARCH.AppImage"

    echo "✓ AppImage created successfully"
else
    echo "⚠ appimagetool not found. AppImage creation skipped."
fi

echo "Creating TAR.XZ archive..."

# Create portable TAR.XZ
cd "$BUILD_DIR"
tar -cJf "$DIST_DIR/kreekt-tools-linux-$ARCH.tar.xz" kreekt-tools/

echo "Creating DEB package..."

# Create DEB package structure
DEB_DIR="$BUILD_DIR/deb"
mkdir -p "$DEB_DIR/DEBIAN"
mkdir -p "$DEB_DIR/opt/kreekt-tools"
mkdir -p "$DEB_DIR/usr/share/applications"
mkdir -p "$DEB_DIR/usr/share/icons/hicolor/256x256/apps"
mkdir -p "$DEB_DIR/usr/bin"

# Copy application to /opt
cp -R "$APP_DIR"/* "$DEB_DIR/opt/kreekt-tools/"

# Create system-wide launchers
cat > "$DEB_DIR/usr/bin/kreekt-tools" << 'EOF'
#!/bin/bash
exec /opt/kreekt-tools/bin/kreekt-tools "$@"
EOF

cat > "$DEB_DIR/usr/bin/kreekt-scene-editor" << 'EOF'
#!/bin/bash
exec /opt/kreekt-tools/bin/scene-editor "$@"
EOF

cat > "$DEB_DIR/usr/bin/kreekt-profiler" << 'EOF'
#!/bin/bash
exec /opt/kreekt-tools/bin/profiler "$@"
EOF

chmod +x "$DEB_DIR/usr/bin/"*

# Copy desktop file and icon
cp "$APP_DIR/share/applications/kreekt-tools.desktop" "$DEB_DIR/usr/share/applications/"
if [ -f "$APP_DIR/share/icons/hicolor/256x256/apps/kreekt-tools.png" ]; then
    cp "$APP_DIR/share/icons/hicolor/256x256/apps/kreekt-tools.png" "$DEB_DIR/usr/share/icons/hicolor/256x256/apps/"
fi

# Create DEBIAN control file
cat > "$DEB_DIR/DEBIAN/control" << EOF
Package: kreekt-tools
Version: $APP_VERSION
Section: devel
Priority: optional
Architecture: $DEB_ARCH
Maintainer: KreeKt Project <tools@kreekt.dev>
Description: 3D graphics development tools for Kotlin Multiplatform
 KreeKt Tools provides a comprehensive development environment for 3D graphics
 applications using Kotlin Multiplatform. Features include:
 .
  * Visual scene editor with real-time preview
  * Material editor with WGSL shader support
  * Animation timeline and keyframe editor
  * Performance profiler with GPU metrics
  * Cross-platform testing framework
 .
 Supports WebGPU and Vulkan rendering backends across JVM, Web, Android,
 iOS, and Native platforms.
Depends: libc6 (>= 2.17), libx11-6, libxext6, libxi6, libxrender1, libxtst6
Suggests: vulkan-tools, mesa-vulkan-drivers
Homepage: https://kreekt.dev
EOF

# Create postinst script
cat > "$DEB_DIR/DEBIAN/postinst" << 'EOF'
#!/bin/bash
set -e

# Update desktop database
if command -v update-desktop-database >/dev/null 2>&1; then
    update-desktop-database /usr/share/applications
fi

# Update icon cache
if command -v gtk-update-icon-cache >/dev/null 2>&1; then
    gtk-update-icon-cache -q /usr/share/icons/hicolor
fi

exit 0
EOF

chmod +x "$DEB_DIR/DEBIAN/postinst"

# Build DEB package
if command -v dpkg-deb >/dev/null 2>&1; then
    dpkg-deb --build "$DEB_DIR" "$DIST_DIR/kreekt-tools_${APP_VERSION}_${DEB_ARCH}.deb"
    echo "✓ DEB package created successfully"
else
    echo "⚠ dpkg-deb not found. DEB package creation skipped."
fi

echo "Creating RPM package..."

# Create RPM package if rpmbuild is available
if command -v rpmbuild >/dev/null 2>&1; then
    RPM_BUILD_DIR="$BUILD_DIR/rpm"
    mkdir -p "$RPM_BUILD_DIR"/{BUILD,RPMS,SOURCES,SPECS,SRPMS}

    # Create source tarball
    cd "$BUILD_DIR"
    tar -czf "$RPM_BUILD_DIR/SOURCES/kreekt-tools-$APP_VERSION.tar.gz" kreekt-tools/

    # Create RPM spec file
    cat > "$RPM_BUILD_DIR/SPECS/kreekt-tools.spec" << EOF
Name:           kreekt-tools
Version:        $APP_VERSION
Release:        1%{?dist}
Summary:        3D graphics development tools for Kotlin Multiplatform
License:        Apache-2.0
URL:            https://kreekt.dev
Source0:        %{name}-%{version}.tar.gz

BuildRequires:  systemd-rpm-macros
Requires:       glibc
Requires:       libX11
Requires:       libXext
Requires:       libXi
Requires:       libXrender
Requires:       libXtst

%description
KreeKt Tools provides a comprehensive development environment for 3D graphics
applications using Kotlin Multiplatform. Features include visual scene editor,
material editor with WGSL shader support, animation tools, and performance
profiler with GPU metrics.

%prep
%autosetup

%install
mkdir -p %{buildroot}/opt/kreekt-tools
cp -R * %{buildroot}/opt/kreekt-tools/

mkdir -p %{buildroot}%{_bindir}
cat > %{buildroot}%{_bindir}/kreekt-tools << 'LAUNCHER_EOF'
#!/bin/bash
exec /opt/kreekt-tools/bin/kreekt-tools "\$@"
LAUNCHER_EOF
chmod +x %{buildroot}%{_bindir}/kreekt-tools

mkdir -p %{buildroot}%{_datadir}/applications
cp share/applications/kreekt-tools.desktop %{buildroot}%{_datadir}/applications/

mkdir -p %{buildroot}%{_datadir}/icons/hicolor/256x256/apps
if [ -f share/icons/hicolor/256x256/apps/kreekt-tools.png ]; then
    cp share/icons/hicolor/256x256/apps/kreekt-tools.png %{buildroot}%{_datadir}/icons/hicolor/256x256/apps/
fi

%files
/opt/kreekt-tools/
%{_bindir}/kreekt-tools
%{_datadir}/applications/kreekt-tools.desktop
%{_datadir}/icons/hicolor/256x256/apps/kreekt-tools.png

%post
update-desktop-database %{_datadir}/applications &> /dev/null || :
touch --no-create %{_datadir}/icons/hicolor &>/dev/null || :

%postun
update-desktop-database %{_datadir}/applications &> /dev/null || :
if [ \$1 -eq 0 ] ; then
    touch --no-create %{_datadir}/icons/hicolor &>/dev/null
    gtk-update-icon-cache %{_datadir}/icons/hicolor &>/dev/null || :
fi

%posttrans
gtk-update-icon-cache %{_datadir}/icons/hicolor &>/dev/null || :

%changelog
* $(date +"%%a %%b %%d %%Y") KreeKt Project <tools@kreekt.dev> - $APP_VERSION-1
- Initial release
EOF

    # Build RPM
    rpmbuild --define "_topdir $RPM_BUILD_DIR" -ba "$RPM_BUILD_DIR/SPECS/kreekt-tools.spec"
    cp "$RPM_BUILD_DIR/RPMS/$RPM_ARCH/kreekt-tools-${APP_VERSION}-1."*".rpm" "$DIST_DIR/"

    echo "✓ RPM package created successfully"
else
    echo "⚠ rpmbuild not found. RPM package creation skipped."
fi

echo "Creating Flatpak manifest..."

# Create Flatpak manifest
cat > "$DIST_DIR/dev.kreekt.tools.yml" << EOF
app-id: dev.kreekt.tools
runtime: org.freedesktop.Platform
runtime-version: '23.08'
sdk: org.freedesktop.Sdk
sdk-extensions:
  - org.freedesktop.Sdk.Extension.openjdk17
command: kreekt-tools
finish-args:
  - --share=ipc
  - --socket=x11
  - --socket=wayland
  - --device=dri
  - --share=network
  - --filesystem=home
  - --filesystem=xdg-documents
  - --env=PATH=/app/bin:/usr/bin:/app/jre/bin
  - --env=JAVA_HOME=/app/jre

modules:
  - name: kreekt-tools
    buildsystem: simple
    build-commands:
      - mkdir -p /app/bin /app/lib /app/jre /app/resources
      - cp -R jre/* /app/jre/
      - cp lib/*.jar /app/lib/
      - cp bin/* /app/bin/
      - chmod +x /app/bin/*
      - install -Dm644 share/applications/kreekt-tools.desktop /app/share/applications/dev.kreekt.tools.desktop
      - install -Dm644 share/icons/hicolor/256x256/apps/kreekt-tools.png /app/share/icons/hicolor/256x256/apps/dev.kreekt.tools.png
    sources:
      - type: dir
        path: kreekt-tools
EOF

echo
echo "========================================"
echo "Linux Packaging completed!"
echo "========================================"
echo "Available packages:"
ls -la "$DIST_DIR"
echo "========================================"

echo "Installation instructions:"
echo
echo "AppImage:"
echo "  chmod +x KreeKt-Tools-$ARCH.AppImage"
echo "  ./KreeKt-Tools-$ARCH.AppImage"
echo
echo "TAR.XZ:"
echo "  tar -xJf kreekt-tools-linux-$ARCH.tar.xz"
echo "  ./kreekt-tools/bin/kreekt-tools"
echo
echo "DEB (Ubuntu/Debian):"
echo "  sudo dpkg -i kreekt-tools_${APP_VERSION}_${DEB_ARCH}.deb"
echo "  sudo apt-get install -f  # if dependencies missing"
echo
echo "RPM (Fedora/RHEL):"
echo "  sudo rpm -i kreekt-tools-${APP_VERSION}-1.*.rpm"
echo
echo "Flatpak:"
echo "  flatpak-builder build-dir dev.kreekt.tools.yml"
echo "  flatpak-builder --user --install build-dir dev.kreekt.tools.yml"