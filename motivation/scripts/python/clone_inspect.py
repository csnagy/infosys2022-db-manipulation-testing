import re
from fnmatch import fnmatch
from shutil import copyfile

import pandas as pd
import subprocess
import os
import logging

from GitSQLInspect.SQLiteDB import SQLiteDB

logging.basicConfig(filename='system.log', level=logging.DEBUG, \
                    format='%(asctime)s -- %(name)s -- %(levelname)s -- %(message)s')


def git_clone_projects(git_repo_df, db, targetfolder, nbprojmax=1000, include_already_cloned=False):
    cloned_projects = pd.DataFrame()
    nbproj = 0
    for index, row in git_repo_df.iterrows():
        name = row["repository_url"].split('/')[-1]
        if name.endswith('.git'):
            name = name[:-len('.git')]
        if os.path.exists(targetfolder + name):
            print('Project git repo ['+name+'] already cloned')
            if (include_already_cloned):
                nbproj += 1
                cloned_projects = cloned_projects.append(row)
                row['project_path']= targetfolder + name
            db.updateClonedProject(row['projectName'], True,targetfolder + name)
        else:
            process = subprocess.run(['git', '-C', targetfolder, 'clone', row["repository_url"]])
            logging.info("Git clone done of [" + name + "]")
            if process.returncode == 0:
                cloned_projects = cloned_projects.append(row)
                nbproj += 1
                row['project_path'] = targetfolder + name
                db.updateClonedProject(row['projectName'], True, targetfolder + name)
            else :
                db.updateClonedProject(row['projectName'],False, "")
            if nbproj == nbprojmax:
                break
    return cloned_projects

def run_sqlinspect_on_projectname_df(project_name_path_df, db, targetfolder,
                                     pathsqlinspect, multiplehotspot=False):
    for index, row in project_name_path_df.iterrows():
        try:
            if os.path.exists(targetfolder + row['git_folder_name'] + '-queries.xml') :
                print('Project [' + row['git_folder_name'] + '] already analysed by SQLInspect. Skipping...')
                db.updateIsSQLInspected(row['projectName'], True)
            else:
                print('Running SQLInspect for project [' + row['git_folder_name'] + ']')
                if multiplehotspot:
                    subprocess.run([pathsqlinspect, "-projectdir", row['project_path'], "-projectname", row['git_folder_name'],
                                    "-hotspotfinder",
                                    "JDBCHotspotFinder,JPAHotspotFinder,SpringHotspotFinder,HibernateHotspotFinder",
                                    "-dialect", "MySQL", "-projectcp-file", row['project_path'] + '\classpath-all2.txt',
                                    "-projectcp-separator", ";"])
                else:
                    subprocess.run([pathsqlinspect, "-projectdir", row['project_path'], "-projectname", row['git_folder_name'],
                                    "-hotspotfinder", "JDBCHotspotFinder", "-dialect", "MySQL"])

                logging.info('SQLInspect run on project [' + row['git_folder_name'] + "]")
                copyfile(row['git_folder_name'] + '-queries.xml', targetfolder + row['git_folder_name'] + '-queries.xml')
                db.updateIsSQLInspected(row['projectName'], True)
        except:
            logging.error("No *-queries.xml file generated after SQLInspect run for [" + row['git_folder_name'])
            db.updateIsSQLInspected(row['projectName'], False)


def searchStatementinJavaFiles(statement, projectpath, projectname, db, boolattribute):
    found=False
    print("Search for project "+projectname)
    for r, d, f in os.walk(projectpath):
        for file in f:
            if fnmatch(file,'*.java'):
                lines = open(os.path.join(r,file), 'r')
                try:
                    for line in lines:
                        if re.match(statement,line):
                            db.updateBoolAttribute(boolattribute, projectname, True)
                            print(projectpath+" contains "+ statement)
                            found=True;
                            break
                except:
                    pass
                if found:
                    break;
        if found:
            break;
    if not found:
        print(projectpath + " does not contain " + statement)
        db.updateBoolAttribute(boolattribute, projectname, False)

def searchSeveralStatementsinJavaFiles(statements, projectpath, projectname, db, boolattribute):
    found=False
    print("Search for project "+projectname)
    for r, d, f in os.walk(projectpath):
        for file in f:
            if fnmatch(file,'*.java'):
                lines = open(os.path.join(r,file), 'r')
                try:
                    for line in lines:
                        for statement in statements:
                            if re.match(statement,line):
                                db.updateBoolAttribute(boolattribute, projectname, True)
                                print(projectpath+" contains "+ statement)
                                found=True;
                                break
                except:
                    pass
                if found:
                    break;
        if found:
            break;
    if not found:
        print(projectpath + " does not contain " + statement)
        db.updateBoolAttribute(boolattribute, projectname, False)

def searchStatementinJavaIsFullTest(statements, projectpath, projectname, db, boolattribute, excltestattribute):
    found=False
    print("Search for project "+projectname)
    globaltest= True

    for r, d, f in os.walk(projectpath):
        for file in f:
            filetest = False
            if fnmatch(file,'*.java'):
                # print(file)
                if (fnmatch(file, '*test*.java')):
                    filetest = True
                lines = open(os.path.join(r,file), 'r')
            try:
                for line in lines:
                        if re.match("import *", line) or re.match("package .*;", line):
                            if re.match("package .*;", line):
                                # print(line)
                                if(re.match('test*',line,re.IGNORECASE)):
                                    packagetest = True
                                else:
                                    packagetest= False
                            for statement in statements :
                                if re.match(statement,line):
                                    if (not filetest and not packagetest):
                                        # print("Found in a non test class")
                                        globaltest = False
                                    # print(projectpath+" contains "+ statement)
                                    found=True;
            except:
                pass
    if(found):
        print(projectpath + " contains one of the import statements")
        if globaltest:
            print("Import statements found only in test package or class file")
            db.updateBoolAttribute(excltestattribute, projectname, True)
    if not found:
        print(projectpath + " does not contain one of the import statements")


def findStatementInProjects(statement, projects, db, boolattribute):
    for index, row in projects.iterrows():
        searchStatementinJavaFiles(statement,row['project_path'],row['projectName'],db, boolattribute)

def findSeveralStatementsInProjects(statement, projects, db, boolattribute):
    for index, row in projects.iterrows():
        searchSeveralStatementsinJavaFiles(statement,row['project_path'],row['projectName'],db, boolattribute)


def findStatementInProjectsTestClasses(statement, projects, db, boolattribute, excltestattribute):
    for index, row in projects.iterrows():
        searchStatementinJavaIsFullTest(statement, row['project_path'], row['projectName'], db, boolattribute, excltestattribute)
        print(index)


def detect_queries_files(folder):
    projects = []
    for r, d, f in os.walk(folder):
        for file in f:
            if fnmatch(file, '*-queries.xml'):
                projects.append(file.split('-queries.xml')[0])
    return projects

