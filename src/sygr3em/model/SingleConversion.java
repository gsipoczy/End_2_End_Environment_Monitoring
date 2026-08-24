package sygr3em.model;

public class SingleConversion {
	
	private String c12c = "";
	private String uom = "";
	private String formula = "";
	
	public void setC12c(String s) { c12c = s == null? "" : s;}
	public String getC12c() { return c12c == null? "" : c12c;}
	public void setUom(String s) { uom = s == null? "" : s;}
	public String getUom() { return uom == null? "" : uom;}
	public void setFormula(String s) { formula = s == null? "" : s;}
	public String getFormula() { return formula == null? "" : formula;}
	
}
