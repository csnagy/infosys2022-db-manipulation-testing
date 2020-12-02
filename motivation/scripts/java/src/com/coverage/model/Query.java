package com.coverage.model;

public class Query {
	private Integer id;
	private String projectName;
	private String value;
	private String execClass;
	private Integer execLine;
	private String execString;
	private String execMethod;
	private Integer buildingStackSize;
	private String hotspotFinder;

	public Query(String projectName, Integer id, String value, String execClass, Integer execLine, String execString,
			String execMethod, Integer buildingStackSize, String hotspotFinder) {
		super();
		this.id = id;
		this.projectName = projectName;
		this.value = value;
		this.execClass = execClass;
		this.execString = execString;
		this.execMethod = execMethod;
		this.execLine = execLine;
		this.buildingStackSize = buildingStackSize;
		this.hotspotFinder = hotspotFinder;
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

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getExecClass() {
		return execClass;
	}

	public void setExecClass(String execClass) {
		this.execClass = execClass;
	}

	public String getExecString() {
		return execString;
	}

	public void setExecString(String execString) {
		this.execString = execString;
	}

	public String getExecMethod() {
		return execMethod;
	}

	public void setExecMethod(String execMethod) {
		this.execMethod = execMethod;
	}

	public Integer getExecLine() {
		return execLine;
	}

	public void setExecLine(Integer execLine) {
		this.execLine = execLine;
	}

	public Integer getBuildingStackSize() {
		return buildingStackSize;
	}

	public void setBuildingStackSize(Integer buildingStackSize) {
		this.buildingStackSize = buildingStackSize;
	}

	public String getHotspotFinder() {
		return hotspotFinder;
	}

	public void setHotspotFinder(String hotspotFinder) {
		this.hotspotFinder = hotspotFinder;
	}
}
