# SubServers Library Patcher: Combines BungeeCord and SubServers.Bungee/SubServers.Sync into one jar file
# Usage: "bash SubServers.Bungee.Patcher.sh <BungeeCord.jar> <SubServers.jar>"
#
#!/usr/bin/env bash
if [ -z "$1" ]
  then
    echo "SubServers Library Patcher: Combines BungeeCord and SubServers.Bungee/SubServers.Sync into one jar file"
    echo "Usage: bash $0 <BungeeCord.jar> <SubServers.jar>"
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
if [ -d "Buildtools" ]; then
    rm -Rf Buildtools
fi
echo ">> Extracting $1..."
mkdir BuildTools
mkdir BuildTools/Modded.jar
cd BuildTools/Modded.jar
jar xvf ../../$1; retvala=$?;
if [ $retvala -eq 0 ]
  then
    if [ -f "LICENSE.txt" ]; then
        rm -Rf LICENSE.txt
    fi
    if [ -f "LICENSE" ]; then
        rm -Rf LICENSE
    fi
    if [ -f "META-INF/MANIFEST.MF" ]; then
        cat META-INF/MANIFEST.MF | sed -e "/^\s*$/d" -e "/^Main-Class:.*$/d" -e "/^Implementation-Title:.*$/d" -e "/^Build-Jdk:.*$/d" -e "/^Created-By:.*$/d" -e "/^Built-By:.*$/d" > ../MANIFEST.MF
    fi
    if [ -f "MODIFICATIONS" ]; then
        mv -f MODIFICATIONS ../MODIFICATIONS
    fi
    echo ">> Extracting $2..."
    mkdir ../Vanilla.jar
    cd ../Vanilla.jar
    jar xvf ../../$2; retvalb=$?;
    if [ $retvalb -eq 0 ]
      then
        echo ">> Writing Changes..."
        if [ -f "META-INF/MANIFEST.MF" ]
          then
            cat META-INF/MANIFEST.MF | sed -e "/^\s*$/d" -e "/^Manifest-Version:.*$/d" -e "/^Class-Path:.*$/d" >> ../MANIFEST.MF
        else
            if [ ! -d "META-INF" ]; then
                mkdir META-INF
            fi
        fi
        if [ -f "MODIFICATIONS" ]; then
            cat MODIFICATIONS >> ../MODIFICATIONS
        fi
        yes | cp -rf . ../Modded.jar
        cd ../
        printf "Built-By: SubServers.Bungee.Patcher\n" >> MANIFEST.MF
        cp -f MANIFEST.MF Modded.jar/META-INF
        if [ ! -f "MODIFICATIONS" ]; then
            printf "# SubServers.Bungee.Patcher generated difference list (may be empty if git is not installed)\n#\n" > MODIFICATIONS
        fi
        printf "@ `date`\n> git --no-pager diff --no-index --name-status BuildTools/Vanilla.jar BuildTools/Modded.jar\n" >> MODIFICATIONS
        git --no-pager diff --no-index --name-status Vanilla.jar Modded.jar | sed -e "s/\tVanilla.jar\//\t\//" -e "s/\tModded.jar\//\t\//" >> MODIFICATIONS
        cp -f MODIFICATIONS Modded.jar
        cd Modded.jar
        echo ">> Recompiling..."
        if [ -f "../../SubServers.Patched.jar" ]; then
            rm -Rf ../../SubServers.Patched.jar
        fi
        jar cvfm ../../SubServers.Patched.jar ../MANIFEST.MF .; retvalc=$?;
        if [ $retvalc -eq 0 ]
          then
            echo ">> Cleaning Up..."
            cd ../../
            rm -Rf BuildTools
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