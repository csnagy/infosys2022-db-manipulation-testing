# Database access code coverage analysis scripts and tools

	
##	**INITIALISATION**
-	Download csv files archive from libraries.io. (https://zenodo.org/record/3626071)
-	Import  'dependencies' & 'projects_with_repository_fields' csv files into a sql database, SQLite database command :
		
	```	.import PATHTOLIBRARIESIOCSV dependencies ```
- 	``` mainDBCoverage.py --datainit --librariesiodb [LIBRARIESIODB] --dpackage junit:junit [RESULTDB] [CLONE_FOLDER] [FILEOUTPUT_FOLDER] ```
	* 	Initializes the results database table 'project_analysed' with projects from librariesio database depending on given DEPENDENCY_PACKAGE
	* 	Clones Github projects into the CLONE_FOLDER
	* 	Scans the source code of the cloned projects looking for java SQL import statements.
			
-	Install **SQLInspect** (https://bitbucket.org/csnagy/sqlinspect/src/master/)
	-	[OPTIONAL]	For multiple query hotspots search (JPA + Spring + Hibernate + JDBC) SQLInspect : `[CLONE_FOLDER]> sqlinspectinit.sh `
			This script will create a file *classpathall2.txt* containing jar path dependencies required by SQLInspect for multiple hotspots search process.

## **RUN**
-	 ``` mainDbCoverage.py --sqlinspect --sqlinspectpath (--multihotspot)[PATHTOEXECUTABLE_SQLINSPECT] [RESULTDB] [CLONE_FOLDER] [FILEOUTPUT_FOLDER] ```

		* 	Modifies 'project_analysed' table with corresponding flags if queries are found.
		*	Produces PROJECTNAME-queries.xml files containing queries found in the project.
-	``` mainDbCoverage.py --jacoco --mvnpath [MVNCMDPATH] [RESULTDB] [CLONE_FOLDER] [FILEOUTPUT_FOLDER]```

	*	Modifies the root project pom.xml file in order to add Jacoco dependency (see below for the dependency added). 
	*	Runs 'mvn clean' command, and produce PROJECTNAME-mvncleanconsole.log
	*	Runs 'mvn -DskipTests=false -f POMPATH test' command and produce PROJECTNAME-mvntestconsole.log
	*	Updates 'projects_analysed' table with corresponding flags.
	*	Produces PROJECTNAME-testcoverageX.xml files if mvn test command is successfully completed
-	```java QueryCoverageAnalyzer.java [SQL_INSPECT_FILE_FOLDER] [COVERAGE_FOLDER] [TEST_REPORT_FOLDER] true [RESULTSDB] true ```
	*	Reads **-queries.xml* files and fill *queries* & *query_construction* tables
	*	Reads **-testcoverage.xml* files and fill *coverage* table
	*	Computes some measures and creates *project_statistics* table
-	Summary.sql contains a few queries helping with the producing of summary results.

### **Program usage**

	mainDbCoverage.py [-h] [--librariesiodb LIBRARIESIODB]
                         [--dpackage DPACKAGE] [--datainit] [--sqlinspect]
                         [--sqlinspectpath SQLINSPECTPATH] [--singlehotspot]
                         [--jacoco] [--executionline] [--mvnpath MVNPATH]
                         resultsdb clonedirectory outputdirectory
	Java Program Usage : 
	Usage: com.coverage.QueryCoverageAnalyzer <sqlinspect xmls dir> <coverage xmls dir> <test reports dir> <maxNameConvention:true|false> <results db> <truncate>
	

### Jacoco Mavien Dependency 
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