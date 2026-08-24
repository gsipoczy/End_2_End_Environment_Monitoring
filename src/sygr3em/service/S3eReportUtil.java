package sygr3em.service;

import java.time.Instant;
import java.util.ArrayList;

import sygr.pots.extensions.Attr;
import sygr.pots.extensions.ExtConstants;
import sygr.pots.extensions.NodeNode;
import sygr.pots.extensions.PluginUtilInterface;
import sygr.pots.extensions.Pot;
import sygr.pots.extensions.ReportingListItem;
import sygr3em.model.RuntimeParameters;
import sygr3em.model.SingleCharacteristic;

public class S3eReportUtil {

	public static String getPotStatus(Pot pot, RuntimeParameters rparams, PluginUtilInterface util) {
		if(pot == null || pot.getFlexi() == null) return "";
		return util.getNodeValue(pot.getFlexi(), rparams.gentextStatus);
	}
	public static String getPotStatusIcon(String status, RuntimeParameters rparams) {		
		if(status == null || status.equals("")) return ExtConstants.reportGREYLAMP;
		if(status.equals(rparams.gentextStatusOk)) return ExtConstants.reportGREENLAMP;
		if(status.equals(rparams.gentextStatusWarning)) return ExtConstants.reportYELLOWLAMP;
		if(status.equals(rparams.gentextStatusError)) return ExtConstants.reportREDLAMP;
		return ExtConstants.reportGREYLAMP;
	}
	public static String getPotStatusHtmlIcon(String status, RuntimeParameters rparams) {		
		if(status == null || status.equals("")) return S3eConstants.lampGREY;
		if(status.equals(rparams.gentextStatusOk)) return S3eConstants.lampGREEN;
		if(status.equals(rparams.gentextStatusWarning)) return S3eConstants.lampYELLOW;
		if(status.equals(rparams.gentextStatusError)) return S3eConstants.lampRED;
		return S3eConstants.lampGREY;
	}
	public static String getLastMessageTime(Pot pot, RuntimeParameters rparams, PluginUtilInterface util) {
		if(pot == null || pot.getFlexi() == null) return "";
		String err = util.getNodeValue(pot.getFlexi(), rparams.gentextLastError + "." + rparams.gentextMessageTime);
		if(err == null || err.equals("")) {
			return util.getNodeValue(pot.getFlexi(), rparams.gentextLastMessage + "." + rparams.gentextMessageTime);
		}
		else return err;
	}
	public static String getLastMessageText(Pot pot, RuntimeParameters rparams, PluginUtilInterface util) {
		if(pot == null || pot.getFlexi() == null) return "";
		String err = util.getNodeValue(pot.getFlexi(), rparams.gentextLastError + "." + rparams.gentextMessageText);
		if(err == null || err.equals("")) {
			return util.getNodeValue(pot.getFlexi(), rparams.gentextLastMessage + "." + rparams.gentextMessageText);
		}
		else return err;
	}
	
	public static ArrayList<ReportingListItem> getPotDetails(Pot pot, RuntimeParameters rparams, 
			PluginUtilInterface util, int counter,
			String c20, String c21, String c22) {
		ArrayList<ReportingListItem> ret = new ArrayList<>();
		
		int lastcounter = 0;
		
		// Task line
		lastcounter = addTaskLine(ret, pot, rparams, util, counter, c20, c21, c22);
		
		// Characteristics
		lastcounter = addCharacteristics(ret, pot, rparams, util, lastcounter, c20, c21, c22);
		
		return ret;
	}
	
	private static int addTaskLine(ArrayList<ReportingListItem> rlis, 
			Pot pot, RuntimeParameters rparams, PluginUtilInterface util, int counter,
			String c20, String c21, String c22) {
		int ret = counter++;
		
		ReportingListItem rli = new ReportingListItem();
		
		// Sort
		rli.setC20(c20);
		rli.setC21(c21);
		rli.setC22(c22);
		rli.setC23("C" + ret);
		
		// Data
		rli.setC02("Current task:");
		rli.setC03(util.getNodeValue(pot.getFixed(), rparams.gentextFixedCurrentTask));
		if(rli.getC03().equals("")) return counter;
		rli.setC04("since:");
		String timestring = util.getNodeValue(pot.getFixed(), rparams.gentextFixedTaskStarted);
		Instant inst = S3eUtil.convertNumericStringToInstant(timestring);
		rli.setC05(S3eUtil.convertInstantToTimestampString(inst, util));
		rlis.add(rli);
		return ret;
	}
	
