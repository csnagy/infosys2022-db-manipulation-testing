package com.coverage;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.coverage.dao.CoverageDAO;
import com.coverage.dao.QueryDAO;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class QueryCoverageAnalyzer {
	private static Logger logger = LoggerFactory.getLogger(QueryCoverageAnalyzer.class);

	private static String resultsDB; // results/results-apps.db"
	private static Connection dbConnection;
	private static PrepareQueries queries;
	private static PrepareCoverage coverage;
	private static TestReport testReport;
	private static PrepareStats stats;
	private static boolean maxNameConvention;
	private static boolean truncate;

	private static QueryDAO queryDAO;
	private static CoverageDAO coverageDAO;

	public static void main(String[] argv) throws Exception {

		if (argv.length != 6) {
			logger.error(
					"Usage:  <sqlinspect xmls dir> <coverage xmls dir> <test reports dir> <maxNameConvention:true|false> <results db> <truncate>");
			return;
		}

		resultsDB = argv[4];

		connectToDB();

		queryDAO = new QueryDAO(dbConnection);
		coverageDAO = new CoverageDAO(dbConnection);

		String sqlinspectOutputDir = argv[0];
		queries = new PrepareQueries(sqlinspectOutputDir, queryDAO);
		coverage = new PrepareCoverage(argv[1], coverageDAO, queryDAO);
		testReport = new TestReport(argv[2], dbConnection);
		stats = new PrepareStats(dbConnection);
		maxNameConvention = Boolean.parseBoolean(argv[3]);
		truncate = Boolean.parseBoolean(argv[5]);
		List<String> l = new ArrayList<>();

//		 Process all -queries.xml file in the given folder
		File dir = new File(sqlinspectOutputDir);
		File[] directoryListing = dir.listFiles();
		if (directoryListing != null) {
			for (File child : directoryListing) {
				// Do something with child

				String fileName = child.getName();
				if (fileName.contains("-queries.xml")) {
					String projectName = fileName.replace("-queries.xml", "");
					l.add(projectName);
				}
			}
		} else {
			// Handle the case where dir is not really a directory.
			// Checking dir.isDirectory() above would not be sufficient
			// to avoid race conditions with another process that deletes
			// directories.
		}
		doItForProjects(l,truncate);

		// Process a single project set of files. Comment above lines and uncomment one below
//        doItForProject("pivot4j", truncate);
//        doItForProject("ServiceFramework", truncate);


		disconnectFromDB();
		logger.info("Done.");
	}

	private static void doItForProjects(List<String> projectNameList, boolean truncate) throws Exception {
		createStatsTable(truncate);
		createProjectTables(truncate);
		int projectNum = projectNameList.size();
		for (int i = 0; i < projectNum; i++) {
			String projectName = projectNameList.get(i);
			logger.info("Load project {}/{}: {}", i, projectNum, projectName);
			doItForProject(projectName,truncate);
		}
		createAdditionalStats();

	}


	public static void doItForProject(String projectName, boolean truncate) throws Exception {
		queries.parseInspectedQueries(projectName);
		coverage.addTestCoverageInfoForMethods(projectName,maxNameConvention);
		testReport.addTestReportInfoForMethods(projectName);
		stats.saveStats(projectName);
	}

	private static void connectToDB() throws ClassNotFoundException, SQLException {
		// Step 1: "Load" the SQLITE driver
		Class.forName("org.sqlite.JDBC");
		// Step 2: Establish the connection to the database
		dbConnection = DriverManager.getConnection("jdbc:sqlite:" + resultsDB);
	}

	private static void disconnectFromDB() throws SQLException {
		dbConnection.close();
	}

	private static void createStatsTable(boolean truncate) throws SQLException {
		Statement s = dbConnection.createStatement();
		if(truncate)
		    s.execute("drop table if exists project_statistics");
		s.execute("create table if not exists project_statistics(" + "projectName char(50) not null," // name of the
																										// project
				+ "nbrOfHotspots int,"// number of unique source code location where at least DB query is executed
										// (extracted via SQLInspect)
				+ "nbrOfMultipleQueryHotspots int,"// number of unique source code location where more than one DB query
				+ "nbrOfHotspotsNonTest int,"
				+ "nbrOfHotspotsNonTestCovered int,"
				+ "nbrOfMultiQueryHotspotsNonTest int,"
				+ "nbrOfMultiQueryHotspotsNonTestCovered int,"
													// is executed (extracted via SQLInspect)
				+ "nbrOfQueries int," // number of queries (extracted via SQLInspect)
				+ "nbrOfQueriesNotInTestClasses int," // number of queries that are not in an identified test class
				+ "nbrOfCoveredExecStringLine int," // number of query execution line that is covered in jacoco test report. Requires 'execLineIsTestCovered' in 'queries' table. (python program --execLine option)
				+ "nbrOfUnResolvedQueries int," // number of queries for which the query value could not be fully
												// resolved by SQLInspect
				+ "nbrOfMethodsJacoco int," // number of methods (extracted via JaCoCo)
				+ "nbrOfQueryExecutionMethodsJacoco int," // number of methods (extracted via JaCoCo) where at least one DB
													// query is executed (as detected via SQLInspect)
				+ "nbrOfQueryConstructionMethodsJacoco int," // number of methods (extracted via JaCoCo) involved in the
														// construction of a query (as detected via SQLInspect)
				+ "nbrOfDBMethods int," // Added by Max. number of methods either isQueryExecution or
										// isQeuryConstruction
				+ "nbrOfDBMethodCovered int," // Added by Max. number of database methods (isQueryExecution or
												// isQueryConstruction from coverage table) that is covered (coverage
												// flag)
				+"nbrOfDBMethodsNonTest int,"
				+"nbrOfDBMethodsNonTestCovered int,"
				+ "nbrOfMethodCovered int," + "nbrOfTestSuites int," + "nbrOfTestCases int" + ")");
	}

	private static void createProjectTables(boolean truncate) throws SQLException {
		try {

			queryDAO.createTables(truncate);
			coverageDAO.createTables(truncate);

			// creating the table for recording information about the DB queries, as
			// extracted via SQLInspect
			Statement s = dbConnection.createStatement();
            if (truncate) {
                s.execute("drop table if exists testsuite");
			    s.execute("drop table if exists testcase");
            }

			s.execute("drop index if exists testsuite_id");

			s.execute("create table if not exists testsuite (id integer primary key," + "projectName char(50) not null,"
					+ "variantName char(256), " + "fileName char(256) not null," + "name text not null)");

			s.execute("create index testsuite_id on testsuite(id)");

			s.execute("create table if not exists testcase (id integer primary key," + "testsuite int, " + "name text not null,"
					+ "className text not null," + "foreign key (testsuite) references testsuite(id))");

		} catch (SQLException e) {
			logger.error("SQL Exception: ", e);
		}
	}


	private static void createAdditionalStats() throws SQLException {
		Statement s = dbConnection.createStatement();
		logger.info("Running SQL commands calculating measures");
		s.execute("update queries set isTestClass = (instr(execClass,'Test') > 0)");
		s.execute("update project_statistics set nbrOfQueriesNotInTestClasses = (select count(*) from queries B where B.isTestClass=0 and B.projectName=project_statistics.projectName)");
		s.execute("update project_statistics set nbrOfCoveredExecStringLine = (select count(*) from queries B where B.execLineIsTestCovered and B.projectName=project_statistics.projectName);");
		s.execute("update query_construction set isMethodCoveredInTests = (CASE WHEN (select count(*) from coverage C where C.name=methodName and C.methodCovered=1) > 0 THEN 1 ELSE 0 END)");
		s.execute("update query_construction set isTest = (instr(methodName,'Test') > 0)");
		s.execute("update project_statistics set nbrOfDBMethodsNonTest = (select count(distinct A.methodName) from query_construction A where project_statistics.projectName=A.projectName and A.isTest=0)");
		s.execute("update project_statistics set nbrOfDBMethodsNonTestCovered = (select count(distinct A.methodName) from query_construction A where project_statistics.projectName=A.projectName and isTest=0 and isMethodCoveredInTests=1)");
		s.execute("update project_statistics set nbrOfHotspotsNonTest = (select nbr from (select projectName, count(*) as nbr from (select projectName, execClass, execLine from queries where isTestClass=0 group by projectName, execClass, execLine) group by projectName) as sub where project_statistics.projectName=sub.projectName)");
		s.execute("update project_statistics set nbrOfHotspotsNonTestCovered = (select nbr from (select projectName, count(*) as nbr from (select projectName, execClass, execLine from queries where isTestClass=0 and execLineIsTestCovered=1 group by projectName, execClass, execLine) group by projectName) as sub where project_statistics.projectName=sub.projectName)");
		s.execute("update project_statistics set nbrOfMultiQueryHotspotsNonTest = (select nbr from (select projectName, count(*) as nbr from (select projectName, execClass, execLine, count(*) from queries where isTestClass=0 group by projectName, execClass, execLine having count(*) > 1) group by projectName) as sub where project_statistics.projectName=sub.projectName)");
		s.execute("update project_statistics set nbrOfMultiQueryHotspotsNonTestCovered = (select nbr from (select projectName, count(*) as nbr from (select projectName, execClass, execLine, count(*) from queries where isTestClass=0 and execLineIsTestCovered=1 group by projectName, execClass, execLine having count(*) > 1) group by projectName) as sub where project_statistics.projectName=sub.projectName)");
		logger.info("Measures calculation done");
	}
}