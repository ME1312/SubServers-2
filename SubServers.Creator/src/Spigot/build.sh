# SubCreator Spigot Build Script
#
#!/usr/bin/env bash
if [[ -z "$version" ]]
  then
    echo ERROR: No Build Version Supplied
    rm -Rf "$0"
    exit 1
fi
function __DL() {
    if [[ -x "$(command -v wget)" ]]; then
        wget -O "$1" "$2"; return $?
    else
        curl -o "$1" "$2"; return $?
    fi
}
if [[ -z "$cache" ]] || [[ ! -f "$cache/Spigot-$version.jar" ]] || [[ "$mode" == "UPDATE" && $(find "$cache/Spigot-$version.jar" -mtime +1 -print) ]]; then
    echo Downloading Buildtools...
    __DL Buildtools.jar https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar; __RETURN=$?
    if [[ $__RETURN -eq 0 ]]; then
        if [[ -d "Buildtools" ]]; then
            rm -Rf Buildtools
        fi
        mkdir Buildtools
        cd "Buildtools"
        echo Launching Buildtools
        if [[ ! -z "$cache" ]] && [[ -d "$cache" ]]; then
            export __HOME="$HOME"
            export HOME="$cache"
        fi
        export MAVEN_OPTS="-Xms2G"
        java -Xms2G -jar ../Buildtools.jar --rev "$version"; __RETURN=$?
        if [[ ! -z "$cache" ]] && [[ ! -z "$__HOME" ]] && [[ "$cache" == "$HOME" ]]; then
            export HOME="$__HOME"
        fi
        cd ../
        if [[ $__RETURN -eq 0 ]]; then
            echo Copying Finished Jar...
            if [[ -f "Spigot.jar" ]]; then
                if [[ -f "Spigot.old.jar.x" ]]; then
                    rm -Rf Spigot.old.jar.x
                fi
                mv Spigot.jar Spigot.old.jar.x
            fi
            if [[ ! -z "$cache" ]] && [[ -d "$cache" ]]; then
                if [[ -f "$cache/Spigot-$version.jar" ]]; then
                    rm -Rf "$cache/Spigot-$version.jar"
                fi
                cp Buildtools/spigot-*.jar "$cache/Spigot-$version.jar"
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
    cp "$cache/Spigot-$version.jar" Spigot.jar
    echo Cleaning Up...
    rm -Rf "$0"
    exit 0
fi
exit 2