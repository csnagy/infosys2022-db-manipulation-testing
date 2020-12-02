package com.coverage.model;

public class Coverage {
	private Integer id;
	private String projectName;
	private String fileName;
	private String name;

	private Integer methodCovered;
	private Integer methodMissed;
	private Integer lineCovered;
	private Integer lineMissed;
	private Integer branchCovered;
	private Integer branchMissed;

	public Coverage(int id, String projectName, String fileName, String name) {
		super();
		this.id = id;
		this.projectName = projectName;
		this.fileName = fileName;
		this.name = name;
	}

	public Coverage(Integer id, String projectName, String fileName, String name, Integer methodCovered, Integer methodMissed,
			Integer lineCovered, Integer lineMissed, Integer branchCovered, Integer branchMissed) {
		super();
		this.id = id;
		this.projectName = projectName;
		this.fileName = fileName;
		this.name = name;
		this.methodCovered = methodCovered;
		this.methodMissed = methodMissed;
		this.lineCovered = lineCovered;
		this.lineMissed = lineMissed;
		this.branchCovered = branchCovered;
		this.branchMissed = branchMissed;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getMethodCovered() {
		return methodCovered;
	}

	public void setMethodCovered(int methodCovered) {
		this.methodCovered = methodCovered;
	}

	public Integer getMethodMissed() {
		return methodMissed;
	}

	public void setMethodMissed(int methodMissed) {
		this.methodMissed = methodMissed;
	}

	public Integer getLineCovered() {
		return lineCovered;
	}

	public void setLineCovered(int lineCovered) {
		this.lineCovered = lineCovered;
	}

	public Integer getLineMissed() {
		return lineMissed;
	}

	public void setLineMissed(int lineMissed) {
		this.lineMissed = lineMissed;
	}

	public Integer getBranchCovered() {
		return branchCovered;
	}

	public void setBranchCovered(int branchCovered) {
		this.branchCovered = branchCovered;
	}

	public Integer getBranchMissed() {
		return branchMissed;
	}

	public void setBranchMissed(int branchMissed) {
		this.branchMissed = branchMissed;
	}

	@Override
	public String toString() {
		return "Coverage [id=" + id + ", projectName=" + projectName + ", fileName=" + fileName + ", name=" + name
				+ ", methodCovered=" + methodCovered + ", methodMissed=" + methodMissed + ", lineCovered=" + lineCovered
				+ ", lineMissed=" + lineMissed + ", branchCovered=" + branchCovered + ", branchMissed=" + branchMissed
				+ "]";
	}
}
