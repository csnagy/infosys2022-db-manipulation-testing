import sqlite3

import pandas as pd

class LibrariesIODAO:

    def __init__(self,db_file):
        self.db_file = db_file
        self.connection = sqlite3.connect(db_file)
        self.cursor = self.connection.cursor()

    def getDependentsProjectsInfoOfPackageReadDB(self, package, onlygithub):
        query = """
                       Select distinct(A."Project Name"), A."Dependency Name", B.Description, B."Repository URL", B."Repository Name with Owner", B."Repository Stars Count", B."Repository Forks Count", B."Repository Watchers Count"
                       from dependencies A, projects_with_repository_fields B 
                       where A."Dependency Name"=? AND A."Project Name"=B.Name
            """
        if onlygithub:
            self.query(query+" AND B.\"Repository URL\" like 'https://github%'", [package])
        else:
            self.query(query,[package])

        df = pd.DataFrame(self.cursor.fetchall())
        df.columns = list(map(lambda x: x[0], self.cursor.description))
        return df

    def getDependentsProjectsInfoOfPackageReadDBOrderByNbStars(self, package, onlygithub):
        query = """
                       Select distinct(A."Project Name"), A."Dependency Name", B.Description, B."Repository URL", B."Repository Name with Owner", B."Repository Stars Count", B."Repository Forks Count", B."Repository Watchers Count"
                       from dependencies A, projects_with_repository_fields B 
                       where A."Dependency Name"=? AND A."Project Name"=B.Name 
            """
        if onlygithub:
            self.query(query+" AND B.\"Repository URL\" like 'https://github%' order by B.\"Repository Stars Count\" desc", [package])
        else:
            self.query(query,[package])

        df = pd.DataFrame(self.cursor.fetchall())
        df.columns = list(map(lambda x: x[0], self.cursor.description))
        return df

    def query(self, query, params):
        print("Executing query :"+ query + ', '.join(params))
        self.cursor.execute(query, params)
        self.connection.commit()

    def __del__(self):
        self.connection.close()

    def getDependentsProjectsOfPackageReadFile(self, package, fields, dependency_file,chunksize=100000):
        '''
        Returns a DataFrame with the specified fields in the dependency file of librariesIO for the given package in 'Dependency Name' field.
        :param package: The package to look for
        :param fields: A list contianing the columns to extract for Dependency file
        :param chunksize: Default number of lines to process by chunk
        :return:
        '''
        dependencies = pd.DataFrame()
        project_dependencies_of_package = pd.DataFrame()
        for chunk in pd.read_csv(dependency_file, chunksize=chunksize, index_col=0):
            dependencies = chunk[fields]
            project_dependencies_of_package = pd.concat([project_dependencies_of_package, dependencies[
            dependencies['Dependency Name'] == package]])
        return project_dependencies_of_package


    def getProjects_With_RepoFromProjectsReadFile(self,projects, fields, projects_with_repo, chunksize=100000):
        projects_repository = pd.DataFrame()
        for chunk in pd.read_csv(projects_with_repo, chunksize=chunksize, index_col=0):
            filtered_chunk = chunk[fields].name.isin(projects)
            projects_repository = pd.concat([projects_repository, filtered_chunk])


if __name__ == '__main__':
    lib = LibrariesIODAO("D:\Database\librariesio.db")
    df = lib.getDependentsProjectsInfoOfPackageReadDB('org.mongodb:mongo-java-driver',False)
    print(df.head())
    print(df.info())