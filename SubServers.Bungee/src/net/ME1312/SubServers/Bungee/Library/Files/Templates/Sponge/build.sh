# SubCreator Sponge Build Script
# Usage: "bash build.sh <sponge version>"
#
#!/usr/bin/env bash
if [ -z "$1" ]
  then
    echo ERROR: No Build Version Supplied
    rm -Rf "$0"
    exit 1
fi
function __DL() {
    if [ -x "$(command -v wget)" ]; then
        wget -O "$1" "$2"; return $?
    else
        curl -o "$1" "$2"; return $?
    fi
}
echo Downloading SpongeVanilla...
__DL Sponge.jar "https://repo.spongepowered.org/maven/org/spongepowered/spongevanilla/$1/spongevanilla-$1.jar"; __RETURN=$?
if [ $__RETURN -eq 0 ]; then
    echo Cleaning Up...
    rm -Rf "$0"
    exit 0
else
    echo ERROR: Failed downloading Sponge. Is MinecraftForge.net down?
    rm -Rf "$0"
    exit 3
fi
exit 2