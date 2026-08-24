package sygr3em.model;

public class Split {

	private String status = "";
	private MultiProperty ids = new MultiProperty();
	
	public void setStatus(String s) { status = s == null? "" : s;}
	public String getStatus() { return status == null? "" : status;}
	public void setIds(MultiProperty s) 
	{ ids = s == null? new MultiProperty() : s;}
	public MultiProperty getIds() 
	{ return ids == null? new MultiProperty() : ids;}
	
}
