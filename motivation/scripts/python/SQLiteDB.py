import sqlite3
import pandas as pd
import os


class SQLiteDB:

    def __init__(self, db_file):
        PROJECT_ANALYTICS_TABLE = """
        create table if not exists projects_analysed
        (
	    projectName char(50),
	    repository_name_with_owner text primary key,
	    git_folder_name text,
	    isCloned boolean,
	    project_path text,
	    mvn_dependency_success,
	    isSQLInspected boolean,
		containsJavaSQL boolean,
		isJPA boolean,
		isHibernate boolean,
		isSpringHibernate boolean, 
	    hasPom boolean,
	    isMvnTestSuccess boolean,
	    isMvnTestRun boolean,
	    isJacocoInPom boolean,
	    hasAspectInPom boolean,
	    hasCoverageFile boolean,
        dependency char(50),
        description text,
        repository_url text,
        repository_stars int,
        repository_forks int,
        repository_watchers int
            );
        """

        PROJECTS_BUILD_TABLE = """
        create table if not exists projects_build(
            projectName char(50),
            isSuccess boolean,
            isFailure boolean,
            noTests int default 0,
            testFailures int default 0,
            skippingJacoco int default 0,
            connectionFail int default 0,
            compilationError int default 0,
            JUnitException int default 0,
            pomError int default 0,
            testsSkipped boolean,
            hasJacocoReport boolean
        );
        """
        self.db_file = db_file
        self.connection = sqlite3.connect(db_file)
        self.cursor = self.connection.cursor()
        self.cursor.execute(PROJECT_ANALYTICS_TABLE)
        self.cursor.execute(PROJECTS_BUILD_TABLE)

    def query(self, query, params):
        self.cursor.execute(query, params)
        self.connection.commit()

    def __del__(self):
        self.connection.close()

    def getDataFrameFromQuery(self, query, params):
        self.query(query,params)
        df = pd.DataFrame(self.cursor.fetchall())
        if df.empty:
            return df
        df.columns = list(map(lambda x: x[0], self.cursor.description))
        return df

    def isProjectChangeHasCoverageFile(self,params):
        self.query("select * from projects_analysed where hasCoverageFile=0 and git_folder_name=?",params)
        if self.cursor.fetchone() is None:
            return False
        else:
            return True


    def existProjectAnalysedRow(self, owner_repo):
        self.query("SELECT * FROM projects_analysed where repository_name_with_owner = ? ", [owner_repo])
        if self.cursor.fetchone() is None:
            return False
        else:
            return True

    def insertProjects(self, projectsdf):
        # Insert DataFrame recrds one by one.
        for i, row in projectsdf.iterrows():
            if not self.existProjectAnalysedRow(row['Repository Name with Owner']):
                sql = "INSERT INTO 'projects_analysed' (projectName, git_folder_name, dependency, description, repository_url, repository_name_with_owner, repository_stars, repository_forks, repository_watchers) VALUES (?,?,?,?,?,?,?,?,?)"
                self.query(sql, (row['Project Name'], row["Repository URL"].split('/')[-1], row['Dependency Name'], row['Description'], row['Repository URL'],
                                 row['Repository Name with Owner'], row['Repository Stars Count'],
                                 row['Repository Forks Count'], row['Repository Watchers Count']))
                self.connection.commit()
                print("INSERT INTO 'project_analysed' (projectname,...) " + row['Project Name'])
            else:
                print("Project "+ row['Project Name']+ " with repository name and owner ["+row['Repository Name with Owner']+"] already in 'projects_analysed' table")
                self.updateDependency(row['Repository Name with Owner'],  row['Dependency Name'])

    def existOrCreateProjectRow(self, projectname):
        self.query("SELECT projectName FROM projects_analysed where projectName = ? ", [projectname])
        if self.cursor.fetchone() is None:
            print("Inserting project [" + projectname + "]row in table 'projects_analysed'")
            self.query("INSERT INTO projects_analysed(projectName) VALUES (?)", [projectname])


    def existOrCreateProjectBuild(self, projectname):
        self.query("SELECT projectName FROM projects_build where projectName = ? ", [projectname])
        if self.cursor.fetchone() is None:
            print("Inserting project [" + projectname + "]row in table 'projects_build'")
            self.query("INSERT INTO projects_build(projectName) VALUES (?)", [projectname])

    def existProjectBuild(self, projectname):
        self.query("SELECT projectName FROM projects_build where projectName = ? ", [projectname])
        if self.cursor.fetchone() is None:
            return False
        else:
            return True


    def updateIsMvnTestSuccess(self, projectname, isMvnTestSucess):
        self.existOrCreateProjectRow(projectname)
        self.query("UPDATE projects_analysed SET isMvnTestSuccess = ? where projectName = ?",
                   (isMvnTestSucess, projectname))

    def updateIsJacoco(self, projectname, boolflag):
        self.existOrCreateProjectRow(projectname)
        self.query("UPDATE projects_analysed SET isJacocoInPom = ? where projectName = ?", (boolflag, projectname))

    def updateClonedProject(self, projectname, boolflag, folder):
        self.existOrCreateProjectRow(projectname)
        self.query("UPDATE projects_analysed SET isCloned = ?, project_path=? where projectName = ?", (boolflag, folder, projectname))

    def updateIsClonedGitFolder(self, projectname, git_path, boolflag):
        self.query("UPDATE projects_analysed SET isCloned = ?, project_path=? where git_folder_name = ?", (boolflag,git_path, projectname))

    def updateIsSQLInspected(self, projectname, boolflag):
        self.existOrCreateProjectRow(projectname)
        self.query("UPDATE projects_analysed SET isSQLInspected = ? where projectName = ?", (boolflag, projectname))

    def updateIsSQLInspectedGitFolder(self, projectname, boolflag):
        self.query("UPDATE projects_analysed SET isSQLInspected = ? where git_folder_name = ?", (boolflag, projectname))


    def updateIsMvnTestRun(self, projectname, boolflag):
        self.existOrCreateProjectRow(projectname)
        self.query("UPDATE projects_analysed SET isMvnTestRun = ? where projectName = ?", (boolflag, projectname))

    def isMvnTestRun(self, projectname):
        self.query("SELECT isMvnTestRun FROM projects_analysed where projectName = ?", [projectname])
        return self.cursor.fetchone()

    def updateIsPom(self, projectname, boolflag):
        self.existOrCreateProjectRow(projectname)
        self.query("UPDATE projects_analysed SET hasPom = ? where projectName = ?", (boolflag, projectname))

    def updateHasCoverageFile(self, projectname, boolflag):
        self.existOrCreateProjectRow(projectname)
        self.query("UPDATE projects_analysed SET hasCoverageFile = ? where projectName = ?", (boolflag, projectname))

    def updateHasCoverageFileFromGitFolderName(self, projectname, boolflag):
        self.query("UPDATE projects_analysed SET hasCoverageFile = ? where git_folder_name = ?", (boolflag, projectname))

    def updateHasAspectInPom(self, projectname, boolflag):
        self.query("UPDATE projects_analysed SET hasAspectInPom = ? where git_folder_name = ?", (boolflag, projectname))

    def updateBoolAttribute(self, boolattribute, projectname, boolflag):
        self.existOrCreateProjectRow(projectname)
        self.query("UPDATE projects_analysed SET "+boolattribute+" = ? where projectName = ?", (boolflag, projectname))

    def setProjectsDependency(self, dependency, projects):
        for index, row in projects.iterrows():
            self.existOrCreateProjectRow(row['projectname'])
            self.query("UPDATE projects_analysed SET dependency = dependency || ?", [dependency])

    def updateProjectBuildSuccess(self, projectname, boolflag):
        self.existOrCreateProjectBuild(projectname)
        self.query("UPDATE projects_build SET isSuccess = ? where projectName = ?", (boolflag, projectname))

    def updateProjectDependencySuccess(self, projectname, boolflag):
        self.query("UPDATE projects_analysed SET mvn_dependency_success = ? where git_folder_name = ?", (boolflag, projectname))

    def updateProjectIsAspectRunSuccess(self, projectname, boolflag):
        self.query("UPDATE projects_analysed SET isAspectRunSuccess = ? where git_folder_name = ?", (boolflag, projectname))

    def updateProjectIsAspectRun(self, projectname, boolflag):
        self.query("UPDATE projects_analysed SET isAspectRun = ? where git_folder_name = ?", (boolflag, projectname))

    def updateProjectBuildFailure(self, projectname, boolflag):
        self.existOrCreateProjectBuild(projectname)
        self.query("UPDATE projects_build SET isFailure = ? where projectName = ?", (boolflag, projectname))

    def updateNoTests(self, projectname):
        self.existOrCreateProjectBuild(projectname)
        self.query("UPDATE projects_build SET noTests = noTests + 1 where projectName = ?", [projectname])

    def updateSkippingJacoco(self, projectname):
        self.existOrCreateProjectBuild(projectname)
        self.query("UPDATE projects_build SET skippingJacoco = skippingJacoco + 1 where projectName = ?", [projectname])

    def updateConnectionFail(self, projectname):
        self.existOrCreateProjectBuild(projectname)
        self.query("UPDATE projects_build SET connectionFail = connectionFail + 1 where projectName = ?", [projectname])

    def updateCompilationError(self, projectname):
        self.existOrCreateProjectBuild(projectname)
        self.query("UPDATE projects_build SET compilationError = compilationError + 1 where projectName = ?",
                   [projectname])

    def updateTestsSkipped(self, projectname):
        self.existOrCreateProjectBuild(projectname)
        self.query("UPDATE projects_build SET testsSkipped = testsSkipped + 1 where projectName = ?", [projectname])

    def updateJUnitException(self, projectname):
        self.existOrCreateProjectBuild(projectname)
        self.query("UPDATE projects_build SET JUnitException = JUnitException + 1 where projectName = ?", [projectname])

    def updatePomError(self, projectname):
        self.existOrCreateProjectBuild(projectname)
        self.query("UPDATE projects_build SET pomError = pomError + 1 where projectName = ?", [projectname])

    def updateTestFailures(self, projectname):
        self.existOrCreateProjectBuild(projectname)
        self.query("UPDATE projects_build SET testFailures = testFailures + 1 where projectName = ?", [projectname])

    def updateQueriesExecutionCovered(self,projectname,classname, execline, boolflag):
        self.query("UPDATE queries SET execLineIsTestCovered = ? where projectName = ? and execClass = ? and execLine = ? ", (boolflag, projectname, classname, execline))

    def isContainsJavaSQL(self, projectname):
        self.query("SELECT containsJavaSQL from projects_analysed where projectName = ?", [projectname])
        row = self.cursor.fetchone()
        if row is None:
            return 0
        else:
            return row[0]

    def updateDependency(self, repo, dependency):
        self.query("SELECT * from projects_analysed where repository_name_with_owner = ? AND dependency like ?", (repo,"'%"+dependency+"%'"))
        row = self.cursor.fetchone()
        if row is None:
            print("Updating dependency")
            self.query("UPDATE projects_analysed set dependency = dependency || ? where repository_name_with_owner = ? ",(', '+dependency, repo))

    def get_projects_with_SQL(self, targetfolder):
        projects_paths = pd.DataFrame(columns=['projectname', 'projectpath'])
        for name in os.listdir(targetfolder):
            if (os.path.isdir(targetfolder + name) and self.isContainsJavaSQL(name)):
                projects_paths = projects_paths.append({'projectname': name, 'projectpath': targetfolder + name},
                                                       ignore_index=True)
        return projects_paths

    def getExecutionClassAndLines(self, projectname):
        return self.getDataFrameFromQuery('select execClass, execLine from queries where projectName = ?',[projectname])

