package sygr3em.model;

import java.time.Instant;
import java.util.ArrayList;
import sygr.pots.extensions.*;

public class InboundMessageForEdit {

	private String command = "";
	
	private SingleProperty id = new SingleProperty();
	
	private Attr fixed = new Attr();
	private Attr flexi = new Attr();
	
	
	public void setCommand(String s) { command = s == null? "" : s;}
	public String getCommand() { return command == null? "" : command;}
	
	public void setId(SingleProperty s) { id = s == null? new SingleProperty() : s;}
	public SingleProperty getId() { return id == null? new SingleProperty() : id;}
	
	public void setId(String key, String value) {
		if(key == null || key.equals("")) return;
		if(value == null || value.equals("")) return;
		if(id == null) id = new SingleProperty();
		id.setKey(key);
		id.setValue(value);
	}
	
	public Attr getFixed() { return this.fixed == null? new Attr() : this.fixed; }
    public void setFixed(Attr s) { this.fixed = s == null? new Attr() : s; }
    public Attr getFlexi() { return this.flexi == null? new Attr() : this.flexi; }
    public void setFlexi(Attr s) { this.flexi = s == null? new Attr() : s; }
	
}
