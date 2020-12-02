package com.coverage.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.coverage.model.Coverage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoverageDAO {
	private static Logger logger = LoggerFactory.getLogger(CoverageDAO.class);

	private final Connection conn;

	public CoverageDAO(Connection conn) {
		super();
		this.conn = conn;
	}

	public boolean existsCoverageForProject(String projectName) throws SQLException {
		List<String> ret = new ArrayList<>();
		String q = "select distinct projectName from coverage where projectName=?";
		PreparedStatement s = conn.prepareStatement(q);
		s.setString(1, projectName);
		ResultSet res = s.executeQuery();
		if(res.next()==false)
			return false;
		else
			return true;
	}

	public void createTables(boolean truncate) throws SQLException {
		// creating the table for recording test coverage information at method-level,
		// as extracted via JaCoCo
		Statement s = conn.createStatement();
		if(truncate)
			s.execute("drop table if exists coverage");

		s.execute("create table coverage (" + "id integer primary key," + "projectName char(50) not null," // project
																											// name
				+ "fileName char(256) not null," // filename=build variant name
				+ "name blob not null," // name of the method
				+ "methodCovered integer," // coverage of the method (1 = coverage, 0 = uncovered)
				+ "methodMissed integer," // missed methods
				+ "lineCovered integer," // coverage of the lines
				+ "lineMissed integer," // missed lines
				+ "branchCovered integer," // covered branches
				+ "branchMissed integer," // missed branches
				+ "isQueryExecutionMethod boolean," // true if the method executes at least one query, false otherwise
				+ "isQueryConstructionMethod boolean)");
	}

	public List<String> getCoveredMethodsStartingWith(String nameBegins, String projectName) throws SQLException {
		List<String> ret = new ArrayList<>();
		String q = "select distinct name from coverage where name like('" + nameBegins + "%') and projectName=?";
		PreparedStatement s = conn.prepareStatement(q);
		s.setString(1, projectName);
		ResultSet res = s.executeQuery();
		while (res.next()) {
			String name = res.getString(1);
			ret.add(name);
		}
		return ret;
	}

	public void addMethodCoverageInDB(Coverage cov) throws SQLException {
		String q = "insert into coverage (projectName, fileName, name, methodCovered, methodMissed, lineCovered, lineMissed, branchCovered, branchMissed, isQueryExecutionMethod, isQueryConstructionMethod) values (?,?,?,?,?,?,?,?,?,?,?)";
		PreparedStatement s = conn.prepareStatement(q);
		s.setString(1, cov.getProjectName());
		s.setString(2, cov.getFileName());
		s.setString(3, cov.getName());
		s.setInt(4, cov.getMethodCovered());
		s.setInt(5, cov.getMethodMissed());
		if (cov.getLineCovered() != null) {
			s.setInt(6, cov.getLineCovered());
		} else {
			s.setNull(6, 0); // todo check type
		}
		if (cov.getLineMissed() != null) {
			s.setInt(7, cov.getLineMissed());
		} else {
			s.setNull(7, 0); // todo check type
		}
		if (cov.getBranchCovered() != null) {
			s.setInt(8, cov.getBranchCovered());
		} else {
			s.setNull(8, 0); // todo check type
		}
		if (cov.getBranchCovered() != null) {
			s.setInt(9, cov.getBranchCovered());
		} else {
			s.setNull(9, 0); // todo check type
		}
		s.setBoolean(10, false);
		s.setBoolean(11, false);
		s.execute();
	}

	public void updateCoverageQueryExecutionMethod(String projectName, String methodName, boolean val)
			throws SQLException {
		String q = "update coverage set isQueryExecutionMethod = ? where projectName = ? and name = ?";
		PreparedStatement s = conn.prepareStatement(q);
		s.setBoolean(1, val);
		s.setString(2, projectName);
		s.setString(3, methodName);
		int nbrUpdatedRows = s.executeUpdate();
		if (nbrUpdatedRows == 0) {
			logger.error("Could not update coverage: {}", q);
		}
	}

	public void updateCoverageQueryConstructionMethod(String projectName, String methodName, boolean val)
			throws SQLException {
		String q = "update coverage set isQueryConstructionMethod = ? where projectName = ? and name = ?";
		PreparedStatement s = conn.prepareStatement(q);
		s.setBoolean(1, val);
		s.setString(2, projectName);
		s.setString(3, methodName);
		int nbrUpdatedRows = s.executeUpdate();
		if (nbrUpdatedRows == 0) {
			logger.error("Could not update coverage: {}", q);
		}
	}
}
