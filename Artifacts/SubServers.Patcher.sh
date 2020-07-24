# SubServers Library Patcher
#
# Can be used to combine the following into one jar file:
#  -> BungeeCord and SubServers.Bungee
#  -> BungeeCord and SubServers.Sync
#  -> GalaxiEngine and SubServers.Host
#
# Usage: "bash SubServers.Patcher.sh <Platform.jar> <SubServers.jar>"
#
#!/usr/bin/env bash
if [ -z "$1" ]
  then
    echo "SubServers Library Patcher"
	echo ""
	echo "Can be used to combine the following into one jar file:"
	echo " -> BungeeCord and SubServers.Bungee"
	echo " -> BungeeCord and SubServers.Sync"
	echo " -> GalaxiEngine and SubServers.Host"
	echo ""
    echo "Usage: bash $0 <Platform.jar> <SubServers.jar>"
    exit 1
fi
if [ ! -f "$1" ]
  then
    echo ERROR: Cannot find $1
    exit 2
fi
if [ -z "$2" ]
  then
    echo ERROR: No SubServers File Supplied
    exit 1
fi
if [ ! -f "$2" ]
  then
    echo ERROR: Cannot find $2
    exit 2
fi
if [ -d "SubServers.Patcher" ]; then
    rm -Rf SubServers.Patcher
fi
echo ">> Extracting $1..."
mkdir SubServers.Patcher
mkdir SubServers.Patcher/Patched.jar
cd SubServers.Patcher/Patched.jar
jar xvf "../../$1"; __RETURN=$?;
if [ $__RETURN -eq 0 ]
  then
    if [ -f "LICENSE.txt" ]; then
        rm -Rf LICENSE.txt
    fi
    if [ -f "LICENSE" ]; then
        rm -Rf LICENSE
    fi
    if [ -f "META-INF/MANIFEST.MF" ]; then
        cat META-INF/MANIFEST.MF | sed 's/\r$//' | sed ':a;N;$!ba;s/\n //g' | sed -e "/^\s*$/d" -e "/^Main-Class:.*$/d" -e "/^Implementation-Title:.*$/d" -e "/^Specification-Title:.*$/d" -e "/^Build-Jdk:.*$/d" -e "/^Created-By:.*$/d" -e "/^Built-By:.*$/d" > ../MANIFEST.MF
    else
        printf "Manifest-Version: 1.0\n" > ../MANIFEST.MF
    fi
    if [ -f "MODIFICATIONS" ]; then
        mv -f MODIFICATIONS ../MODIFICATIONS
    fi
    echo ">> Extracting $2..."
    mkdir ../Original.jar
    cd ../Original.jar
    jar xvf "../../$2"; __RETURN=$?;
    if [ $__RETURN -eq 0 ]
      then
        echo ">> Writing Changes..."
        if [ -f "META-INF/MANIFEST.MF" ]
          then #     (Read File)      (Convert to LF)    (Rejoin Split Lines)      (Omit Empty, Duplicate, and Unnecessary Properties)
            cat META-INF/MANIFEST.MF | sed 's/\r$//' | sed ':a;N;$!ba;s/\n //g' | sed -e "/^\s*$/d" -e "/^Manifest-Version:.*$/d" -e "/^Class-Path:.*$/d" -e "/^Build-Jdk:.*$/d" -e "/^Created-By:.*$/d" -e "/^Built-By:.*$/d" >> ../MANIFEST.MF
        else
            if [ ! -d "META-INF" ]; then
                mkdir META-INF
            fi
        fi
        if [ -f "MODIFICATIONS" ]; then
            cat MODIFICATIONS >> ../MODIFICATIONS
        fi
        yes | cp -rf . ../Patched.jar
        cd ../
        printf "Built-By: SubServers.Patcher\n" >> MANIFEST.MF
        cp -f MANIFEST.MF Patched.jar/META-INF
        if [ -f "Patched.jar/bungee.yml" ]; then
            rm -Rf Patched.jar/bungee.yml
        fi
        if [ ! -f "MODIFICATIONS" ]; then
            printf "# SubServers.Patcher generated difference list (may be empty if git is not installed)\n#\n" > MODIFICATIONS
        fi
        printf "@ `date`\n> git --no-pager diff --no-index --name-status SubServers.Patcher/Original.jar SubServers.Patcher/Patched.jar\n" >> MODIFICATIONS
        git --no-pager diff --no-index --name-status Original.jar Patched.jar | sed -e "s/\tOriginal.jar\//\t\//" -e "s/\tPatched.jar\//\t\//" >> MODIFICATIONS
        cp -f MODIFICATIONS Patched.jar
        cd Patched.jar
        echo ">> Recompiling..."
        if [ -f "../../SubServers.Patched.jar" ]; then
            rm -Rf ../../SubServers.Patched.jar
        fi
        jar cvfm ../../SubServers.Patched.jar META-INF/MANIFEST.MF .; __RETURN=$?;
        if [ $__RETURN -eq 0 ]
          then
            echo ">> Cleaning Up..."
            cd ../../
            rm -Rf SubServers.Patcher
            exit 0;
        else
            echo ">> Error Recomiling Files"
            exit 4
        fi
    else
        echo ">> Error Decompiling $2"
        exit 3
    fi
else
    echo ">> Error Decompiling $1"
    exit 3
fi