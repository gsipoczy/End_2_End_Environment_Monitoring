package sygr3em.model;

public class SingleTask implements Comparable<SingleTask>{
	
	private String nodename = "";
	private String lastnodename = "";
	private String taskname = "";
	private long alerttime = 0L;
	private long maxtime = 0L;
	private long mintime = 0L;
	private long usedtime = 0L;
	private long millistoadd = 0L;
	private boolean repeatable = false;
	
	public void setNodename(String s) { nodename = s == null? "" : s;}
	public String getNodename() { return nodename == null? "" : nodename;}
	public void setLastnodename(String s) { lastnodename = s == null? "" : s;}
	public String getLastnodename() { return lastnodename == null? "" : lastnodename;}
	public void setTaskname(String s) { taskname = s == null? "" : s;}
	public String getTaskname() { return taskname == null? "" : taskname;}
	
	public void setAlerttime(long s) { alerttime = s;}
	public long getAlerttime() { return alerttime;}
	public void setMaxtime(long s) { maxtime = s;}
	public long getMaxtime() { return maxtime;}
	public void setMintime(long s) { mintime = s;}
	public long getMintime() { return mintime;}
	public void setUsedtime(long s) { usedtime = s;}
	public long getUsedtime() { return usedtime;}
	public void setMillistoadd(long s) { millistoadd = s;}
	public long getMillistoadd() { return millistoadd;}
	public void setRepeatable(boolean s) { repeatable = s;}
	public boolean getRepeatable() { return repeatable;}
	
	@Override
	public int compareTo(SingleTask other) {
    	if(other == null) return 0;
    	return nodename.compareTo(other.getNodename());
    }
	
}
