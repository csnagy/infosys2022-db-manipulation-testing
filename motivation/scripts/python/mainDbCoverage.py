import argparse
import os

import clone_inspect, testCoverage
import LibrariesIO
import SQLiteDB


def mainDbCoverage():
    parser = argparse.ArgumentParser()

    parser.add_argument("resultsdb",
                        help="Path to the results db")
    parser.add_argument("clonedirectory",
                        help="Path to the directory where projects will be cloned")
    parser.add_argument("outputdirectory",
                        help="Path to the directory where output files will be stored")
    parser.add_argument("--librariesiodb",
                        help="Path to the librariesio database")
    parser.add_argument("--dpackage",
                        help="Name of the project's package dependency in librariesio dataset. (ex : junit:junit )")
    parser.add_argument("--datainit", action="store_true", help="Select projects, clone them and scans for SQL import statements")
    parser.add_argument("--sqlinspect", action="store_true", help="Runs SQLInspect process")
    parser.add_argument("--sqlinspectpath", help="Path to the SQLInspect installation diretory")
    parser.add_argument("--singlehotspot", action="store_true", default=1, help="Indicate if SQLInspect must look for JPA or Spring, or Hibernate dependencies")
    parser.add_argument("--jacoco", action="store_true", help="Modify Pom of projects to add Jacoco dependency and runs mvn test command")
    parser.add_argument("--executionline", action="store_true", help= "Reads the jacoco reports and look for the execution line of queries in 'queries' table. (Requires the java program to run first)")
    parser.add_argument("--mvnpath", help="Path to the mvn command")
    # PARAMETERS INIT #
    args = parser.parse_args()
    db = SQLiteDB(args.resultsdb)
    librariesio = LibrariesIODAO(args.librariesiodb)
    search_package = args.dpackage
    sqlinspectpath = args.sqlinspectpath
    mvnpath = args.mvnpath
    TARGET_CLONE_FOLDER = os.path.join(args.clonedirectory,'')
    TARGET_FOLDER = os.path.join(args.outputdirectory,'')
    QUERY_PROJECTS = "select * from projects_analysed"
    PARAM = []

    if(not(args.datainit or args.sqlinspect or args.jacoco or args.executionline)):
        parser.error("Error : please add an action argument (--datainit, --sqlinspect, --jacoco, --executionline)")

    if(args.datainit):
        print("# Data initialisation started...")
        print('# Search for projects depending on ['+ search_package+'] in librariesIO database')
        if(args.librariesiodb is None or args.dpackage is None):
            parser.error("Please provide a database containing libraries.io data using --librariesiodb option and a dependency package using --dpackage")
        df = librariesio.getDependentsProjectsInfoOfPackageReadDBOrderByNbStars(search_package, True)
        db.insertProjects(df)
        projects = db.getDataFrameFromQuery(QUERY_PROJECTS, PARAM)
        print('# Cloning identified projects into ['+TARGET_CLONE_FOLDER+']')
        projects = clone_inspect.git_clone_projects(projects, db, TARGET_CLONE_FOLDER, nbprojmax=50000,
                                                    include_already_cloned=False)
        print('# Projects cloned  : '+str(len(projects.index)))
        QUERY_PROJECTS = "select * from projects_analysed where isCloned=1"
        projects = db.getDataFrameFromQuery(QUERY_PROJECTS, PARAM)
        # SCAN IMPORT STATEMENTS
        print('# Scan projects source code for import statements...')
        clone_inspect.findStatementInProjects('import java.sql.*', projects, db, 'containsJavaSQL')
        clone_inspect.findStatementInProjects('import javax.persistence.*', projects, db, 'isJPA')
        clone_inspect.findStatementInProjects('import org.hibernate.*', projects, db, 'isHibernate')
        clone_inspect.findStatementInProjects('import org.springframework.orm.hibernate.*', projects, db,
                                              'isSpringHibernate')
        print('# Init process finished')

    if(args.sqlinspect):
        if(sqlinspectpath is None):
            parser.error("Error : to use SQLInspect process program. Please provide SQLInspect installation directory using --sqlinspectpath")
        print('# SQLInspect process started...')
        if(args.singlehotspot) :
            QUERY_PROJECTS = "select * from projects_analysed where containsJavaSQL=1"
            projects = db.getDataFrameFromQuery(QUERY_PROJECTS, PARAM)
            clone_inspect.run_sqlinspect_on_projectname_df(projects, db, pathsqlinspect=sqlinspectpath,
                                                           targetfolder=TARGET_FOLDER, multiplehotspot=False)
        elif(not args.singlehotspot):
            QUERY_PROJECTS = "select * from projects_analysed where containsJavaSQL=1 or isHibernate=1 or isSpringHibernate=1 or isJPA=1"
            projects = db.getDataFrameFromQuery(QUERY_PROJECTS, PARAM)
            clone_inspect.run_sqlinspect_on_projectname_df(projects, db,
                                                           pathsqlinspect=sqlinspectpath,
                                                           targetfolder=TARGET_FOLDER, multiplehotspot=True)

    if(args.jacoco):
        if(args.mvnpath is None):
            parser.error("Please provide mvn command path with --mvnpath")
        QUERY_PROJECTS = "select * from projects_analysed where containsJavaSQL=1 or isHibernate=1 or isSpringHibernate=1 or isJPA=1"
        projects = db.getDataFrameFromQuery(QUERY_PROJECTS, PARAM)
        testCoverage.modify_pom_projects(True, False, projects, db)
        QUERY_PROJECTS = "select * from projects_analysed where hasPom=1"
        projects = db.getDataFrameFromQuery(QUERY_PROJECTS, PARAM)
        print('# Running Mvn test')
        testCoverage.mvn_test_all_projects(projects, TARGET_FOLDER, db, False, mvnpath)
        read_all_mvn_log(TARGET_FOLDER, db)
        print('# Mvn tests done')

    if(args.executionline):
        testCoverage.searchExecuteQueryCoverageLineForAllReports(TARGET_FOLDER, db)


if __name__ == "__main__":
    # execute only if run as a script
    mainDbCoverage()