	private static int addCharacteristics(ArrayList<ReportingListItem> rlis, 
			Pot pot, RuntimeParameters rparams, PluginUtilInterface util, int counter,
			String c20, String c21, String c22) {
		int ret = counter++;
		
		// We need the list of characteristics
		ArrayList<NodeNode> nns = util.nodeNodeList(pot.getFixed(), "", true);
		if(nns == null || nns.size() == 0) return ret;
		
		// Process only the ones that are defined as characteristic
		for(NodeNode nn: nns) {
			boolean itsachar = false;
			for(SingleCharacteristic sc: rparams.characteristics) {
				if(nn.getNode().equals(sc.getName())) itsachar = true;
			}
			if(itsachar) {
				ret = addSingleCharacteristic(rlis, nn.getNode(), nn.getSubnode(), rparams, 
					util, ret++, c20, c21, c22);
			}
			else {
				S3eUtil.logg(S3eConstants.logDebug, "Not a characteristic, skip: " + nn.getNode(), util, rparams);
			}
		}
		
		return ret;
	}
	
	private static int addSingleCharacteristic(ArrayList<ReportingListItem> rlis, String charname,
			Attr charnode, RuntimeParameters rparams, PluginUtilInterface util, int counter,
			String c20, String c21, String c22) {
		int ret = counter++;
		
		// Characteristic main line
		ReportingListItem rli = new ReportingListItem();
		
		// Sort
		rli.setC20(c20);
		rli.setC21(c21);
		rli.setC22(c22);
		rli.setC23("C" + ret++);
		
		// data
		rli.setC02("Characteristic:");
		rli.setC03(charname);
		rli.setC04("Last value:");
		rli.setC05(util.getNodeValue(charnode, rparams.gentextFixedLastValue));
		rli.setC06("Since:");
		Instant inst = S3eUtil.convertNumericStringToInstant(util.getNodeValue(charnode, rparams.gentextFixedLastTime)); 
		rli.setC07(S3eUtil.convertInstantToTimestampString(inst, util));
		rli.setC08("Status:");
		rli.setC09(getPotStatusHtmlIcon(util.getNodeValue(charnode, rparams.gentextFixedStatus), rparams));
		rlis.add(rli);
		
		// Now come the master data one by one for this characteristic
		ArrayList<NodeNode> nns = util.nodeNodeList(charnode, rparams.gentextFixedBudget, true);
		if(nns == null || nns.size() == 0) return ret;
		for(NodeNode nn: nns) {
			ret = addMasterData(rlis, nn.getNode(), nn.getSubnode(), rparams, util, ret,
					c20, c21, c22);
		}
		
		return ret;
	}
	
	private static int addMasterData(ArrayList<ReportingListItem> rlis, String mdname,
			Attr mdnode, RuntimeParameters rparams, PluginUtilInterface util, int counter,
			String c20, String c21, String c22) {
		int ret = counter++;
		
		// Line with the master data name
		ReportingListItem rli = new ReportingListItem();
		
		// Sort
		rli.setC20(c20);
		rli.setC21(c21);
		rli.setC22(c22);
		rli.setC23("C" + ret++);
		// data
		rli.setC03(mdname);
		rlis.add(rli);
		
		// Intervals
		ArrayList<NodeNode> nns = util.nodeNodeList(mdnode, rparams.mdfnRuleTechIntField, true);
		if(!(nns == null) && nns.size() > 0) {
		
			boolean firstline = true;
			for(NodeNode nn: nns) {
				// Create one interval line
				ReportingListItem rlii = new ReportingListItem();
				rlii.setC20(c20);
				rlii.setC21(c21);
				rlii.setC22(c22);
				rlii.setC23("C" + ret++);
				
				if(firstline) {
					rlii.setC04("Intervals:");
					firstline = false;
				}
				
				// Get the main entry
				Attr ivmain = util.getNode(mdnode, util.getNodeValue(nn.getSubnode(), S3eConstants.IVLISTLOC));
				if(ivmain == null) continue;
				// Name
				rlii.setC05(ivmain.getValue());
				if(rlii.getC05().equals("")) {
					rlii.setC05(util.getNodeValue(nn.getSubnode(), S3eConstants.IVLISTLOC));
				}
				
				// From and to value
				rlii.setC06(util.getNodeValue(nn.getSubnode(), S3eConstants.IVLISTFROM));
				rlii.setC07(util.getNodeValue(nn.getSubnode(), S3eConstants.IVLISTTO));
				
				// Times
				rlii.setC08(S3eUtil.convertMillisToDurationString(
						S3eUtil.convertLongStringToMillis(
								util.getNodeValue(ivmain, rparams.mdfnRuleDetailsMaxTime), rparams, util)));
				rlii.setC09(S3eUtil.convertMillisToDurationString(
						S3eUtil.convertLongStringToMillis(
								util.getNodeValue(ivmain, rparams.mdfnRuleDetailsAlertTime), rparams, util)));
				rlii.setC10(S3eUtil.convertMillisToDurationString(
						S3eUtil.convertLongStringToMillis(
								util.getNodeValue(ivmain, rparams.mdfnRuleDetailsUsedTime), rparams, util)));
				
				rlis.add(rlii);
				
				ret = addIvTasks(rlis, util.getNode(ivmain, rparams.mdfnTasksSection), 
						rparams, util, ret, c20, c21, c22);
				
			}
		}
		
		// Crossings
		ArrayList<NodeNode> nns1 = util.nodeNodeList(mdnode, rparams.mdfnSectionCrossings, true);
		if(!(nns == null) && nns.size() > 0) {
			
			boolean firstline = true;
			for(NodeNode nn: nns1) {
				// Create one crossings line
				ReportingListItem rlii = new ReportingListItem();
				rlii.setC20(c20);
				rlii.setC21(c21);
				rlii.setC22(c22);
				rlii.setC23("C" + ret++);
				
				if(firstline) {
					rlii.setC04("Crossings:");
					firstline = false;
				}
				
				rlii.setC05(nn.getSubnode().getValue());
				if(rlii.getC05().equals("")) rlii.setC05(nn.getNode());
				
				// From and to value
				rlii.setC06(util.getNodeValue(nn.getSubnode(), rparams.mdfnCrossingsBottomValue));
				rlii.setC07(util.getNodeValue(nn.getSubnode(), rparams.mdfnCrossingsTopValue));
				
				// Times
				rlii.setC08(util.getNodeValue(nn.getSubnode(), rparams.mdfnCrossingsMaxCross));
				rlii.setC09(util.getNodeValue(nn.getSubnode(), rparams.mdfnCrossingsAlertCross));
				rlii.setC10(util.getNodeValue(nn.getSubnode(), rparams.mdfnCrossingsCurrentCross));
				
				rlis.add(rlii);
				
			}
		}
		
		return ret;
	}
	
