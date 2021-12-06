# SubCreator Purpur Build Script
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
        curl -Lo "$1" "$2"; return $?
    fi
}
function __Restore() {
    if [[ -f "Purpur.old.jar.x" ]]; then
        if [[ -f "Purpur.jar" ]]; then
            rm -Rf Purpur.jar
        fi
        mv Purpur.old.jar.x Purpur.jar
    fi
}
echo Downloading Purpur...
if [[ -f "Purpur.jar" ]]; then
    if [[ -f "Purpur.old.jar.x" ]]; then
        rm -Rf Purpur.old.jar.x
    fi
    mv Purpur.jar Purpur.old.jar.x
fi
__DL Purpur.jar "https://api.purpurmc.org/v2/purpur/$version/latest/download"; __RETURN=$?
if [[ $__RETURN -eq 0 ]]; then
    if [[ $(stat -c%s "Purpur.jar") -ge 1000000 ]]; then
        echo Cleaning Up...
        rm -Rf "$0"
        exit 0
    else
        echo ERROR: Received invalid jarfile when requesting Purpur version $version:
        cat Purpur.jar
        printf "\n"
        __Restore
        rm -Rf "$0"
        exit 4
    fi
else
    echo ERROR: Failed downloading Purpur. Is PurpurMC.org down?
    __Restore
    rm -Rf "$0"
    exit 3
fi
exit 2