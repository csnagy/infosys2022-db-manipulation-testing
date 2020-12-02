package com.coverage;

import java.io.*;
import java.sql.SQLException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.coverage.dao.QueryDAO;
import com.coverage.model.Query;

public class PrepareQueries {
	@SuppressWarnings("unused")
	private static Logger logger = LoggerFactory.getLogger(PrepareQueries.class);

	private final String sqlinspectOutputDirectory;// this path should end with a '/'
	private final QueryDAO queryDAO;

	public PrepareQueries(String sqlinspectOutputDirectory, QueryDAO queryDAO) {
		this.sqlinspectOutputDirectory = sqlinspectOutputDirectory;
		this.queryDAO = queryDAO;
	}

	public void parseInspectedQueries(String projectName)
			throws ParserConfigurationException, SAXException, IOException, SQLException {
		if (!queryDAO.existsQueriesForProject(projectName)) {

//			File fXmlFile = new File(sqlinspectOutputDirectory + "/" + projectName + "-queries.xml");
			InputStream inputStream= new FileInputStream(sqlinspectOutputDirectory + "/" + projectName + "-queries.xml");
			Reader reader = new InputStreamReader(inputStream,"UTF-8");
			InputSource is = new InputSource(reader);
			is.setEncoding("UTF-8");

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(is);
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("Query");
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					addQueryInDB(projectName, eElement);
				}
			}
		}else{
			logger.info("Query table already contains rows for [{}]", projectName);
		}
	}

	private void addQueryInDB(String projectName, Element eElement) throws SQLException {
		Integer queryId = Integer.parseInt(eElement.getAttribute("id"));
		String value = eElement.getElementsByTagName("Value").item(0).getTextContent();
		String execClass = eElement.getElementsByTagName("ExecClass").item(0).getTextContent();
		int execLine = Integer.parseInt(eElement.getElementsByTagName("ExecLine").item(0).getTextContent());
		String execString = eElement.getElementsByTagName("ExecString").item(0).getTextContent();
		String hotspotFinder = "";
		if (eElement.getElementsByTagName("HotspotFinder").item(0) == null) {
			hotspotFinder = "JDBCHotspotFinder";
		} else {
			hotspotFinder = eElement.getElementsByTagName("HotspotFinder").item(0).getTextContent();
		}
		int callStackSize = eElement.getElementsByTagName("Call").getLength();
		String execMethod = "";
		NodeList mList = eElement.getElementsByTagName("Call");
		int nbrOfCallingMethods = mList.getLength();
		if (nbrOfCallingMethods > 0) {
			// extracting the query execution method (i.e., the last one of the call stack)
			Node n = mList.item(nbrOfCallingMethods - 1);
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				Element elem = (Element) n;
				execMethod = elem.getAttribute("method");
			}
		}

		Query q = new Query(projectName, queryId, value, execClass, execLine, execString, execMethod, callStackSize, hotspotFinder);
		queryDAO.addQuery(q);

		if (nbrOfCallingMethods > 0) {
			for (int i = 1; i <= nbrOfCallingMethods; i++) {
				Node n = mList.item(i - 1);
				if (n.getNodeType() == Node.ELEMENT_NODE) {
					Element elem = (Element) n;
					String methodName = elem.getAttribute("method");
					queryDAO.addQueryConstruction(projectName, queryId, methodName, i);
				}
			}
		}
	}

}
