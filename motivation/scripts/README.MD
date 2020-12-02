# Database access code coverage analysis scripts and tools

	
##	How To 
	-	Download csv files archive from libraries.io. (https://zenodo.org/record/3626071)
	-	Import  'dependencies' & 'projects_with_repository_fields' csv files into a sql database (example for SQLite database :
			.import PATHTOLIBRARIESIOCSV dependencies
	- 	Run data init process of mainDbCoverage.py python program with required arguments (example : --datainit --librariesiodb [LIBRARIESIODB] --dpackage junit:junit [RESULTDB] [CLONE_FOLDER] [FILEOUTPUT_FOLDER]
		This program will :
			* 	Initialize the results database table 'project_analysed' with projects from librariesio database depending on given DEPENDENCY_PACKAGE
			* 	Clone Github projects into the TARGET_CLONE_FOLDER
			* 	Scan the source code of the cloned projects looking for java SQL import statements.
			
	-	Install SQLInspect (https://bitbucket.org/csnagy/sqlinspect/src/master/)
	-	[OPTIONNAL]	For multiple hotspots search (JPA + Spring + Hibernate + JDBC) SQLInspect : run first sqlinspectinit.sh script in the root folder where projects were cloned.
			This script will create a file 'classpathall2.txt' containing path jar dependencies required by SQLInspect multiple hotspots search process.
	-	Run sqlinspect python program. (--sqlinspect --sqlinspectpath [PATHTOEXECUTABLE_SQLINSPECT] [RESULTDB] [CLONE_FOLDER] [FILEOUTPUT_FOLDER])
			Add option --multihotspot for multiple hotspots search
			It will :
			* 	Modify 'project_analysed' table with corresponding flags if queries are found.
			*	Produce PROJECTNAME-queries.xml files containing queries found in the project.
	-	Run jacoco python program (--jacoco --mvnpath [MVNCMDPATH] [RESULTDB] [CLONE_FOLDER] [FILEOUTPUT_FOLDER])
			It will :
			*	Modify the root project pom.xml file in order to add Jacoco dependency (see below for the dependency added). 
			*	Run 'mvn clean' command, and produce PROJECTNAME-mvncleanconsole.log
			*	Run 'mvn -DskipTests=false -f POMPATH test' command and produce PROJECTNAME-mvntestconsole.log
			*	Update 'projects_analysed' table with corresponding flags.
			*	Produce PROJECTNAME-testcoverageX.xml files if mvn test command is successfully completed
	-	Run java program (example : [SQL_INSPECT_FILE_FOLDER] [COVERAGE_FOLDER] [TEST_REPORT_FOLDER] true [RESULTSDB] true)
			It will :
			*	Read *-queries.xml files and fill 'queries' & 'query_construction' tables
			*	Read *-testcoverage.xml files and fill 'coverage' table
			*	Computes some measures and creates 'project_statistics' table
	-	Summary.sql contains a few queries helping with the producing of summary results.

## Program usage

	```
	Python usage :
	usage: mainDbCoverage.py [-h] [--librariesiodb LIBRARIESIODB]
                         [--dpackage DPACKAGE] [--datainit] [--sqlinspect]
                         [--sqlinspectpath SQLINSPECTPATH] [--singlehotspot]
                         [--jacoco] [--executionline] [--mvnpath MVNPATH]
                         resultsdb clonedirectory outputdirectory
	```
	```
	Java Program Usage : 
	Usage: com.coverage.QueryCoverageAnalyzer <sqlinspect xmls dir> <coverage xmls dir> <test reports dir> <maxNameConvention:true|false> <results db> <truncate>
	```
	

### Jacoco dependency 
```
<plugin>
	    <groupId>org.jacoco</groupId>
	    <artifactId>jacoco-maven-plugin</artifactId>
	    <version>0.8.5</version>
	    <executions>
	        <execution>
	            <goals>
	                <goal>prepare-agent</goal>
	            </goals>
	        </execution>
	        <execution>
	            <id>report</id>
	            <phase>test</phase>
	            <goals>
	                <goal>report</goal>
	            </goals>
	        </execution>
	    </executions>
        </plugin>
```