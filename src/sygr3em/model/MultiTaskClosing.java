package sygr3em.model;

import java.util.ArrayList;

public class MultiTaskClosing {
	private String characteristic = "";
	private double value = 0D;
	private ArrayList<SingleTaskClosing> tcs = new ArrayList<>();
	
	public void setCharacteristic(String s) { characteristic = s == null? "" : s;}
	public String getCharacteristic() { return characteristic == null? "" : characteristic;}
	public void setValue(double s) { value = s; }
	public double getValue() { return value; }
	public void setTcs(ArrayList<SingleTaskClosing> s) { tcs = s == null? new ArrayList<SingleTaskClosing>() : s;}
	public ArrayList<SingleTaskClosing> getTcs() { return tcs == null? new ArrayList<SingleTaskClosing>() : tcs;}	
}
