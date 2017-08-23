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
cd BuildTools
echo ">> Extracting $1..."
jar xvf ../$1; retvala=$?;
if [ $retvala -eq 0 ]
  then
    echo ">> Extracting $2..."
    jar xvf ../$2; retvalb=$?;
	if [ $retvalb -eq 0 ]
	  then
		if [ -d "../SubServers.Patched.jar" ]; then
			rm -Rf ../SubServers.Patched.jar
		fi
		echo ">> Recompiling..."
		jar cvfm ../SubServers.Patched.jar META-INF/MANIFEST.MF .; retvalc=$?;
		if [ $retvalc -eq 0 ]
		  then
			echo ">> Cleaning Up..."
			rm -Rf ../Buildtools
			exit 0;
		else
			echo ">> Error Recomiling Files"
			rm -Rf ../Buildtools
			exit 4
		fi
	else
		echo ">> Error Decompiling $2 Files"
		rm -Rf ../Buildtools
		exit 3
	fi
else
    echo ">> Error Decompiling $1 Files"
    rm -Rf ../Buildtools
	exit 3
fi