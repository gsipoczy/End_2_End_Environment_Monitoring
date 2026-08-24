package sygr3em.model;

public class SingleMerge {
	
	private String key = "";
	private String value = "";
	private String status = "";
	
	public void setKey(String s) { key = s == null? "" : s;}
	public String getKey() { return key == null? "" : key;}
	public void setValue(String s) { value = s == null? "" : s;}
	public String getValue() { return value == null? "" : value;}
	public void setStatus(String s) { status = s == null? "" : s;}
	public String getStatus() { return status == null? "" : status;}
	
}
