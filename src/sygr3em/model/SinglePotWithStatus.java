package sygr3em.model;

import sygr.pots.extensions.Pot;

public class SinglePotWithStatus {
	
	private Pot pot = new Pot();
	private String newstatus = "";
	
	public void setPot(Pot s) { pot = s == null? new Pot() : s;}
	public Pot getPot() { return pot == null? new Pot() : pot;}
	public void setNewstatus(String s) { newstatus = s == null? "" : s;}
	public String getNewstatus() { return newstatus == null? "" : newstatus;}
	
}
