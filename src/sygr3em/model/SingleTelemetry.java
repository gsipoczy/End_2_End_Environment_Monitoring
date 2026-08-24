package sygr3em.model;

public class SingleTelemetry {
	
	private String c12c = "";
	private double value = 0D;
	private String uom = "";
	
	public void setC12c(String s) { c12c = s == null? "" : s;}
	public String getC12c() { return c12c == null? "" : c12c;}
	public void setUom(String s) { uom = s == null? "" : s;}
	public String getUom() { return uom == null? "" : uom;}
	public void setValue(double s) { value = s; }
	public double getValue() { return value; }
	
}
