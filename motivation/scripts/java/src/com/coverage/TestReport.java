package com.coverage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class TestReport {
	private static Logger logger = LoggerFactory.getLogger(TestReport.class);

	private final String testReportOutputDirectory; // this path should end with a '/'
	private final Connection dbConnection;

	public TestReport(String testReportOutputDirectory, Connection dbConnection) {
		this.testReportOutputDirectory = testReportOutputDirectory;
		this.dbConnection = dbConnection;
	}

	private void loadTestReportXML(String projectName, String variantName, String fileName)
			throws ParserConfigurationException, SAXException, IOException, SQLException {
		File fXmlFile = new File(fileName);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		dbFactory.setValidating(false); // ignore dtd
		dbFactory.setNamespaceAware(true);
		dbFactory.setFeature("http://xml.org/sax/features/namespaces", false);
		dbFactory.setFeature("http://xml.org/sax/features/validation", false);
		dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
		dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(fXmlFile);
		doc.getDocumentElement().normalize();

		NodeList nList = doc.getElementsByTagName("testsuite");
		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node tsNode = nList.item(temp);
			if (tsNode.getNodeType() == Node.ELEMENT_NODE) {
				Element tsElement = (Element) tsNode;
				String testSuiteName = tsElement.getAttribute("name");
				int tsId = addTestSuite(projectName, variantName, fileName, testSuiteName);

				NodeList mList = tsElement.getElementsByTagName("testcase");
				for (int temp2 = 0; temp2 < mList.getLength(); temp2++) {
					Node tcNode = mList.item(temp2);
					if (tcNode.getNodeType() == Node.ELEMENT_NODE) {
						Element tcElement = (Element) tcNode;
						String testCaseName = tcElement.getAttribute("name");
						String className = tcElement.getAttribute("classname");
						addTestCase(tsId, testCaseName, className);
					}
				}
			}
		}
	}

	public void addTestReportInfoForMethods(String projectName) {
		Path dir = Paths.get(testReportOutputDirectory + "/" + projectName);
		if (dir.toFile().exists()) {
			try (Stream<Path> walk = Files.list(dir)) {

				List<String> variants = walk.filter(p -> p.toFile().isDirectory()).map(Path::toString)
						.collect(Collectors.toList());

				for (String variant : variants) {
					Path variantDir = Paths.get(variant);
					String variantName = variantDir.getFileName().toString();
					List<String> files = Files.walk(variantDir).filter(p -> p.toFile().isFile()).map(Path::toString)
							.collect(Collectors.toList());

					files.forEach(f -> {
						try {
							if (f.endsWith(".xml")) {
								loadTestReportXML(projectName, variantName, f);
							}
						} catch (DOMException | ParserConfigurationException | SAXException | IOException
								| SQLException e) {
							logger.error("Error: ", e);
						}
					});
				}

			} catch (IOException e) {
				logger.error("IO Exception", e);
			}
		}

	}

	private int addTestSuite(String projectName, String variantName, String fileName, String testSuiteName)
			throws SQLException {
		String q = "insert into testsuite (projectName, variantName, fileName, name) values (?,?,?,?)";
		PreparedStatement s = dbConnection.prepareStatement(q);
		s.setString(1, projectName);
		s.setString(2, variantName);
		s.setString(3, fileName);
		s.setString(4, testSuiteName);
		s.execute();
		return s.getGeneratedKeys().getInt(1);
	}

	private void addTestCase(int testSuiteId, String testCaseName, String className) throws SQLException {
		String q = "insert into testcase (testsuite, name, className) values (?,?,?)";
		PreparedStatement s = dbConnection.prepareStatement(q);
		s.setInt(1, testSuiteId);
		s.setString(2, testCaseName);
		s.setString(3, className);
		s.execute();
	}

}
