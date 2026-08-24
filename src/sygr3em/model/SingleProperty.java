package sygr3em.model;

public class SingleProperty {
	
	private String key = "";
	private String value = "";
	
	public void setKey(String s) { key = s == null? "" : s;}
	public String getKey() { return key == null? "" : key;}
	public void setValue(String s) { value = s == null? "" : s;}
	public String getValue() { return value == null? "" : value;}
	
}
