package sygr3em.model;

public class SingleLocMessage {

	private String location = "";
	private String text = "";
	
	public void setLocation(String s) { location = s == null? "" : s;}
	public String getLocation() { return location == null? "" : location;}
	public void setText(String s) { text = s == null? "" : s;}
	public String getText() { return text == null? "" : text;}
	
}
