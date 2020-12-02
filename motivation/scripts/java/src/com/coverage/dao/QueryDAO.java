package com.coverage.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.coverage.model.Query;

public class QueryDAO {
	private final Connection conn;

	public QueryDAO(Connection conn) {
		super();
		this.conn = conn;
	}

	public void createTables(boolean truncate) throws SQLException {
		Statement s = conn.createStatement();
		if (truncate) {
			s.execute("drop table if exists queries");
			s.execute("drop table if exists query_construction");
		}
		s.execute("create table queries (" + "projectName char(50) not null," // project name
				+ "id integer not null, " // query id
				+ "value blob not null, " // query value
				+ "hotspotfinder blob not null, " // hotspot finder value
				+ "execClass char(100) not null, " // class where the query is executed
				+ "isTestClass boolean,"
				+ "execLine integer not null, " // line of code where the query is executed
				+ "execLineIsTestCovered boolean,"
				+ "execString blob not null, " // Java statement executing the query
				+ "execMethod blob, " // method where the query is executed
				+ "execMethodLine integer, " // line of code where the executing method starts
				+ "buildingStackSize numeric(5))"); // size of the query construction call stack (> 1 if
		// interprocedural)

		// creating the table for recording the query construction methods, as extracted
		// via SQLInspect
		s.execute("create table query_construction (" + "projectName char(50) not null," // project name
				+ "queryId integer not null references queries," // reference to the query
				+ "methodName blob not null references coverage," // reference to the method name
				+ "isTest boolean," // does the name of the method contains 'test'. Used to spot if the method is a test method.
				+ "isMethodCoveredInTests boolean,"
				+ "sequenceNumber integer not null)"); // sequence index of the method in the query construction
		// call stack
	}

	public List<String> getExecMethods(String projectName) throws SQLException {
		List<String> ret = new ArrayList<>();
		String q = "select distinct execMethod from queries where projectName=?";
		PreparedStatement s = conn.prepareStatement(q);
		s.setString(1, projectName);
		ResultSet res = s.executeQuery();
		while (res.next()) {
			ret.add(res.getString(1));
		}
		return ret;
	}

	public boolean existsQueriesForProject(String projectName) throws SQLException {
		List<String> ret = new ArrayList<>();
		String q = "select distinct projectName from queries where projectName=?";
		PreparedStatement s = conn.prepareStatement(q);
		s.setString(1, projectName);
		ResultSet res = s.executeQuery();
		if(res.next()==false)
			return false;
		else
			return true;
	}

	public List<String> getConstructionMethods(String projectName) throws SQLException {
		List<String> ret = new ArrayList<>();
		String q = "select distinct methodName from query_construction where projectName=?";
		PreparedStatement s = conn.prepareStatement(q);
		s.setString(1, projectName);
		ResultSet res = s.executeQuery();
		while (res.next()) {
			ret.add(res.getString(1));
		}
		return ret;
	}

	public void addQuery(Query query) throws SQLException {
		String q = "insert into queries(projectName, id, value, execClass, execLine, execString, buildingStackSize, execMethod, hotspotfinder) values (?,?,?,?,?,?,?,?,?)";
		PreparedStatement s = conn.prepareStatement(q);
		s.setString(1, query.getProjectName());
		s.setInt(2, query.getId());
		s.setString(3, query.getValue());
		s.setString(4, query.getExecClass());
		s.setInt(5, query.getExecLine());
		s.setString(6, query.getExecString());
		s.setInt(7, query.getBuildingStackSize());
		s.setString(8, query.getExecMethod());
		s.setString(9, query.getHotspotFinder());
		s.execute();
	}

	public void addQueryConstruction(String projectName, int queryId, String methodName, int seq) throws SQLException {
		String q = "insert into query_construction(projectName, queryId, methodName, sequenceNumber) values (?,?,?,?)";
		PreparedStatement s = conn.prepareStatement(q);
		s.setString(1, projectName);
		s.setInt(2, queryId);
		s.setString(3, methodName);
		s.setInt(4, seq);
		s.execute();
	}
}
