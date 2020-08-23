# SubCreator Paper Build Script
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
function __Restore() {
    if [[ -f "Paper.old.jar.x" ]]; then
        if [[ -f "Paper.jar" ]]; then
            rm -Rf Paper.jar
        fi
        mv Paper.old.jar.x Paper.jar
    fi
}
echo Downloading Paper...
if [[ -f "Paper.jar" ]]; then
    if [[ -f "Paper.old.jar.x" ]]; then
        rm -Rf Paper.old.jar.x
    fi
    mv Paper.jar Paper.old.jar.x
fi
__DL Paper.jar "https://papermc.io/api/v1/paper/$version/latest/download"; __RETURN=$?
if [[ $__RETURN -eq 0 ]]; then
    if [[ $(stat -c%s "Paper.jar") -ge 1000000 ]]; then
        echo Cleaning Up...
        rm -Rf "$0"
        exit 0
    else
        echo ERROR: Received invalid jarfile when requesting Paper version $version:
        cat Paper.jar
        printf "\n"
        __Restore
        rm -Rf "$0"
        exit 4
    fi
else
	  echo ERROR: Failed downloading Paper. Is PaperMC.io down?
	  __Restore
	  rm -Rf "$0"
	  exit 3
fi
exit 2