package sygr3em.model;

public class SingleTaskClosing {
	private String oldtasklocation = "";
	private String nexttasklocation = "";
	
	public void setOldtasklocation(String s) { oldtasklocation = s == null? "" : s;}
	public String getOldtasklocation() { return oldtasklocation == null? "" : oldtasklocation;}
	public void setNexttasklocation(String s) { nexttasklocation = s == null? "" : s;}
	public String getNexttasklocation() { return nexttasklocation == null? "" : nexttasklocation;}
	
}
