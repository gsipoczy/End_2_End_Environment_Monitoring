package sygr3em.model;

import java.util.ArrayList;

public class MultiMerge {
	
	private ArrayList<SingleMerge> list = new ArrayList<>();
	
	public void setList(ArrayList<SingleMerge> s) 
		{ list = s == null? new ArrayList<SingleMerge>() : s;}
	public ArrayList<SingleMerge> getList()
		{ return list == null? new ArrayList<SingleMerge>() : list;}
	
	public void add(SingleMerge sp) {
		if(sp == null) return;
		if(list == null) list = new ArrayList<SingleMerge>();
		list.add(sp);
	}
	public void add(String key, String value, String status) {
		if(key == null) return;
		if(value == null) return;
		if(status == null) return;
		SingleMerge sp = new SingleMerge();
		sp.setKey(key);
		sp.setValue(value);
		sp.setStatus(status);
		if(list == null) list = new ArrayList<SingleMerge>();
		list.add(sp);
	}
	
}
