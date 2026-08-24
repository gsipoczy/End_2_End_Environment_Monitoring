package sygr3em.model;

import java.time.Instant;
import java.util.ArrayList;

public class MultiTelemetry implements Comparable<MultiTelemetry> {
	
	private Instant timestamp = Instant.ofEpochMilli(0);
	private ArrayList<SingleTelemetry> list = new ArrayList<>();
	
	public void setList(ArrayList<SingleTelemetry> s) 
		{ list = s == null? new ArrayList<SingleTelemetry>() : s;}
	public ArrayList<SingleTelemetry> getList()
		{ return list == null? new ArrayList<SingleTelemetry>() : list;}
	
	public void add(SingleTelemetry sp) {
		if(sp == null) return;
		if(list == null) list = new ArrayList<SingleTelemetry>();
		list.add(sp);
	}
	public void add(String c12c, double value, String uom) {
		if(c12c == null) return;
		if(uom == null) return;
		SingleTelemetry st = new SingleTelemetry();
		st.setC12c(c12c);
		st.setUom(uom);
		st.setValue(value);
		if(list == null) list = new ArrayList<SingleTelemetry>();
		list.add(st);
	}
	
	public void setTimestamp(Instant s) { timestamp = s == null ? Instant.ofEpochMilli(0) : s; }
	public Instant getTimestamp() { return timestamp == null ? Instant.ofEpochMilli(0) : timestamp; }
	
	@Override
	public int compareTo(MultiTelemetry other) {
    	if(other == null || other.getTimestamp() == null) return 0;
    	return timestamp.compareTo(other.getTimestamp());
    }
	
}
