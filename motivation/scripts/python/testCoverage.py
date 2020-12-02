import re

from lxml import etree
import subprocess
from lxml.etree import XMLSyntaxError
import logging
import os
from GitSQLInspect.SQLiteDB import SQLiteDB
from shutil import copyfile

logging.basicConfig(filename='system.log', level=logging.DEBUG, \
                    format='%(asctime)s -- %(name)s -- %(levelname)s -- %(message)s')

MVNCMDPATH = ''

JACOCO = etree.fromstring("""
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
    """)

def addJaCoCoToPomFile(projectname, projectpath, db):
    try:
        print("Modifying project [" + projectname + ']')
        tree = etree.parse(projectpath + "\pom.xml")
        db.updateIsPom(projectname, True)
    except OSError:
        print("Project [" + projectname + "] has no pom.xml file")
        logging.warning("Project [" + projectname + "] has no pom.xml file")
        db.updateIsPom(projectname, False)
        return
    except XMLSyntaxError:
        print("Errors in [" + projectname + "] pom.xml")
        logging.error("Errors in [" + projectname + "] pom.xml")
        return
    pom = tree.getroot()
    if pom.xpath('.//groupId[text()="org.jacoco"]'):
        db.updateIsJacoco(projectname, True)
        return
    tree.write(projectpath + "\pom-backup.xml")
    try:
        build = pom.find('{http://maven.apache.org/POM/4.0.0}build')
    except:
        build = pom.find('build')
    if (build is None):
        build = etree.fromstring(("<build></build>"))

    try:
        plugins = build.find('{http://maven.apache.org/POM/4.0.0}plugins')
    except:
        plugins = build.find('plugins')
    if (plugins is None):
        plugins = etree.fromstring("<plugins></plugins>")
        plugins.append(JACOCO)
        build.append(plugins)
    else:
        plugins.append(JACOCO)

    pom.append(build)
    tree.write(projectpath + "\pom.xml")
    print('JaCoCo has been added to the pom file of [' + projectname + "]")
    db.updateIsJacoco(projectname, True)
    logging.info('JaCoCo has been added to the pom file of [' + projectname + "]")



def modify_pom_for_test(projectname, projectpath, db):
    addJaCoCoToPomFile(projectname, projectpath, db)



def modify_pom_projects(testMod, aspectMod, projects, db, aspectpath=None):
    for index, row in projects.iterrows():
        if testMod:
            modify_pom_for_test(row['projectName'], row['project_path'], db)


def searchLogFile(targetpath, git_folder_name):
    if (os.path.exists(targetpath + git_folder_name + '-mvntestconsole.log')):
        return True
    else:
        return False


def run_mvn_test_once(projectname, git_folder_name, projectpath, targetpath, db):
    # mvn -f path/to/pom.xml
    if (searchAndMoveJacocoFiles(projectpath, projectname, git_folder_name, targetpath) or searchLogFile(targetpath,
                                                                                                         git_folder_name)):
        db.updateIsPom(projectname, True)
        db.updateIsMvnTestRun(projectname, True)
        if (searchLogFile(targetpath, git_folder_name)):
            print('Skipping mvn test [' + git_folder_name + ']. Log mvn found.')
            db.updateIsMvnTestRun(projectname, True)
            db.updateIsMvnTestSuccess(projectname, False)
        else:
            print('Skipping mvn test [' + git_folder_name + ']. Jacoco report found.')
            db.updateHasCoverageFile(projectname, True)
            db.updateIsMvnTestSuccess(projectname, True)
        return
    else:
        run_mvn_test(projectname, git_folder_name, projectpath, targetpath, db)


def run_mvn_test(projectname, git_folder_name, projectpath, targetpath, db):
    if (os.path.exists(projectpath + '\pom.xml')):
        pompath = projectpath + '\pom.xml'
        db.updateIsPom(projectname, True)
        with open(targetpath + git_folder_name + '-mvncleanconsole.log', 'w') as fc:
            print("Running Mvn clean for [" + git_folder_name + "]")
            processclean = subprocess.run(
                [MVNCMDPATH, '-f', pompath, 'clean'], stdout=fc, stderr=fc)
        with open(targetpath + git_folder_name + '-mvntestconsole.log', 'w') as fp:
            print("Running Mvn test for [" + git_folder_name + "]")
            process = subprocess.run(
                [MVNCMDPATH, '-DskipTests=false', '-f', pompath, 'test'], stdout=fp,
                stderr=fp)
        if process.returncode == 0:
            print("Mvn Test succesfull : [" + git_folder_name + "]")
            logging.info("Mvn Test succesfull : [" + git_folder_name + "]")
            db.updateIsMvnTestSuccess(projectname, True)
            db.updateIsMvnTestRun(projectname, True)
            found = searchAndMoveJacocoFiles(projectpath, projectname, git_folder_name, targetpath)
            if not found:
                db.updateHasCoverageFile(projectname, False)
                print(
                    "Mvn test success and Jacoco added in pom but no report file found [" + git_folder_name + " ]")
                logging.error(
                    "Mvn test success and Jacoco added in pom but no report file found [" + git_folder_name + ' ]')
            else:
                db.updateHasCoverageFile(projectname, True)
        else:
            print("Mvn test failed [" + git_folder_name + ']')
            logging.error("Mvn test failed [" + git_folder_name + ']')
            db.updateIsMvnTestRun(projectname, True)
            db.updateIsMvnTestSuccess(projectname, False)
            found = searchAndMoveJacocoFiles(projectpath, projectname, git_folder_name, targetpath)
            if not found:
                db.updateHasCoverageFile(projectname, False)
            else:
                db.updateHasCoverageFile(projectname, True)
    else:
        print('Project ' + git_folder_name + ' has no pom file')
        db.updateIsPom(projectname, False)