	private static int addIvTasks(ArrayList<ReportingListItem> rlis, 
			Attr tasksnode, RuntimeParameters rparams, PluginUtilInterface util, int counter,
			String c20, String c21, String c22) {
		int ret = counter++;
		
		ArrayList<NodeNode> nns = util.nodeNodeList(tasksnode, "", true);
		if(nns == null || nns.size() == 0) {
			return ret;
		}
		
		boolean firstline = true;
		for(NodeNode nn: nns) {
			
			// Need to check whether it's a task or a task group
			String taskname = util.getNodeValue(nn.getSubnode(), rparams.mdfnTasksFieldTaskName);
			if(!(taskname == null) && !(taskname.equals(""))) {
				
				ReportingListItem rli = new ReportingListItem();
				rli.setC20(c20);
				rli.setC21(c21);
				rli.setC22(c22);
				rli.setC23("C" + ret++);
				if(firstline) {
					rli.setC06("Tasks:");
					firstline = false;
				}
				rli.setC07(taskname);
				rli.setC08(S3eUtil.convertMillisToDurationString(
						S3eUtil.convertLongStringToMillis(
								util.getNodeValue(nn.getSubnode(), rparams.mdfnRuleDetailsMaxTime), rparams, util)));
				rli.setC09(S3eUtil.convertMillisToDurationString(
						S3eUtil.convertLongStringToMillis(
								util.getNodeValue(nn.getSubnode(), rparams.mdfnRuleDetailsAlertTime), rparams, util)));
				rli.setC10(S3eUtil.convertMillisToDurationString(
						S3eUtil.convertLongStringToMillis(
								util.getNodeValue(nn.getSubnode(), rparams.mdfnRuleDetailsUsedTime), rparams, util)));
				rlis.add(rli);
				
			}
			else {
				ArrayList<NodeNode> nns1 = util.nodeNodeList(nn.getSubnode(), "", true);
				if(nns1 == null || nns1.size() == 0) continue;
				for(NodeNode nn1: nns1) {
					ReportingListItem rli = new ReportingListItem();
					rli.setC20(c20);
					rli.setC21(c21);
					rli.setC22(c22);
					rli.setC23("C" + ret++);
					if(firstline) {
						rli.setC06("Tasks:");
						firstline = false;
					}
					rli.setC07(util.getNodeValue(nn1.getSubnode(), rparams.mdfnTasksFieldTaskName));
					rli.setC08(S3eUtil.convertMillisToDurationString(
							S3eUtil.convertLongStringToMillis(
									util.getNodeValue(nn1.getSubnode(), rparams.mdfnRuleDetailsMaxTime), rparams, util)));
					rli.setC09(S3eUtil.convertMillisToDurationString(
							S3eUtil.convertLongStringToMillis(
									util.getNodeValue(nn1.getSubnode(), rparams.mdfnRuleDetailsAlertTime), rparams, util)));
					rli.setC10(S3eUtil.convertMillisToDurationString(
							S3eUtil.convertLongStringToMillis(
									util.getNodeValue(nn1.getSubnode(), rparams.mdfnRuleDetailsUsedTime), rparams, util)));
					rlis.add(rli);
				}
			}
			
		}
		
		return ret;
	}
}
