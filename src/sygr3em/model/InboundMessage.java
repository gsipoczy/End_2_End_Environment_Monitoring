package sygr3em.model;

import java.time.Instant;
import java.util.ArrayList;
import sygr.pots.extensions.*;

public class InboundMessage {

	private String command = "";
	
	private SingleProperty id = new SingleProperty();
	
	private MultiProperty secondaryIds = new MultiProperty();
	private MultiProperty primaryAttributes  = new MultiProperty();
	private MultiProperty secondaryAttributes  = new MultiProperty();
	private MultiProperty parameters  = new MultiProperty();
	private MultiMerge merges  = new MultiMerge();
	private Split split = new Split();
	private ArrayList<MultiTelemetry> measurements  = new ArrayList<>();
	private String task = "";
	private Instant timestamp = Instant.ofEpochMilli(0);
	private Attr fixed = new Attr();
	private Attr flexi = new Attr();
	
	
	public void setCommand(String s) { command = s == null? "" : s;}
	public String getCommand() { return command == null? "" : command;}
	
	public void setId(SingleProperty s) { id = s == null? new SingleProperty() : s;}
	public SingleProperty getId() { return id == null? new SingleProperty() : id;}
	
	public void setSecondaryIds(MultiProperty s) 
		{ secondaryIds = s == null? new MultiProperty() : s;}
	public MultiProperty getSecondaryIds() 
		{ return secondaryIds == null? new MultiProperty() : secondaryIds;}
	
	public void setPrimaryAttributes(MultiProperty s) 
	{ primaryAttributes = s == null? new MultiProperty() : s;}
	public MultiProperty getPrimaryAttributes() 
	{ return primaryAttributes == null? new MultiProperty() : primaryAttributes;}
	
	public void setSecondaryAttributes(MultiProperty s) 
	{ secondaryAttributes = s == null? new MultiProperty() : s;}
	public MultiProperty getSecondaryAttributes() 
	{ return secondaryAttributes == null? new MultiProperty() : secondaryAttributes;}
	
	public void setParameters(MultiProperty s) 
	{ parameters = s == null? new MultiProperty() : s;}
	public MultiProperty getParameters() 
	{ return parameters == null? new MultiProperty() : parameters;}
	
	public void setMerges(MultiMerge s) 
	{ merges = s == null? new MultiMerge() : s;}
	public MultiMerge getMerges() 
	{ return merges == null? new MultiMerge() : merges;}
	public void setSplit(Split s) 
	{ split = s == null? new Split() : s;}
	public Split getSplit() 
	{ return split == null? new Split() : split;}
	
	public void setMeasurements(ArrayList<MultiTelemetry> s) {
		measurements = s == null? new ArrayList<MultiTelemetry>() : s; 
	}
	public ArrayList<MultiTelemetry> getMeasurements() {
		return measurements == null ? new ArrayList<MultiTelemetry>() : measurements;
	}
	
	
	public void setId(String key, String value) {
		if(key == null || key.equals("")) return;
		if(value == null || value.equals("")) return;
		if(id == null) id = new SingleProperty();
		id.setKey(key);
		id.setValue(value);
	}
	
	public void addSecondaryId(SingleProperty sp) {
		if(sp == null) return;
		if(secondaryIds == null) secondaryIds = new MultiProperty();
		secondaryIds.getList().add(sp);
	}
	public void addSecondaryId(String key, String value) {
		if(key == null) return;
		if(value == null) return;
		SingleProperty sp = new SingleProperty();
		sp.setKey(key);
		sp.setValue(value);
		if(secondaryIds == null) secondaryIds = new MultiProperty();
		secondaryIds.getList().add(sp);
	}
	
	public void addPrimaryAttribute(SingleProperty sp) {
		if(sp == null) return;
		if(primaryAttributes == null) primaryAttributes = new MultiProperty();
		primaryAttributes.getList().add(sp);
	}
	public void addPrimaryAttribute(String key, String value) {
		if(key == null) return;
		if(value == null) return;
		SingleProperty sp = new SingleProperty();
		sp.setKey(key);
		sp.setValue(value);
		if(primaryAttributes == null) primaryAttributes = new MultiProperty();
		primaryAttributes.getList().add(sp);
	}
	
	public void addSecondaryAttribute(SingleProperty sp) {
		if(sp == null) return;
		if(secondaryAttributes == null) secondaryAttributes = new MultiProperty();
		secondaryAttributes.getList().add(sp);
	}
	public void addSecondaryAttribute(String key, String value) {
		if(key == null) return;
		if(value == null) return;
		SingleProperty sp = new SingleProperty();
		sp.setKey(key);
		sp.setValue(value);
		if(secondaryAttributes == null) secondaryAttributes = new MultiProperty();
		secondaryAttributes.getList().add(sp);
	}
	
	public void addParameter(SingleProperty sp) {
		if(sp == null) return;
		if(parameters == null) parameters = new MultiProperty();
		parameters.getList().add(sp);
	}
	public void addParameter(String key, String value) {
		if(key == null) return;
		if(value == null) return;
		SingleProperty sp = new SingleProperty();
		sp.setKey(key);
		sp.setValue(value);
		if(parameters == null) parameters = new MultiProperty();
		parameters.getList().add(sp);
	}
	
	public Attr getFixed() { return this.fixed == null? new Attr() : this.fixed; }
    public void setFixed(Attr s) { this.fixed = s == null? new Attr() : s; }
    public Attr getFlexi() { return this.flexi == null? new Attr() : this.flexi; }
    public void setFlexi(Attr s) { this.flexi = s == null? new Attr() : s; }
	
	public void setTask(String s) { task = s == null? "" : s;}
	public String getTask() { return task == null? "" : task;}
	
	public void setTimestamp(Instant s) { timestamp = s == null ? Instant.ofEpochMilli(0) : s; }
	public Instant getTimestamp() { return timestamp == null ? Instant.ofEpochMilli(0) : timestamp; }
	
}
