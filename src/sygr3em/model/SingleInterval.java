package sygr3em.model;

public class SingleInterval implements Comparable<SingleInterval> {
	
	private double from = 0;
	private double to = 0;
	private String location = "";
	private long duration = 0L;
	
	public void setFrom(double s) { from = s;}
	public double getFrom() { return from;}
	public void setTo(double s) { to = s;}
	public double getTo() { return to;}
	public void setLocation(String s) { location = s == null? "" : s;}
	public String getLocation() { return location == null? "" : location;}
	public void setDuration(long s) { duration = s;}
	public long getDuration() { return duration;}
	
	@Override
	public int compareTo(SingleInterval other) {
    	if(other == null) return 0;
    	if(this.from < other.getFrom()) return -1;
    	if(this.from > other.getFrom()) return 1;
    	return 0;
    }
	
}
