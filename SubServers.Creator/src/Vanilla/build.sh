# SubCreator Vanilla Build Script
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
if [[ -z "$cache" ]] || [[ ! -f "$cache/Vanilla-$version.jar" ]]; then
    if [[ -d "VanillaCord" ]]; then
        rm -Rf VanillaCord
    fi
    mkdir VanillaCord
    echo Downloading the VanillaCord Launcher...
    __DL VanillaCord/VanillaCord.jar https://dev.me1312.net/jenkins/job/VanillaCord/job/master/lastSuccessfulBuild/artifact/artifacts/VanillaCord.jar; __RETURN=$?
    if [[ $__RETURN -eq 0 ]]; then
        cd VanillaCord
        echo Launching VanillaCord
        java -jar VanillaCord.jar "$version"; __RETURN=$?;
        if [[ $__RETURN -eq 0 ]]; then
            echo Copying Finished Jar...
            cd ../
            if [[ -f "Vanilla.jar" ]]; then
                if [[ -f "Vanilla.old.jar.x" ]]; then
                    rm -Rf Vanilla.old.jar.x
                fi
                mv Vanilla.jar Vanilla.old.jar.x
            fi
            if [[ ! -z "$cache" ]] && [[ -d "$cache" ]]; then
                cp "VanillaCord/out/$version-bungee.jar" "$cache/Vanilla-$version.jar"
            fi
            cp "VanillaCord/out/$version-bungee.jar" Vanilla.jar
            echo Cleaning Up...
            rm -Rf VanillaCord
            rm -Rf "$0"
            exit 0
        else
            echo ERROR: VanillaCord exited with an error. Please try again
            rm -Rf VanillaCord
            rm -Rf "$0"
            exit 4
        fi
    else
        echo ERROR: Failed Downloading Patcher. Is Github.com down?
        rm -Rf VanillaCord
        rm -Rf "$0"
        exit 3
    fi
else
    echo Copying Cached Jar...
    cp "$cache/Vanilla-$version.jar" Vanilla.jar
    echo Cleaning Up...
    rm -Rf "$0"
    exit 0
fi
exit 2