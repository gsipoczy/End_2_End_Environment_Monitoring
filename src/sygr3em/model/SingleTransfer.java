package sygr3em.model;

public class SingleTransfer {
	
	private String type = "";
	private String key = "";
	private String value = "";
	
	public void setType(String s) { type = s == null? "" : s;}
	public String getType() { return type == null? "" : type;}
	public void setKey(String s) { key = s == null? "" : s;}
	public String getKey() { return key == null? "" : key;}
	public void setValue(String s) { value = s == null? "" : s;}
	public String getValue() { return value == null? "" : value;}
	
}
