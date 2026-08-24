package sygr3em.model;

public class MeasurementCalcResult {

	private String status = "";
	private String message = "";
	private String severity = "";
	private String problemtype = "";
	private String location = "";
	private String budgetname = "";
	private String taskname = "";
	
	public MeasurementCalcResult(String initialstatus) {
		status = initialstatus;
	}
	
	public void setStatus(String s) { status = s == null? "" : s;}
	public String getStatus() { return status == null? "" : status;}
	public void setMessage(String s) { message = s == null? "" : s;}
	public String getMessage() { return message == null? "" : message;}
	public void setSeverity(String s) { severity = s == null? "" : s;}
	public String getSeverity() { return severity == null? "" : severity;}
	public void setProblemtype(String s) { problemtype = s == null? "" : s;}
	public String getProblemtype() { return problemtype == null? "" : problemtype;}
	public void setLocation(String s) { location = s == null? "" : s;}
	public String getLocation() { return location == null? "" : location;}
	public void setBudgetname(String s) { budgetname = s == null? "" : s;}
	public String getBudgetname() { return budgetname == null? "" : budgetname;}
	public void setTaskname(String s) { taskname = s == null? "" : s;}
	public String getTaskname() { return taskname == null? "" : taskname;}
	
}
