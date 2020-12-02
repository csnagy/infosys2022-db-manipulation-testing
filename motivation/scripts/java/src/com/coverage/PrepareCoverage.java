package com.coverage;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
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

import com.coverage.dao.CoverageDAO;
import com.coverage.dao.QueryDAO;
import com.coverage.model.Coverage;

public class PrepareCoverage {
    private static Logger logger = LoggerFactory.getLogger(PrepareCoverage.class);

    private final String coverageOutputDirectory; // this path should end with a '/'
    private final CoverageDAO coverageDAO;
    private final QueryDAO queryDAO;

    public PrepareCoverage(String coverageOutputDirectory, CoverageDAO coverageDAO, QueryDAO queryDAO) {
        this.coverageOutputDirectory = coverageOutputDirectory;
        this.coverageDAO = coverageDAO;
        this.queryDAO = queryDAO;
    }

    public void loadJacocoXML(String projectName, File fXmlFile)
            throws ParserConfigurationException, SAXException, IOException, SQLException {
        JavaNames names = new JavaNames();
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

        NodeList nList = doc.getElementsByTagName("class");
        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element cElement = (Element) nNode;
                NodeList mList = cElement.getElementsByTagName("method");

                for (int temp2 = 0; temp2 < mList.getLength(); temp2++) {
                    Node mNode = mList.item(temp2);

                    if (mNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element mElement = (Element) mNode;
                        Integer methodCovered = null;
                        Integer methodMissed = null;
                        Integer lineCovered = null;
                        Integer lineMissed = null;
                        Integer branchCovered = null;
                        Integer branchMissed = null;

                        NodeList counterList = mElement.getElementsByTagName("counter");
                        for (int tempCounter = 0; tempCounter < counterList.getLength(); tempCounter++) {
                            Node counterNode = counterList.item(tempCounter);
                            if (counterNode.getNodeType() == Node.ELEMENT_NODE) {
                                Element counterElem = (Element) counterNode;
                                if (counterElem.getAttribute("type").equals("METHOD")) {
                                    methodCovered = Integer.parseInt(counterElem.getAttribute("covered"));
                                    methodMissed = Integer.parseInt(counterElem.getAttribute("missed"));
                                } else if (counterElem.getAttribute("type").equals("LINE")) {
                                    lineCovered = Integer.parseInt(counterElem.getAttribute("covered"));
                                    lineMissed = Integer.parseInt(counterElem.getAttribute("missed"));
                                } else if (counterElem.getAttribute("type").equals("BRANCH")) {
                                    branchCovered = Integer.parseInt(counterElem.getAttribute("covered"));
                                    branchMissed = Integer.parseInt(counterElem.getAttribute("missed"));
                                }
                            }
                        }
                        coverageDAO.addMethodCoverageInDB(new Coverage(null, projectName, fXmlFile.getName(),
                                names.getQualifiedMethodName(cElement.getAttribute("name"),
                                        mElement.getAttribute("name"), mElement.getAttribute("desc"), ""),
                                methodCovered, methodMissed, lineCovered, lineMissed, branchCovered, branchMissed));
                    }
                }

            }
        }
    }

    /**
     *
     * @param projectName
     * @param maxNameConvention True to search for files named [projectname]-testcoverage{0-9}.xml in given coverageOutputDirectory.
     * @throws Exception
     */
    public void addTestCoverageInfoForMethods(String projectName,boolean maxNameConvention) throws Exception {
        boolean testCoverageExists;
        if (!coverageDAO.existsCoverageForProject(projectName)) {

            if (maxNameConvention) {
                testCoverageExists= false;
                for (File fXmlFile : listFilesMatching(new File(coverageOutputDirectory), projectName + "-testcoverage\\d+\\.xml")) {
                    try {
                        loadJacocoXML(projectName, fXmlFile);
                        testCoverageExists=true;
                    } catch (DOMException | ParserConfigurationException | SAXException | IOException
                            | SQLException e) {
                        logger.error("Error: ", e);
                    }
                }
                if (testCoverageExists) {
                    setDBMethodBooleansInCoverageTable(projectName);
                }
            } else {

                Path dir = Paths.get(coverageOutputDirectory + "/" + projectName);
                if (dir.toFile().exists()) {
                    try (Stream<Path> walk = Files.walk(dir)) {

                        List<String> result = walk.filter(p -> p.toFile().isFile()).map(Path::toString)
                                .collect(Collectors.toList());

                        result.forEach(f -> {
                            try {
                                loadJacocoXML(projectName, new File(f));
                            } catch (DOMException | ParserConfigurationException | SAXException | IOException
                                    | SQLException e) {
                                logger.error("Error: ", e);
                            }
                        });
                    } catch (IOException e) {
                        logger.error("IO Exception: ", e);
                    }
                    setDBMethodBooleansInCoverageTable(projectName);
                }
            }
        }else{
        logger.info("Coverage file for project [{}] already in database tables", projectName);
    }
    }

    public static File[] listFilesMatching(File root, String regex) {
        if(!root.isDirectory()) {
            throw new IllegalArgumentException(root+" is no directory.");
        }
        final Pattern p = Pattern.compile(regex); // careful: could also throw an exception!
        return root.listFiles(new FileFilter(){
            @Override
            public boolean accept(File file) {
                return p.matcher(file.getName()).matches();
            }
        });
    }

    private void setDBMethodBooleansInCoverageTable(String projectName) throws SQLException  {
        // set isQueryExecutionMethod to true if the method appears at least once as
        // execMethod in the _queries table

        List<String> execMethods = queryDAO.getExecMethods(projectName);
        for (String fullMethodName : execMethods) {
            int lparens = fullMethodName.indexOf('(');
            int rparens = fullMethodName.indexOf(')');
            if (lparens > 0 && rparens > lparens) {
                String params = fullMethodName.substring(lparens + 1, rparens);
                int paramNum = params.split(",").length;

                String fullMethodNameNoParams = fullMethodName.substring(0, fullMethodName.indexOf('('));
                List<String> coveredMethodsWithSameName = coverageDAO
                        .getCoveredMethodsStartingWith(fullMethodNameNoParams + "(", projectName);

                List<String> candidates = new ArrayList<>();
                boolean exactMatch = false;
                for (String candidateName : coveredMethodsWithSameName) {
                    int candLParens = candidateName.indexOf('(');
                    int candRParens = candidateName.indexOf(')');
                    if (candLParens > 0 && candRParens > candLParens) {
                        String candidateParams = candidateName.substring(candLParens + 1, candRParens);
                        int candidateParamNum = candidateParams.split(",").length;
                        if (paramNum == candidateParamNum) {
                            candidates.add(candidateName);
                            if (fullMethodName.equals(candidateName)) {
                                exactMatch = true;
                            }
                        }
                    }
                }

                if (exactMatch) {
                    coverageDAO.updateCoverageQueryExecutionMethod(projectName, fullMethodName, true);
                } else {
                    if (candidates.size() == 1) {
                        coverageDAO.updateCoverageQueryExecutionMethod(projectName, candidates.get(0), true);
                    } else if (candidates.isEmpty()) {
                        logger.warn("Could not find query execution method in coverage table {}", fullMethodName);
                    } else {
                        logger.warn("Too many candidates for query execution method in coverage table {}",
                                fullMethodName);
                        for (String c : candidates) {
                            logger.debug(" - {}", c);
                        }
                    }
                }
            } else {
                // method without parameters, ignoring it
            }
        }

        // set isQueryConstructionMethod to true if the method appears at least once as
        // methodName in the _query_construction table
        List<String> constructionMethods = queryDAO.getConstructionMethods(projectName);
        for (String fullMethodName : constructionMethods) {
            int lparens = fullMethodName.indexOf('(');
            int rparens = fullMethodName.indexOf(')');
            if (lparens > 0 && rparens > lparens) {
                String params = fullMethodName.substring(lparens + 1, rparens);
                int paramNum = params.split(",").length;

                String fullMethodNameNoParams = fullMethodName.substring(0, fullMethodName.indexOf('('));
                List<String> coveredMethodsWithSameName = coverageDAO
                        .getCoveredMethodsStartingWith(fullMethodNameNoParams + "(", projectName);

                List<String> candidates = new ArrayList<>();
                boolean exactMatch = false;
                for (String candidateName : coveredMethodsWithSameName) {
                    int candLParens = candidateName.indexOf('(');
                    int candRParens = candidateName.indexOf(')');
                    if (candLParens > 0 && candRParens > candLParens) {
                        String candidateParams = candidateName.substring(candLParens + 1, candRParens);
                        int candidateParamNum = candidateParams.split(",").length;
                        if (paramNum == candidateParamNum) {
                            candidates.add(candidateName);
                            if (fullMethodName.equals(candidateName)) {
                                exactMatch = true;
                            }
                        }
                    }
                }

                if (exactMatch) {
                    coverageDAO.updateCoverageQueryConstructionMethod(projectName, fullMethodName, true);
                } else {
                    if (candidates.size() == 1) {
                        coverageDAO.updateCoverageQueryConstructionMethod(projectName, candidates.get(0), true);
                    } else if (candidates.isEmpty()) {
                        logger.warn("Could not find query execution method in coverage table {}", fullMethodName);
                    } else {
                        logger.warn("Too many candidates for query execution method in coverage table {}",
                                fullMethodName);
                        for (String c : candidates) {
                            logger.debug(" - {}", c);
                        }
                    }
                }
            } else {
                // method without parameters, ignoring it
            }
        }

    }

}
