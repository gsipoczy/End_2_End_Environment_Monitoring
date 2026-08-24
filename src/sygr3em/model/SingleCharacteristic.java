package sygr3em.model;

public class SingleCharacteristic {
	
	private String name = "";
	private String uom = "";
	private String displayname = "";
	private String mdfield = "";
	private String mdpottype = "";
	private long minfrequency = 0L;
	
	public void setName(String s) { name = s == null? "" : s;}
	public String getName() { return name == null? "" : name;}
	public void setUom(String s) { uom = s == null? "" : s;}
	public String getUom() { return uom == null? "" : uom;}
	public void setDisplayname(String s) { displayname = s == null? "" : s;}
	public String getDisplayname() { return displayname == null? "" : displayname;}
	public void setMdfield(String s) { mdfield = s == null? "" : s;}
	public String getMdfield() { return mdfield == null? "" : mdfield;}
	public void setMdpottype(String s) { mdpottype = s == null? "" : s;}
	public String getMdpottype() { return mdpottype == null? "" : mdpottype;}
	public void setMinfrequency(long s) { minfrequency = s; }
	public long getMinfrequency() { return minfrequency; }
	
}
