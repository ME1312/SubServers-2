# SubCreator Spigot Build Script
# Usage: "bash build.sh <version> [cache]"
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
if [ -z "$2" ] || [ ! -f "$2/Spigot-$1.jar" ]; then
    echo Downloading Buildtools...
    __DL Buildtools.jar https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar; __RETURN=$?
    if [ $__RETURN -eq 0 ]; then
        if [ -d "Buildtools" ]; then
            rm -Rf Buildtools
        fi
        mkdir Buildtools
        cd "Buildtools"
        echo Launching Buildtools
        if [ ! -z "$2" ] && [ -d "$2" ]; then
            export __HOME="$HOME"
            export HOME="$2"
        fi
        export MAVEN_OPTS="-Xms2G"
        java -Xms2G -jar ../Buildtools.jar --rev "$1"; __RETURN=$?
        if [ ! -z "$2" ] && [ ! -z "$__HOME" ] && [ "$2" == "$HOME" ]; then
            export HOME="$__HOME"
        fi
        cd ../
        if [ $__RETURN -eq 0 ]; then
            echo Copying Finished Jar...
            if [ ! -z "$2" ] && [ -d "$2" ]; then
                cp Buildtools/spigot-*.jar "$2/Spigot-$1.jar"
            fi
            cp Buildtools/spigot-*.jar Spigot.jar
            echo Cleaning Up...
            rm -Rf Buildtools.jar
            rm -Rf Buildtools
            rm -Rf "$0"
            exit 0
        else
            echo ERROR: Buildtools exited with an error. Please try again
            rm -Rf Buildtools.jar
            rm -Rf Buildtools
            rm -Rf "$0"
            exit 4
        fi
    else
        echo ERROR: Failed downloading Buildtools. Is SpigotMC.org down?
        rm -Rf "$0"
        exit 3
    fi
else
    echo Copying Cached Jar...
    cp "$2/Spigot-$1.jar" Spigot.jar
    echo Cleaning Up...
    rm -Rf "$0"
    exit 0
fi
exit 2