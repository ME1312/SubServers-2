# Version: 2.11.2b+
#
# SubCreator Build Script
# Usage: "bash build.sh <version> <software> [jre]"
#
#!/usr/bin/env bash
if [ -z "$1" ]
  then
    echo ERROR: No Build Version Supplied
    rm -Rf $0
    exit 1
fi
if [ -z "$2" ]
    then
    echo ERROR: No Server Software Supplied
    rm -Rf $0
    exit 1
fi
function __DL() {
    if [ hash wget 2>/dev/null ]; then
        wget -o $1 $2; return $?
    else
        curl -o $1 $2; return $?
    fi
}
if [ $2 == bukkit ] || [ $2 == spigot ]; then
    echo Downloading Buildtools...
    __DL BuildTools.jar https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar; retvalb=$?
    if ! [ $retvalb -eq 0 ]; then
        echo ERROR: Failed Downloading Buildtools. Is SpigotMC.org down?
        rm -Rf $0
        exit 3
    fi
    if [ -d "Buildtools" ]; then
        rm -Rf Buildtools
    fi
    mkdir Buildtools
    cd "Buildtools"
    echo Launching BuildTools.jar
    export MAVEN_OPTS="-Xms2G"
    if [ -z "$3" ]; then
        java -Xms2G -jar ../BuildTools.jar --rev $1; retvalc=$?
    else
        HOME=$3 java -Xms2G -jar ../BuildTools.jar --rev $1; retvalc=$?
    fi
    cd ../
    if [ $retvalc -eq 0 ]; then
        echo Copying Final Jar...
        if [ $2 == "spigot" ]; then
            cp Buildtools/spigot-*.jar Spigot.jar
        else
            cp Buildtools/craftbukkit-*.jar Craftbukkit.jar
        fi
        echo Cleaning Up...
        rm -Rf BuildTools.jar
        rm -Rf Buildtools
        rm -Rf $0
        exit 0
    else
        echo ERROR: Buildtools exited with an error. Please try again
        rm -Rf BuildTools.jar
        rm -Rf Buildtools
        rm -Rf $0
        exit 4
    fi
else
    if [ $2 == "vanilla" ]; then
        if [ -d "Buildtools" ]; then
            rm -Rf Buildtools
        fi
        mkdir Buildtools
        mkdir Buildtools/Vanilla
        echo Downloading Vanilla Jar...
        __DL Buildtools/Vanilla/minecraft_server.$1.jar https://s3.amazonaws.com/Minecraft.Download/versions/$1/minecraft_server.$1.jar; retvald=$?
        if [ $retvald -eq 0 ]; then
            echo Downloading Vanilla Patches...
            __DL Buildtools/Vanilla/bungee-patch.jar https://raw.githubusercontent.com/ME1312/SubServers-2/master/SubServers.Bungee/Vanilla-Patch.jar; retvale=$?
            if [ $retvale -eq 0 ]; then
                echo Patching Vanilla for BungeeCord Support
                cd Buildtools/Vanilla
                if [ -z "$3" ]; then
                    java -jar bungee-patch.jar $1; retvalf=$?;
                else
                    HOME=$3 java -jar bungee-patch.jar $1; retvalf=$?;
                fi
                if [ $retvalf -eq 0 ]; then
                    echo Copying Final Jar...
                    cd ../../
                    cp Buildtools/Vanilla/out/$1-bungee.jar Buildtools/vanilla-$1.jar
                    cp Buildtools/Vanilla/out/$1-bungee.jar Vanilla.jar
                    echo Cleaning Up...
                    rm -Rf Buildtools
                    rm -Rf $0
                    exit 0
                else
                    echo ERROR: Failed Applying Patch.
                    rm -Rf Buildtools
                    rm -Rf $0
                    exit 5
                fi
            else
                echo ERROR: Failed Downloading Patch. Is Github.com down?
                rm -Rf Buildtools
                rm -Rf $0
                exit 4
            fi
        else
            echo ERROR: Failed Downloading Jarfile. Is Minecraft.net down?
            rm -Rf Buildtools
            rm -Rf $0
            exit 3
        fi
    else
        if [ $2 == "sponge" ]; then
            IFS='::' read -r -a version <<< "$1"
            sversion=$(echo ${version[@]:1} | tr -d ' ')
            echo Downloading Minecraft Forge...
            __DL forge-${version[0]}-installer.jar http://files.minecraftforge.net/maven/net/minecraftforge/forge/${version[0]}/forge-${version[0]}-installer.jar; retvalg=$?
            if [ $retvalg -eq 0 ]; then
                echo Installing Minecraft Forge Server...
                if [ -z "$3" ]; then
                    java -jar ./forge-${version[0]}-installer.jar --installServer; retvalh=$?
                else
                    HOME=$3 java -jar ./forge-${version[0]}-installer.jar --installServer; retvalh=$?
                fi
                if [ $retvalh -eq 0 ]; then
                    mkdir ./mods
                    echo Downloading SpongeForge...
                    __DL mods/Sponge.jar http://files.minecraftforge.net/maven/org/spongepowered/spongeforge/$sversion/spongeforge-$sversion.jar; retvali=$?
                    if [ $retvali -eq 0 ]; then
                        echo Cleaning Up...
                        rm -Rf forge-${version[0]}-installer.jar
                        rm -Rf forge-${version[0]}-installer.jar.log
                        mv -f forge-${version[0]}-universal.jar Forge.jar
                        rm -Rf $0
                        exit 0
                    else
                        echo ERROR: Failed Downloading Jarfile. Is MinecraftForge.net down?
                        rm -Rf forge-${version[0]}-installer.jar
                        rm -Rf forge-${version[0]}-installer.jar.log
                        rm -Rf forge-${version[0]}-universal.jar
                        rm -Rf $0
                        exit 5
                    fi
                else
                    echo ERROR: Failed Installing Forge.
                    rm -Rf forge-${version[0]}-installer.jar
                    rm -Rf forge-${version[0]}-installer.jar.log
                    rm -Rf $0
                    exit 4
                fi
            else
                echo ERROR: Failed Downloading Jarfile. Is MinecraftForge.net down?
                rm -Rf $0
                exit 3
            fi
        fi
    fi
fi
echo ERROR: Unknown Server Software
exit 2