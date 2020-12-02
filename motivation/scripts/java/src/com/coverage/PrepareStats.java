package com.coverage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrepareStats {
	private static Logger logger = LoggerFactory.getLogger(PrepareStats.class);

	private final Connection dbConnection;

	public PrepareStats(Connection dbConnection) {
		this.dbConnection = dbConnection;
	}

	public void saveStats(String projectName) throws SQLException {
		String q;
		q = "delete from project_statistics where projectName = ?";
		PreparedStatement s = dbConnection.prepareStatement(q);
		s.setString(1, projectName);
		s.execute();
		q = "insert into project_statistics(projectName, " + "nbrOfHotspots," + "nbrOfMultipleQueryHotspots,"
				+ "nbrOfQueries," + "nbrOfUnResolvedQueries," + "nbrOfMethodsJacoco," + "nbrOfQueryExecutionMethodsJacoco,"
				+ "nbrOfQueryConstructionMethodsJacoco,nbrOfDBMethods, nbrOfDBMethodCovered, nbrOfMethodCovered,"
				+ "nbrOfTestSuites, nbrOfTestCases) values (?,?,?,?,?,?,?,?,?,?,?,?,?)";
		s = dbConnection.prepareStatement(q);
		s.setString(1, projectName);
		s.setInt(2, getNumberOfHotspots(projectName));
		s.setInt(3, getNumberOfMultiQueryHotspots(projectName));
		s.setInt(4, getNumberOfQueries(projectName));
		s.setInt(5, getNumberOfUnresolvedQueries(projectName));
		s.setInt(6, getNumberOfMethods(projectName));
		s.setInt(7, getNumberOfQueryExecutionMethods(projectName));
		s.setInt(8, getNumberOfQueryConstructionMethods(projectName));
		s.setInt(9, getNumberOfDBMethods(projectName));
		s.setInt(10, getNumberOfCoveredMethodQueryConstructionOrQueryExec(projectName));
		s.setInt(11, getNumberOfCoveredMethod(projectName));
		s.setInt(12, getNumberOfTestSuites(projectName));
		s.setInt(13, getNumberOfTestCases(projectName));
		s.execute();
	}

	private int getNumberOfMultiQueryHotspots(String projectName) throws SQLException {
		String q;
		int nbr = 0;
		PreparedStatement s;
		ResultSet res;
		q = "select count(*) from (select execClass, execLine, count(*) from queries where projectName=? group by execClass, execLine having count(*) > 1)";
		s = dbConnection.prepareStatement(q);
		s.setString(1, projectName);
		res = s.executeQuery();
		if (res.next()) {
			nbr = res.getInt(1);
		}
		return nbr;
	}

	private int getNumberOfQueries(String projectName) throws SQLException {
		String q;
		int nbr = 0;
		PreparedStatement s;
		ResultSet res;
		q = "select count(*) from queries where projectName=?";
		s = dbConnection.prepareStatement(q);
		s.setString(1, projectName);
		res = s.executeQuery();
		if (res.next()) {
			nbr = res.getInt(1);
		}
		return nbr;
	}

	private int getNumberOfQueryExecutionMethods(String projectName) throws SQLException {
		String q;
		int nbr = 0;
		PreparedStatement s;
		ResultSet res;
		q = "select count(distinct name) from coverage where isQueryExecutionMethod = true and projectName=?";
		s = dbConnection.prepareStatement(q);
		s.setString(1, projectName);
		res = s.executeQuery();
		if (res.next()) {
			nbr = res.getInt(1);
		}
		return nbr;
	}

	private int getNumberOfQueryConstructionMethods(String projectName) throws SQLException {
		String q;
		int nbr = 0;
		PreparedStatement s;
		ResultSet res;
		q = "select count(distinct name) from coverage where isQueryConstructionMethod = true and projectName=?";
		s = dbConnection.prepareStatement(q);
		s.setString(1, projectName);
		res = s.executeQuery();
		if (res.next()) {
			nbr = res.getInt(1);
		}
		return nbr;
	}

	private int getNumberOfCoveredMethodQueryConstructionOrQueryExec(String projectName) throws SQLException {
		String q;
		int nbr = 0;
		PreparedStatement s;
		ResultSet res;
		q = "select count(distinct name) from coverage where projectName=? AND methodCovered=1 AND (isQueryConstructionMethod=1 OR isQueryConstructionMethod=1)";
		s = dbConnection.prepareStatement(q);
		s.setString(1, projectName);
		res = s.executeQuery();
		if (res.next()) {
			nbr = res.getInt(1);
		}
		return nbr;
	}

	private int getNumberOfDBMethods(String projectName) throws SQLException {
		String q;
		int nbr = 0;
		PreparedStatement s;
		ResultSet res;
		q = "select count(distinct methodName) from query_construction where projectName=?";
		s = dbConnection.prepareStatement(q);
		s.setString(1, projectName);
		res = s.executeQuery();
		if (res.next()) {
			nbr = res.getInt(1);
		}
		return nbr;
	}

	private int getNumberOfHotspots(String projectName) throws SQLException {
		String q = "select count(*) from (select execClass, execLine from queries where projectName=? group by execClass, execLine)";
		PreparedStatement s = dbConnection.prepareStatement(q);
		int nbr = 0;
		s.setString(1, projectName);
		ResultSet res = s.executeQuery();
		if (res.next()) {
			nbr = res.getInt(1);
		}
		return nbr;
	}

	private int getNumberOfUnresolvedQueries(String projectName) throws SQLException {
		String q;
		int nbr = 0;
		PreparedStatement s;
		ResultSet res;
		q = "select count(*) from queries where value = '{{na}}' and projectName=?";
		s = dbConnection.prepareStatement(q);
		s.setString(1, projectName);
		res = s.executeQuery();
		if (res.next()) {
			nbr = Integer.parseInt(res.getString(1));
		}
		return nbr;

	}

	private int getNumberOfMethods(String projectName) throws SQLException {
		int nbr = 0;
		String q = "select count(distinct name) from coverage where projectName=?";
		PreparedStatement s = dbConnection.prepareStatement(q);
		s.setString(1, projectName);
		ResultSet res = s.executeQuery();
		if (res.next()) {
			nbr = res.getInt(1);
		}
		return nbr;
	}

	// TODO: add this information to the project_statistics table
	@SuppressWarnings("unused")
	private void displayQueryExecMethodCoverageStats(String projectName) throws SQLException {
		String q = "select coverage, count(distinct name) from coverage where isQueryExecutionMethod = true  and projectName=? group by coverage";
		PreparedStatement s = dbConnection.prepareStatement(q);
		s.setString(1, projectName);
		ResultSet res = s.executeQuery();
		while (res.next()) {
			logger.info("coverage stats (DB query exec methods): {} - {}", res.getString(1), res.getString(2));
		}
	}

	// TODO: add this information to the project_statistics table
	@SuppressWarnings("unused")
	private void displayQueryConstructionMethodCoverageStats(String projectName) throws SQLException {
		String q = "select coverage, count(distinct name) from coverage where isQueryConstructionMethod = true  and projectName=? group by coverage";
		PreparedStatement s = dbConnection.prepareStatement(q);
		s.setString(1, projectName);
		ResultSet res = s.executeQuery();
		while (res.next()) {
			logger.info("coverage stats (DB query construction methods): {} - {}", res.getString(1), res.getString(2));
		}
	}

	private int getNumberOfTestCases(String projectName) throws SQLException {
		int nbr = 0;
		String q = "select count(distinct tc.name) from testcase tc join testsuite ts on (tc.testsuite = ts.id) where projectName=?";
		PreparedStatement s = dbConnection.prepareStatement(q);
		s.setString(1, projectName);
		ResultSet res = s.executeQuery();
		if (res.next()) {
			nbr = res.getInt(1);
		}
		return nbr;
	}

	private int getNumberOfTestSuites(String projectName) throws SQLException {
		int nbr = 0;
		String q = "select count(distinct name) from testsuite where projectName=?";
		PreparedStatement s = dbConnection.prepareStatement(q);
		s.setString(1, projectName);
		ResultSet res = s.executeQuery();
		if (res.next()) {
			nbr = res.getInt(1);
		}
		return nbr;
	}

	private int getNumberOfCoveredMethod(String projectName) throws SQLException {
		int nbr = 0;
		String q = "select count(distinct name) from coverage where projectName=? AND methodCovered=1";
		PreparedStatement s = dbConnection.prepareStatement(q);
		s.setString(1, projectName);
		ResultSet res = s.executeQuery();
		if (res.next()) {
			nbr = res.getInt(1);
		}
		return nbr;
	}

}