def run_mvn_dependency(projectname, projectpath, targetpath):
    if (os.path.exists(projectpath + '\pom.xml')):
        pompath = projectpath + '\pom.xml'
        print("Generating classpath txt file for [" + projectname + "]")
        with open(targetpath + projectname + '-classpath.log', 'w') as fp:
            # TODO Change absolute path of maven
            process = subprocess.run(
                [MVNCMDPATH, '-f', pompath, 'dependency:build-classpath', '-DSkipTests',
                 '-Dsilent=true', '-Dmdep.outputFile=classpath.txt'], stdout=fp,
                stderr=fp)
        if process.returncode == 0:
            print("Classpath file Test succesfull : [" + projectname + "]")


def mvn_test_all_projects(projects, targetpathjacoco, db, rerun, mvncmdpath):
    global MVNCMDPATH
    MVNCMDPATH= mvncmdpath
    for index, row in projects.iterrows():
        if rerun:
            run_mvn_test(row['projectName'], row['git_folder_name'], row['project_path'], targetpathjacoco, db)
        else:
            run_mvn_test_once(row['projectName'], row['git_folder_name'], row['project_path'], targetpathjacoco, db)


def mvn_create_classpath_dependency_file(projects, targetpath):
    for index, row in projects.iterrows():
        run_mvn_dependency(row['git_folder_name'], row['project_path'], targetpath)


def detect_move_jacoco_files(projects, targetpathjacoco, db):
    for index, row in projects.iterrows():
        files = []
        if os.path.exists(row['project_path'] + row['git_folder_name'] + "-testcoverage0.xml"):
            print('Project [' + row['projectName'] + '] test coverage files already exist')
            db.updateHasCoverageFile(row['projectName'], True)
        else:
            found = searchAndMoveJacocoFiles(row['project_path'], row['git_folder_name'], row['git_folder_name'],
                                             targetpathjacoco)
            if found:
                db.updateIsMvnTestSuccess(row['projectName'], True)
                db.updateIsMvnTestRun(row['projectName'], True)
                db.updateIsJacoco(row['projectName'], True)
                db.updateHasCoverageFile(row['projectName'], True)
            else:
                db.updateHasCoverageFile(row['projectName'], False)
                print('No coverage report for [' + row['projectName'] + ']')


def searchAndMoveJacocoFiles(projectpath, projectname, git_folder_name, targetpath):
    files = []
    found = False
    for r, d, f in os.walk(projectpath):
        for file in f:
            if 'jacoco.xml' in file:
                found = True
                files.append(os.path.join(r, file))

    for idx, item in enumerate(files):
        print('Found Jacoco files for [' + projectname + ']')
        copyfile(item, targetpath + git_folder_name + "-testcoverage" + str(idx) + ".xml")
    return found



def read_all_mvn_log(folder, db):
    for file in os.listdir(folder):
        match = re.match(".*-mvntestconsole.log", file)
        if match:
            print('Reading ['+file+']')
            projectname = match.group(0).split('-mvntestconsole.log')[0]
            if not db.existProjectBuild(projectname):
                f = open(folder + file, 'r')
                try:
                    for line in f:
                        if re.match(".*BUILD SUCCESS.*", line):
                            db.updateProjectBuildSuccess(projectname, True)
                        if re.match(".*BUILD FAILURE.*", line):
                            db.updateProjectBuildFailure(projectname, True)
                        if re.match(".*No tests to run.*", line):
                            db.updateNoTests(projectname)
                        if re.match(".*Skipping JaCoCo execution due to missing execution data file.*", line):
                            db.updateSkippingJacoco(projectname)
                        if re.match(".*Connection refused: connect.*", line):
                            db.updateConnectionFail(projectname)
                        if re.match(".*COMPILATION ERROR.*", line):
                            db.updateCompilationError(projectname)
                        if re.match(".*Tests are skipped.*", line):
                            db.updateTestsSkipped(projectname)
                        if re.match(".*junit.framework..*Error|.*junit.framework..*Failure.*|.*org.junit.*Failure.*",
                                    line):
                            db.updateJUnitException(projectname)
                        if re.match(
                                ".*Could not find artifact.*|.*Failed to execute goal.*|.*The POM .* is missing, no dependency information available.*",
                                line):
                            db.updatePomError(projectname)
                        if re.match(".* There are test failures.*",line):
                            db.updateTestFailures(projectname)
                except Exception as e:
                    print('Error reading line : [' + line + ']')
                    print(e)
                    logging.error('Error reading line : [' + line + ']')
            else:
                print(projectname + " log already treated.")
