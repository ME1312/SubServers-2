# SubCreator Sponge Forge Build Script
# Usage: "bash build.sh <forge version> <sponge version>"
#
#!/usr/bin/env bash
if [ -z "$1" ] || [ -z "$2" ]
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
echo Downloading the Minecraft Forge Installer...
__DL "forge-$1-installer.jar" "http://files.minecraftforge.net/maven/net/minecraftforge/forge/$1/forge-$1-installer.jar"; __RETURN=$?
if [ $__RETURN -eq 0 ]; then
    echo Installing Minecraft Forge...
    java -jar "forge-$1-installer.jar" --installServer; __RETURN=$?
    if [ $__RETURN -eq 0 ]; then
        echo Cleaning Up...
        rm -Rf "forge-$1-installer.jar"
        rm -Rf "forge-$1-installer.jar.log"
        mv -f "forge-$1-universal.jar" Forge.jar
        if [ ! -d "mods" ]; then
            mkdir mods
        fi
        echo Downloading SpongeForge...
        __DL mods/Sponge.jar "https://repo.spongepowered.org/maven/org/spongepowered/spongeforge/$2/spongeforge-$2.jar"; __RETURN=$?
        if [ $__RETURN -eq 0 ]; then
            echo Cleaning Up...
            rm -Rf "$0"
            exit 0
        else
            echo ERROR: Failed downloading Sponge. Is MinecraftForge.net down?
            rm -Rf "$0"
            exit 5
        fi
    else
        echo ERROR: The Installer exited with an error. Please try again
        rm -Rf "forge-$1-installer.jar"
        rm -Rf "forge-$1-installer.jar.log"
        rm -Rf "$0"
        exit 4
    fi
else
    echo ERROR: Failed downloading Forge. Is MinecraftForge.net down?
    rm -Rf "$0"
    exit 3
fi
exit 2