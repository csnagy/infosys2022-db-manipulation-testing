	#!/bin/bash

for f in `find "$(pwd)" -type d -mindepth 1 -maxdepth 1 -not -path '*/\.*'`; 
	do 
		cd "$f";
		FILE=${f}/classpath-all2.txt
		if test -f "$FILE"; then 
			echo "$FILE exists"
		else
			mvn dependency:build-classpath -DSkipTests -Dsilent=true -Dmdep.outputFile=classpath.txt
			for d in `find . -name classpath.txt`; do cat $d; echo ""; done > classpath-all2.txt 
			echo "$FILE created"
		fi
	done  