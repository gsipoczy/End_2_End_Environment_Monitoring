package sygr3em.model;

import java.util.ArrayList;

public class MultiProperty {
	
	private ArrayList<SingleProperty> list = new ArrayList<>();
	private boolean deleteexisting = false;
	
	public void setDeleteexisting(boolean s) { deleteexisting = s; }
	public boolean getDeleteexising() { return deleteexisting; }
	public void setList(ArrayList<SingleProperty> s) 
		{ list = s == null? new ArrayList<SingleProperty>() : s;}
	public ArrayList<SingleProperty> getList()
		{ return list == null? new ArrayList<SingleProperty>() : list;}
	
	public void add(SingleProperty sp) {
		if(sp == null) return;
		if(list == null) list = new ArrayList<SingleProperty>();
		list.add(sp);
	}
	public void add(String key, String value) {
		if(key == null) return;
		if(value == null) return;
		SingleProperty sp = new SingleProperty();
		sp.setKey(key);
		sp.setValue(value);
		if(list == null) list = new ArrayList<SingleProperty>();
		list.add(sp);
	}
	
}
