package sygr3em.model;

public class SinglePotIdWithStoretype {
	
	private String potid = "";
	private String storetype = "";
	
	public void setPotid(String s) { potid = s == null? "" : s;}
	public String getPotid() { return potid == null? "" : potid;}
	public void setStoretype(String s) { storetype = s == null? "" : s;}
	public String getStoretype() { return storetype == null? "" : storetype;}
	
}
