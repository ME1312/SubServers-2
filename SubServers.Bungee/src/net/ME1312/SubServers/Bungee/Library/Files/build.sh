# Version: 2.11.2a+
#
# SubCreator Build Script
# Usage: "bash build.sh <version> <software> [jre]"
#
#!/usr/bin/env bash
if [ -z "$1" ]
  then
    echo ERROR: No Build Version Supplied
    rm -Rf build-subserver.sh
    exit 1
fi
if [ -z "$2" ]
    then
    echo ERROR: No Server Software Supplied
    rm -Rf build-subserver.sh
    exit 1
fi
echo ---------- SERVER BUILD START ----------
if [ $2 == bukkit ] || [ $2 == spigot ]
    then
    echo Downloading Buildtools...
    curl -o BuildTools.jar https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar; retvalb=$?
    if [ $retvalb -eq 0 ]; then
        echo Downloaded Buildtools!
    else
        echo ERROR: Failed Downloading Buildtools. Is SpigotMC.org down?
        rm -Rf build-subserver.sh
        exit 3
    fi
    if [ -d "Buildtools" ]
    then
        rm -Rf Buildtools
    fi
    mkdir Buildtools
    cd "Buildtools"
    echo Building CraftBukkit/Spigot Jarfiles...
    export MAVEN_OPTS="-Xmx2G"
    if [ -z "$3" ]
    then
        java -Xmx2G -jar ../BuildTools.jar --rev $1; retvalc=$?
    else
        HOME=$3 java -Xmx2G -jar ../BuildTools.jar --rev $1; retvalc=$?
    fi
    cd ../
    if [ $retvalc -eq 0 ]; then
        echo CraftBukkit/Spigot Jarfiles Built!
        if [ $2 == "spigot" ]; then
            cp Buildtools/spigot-*.jar Spigot.jar
        else
            cp Buildtools/craftbukkit-*.jar Craftbukkit.jar
        fi
        echo Added Jarfiles!
        echo Cleaning Up...
        rm -Rf BuildTools.jar
        rm -Rf Buildtools
        echo ---------- END SERVER BUILD ----------
        rm -Rf build-subserver.sh
        exit 0
    else
        echo ERROR: Buildtools exited with an error. Please try again
        rm -Rf BuildTools.jar
        rm -Rf Buildtools
        rm -Rf build-subserver.sh
        exit 4
    fi
else
    if [ $2 == "vanilla" ]; then
        if [ -d "Buildtools" ]
        then
            rm -Rf Buildtools
        fi
        mkdir Buildtools
        mkdir Buildtools/Vanilla
        echo Downloading Vanilla Server Jarfile
        curl -o Buildtools/Vanilla/minecraft_server.$1.jar https://s3.amazonaws.com/Minecraft.Download/versions/$1/minecraft_server.$1.jar; retvald=$?
        if [ $retvald -eq 0 ]; then
            echo Downloading Vanilla Patches...
            curl -o Buildtools/Vanilla/bungee-patch.jar https://raw.githubusercontent.com/ME1312/SubServers-2/master/SubServers.Bungee/Vanilla-Patch.jar; retvale=$?
            if [ $retvale -eq 0 ]; then
                echo Patching Vanilla for BungeeCord Support
                cd Buildtools/Vanilla
                java -jar bungee-patch.jar $1; retvalf=$?;
                if [ $retvalf -eq 0 ]; then
                    echo Patched Vanilla Jar!
                    cd ../../
                    cp Buildtools/Vanilla/out/$1-bungee.jar Buildtools/vanilla-$1.jar
                    cp Buildtools/Vanilla/out/$1-bungee.jar Vanilla.jar
                    echo Added Jarfiles!
                    echo Cleaning Up...
                    rm -Rf Buildtools
                    echo ---------- END SERVER BUILD ----------
                    rm -Rf build-subserver.sh
                    exit 0
                else
                    echo ERROR: Failed Applying Patch.
                    rm -Rf Buildtools
                    rm -Rf build-subserver.sh
                    exit 5
                fi
            else
                echo ERROR: Failed Downloading Patch. Is Github.com down?
                rm -Rf Buildtools
                rm -Rf build-subserver.sh
                exit 4
            fi
        else
            echo ERROR: Failed Downloading Jarfile. Is Minecraft.net down?
            rm -Rf Buildtools
            rm -Rf build-subserver.sh
            exit 3
        fi
    else
        if [ $2 == "sponge" ]; then
            IFS='::' read -r -a version <<< "$1"
            sversion=$(echo ${version[@]:1} | tr -d ' ')
            echo Downloading Minecraft Forge
            curl -o forge-${version[0]}-installer.jar http://files.minecraftforge.net/maven/net/minecraftforge/forge/${version[0]}/forge-${version[0]}-installer.jar; retvalg=$?
            if [ $retvalg -eq 0 ]; then
                echo Installing Minecraft Forge Server
                java -jar ./forge-${version[0]}-installer.jar --installServer; retvalh=$?
                if [ $retvalh -eq 0 ]; then
                    mkdir ./mods
                    echo Downloading SpongeForge
                    curl -o mods/Sponge.jar http://files.minecraftforge.net/maven/org/spongepowered/spongeforge/$sversion/spongeforge-$sversion.jar; retvali=$?
                    if [ $retvali -eq 0 ]; then
                        echo Cleaning Up...
                        rm -Rf forge-${version[0]}-installer.jar
                        rm -Rf forge-${version[0]}-installer.jar.log
                        mv -f forge-${version[0]}-universal.jar Forge.jar
                        echo ---------- END SERVER BUILD ----------
                        rm -Rf build-subserver.sh
                        exit 0
                    else
                        echo ERROR: Failed Downloading Jarfile. Is MinecraftForge.net down?
                        rm -Rf forge-${version[0]}-installer.jar
                        rm -Rf forge-${version[0]}-installer.jar.log
                        rm -Rf forge-${version[0]}-universal.jar
                        rm -Rf build-subserver.sh
                        exit 5
                    fi
                else
                    echo ERROR: Failed Installing Forge.
                    rm -Rf forge-${version[0]}-installer.jar
                    rm -Rf forge-${version[0]}-installer.jar.log
                    rm -Rf build-subserver.sh
                    exit 4
                fi
            else
                echo ERROR: Failed Downloading Jarfile. Is MinecraftForge.net down?
                rm -Rf build-subserver.sh
                exit 3
            fi
        fi
    fi
fi
exit 2