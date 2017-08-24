# SubServers Library Patcher: Combines BungeeCord and SubServers.Bungee/SubServers.Sync into one jar file
# Usage: "bash SubServers.Bungee.Patcher.sh <BungeeCord.jar> <SubServers.jar>"
#
#!/usr/bin/env bash
if [ -z "$1" ]
  then
    echo ERROR: No BungeeCord File Supplied
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
mkdir BuildTools
mkdir BuildTools/Vanilla.jar
mkdir BuildTools/Modded.jar
cd BuildTools/Modded.jar
echo ">> Extracting $1..."
jar xvf ../../$1; retvala=$?;
if [ $retvala -eq 0 ]
  then
    if [ -f "LICENSE.txt" ]; then
		rm -Rf LICENSE.txt
	fi
	if [ -f "LICENSE" ]; then
		rm -Rf LICENSE
	fi
	cd ../Vanilla.jar
    echo ">> Extracting $2..."
    jar xvf ../../$2; retvalb=$?;
	if [ $retvalb -eq 0 ]
	  then
		echo ">> Writing Changes..."
		yes | cp -rf . ../Modded.jar
		printf "\n " >> META-INF/MANIFEST.MF
		cd ../
		printf "# SubServers.Bungee.Patcher generated difference list (may be empty if git is not installed)\n#\n> git --no-pager diff --no-index --name-status BuildTools/Vanilla.jar BuildTools/Modded.jar\n" > MODIFICATIONS
		git --no-pager diff --no-index --name-status Vanilla.jar Modded.jar | sed -e "s/\tVanilla.jar\//\t\//" -e "s/\tModded.jar\//\t\//" >> MODIFICATIONS
		mv -f MODIFICATIONS Modded.jar
		cd Modded.jar
		echo ">> Recompiling..."
		jar cvfm ../../SubServers.Patched.jar META-INF/MANIFEST.MF .; retvalc=$?;
		if [ $retvalc -eq 0 ]
		  then
			echo ">> Cleaning Up..."
			rm -Rf ../../Buildtools
			exit 0;
		else
			echo ">> Error Recomiling Files"
			rm -Rf ../../Buildtools
			exit 4
		fi
	else
		echo ">> Error Decompiling $2 Files"
		rm -Rf ../../Buildtools
		exit 3
	fi
else
    echo ">> Error Decompiling $1 Files"
    rm -Rf ../../Buildtools
	exit 3
fi