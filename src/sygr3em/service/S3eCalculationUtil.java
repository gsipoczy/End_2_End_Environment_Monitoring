package sygr3em.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import sygr.pots.extensions.Attr;
import sygr.pots.extensions.Ball;
import sygr.pots.extensions.ExtConstants;
import sygr.pots.extensions.NodeNode;
import sygr.pots.extensions.NodeValue;
import sygr.pots.extensions.PluginData;
import sygr.pots.extensions.PluginInterface;
import sygr.pots.extensions.PluginUtilInterface;
import sygr.pots.extensions.Pot;
import sygr.pots.extensions.PotType;
import sygr3em.model.MeasurementCalcResult;
import sygr3em.model.MultiTelemetry;
import sygr3em.model.RuntimeParameters;
import sygr3em.model.SingleCharacteristic;
import sygr3em.model.SingleConversion;
import sygr3em.model.SingleInterval;
import sygr3em.model.SingleTask;
import sygr3em.model.SingleTelemetry;
import sygr3em.model.TaskSteal;

public class S3eCalculationUtil {

	// Process telemetry message
	public static void processTelemetry(PluginData data, PluginUtilInterface util,
			RuntimeParameters rparams, HashMap<String, ArrayList<PotType>> pottypes) {
		
		// First of all get the measurements from the ball
		if(data.ball.getAttr() == null) {
			S3eUtil.logg(S3eConstants.logDebug, "data.ball has no Attr, stop.", util, rparams);
			return;
		}
		Attr msattr = util.getNode(data.ball.getAttr(), S3eConstants.MEASUREMENTS);
		if(msattr == null) {
			S3eUtil.logg(S3eConstants.logDebug, "measurements node is not readable, stop.", util, rparams);
			return;
		}
		ArrayList<MultiTelemetry> mts = S3eUtil.copyAttrToMeasurements(msattr, util);
		if(mts == null || mts.size() == 0) {
			S3eUtil.logg(S3eConstants.logDebug, "no measurements, stop.", util, rparams);
			return;
		}
		
		// Theoretically it is sorted by timestamp, but to be sure do again.
		try {
			Collections.sort(mts);
		} catch(Exception e) {}
		
		// Now, work in order of the timestamps
		for(MultiTelemetry mt: mts) {
			if(mt.getList() == null || mt.getList().size() == 0) continue;
			// Process the measurements one by one
			for(SingleTelemetry st: mt.getList()) {
				
				// Let's check that it's a valid characteristic
				SingleCharacteristic sc = null;
				for(SingleCharacteristic dsc: rparams.characteristics) {
					if(dsc.getName().equals(st.getC12c())) sc = dsc;
				}
				if(sc == null) {
					S3eUtil.logg(S3eConstants.logDebug, "Telemetry characteristic unknown: " 
							+ st.getC12c(), util, rparams);
					S3eUtil.sendAlert(ExtConstants.alertseverityWARNING, 
							data.pot.getType(), 
							data.pot.getMatchkey0(), 
							data.pot.getMatchval0(), 
							"Unknown characteristic arrived", 
							"Characteristic " + st.getC12c() 
								+ "is not defined.", rparams, util);
				}
				else {
					
					boolean finetogo = true;
					
					// Check the UOM
					if(st.getUom().equals("")) st.setUom(sc.getUom());
					else {
						if(!(st.getUom().equals(sc.getUom()))) {
							// Need to convert
							boolean found = false;
							for(SingleConversion sconv: rparams.conversions) {
								if(st.getC12c().equals(sconv.getC12c()) 
										&& st.getUom().equals(sconv.getUom())) {
									found = true;
									double dbl = S3eUtil.convertUom(st.getValue(), sconv.getFormula(), rparams, util);
									if(Double.isNaN(dbl)) {
										finetogo = false;
										S3eUtil.logg(S3eConstants.logDebug, "UOM convestion formula " 
												+ sconv.getFormula() + " is wrong.", util, rparams);
										S3eUtil.sendAlert(ExtConstants.alertseverityWARNING, 
												data.pot.getType(), 
												data.pot.getMatchkey0(), 
												data.pot.getMatchval0(), 
												"UOM conversion formula error", 
												"UOM conversion formula " + sconv.getFormula() 
													+ " is wrong.", rparams, util);
									}
									else {
										S3eUtil.logg(S3eConstants.logDebug, "Measurement converted from " 
												+ String.valueOf(st.getValue()) + st.getUom() + " to "
												+ String.valueOf(dbl) + sc.getUom(), util, rparams);
										st.setValue(dbl);
									}
								}
							}
							if(!found) {
								finetogo = false;
								S3eUtil.logg(S3eConstants.logDebug, "No UOM convestion for " 
										+ st.getC12c() + " " + st.getUom(), util, rparams);
								S3eUtil.sendAlert(ExtConstants.alertseverityWARNING, 
										data.pot.getType(), 
										data.pot.getMatchkey0(), 
										data.pot.getMatchval0(), 
										"UOM conversion missing", 
										"No UOM conversion is defined for " + st.getC12c() 
											+ " " + st.getUom() + " -> " + sc.getUom(), rparams, util);
							}
						}
					}
					
					if(!finetogo) continue;
					
					// OK, we can process it
					MeasurementCalcResult mcr = handleOneMeasurement(data.pot, mt.getTimestamp(), st, util,
							rparams, pottypes, sc);
					// If something went wrong, handle it.
					if(!(mcr == null) && !(mcr.getStatus().equals(rparams.gentextStatusOk))) {
						handleMeasurementResult(data.pot, data.ball, st, mcr, util,
								rparams, pottypes);
					}
					
				}
				
			}
		}
		
		
	}
	
	private static MeasurementCalcResult handleOneMeasurement(Pot pot, Instant timestamp, SingleTelemetry st, PluginUtilInterface util,
			RuntimeParameters rparams, HashMap<String, ArrayList<PotType>> pottypes,
			SingleCharacteristic sc) {
		MeasurementCalcResult ret = new MeasurementCalcResult(rparams.gentextStatusOk);
		
		// We need the current task
		String currenttask = util.getNodeValue(pot.getFixed(), rparams.gentextFixedCurrentTask);
		String taskstarted = util.getNodeValue(pot.getFixed(), rparams.gentextFixedTaskStarted);
		
		// Get the Attr according to the characteristic.
		// If does not exist, no problem, not necessarily everything measures everything.
		Attr charattr = util.getNode(pot.getFixed(), st.getC12c());
		
		// It should have a structure (if exists) like
		//
		// budget
		// 		... different budgets for the characteristic ...
		// last_time
		// last_value
		// status
		// Check with the status, that must have value
		String currentstatus = util.getNodeValue(charattr, rparams.gentextStatus);
		if(currentstatus == null || currentstatus.equals("")) {
			S3eUtil.logg(S3eConstants.logDebug, "Unit " + pot.getMatchkey0() + " = " + pot.getMatchval0() 
					+ " does not have characteristic " + st.getC12c() + ", skipping.", util, rparams);
			return ret;
		}
		
		// We need the Attr with the list of budgets
		Attr budgets = util.getNode(charattr, rparams.gentextFixedBudget);
		
		// Check whether we have last time and last value
		String lasttime = util.getNodeValue(charattr, rparams.gentextFixedLastTime);
		// If it's empty, that means this is the first time a measurement arrived.
		boolean first_telemetry = false;
		if(lasttime == null || lasttime.equals("")) {
			first_telemetry = true;
		}
		Instant lasttimestamp = Instant.ofEpochMilli(0);
		if(!first_telemetry) {
			S3eUtil.logg(S3eConstants.logDebug, "Not first telemetry, get last", util, rparams);
			lasttimestamp = S3eUtil.convertNumericStringToInstant(lasttime);
			S3eUtil.logg(S3eConstants.logDebug, "Last is " + lasttimestamp.toString(), util, rparams);
			S3eUtil.logg(S3eConstants.logDebug, "New is " + timestamp.toString(), util, rparams);
			// The new timestamp must be later than the old!
			if(!(timestamp.isAfter(lasttimestamp))) {
				S3eUtil.logg(S3eConstants.logError, "New telemetry time stamp " 
						+ S3eUtil.convertInstantToTimestampString(timestamp, util) 
						+ " is earlier than the last " 
						+  S3eUtil.convertInstantToTimestampString(lasttimestamp, util)
						+ " for " + pot.getMatchkey0() + " = " + pot.getMatchval0()
						, util, rparams);
				return ret;
			}
			// Also check the minimum frequency
			if(sc.getMinfrequency() > 0L) {
				long elapsedtime = timestamp.toEpochMilli() - lasttimestamp.toEpochMilli();
				if(elapsedtime < sc.getMinfrequency()) return ret;
			}
		}
		
		// check that the value is covered in every budget.
		boolean wrong_value = false;
		ret = isValueCovered(budgets, st.getValue(), rparams, util);
		// If the value is total wrong, we don't start to work with it at all.
		if(ret.getStatus().equals(rparams.gentextStatusError)) {
			wrong_value = true;
			S3eUtil.logg(S3eConstants.logDebug, "Telemetry value " 
					+ st.getValue() + st.getUom() + " for " 
					+ st.getC12c() + " is not covered by interval!" , util, rparams);
			setStatus(pot, charattr, ret, util, rparams);
		}
		
		ArrayList<NodeNode> budgetlist = util.nodeNodeList(budgets, "", false);
		if(budgetlist == null || budgetlist.size() == 0) return ret;
		
		if(first_telemetry) {
			// We set it to the numeric value of the timestamp
			util.setNodeValue(charattr, S3eUtil.convertInstantToNumericString(timestamp), 
					rparams.gentextFixedLastTime);
			// We also set the last value
			util.setNodeValue(charattr, String.valueOf(st.getValue()),
					rparams.gentextFixedLastValue);
			// And already at the 1st telemetry handle the OR-rules
			for(NodeNode budget: budgetlist) {
				S3eUtil.logg(S3eConstants.logDebug, "Processing master data " + budget.getNode(), 
						util, rparams);
				MeasurementCalcResult mcr = handleOneBudget(budget.getSubnode(),
						currenttask, taskstarted,
						lasttimestamp,
						timestamp, 
						0D,
						st.getValue(), 
						util, rparams, pottypes, true, budget.getNode());
				if(mcr.getStatus().equals(rparams.gentextStatusError)) {
					ret = mcr;
					ret.setBudgetname(budget.getNode());
				}
				if(mcr.getStatus().equals(rparams.gentextStatusWarning) &&
						!(ret.getStatus().equals(rparams.gentextStatusError))) {
					ret = mcr;
					ret.setBudgetname(budget.getNode());
				}
			}
			return ret;
		}
		
		// Continue only if the value is fine
		if(wrong_value) return ret;
		
		double lastvalue = S3eUtil.convertStringToNumeric(util.getNodeValue(charattr, rparams.gentextFixedLastValue), util);
		
		
		
		// OK, this is not the first measurement, so we process all budgets.
		// We collect the error and warning rets separate.
		ArrayList<MeasurementCalcResult> errors = new ArrayList<>();
		ArrayList<MeasurementCalcResult> warnings = new ArrayList<>();
		
		for(NodeNode budget: budgetlist) {
			S3eUtil.logg(S3eConstants.logDebug, "Processing master data " + budget.getNode(), 
					util, rparams);
			MeasurementCalcResult mcr = handleOneBudget(budget.getSubnode(),
					currenttask, taskstarted,
					lasttimestamp,
					timestamp, 
					lastvalue,
					st.getValue(), 
					util, rparams, pottypes, false, budget.getNode());
			if(mcr.getStatus().equals(rparams.gentextStatusError)) {
				mcr.setBudgetname(budget.getNode());
				errors.add(mcr);
			}
			if(mcr.getStatus().equals(rparams.gentextStatusWarning)) {
				mcr.setBudgetname(budget.getNode());
				warnings.add(mcr);
			}
		}
		
		// Let's see whether we have error or warning
		if(warnings.size() > 0) {
			try {
				ret = warnings.get(0);
			} catch(Exception e) {}
		}
		if(errors.size() > 0) {
			try {
				ret = errors.get(0);
			} catch(Exception e) {}
		}
		
		util.setNodeValue(charattr, S3eUtil.convertInstantToNumericString(timestamp), 
				rparams.gentextFixedLastTime);
		// We also set the last value
		util.setNodeValue(charattr, String.valueOf(st.getValue()),
				rparams.gentextFixedLastValue);
		setStatus(pot, charattr, ret, util, rparams);
		return ret;
	}
	
	private static MeasurementCalcResult handleOneBudget(Attr budget,
			String currenttask,
			String taskstarted,
			Instant lasttimestamp,
			Instant currenttimestamp, 
			double lastvalue,
			double currentvalue,
			PluginUtilInterface util,
			RuntimeParameters rparams, HashMap<String, ArrayList<PotType>> pottypes,
			boolean orrulesonly,
			String budgetname) {
		MeasurementCalcResult ret = new MeasurementCalcResult(rparams.gentextStatusOk);
		
		// We need to find, which interval the old and new measurements belong to
		Attr techint = util.getNode(budget, rparams.mdfnRuleTechIntField);
		if(techint == null) {
			S3eUtil.logg(S3eConstants.logDebug, "Master data has no "
					+ rparams.mdfnRuleTechIntField + " node. ", util, rparams);
			return ret;
		}
		
		SingleInterval oldint = findInterval(techint, lastvalue, rparams, util);
		if(oldint == null || oldint.getLocation().equals("")) {
			S3eUtil.logg(S3eConstants.logDebug, "No interval found for old value "
					+ lastvalue, util, rparams);
			return ret;
		}
		SingleInterval newint = findInterval(techint, currentvalue, rparams, util);
		if(newint == null || newint.getLocation().equals("")) {
			S3eUtil.logg(S3eConstants.logDebug, "No interval found for new value "
					+ currentvalue, util, rparams);
			return ret;
		}
		
		if(!orrulesonly) {
			
			// We need to remember the last location
			util.setNodeValue(budget, newint.getLocation(), rparams.gentextFixedLastLocation);
		
			long duration = currenttimestamp.toEpochMilli() - lasttimestamp.toEpochMilli();
			if(duration <= 0) {
				S3eUtil.logg(S3eConstants.logDebug, "Duration is "
						+ duration, util, rparams);
				return ret;
			}
		
			// Easy case: old and new are in the same interval
			if(oldint.getLocation().equals(newint.getLocation())) {
				ret = handleOneInterval(budget, newint, currenttask, taskstarted, 
						currenttimestamp, duration, lastvalue, currentvalue,
						util, rparams, pottypes);
				// Need to process the crossings
				ArrayList<NodeNode> crns = util.nodeNodeList(budget, rparams.mdfnSectionCrossings, false);
				if(!(crns == null) && crns.size() > 0) {
					for(NodeNode crn: crns) {
						MeasurementCalcResult mcr2 = handleOneCrossing(crn.getSubnode(),
								currenttask, taskstarted, lasttimestamp, currenttimestamp,
								lastvalue, currentvalue, util, rparams, pottypes,
								budgetname + "." + crn.getNode());
						if(mcr2.getStatus().equals(rparams.gentextStatusError)) ret = mcr2;
						if(mcr2.getStatus().equals(rparams.gentextStatusWarning) &&
								!(ret.getStatus().equals(rparams.gentextStatusError))) ret = mcr2;
					}
				}
				else {
					S3eUtil.logg(S3eConstants.logError, "No crossings found.", util, rparams);
				}
				handleOrRules(budget, techint, newint, "", util, rparams, pottypes, 0);
				return ret;
			}
		
			// If different intervals, we need to cover all
			ArrayList<SingleInterval> ivs = getDurations(budget, oldint, newint,
					lastvalue, currentvalue, duration,
					util, rparams, pottypes);
			if(ivs == null || ivs.size() == 0) {
				S3eUtil.logg(S3eConstants.logError, "No intervals found.", util, rparams);
				return ret;
			}
		
			for(SingleInterval iv: ivs) {
				S3eUtil.logg(S3eConstants.logDebug, "Processing interval "
						+ iv.getFrom() + " - " + iv.getTo() 
						+ ", duration " + iv.getDuration() + " millisec.", util, rparams);
				MeasurementCalcResult mcr = handleOneInterval(budget, iv, currenttask, taskstarted,
						currenttimestamp, iv.getDuration(), lastvalue, currentvalue,
						util, rparams, pottypes);
				if(mcr.getStatus().equals(rparams.gentextStatusError)) ret = mcr;
				if(mcr.getStatus().equals(rparams.gentextStatusWarning) &&
						!(ret.getStatus().equals(rparams.gentextStatusError))) ret = mcr;
			}
			
			// Need to process the crossings
			ArrayList<NodeNode> crns = util.nodeNodeList(budget, rparams.mdfnSectionCrossings, false);
			if(!(crns == null) && crns.size() > 0) {
				for(NodeNode crn: crns) {
					MeasurementCalcResult mcr2 = handleOneCrossing(crn.getSubnode(),
							currenttask, taskstarted, lasttimestamp, currenttimestamp,
							lastvalue, currentvalue, util, rparams, pottypes,
							budgetname + "." + crn.getNode());
					if(mcr2.getStatus().equals(rparams.gentextStatusError)) ret = mcr2;
					if(mcr2.getStatus().equals(rparams.gentextStatusWarning) &&
							!(ret.getStatus().equals(rparams.gentextStatusError))) ret = mcr2;
				}
			}
			else {
				S3eUtil.logg(S3eConstants.logError, "No crossings found.", util, rparams);
			}
			
		}
		
		// Handle OR-rules
		handleOrRules(budget, techint, newint, "", util, rparams, pottypes, 0);
		return ret;
	}
	
	private static MeasurementCalcResult handleOneCrossing(Attr crattr,
			String currenttask,
			String taskstarted,
			Instant lasttimestamp,
			Instant currenttimestamp, 
			double lastvalue,
			double currentvalue,
			PluginUtilInterface util,
			RuntimeParameters rparams, HashMap<String, ArrayList<PotType>> pottypes,
			String location) {
		MeasurementCalcResult ret = new MeasurementCalcResult(rparams.gentextStatusOk);
		
		// Get all values from the crossing
		int alert_cross = S3eUtil.convertStringToInteger(util.getNodeValue(crattr, 
				rparams.mdfnCrossingsAlertCross), util);
		int max_cross = S3eUtil.convertStringToInteger(util.getNodeValue(crattr, 
				rparams.mdfnCrossingsMaxCross), util);
		int old_current_cross = S3eUtil.convertStringToInteger(util.getNodeValue(crattr, 
				rparams.mdfnCrossingsCurrentCross), util);
		double bottom_value = S3eUtil.convertStringToNumeric(util.getNodeValue(crattr,
				rparams.mdfnCrossingsBottomValue), util);
		double top_value = S3eUtil.convertStringToNumeric(util.getNodeValue(crattr,
				rparams.mdfnCrossingsTopValue), util);
		long min_time = S3eUtil.convertLongStringToMillis(util.getNodeValue(crattr,
				rparams.mdfnRuleDetailsMinTime), rparams, util);
		if(min_time < 0L) min_time = 0L;
		Instant last_time = S3eUtil.convertNumericStringToInstant(util.getNodeValue(crattr,
				rparams.gentextFixedLastTime));
		String last_time_string = util.getNodeValue(crattr,	rparams.gentextFixedLastTime);
		boolean crhappened = false;
		
		// Log
		S3eUtil.logg(S3eConstants.logDebug, "Crossing: " + crattr.getValue(), util, rparams);
		S3eUtil.logg(S3eConstants.logDebug, "Alert cross: " + alert_cross, util, rparams);
		S3eUtil.logg(S3eConstants.logDebug, "Max cross: " + max_cross, util, rparams);
		S3eUtil.logg(S3eConstants.logDebug, "Old current cross: " + old_current_cross, util, rparams);
		S3eUtil.logg(S3eConstants.logDebug, "Bottom value: " + bottom_value, util, rparams);
		S3eUtil.logg(S3eConstants.logDebug, "Top value: " + top_value, util, rparams);
		S3eUtil.logg(S3eConstants.logDebug, "Min time: " + min_time, util, rparams);
		S3eUtil.logg(S3eConstants.logDebug, "Last value: " + lastvalue, util, rparams);
		S3eUtil.logg(S3eConstants.logDebug, "Current value: " + currentvalue, util, rparams);
		S3eUtil.logg(S3eConstants.logDebug, "Last cross time: " + last_time, util, rparams);
		S3eUtil.logg(S3eConstants.logDebug, "Last cross time String: " + last_time_string, util, rparams);
		S3eUtil.logg(S3eConstants.logDebug, "Current time: " + currenttimestamp, util, rparams);
		
		// Check whether a crossing happened
		if(lastvalue < bottom_value && currentvalue > top_value) crhappened = true;
		if(lastvalue > top_value && currentvalue < bottom_value) crhappened = true;
		S3eUtil.logg(S3eConstants.logDebug, "Crossing happened: " + crhappened, util, rparams);
		
		long timesincelast = currenttimestamp.toEpochMilli() - last_time.toEpochMilli();
		S3eUtil.logg(S3eConstants.logDebug, "Elapsed time since last crossing: " + timesincelast, util, rparams);
		// If crossing happened
		if(crhappened) {
			
			// If very short time elapsed since the last crossing, it does not count
			
			if(timesincelast < min_time) {
				S3eUtil.logg(S3eConstants.logDebug, "Too short double crossing, forget it.", util, rparams);
				util.setNodeValue(crattr, "", rparams.gentextFixedLastTime);
				return ret;
			}
			else { // Long time elapsed
				S3eUtil.logg(S3eConstants.logDebug, "Elapsed time is long enough, handle crossing.", util, rparams);
				// There are 2 possibilities:
				// If there is no pending crossing, we just set the last time, so
				// we have a pending crossing
				if(last_time_string.equals("")) {
					S3eUtil.logg(S3eConstants.logDebug, "No pending crossing, just set this pending.", util, rparams);
					util.setNodeValue(crattr, S3eUtil.convertInstantToNumericString(currenttimestamp), rparams.gentextFixedLastTime);
					return ret;
				}
				
				// But if there was a previous pending crossing, we add it now,
				// and change the last time, from now this crossing is pending
				S3eUtil.logg(S3eConstants.logDebug, "There was pending crossing, add it..", util, rparams);
				int new_current_cross = old_current_cross + 1;
				util.setNodeValue(crattr, String.valueOf(new_current_cross), rparams.mdfnCrossingsCurrentCross);
				util.setNodeValue(crattr, S3eUtil.convertInstantToNumericString(currenttimestamp), rparams.gentextFixedLastTime);
				
				// Check if we are in the warning zone
				if(old_current_cross <= alert_cross && new_current_cross > alert_cross) {
					ret.setLocation(location);
					ret.setMessage("Alert crossing " 
							+ alert_cross 
							+ " exceeded, new crossing is " 
							+ new_current_cross);
					ret.setProblemtype(S3eConstants.problemtypeALERTCROSSEXC);
					ret.setSeverity(ExtConstants.alertseverityWARNING);
					ret.setStatus(rparams.gentextStatusWarning);
				}
				
				// Check if we are in the error zone
				if(old_current_cross <= max_cross && new_current_cross > max_cross) {
					ret.setLocation(location);
					ret.setMessage("Max crossing " 
							+ max_cross 
							+ " exceeded, new crossing is " 
							+ new_current_cross);
					ret.setProblemtype(S3eConstants.problemtypeMAXCROSSEXC);
					ret.setSeverity(ExtConstants.alertseverityERROR);
					ret.setStatus(rparams.gentextStatusError);
				}
				
				return ret;
				
			}
			
			
		}
		else { // no crossing happened
			if(timesincelast < min_time) {
				S3eUtil.logg(S3eConstants.logDebug, "Too short time, do nothing.", util, rparams);
				return ret;
			}
			else {
				// Long time, use the pending crossing
				if(!(last_time_string.equals(""))) {
					S3eUtil.logg(S3eConstants.logDebug, "There was pending crossing, add it..", util, rparams);
					int new_current_cross = old_current_cross + 1;
					util.setNodeValue(crattr, String.valueOf(new_current_cross), rparams.mdfnCrossingsCurrentCross);
					util.setNodeValue(crattr, "", rparams.gentextFixedLastTime);
					
					// Check if we are in the warning zone
					if(old_current_cross <= alert_cross && new_current_cross > alert_cross) {
						ret.setLocation(location);
						ret.setMessage("Alert crossing " 
								+ alert_cross 
								+ " exceeded, new crossing is " 
								+ new_current_cross);
						ret.setProblemtype(S3eConstants.problemtypeALERTCROSSEXC);
						ret.setSeverity(ExtConstants.alertseverityWARNING);
						ret.setStatus(rparams.gentextStatusWarning);
					}
					
					// Check if we are in the error zone
					if(old_current_cross <= max_cross && new_current_cross > max_cross) {
						ret.setLocation(location);
						ret.setMessage("Max crossing " 
								+ max_cross 
								+ " exceeded, new crossing is " 
								+ new_current_cross);
						ret.setProblemtype(S3eConstants.problemtypeMAXCROSSEXC);
						ret.setSeverity(ExtConstants.alertseverityERROR);
						ret.setStatus(rparams.gentextStatusError);
					}
				}
			}
		}
		
		return ret;
	}
	
	private static void handleOrRules(Attr budget, Attr techint, SingleInterval newint, String upnode,
			PluginUtilInterface util,
			RuntimeParameters rparams, HashMap<String, ArrayList<PotType>> pottypes, int level) {
		
		S3eUtil.logg(S3eConstants.logDebug, "OR processing start at " + upnode, util, rparams);
		
		int newlevel = level + 1;
		if(newlevel > 1000) return;
		
		// We need the first part of the newint.location
		String currentnode = "";
		String undernode = "";
		String subnode = "";
		try {
			String[] pts = newint.getLocation().split(S3eConstants.nodeSeparatorRegex, 3);
			if(pts.length > 0) currentnode = pts[0];
			if(pts.length > 1) undernode = pts[1];
			if(pts.length > 2) subnode = pts[2];
		} catch (Exception e) {
			return;
		}
		if(currentnode.equals("")) return;
		S3eUtil.logg(S3eConstants.logDebug, "currentnode = " + currentnode, util, rparams);
		S3eUtil.logg(S3eConstants.logDebug, "undernode = " + undernode, util, rparams);
		S3eUtil.logg(S3eConstants.logDebug, "subnode = " + subnode, util, rparams);
		S3eUtil.logg(S3eConstants.logDebug, "upnode = " + upnode, util, rparams);
		
		// Check whether it's an Or-rule
		int nodetype = 0;
		if(currentnode.equals(rparams.mdfnSectionIntervals)) nodetype = S3eConstants.mdiAnd;  
		else nodetype = S3eMdUtil.getIntNodeType(currentnode, rparams, util);
		S3eUtil.logg(S3eConstants.logDebug, "Type of the current node is = " + nodetype, util, rparams);
		
		S3eUtil.logg(S3eConstants.logDebug, "Getting the node of = " + currentnode, util, rparams);
		Attr subbudget = util.getNode(budget, currentnode);
		String fullnode = "";
		if(!(upnode.equals(""))) fullnode = upnode + "." + currentnode;
		else fullnode = currentnode;
		S3eUtil.logg(S3eConstants.logDebug, "fullnode = " + fullnode, util, rparams);
		
		// If it's an Or-rule, we need to handle
		if(nodetype == S3eConstants.mdiOr ) {
			S3eUtil.logg(S3eConstants.logDebug, "This is an OR rule, so process.", util, rparams);
		
			// Delete the unwanted nodes
			ArrayList<NodeNode> nns = util.nodeNodeList(subbudget, "", false);
			ArrayList<NodeNode> tns = util.nodeNodeList(techint, "", false);
			for(NodeNode nn: nns) {
				S3eUtil.logg(S3eConstants.logDebug, "checking node = " + nn.getNode(), util, rparams);
				S3eUtil.logg(S3eConstants.logDebug, "if it's not = " + undernode +", we delete.", util, rparams);
				if(!(nn.getNode().equals(undernode))) {
					S3eUtil.logg(S3eConstants.logDebug, "deleting node " + nn.getNode(), util, rparams);
					util.deleteNode(subbudget, nn.getNode());
					String technodepattern = util.escapeRegex(fullnode + "." + nn.getNode() + "*");
					S3eUtil.logg(S3eConstants.logDebug, "tech pattern = " + technodepattern, util, rparams);
					for(NodeNode tn: tns) {
						String locvalue = util.getNodeValue(tn.getSubnode(), S3eConstants.IVLISTLOC);
						S3eUtil.logg(S3eConstants.logDebug, "tech node = " + locvalue, util, rparams);
						S3eUtil.logg(S3eConstants.logDebug, "high node = " + tn.getNode(), util, rparams);
						if(locvalue.matches(technodepattern)) {
							S3eUtil.logg(S3eConstants.logDebug, "deleting node " + tn.getNode(), util, rparams);
							util.deleteNode(techint, tn.getNode());
						}
					}
				}
				
			}
			
		}
		
		// and just go down recursively
		if(!(undernode.equals(""))) { 
			SingleInterval subint = new SingleInterval();
			subint.setLocation(undernode + "." + subnode);
			handleOrRules(subbudget, techint, subint, fullnode, util, rparams, pottypes, newlevel);
		}
		
	}
	
	private static ArrayList<SingleInterval> getDurations(Attr budget,
			SingleInterval oldint, SingleInterval newint,
			double lastvalue,
			double currentvalue,
			long duration,
			PluginUtilInterface util,
			RuntimeParameters rparams, HashMap<String, ArrayList<PotType>> pottypes) {
		ArrayList<SingleInterval> ret = new ArrayList<>();
		
		// First we need the split type
		String splittype = util.getNodeValue(budget, 
				rparams.mdfnSectionSettings + "." + rparams.mdfnSettingsTimeSplit);
		if(splittype == null || splittype.equals("")) 
			splittype = rparams.mdfnSettingsDefaultTimeSplit;
		
		// Easy cases if assign everything to one
		if(splittype.equals(rparams.mdfnSettingsTimeSplitOld)) {
			oldint.setDuration(duration);
			ret.add(oldint);
			return ret;
		}
		if(splittype.equals(rparams.mdfnSettingsTimeSplitNew)) {
			newint.setDuration(duration);
			ret.add(newint);
			return ret;
		}
		
		// for the half and linear we need to know whether there are intervals between
		SingleInterval fromint = oldint.getFrom() < newint.getFrom() ? oldint : newint;
		SingleInterval toint = oldint.getFrom() < newint.getFrom() ? newint : oldint;
		Attr techint = util.getNode(budget, rparams.mdfnRuleTechIntField);
		ArrayList<SingleInterval> sis = S3eUtil.readIntervalListNode(techint, util);
		boolean started = false;
		boolean finished = false;
		
		for(SingleInterval si: sis) {
			if(finished) continue;
			if(si.getLocation().equals(fromint.getLocation())) {
				started = true;
			}
			if(si.getLocation().equals(toint.getLocation())) {
				finished = true;
			}
			if(started) {
				ret.add(si);
			}
		}
		int numivs = ret.size();
		if(numivs == 0) return ret;
		
		// Now we still need the individual durations.
		// If the rule is "half", it's easy
		if(splittype.equals(rparams.mdfnSettingsTimeSplitHalf)) {
			long partduration = Math.floorDiv(duration, numivs);
			for(SingleInterval si: ret) {
				si.setDuration(partduration);
			}
			return ret;
		}
		
		// Now we have a linear rule.
		// We need to distribute
		boolean goingup = oldint.getFrom() < newint.getFrom() ? true : false;
		double fulldiff = Math.abs(currentvalue - lastvalue);
		if(fulldiff == 0) return ret;
		S3eUtil.logg(S3eConstants.logDebug, "Full value difference = " + fulldiff, util, rparams);
		for(SingleInterval si: ret) {
			double partdiff = 0D;
			if(si.getLocation().equals(oldint.getLocation())) {
				if(goingup) partdiff = Math.abs(si.getTo() - lastvalue);
				else partdiff = Math.abs(lastvalue - si.getFrom());
			}
			else {
				if(si.getLocation().equals(newint.getLocation())) {
					if(goingup) partdiff = Math.abs(currentvalue - si.getFrom());
					else partdiff = Math.abs(si.getTo() - currentvalue);
				}
				else partdiff = Math.abs(si.getTo() - si.getFrom());
			}
			double part = partdiff / fulldiff;
			si.setDuration(( long ) ( duration * part));
		}
		
		return ret;
	}
			
	
	private static MeasurementCalcResult handleOneInterval(Attr budget,
			SingleInterval interval,
			String currenttask,
			String taskstarted,
			Instant timestamp,
			long millistoadd, 
			double lastvalue,
			double currentvalue,
			PluginUtilInterface util,
			RuntimeParameters rparams, HashMap<String, ArrayList<PotType>> pottypes) {
		MeasurementCalcResult ret = new MeasurementCalcResult(rparams.gentextStatusOk);
		
		ArrayList<NodeValue> bnodes = util.nodeValueList(budget, "", false);
		
		S3eUtil.logg(S3eConstants.logDebug, "START handling an interval", util, rparams);
		S3eUtil.logg(S3eConstants.logDebug, "Interval Location: " + interval.getLocation(), util, rparams);
		S3eUtil.logg(S3eConstants.logDebug, "Interval From value: " + interval.getFrom(), util, rparams);
		S3eUtil.logg(S3eConstants.logDebug, "Interval To value: " + interval.getTo(), util, rparams);
		S3eUtil.logg(S3eConstants.logDebug, "Interval Duration: " + interval.getDuration(), util, rparams);
		S3eUtil.logg(S3eConstants.logDebug, "Current task: " + currenttask, util, rparams);
		S3eUtil.logg(S3eConstants.logDebug, "Task started: " + taskstarted, util, rparams);
		S3eUtil.logg(S3eConstants.logDebug, "Duration to add: " + millistoadd, util, rparams);
		S3eUtil.logg(S3eConstants.logDebug, "Last value: " + lastvalue, util, rparams);
		S3eUtil.logg(S3eConstants.logDebug, "Current value: " + currentvalue, util, rparams);
		S3eUtil.logg(S3eConstants.logDebug, "Nodes in the Attr received: ", util, rparams);
		for(NodeValue nv: bnodes) {
			S3eUtil.logg(S3eConstants.logDebug, nv.getNode() + " = " + nv.getValue(), util, rparams);
		}
		
		// Get the interval node we have to process
		Attr ivnode = util.getNode(budget, interval.getLocation());
		ArrayList<NodeValue> ivsubs = util.nodeValueList(ivnode, "", false);
		S3eUtil.logg(S3eConstants.logDebug, "Nodes in the interval node: ", util, rparams);
		for(NodeValue nv: ivsubs) {
			S3eUtil.logg(S3eConstants.logDebug, nv.getNode() + " = " + nv.getValue(), util, rparams);
		}
		
		// Get the numeric values
		long maxtime = S3eUtil.convertLongStringToMillis(util.getNodeValue(ivnode, 
				rparams.mdfnRuleDetailsMaxTime), rparams, util);
		long alerttime = S3eUtil.convertLongStringToMillis(util.getNodeValue(ivnode, 
				rparams.mdfnRuleDetailsAlertTime), rparams, util);
		long oldusedtime = S3eUtil.convertLongStringToMillis(util.getNodeValue(ivnode, 
				rparams.mdfnRuleDetailsUsedTime), rparams, util);
		long newusedtime = oldusedtime + millistoadd;
		
		// Set the new used time
		util.setNodeValue(ivnode, String.valueOf(newusedtime), rparams.mdfnRuleDetailsUsedTime);
		
		// Handle tasks
		Attr tasksnode = util.getNode(ivnode, rparams.mdfnTasksSection);
		if(!(tasksnode == null)) {
			ret = handleTasks(tasksnode, currenttask, taskstarted, timestamp, millistoadd, 
					interval.getLocation(), util, rparams, pottypes);
		}
		
		// Check if we are in the warning zone
		if(oldusedtime <= alerttime && newusedtime > alerttime) {
			ret.setLocation(interval.getLocation());
			ret.setMessage("Alert time " 
					+ S3eUtil.convertMillisToDurationString(alerttime) 
					+ " exceeded, new used time is " 
					+ S3eUtil.convertMillisToDurationString(newusedtime));
			ret.setProblemtype(S3eConstants.problemtypeALERTTIMEEXC);
			ret.setSeverity(ExtConstants.alertseverityWARNING);
			ret.setStatus(rparams.gentextStatusWarning);
		}
		
		// Check if we are in the error zone
		if(oldusedtime <= maxtime && newusedtime > maxtime) {
			ret.setLocation(interval.getLocation());
			ret.setMessage("Maximum time " 
					+ S3eUtil.convertMillisToDurationString(maxtime) 
					+ " exceeded, new used time is " 
					+ S3eUtil.convertMillisToDurationString(newusedtime));
			ret.setProblemtype(S3eConstants.problemtypeMAXTIMEEXC);
			ret.setSeverity(ExtConstants.alertseverityERROR);
			ret.setStatus(rparams.gentextStatusError);
		}
		
		return ret;
	}
	
	private static MeasurementCalcResult handleTasks(Attr tasksnode,
			String currenttask,
			String taskstarted,
			Instant timestamp,
			long millistoadd, 
			String location,
			PluginUtilInterface util,
			RuntimeParameters rparams, HashMap<String, ArrayList<PotType>> pottypes) {
		MeasurementCalcResult ret = new MeasurementCalcResult(rparams.gentextStatusOk);
		
		// The tasksnode is a list of task groups, get them.
		ArrayList<NodeNode> tgs = util.nodeNodeList(tasksnode, "", false);
		// If no, no problem, finish.
		if(tgs == null || tgs.size() == 0) return ret;
		
		S3eUtil.logg(S3eConstants.logDebug, "Nodes in the tasks node: ", util, rparams);
		for(NodeNode nv: tgs) {
			S3eUtil.logg(S3eConstants.logDebug, nv.getNode(), util, rparams);
		}
		
		// Loop on the task groups
		for(NodeNode nn: tgs) {
			MeasurementCalcResult mcr = handleTaskGroup(nn.getSubnode(), nn.getNode(), currenttask, taskstarted, 
					timestamp, millistoadd,
					location, util, rparams, pottypes);
			if(mcr.getSeverity().equals(ExtConstants.alertseverityERROR)) ret = mcr;
			else {
				if(mcr.getSeverity().equals(ExtConstants.alertseverityWARNING) 
						&& !(ret.getSeverity().equals(ExtConstants.alertseverityERROR)))
					ret = mcr;
			}
		}
		
		return ret;
	}
	
	private static MeasurementCalcResult handleTaskGroup(Attr tgnode,
			String nodename,
			String currenttask,
			String taskstarted,
			Instant timestamp,
			long millistoadd, 
			String location,
			PluginUtilInterface util,
			RuntimeParameters rparams, HashMap<String, ArrayList<PotType>> pottypes) {
		MeasurementCalcResult ret = new MeasurementCalcResult(rparams.gentextStatusOk);
		
		// A task group can be itself a single task, or a list of single tasks.
		ArrayList<NodeNode> tgs = util.nodeNodeList(tgnode, "", true);
		
		ArrayList<SingleTask> techtasks = new ArrayList<>();
		
		// We collect the fields into a separate Attr
		Attr single = new Attr();
		boolean tgissingle = false;
		
		S3eUtil.logg(S3eConstants.logDebug, "Nodes in the tasks group: "
				+ nodename, util, rparams);
		for(NodeNode nv: tgs) {
			S3eUtil.logg(S3eConstants.logDebug, nv.getNode(), util, rparams);
			
			// If it's a single task, we handle so
			if(S3eMdUtil.getIntNodeType(nv.getNode(), rparams, util) == S3eConstants.mdiSingleTask) {
				SingleTask st = convertNodeToSingleTask(nodename + "." + nv.getNode(), 
						nv.getNode(), nv.getSubnode(), util, rparams, pottypes);
				if(!(st == null)) techtasks.add(st);
			}
			else {
				util.setNodeValue(single, util.getNodeValue(tgnode, nv.getNode()), nv.getNode());
				tgissingle = true;
			}
			
		}
		
		// If tg is a single task, handle here
		if(tgissingle) {
			SingleTask st = convertNodeToSingleTask(nodename, nodename, single, util, rparams, pottypes);
			if(!(st == null)) techtasks.add(st);
		}
		
		// Sort the techtasks by nodename
		if(techtasks == null || techtasks.size() == 0) return ret;
		try {
			Collections.sort(techtasks);
		} catch(Exception e) {}
		
		// Theoretically we have now a technical list of the tasks.
		// We need to find out, how much time to add to which
		ret = handleTechTask(techtasks, currenttask, taskstarted, timestamp, millistoadd, location,
				util, rparams, pottypes);
		
		// Log what we have
		S3eUtil.logg(S3eConstants.logDebug, "Task - time distribution: ", util, rparams);
		for(SingleTask st: techtasks) {
			S3eUtil.logg(S3eConstants.logDebug, 
					st.getNodename() + " - "
					+ st.getTaskname() 
					+ " alt = " + st.getAlerttime()
					+ " min = " + st.getMintime()
					+ " max = " + st.getMaxtime()
					+ " usd = " + st.getUsedtime()
					+ " ADD = " + st.getMillistoadd(),
					util, rparams);
		}
		
		if(techtasks == null || techtasks.size() == 0) return ret;
		
		// And now we need to write back the results
		if(tgissingle) {
			try {
				SingleTask st = techtasks.get(0);
				if(!(st == null)) {
					copyTaskResultToTaskNode(tgnode, st, util, rparams, pottypes);
				}
			} catch(Exception e) {}
		}
		else {
			S3eUtil.logg(S3eConstants.logDebug, "handleTaskGroup tgnode content:", util, rparams);
			ArrayList<NodeValue> nvs = util.nodeValueList(tgnode, "", false);
			for(NodeValue nv: nvs) {
				S3eUtil.logg(S3eConstants.logDebug, nv.getNode() + " = " + nv.getValue(), util, rparams);
			}
			for(SingleTask st: techtasks) {
				S3eUtil.logg(S3eConstants.logDebug, "Picking node: " + st.getLastnodename(), util, rparams);
				Attr tnode = util.getNode(tgnode, st.getLastnodename());
				copyTaskResultToTaskNode(tnode, st, util, rparams, pottypes);
			}
		}
		
		return ret;
	}
	
	private static MeasurementCalcResult handleTechTask(ArrayList<SingleTask> techtasks,
			String currenttask,
			String taskstarted,
			Instant timestamp,
			long millistoadd,
			String location,
			PluginUtilInterface util,
			RuntimeParameters rparams, HashMap<String, ArrayList<PotType>> pottypes) {
		MeasurementCalcResult ret = new MeasurementCalcResult(rparams.gentextStatusOk);
		
		S3eUtil.logg(S3eConstants.logDebug, "Start processing tech tasks", util, rparams);
		
		// We need to know when was the task started
		Instant tstart = S3eUtil.convertNumericStringToInstant(taskstarted);
		S3eUtil.logg(S3eConstants.logDebug, "Task " + currenttask 
				+ " started at " + tstart.toString(), util, rparams);
		long startmillis = tstart.toEpochMilli();
		long stampmillis = timestamp.toEpochMilli();
		long tasktime = stampmillis - startmillis;
		if(tasktime < 0L) tasktime = 0L;
		if(millistoadd > tasktime) millistoadd = tasktime;
		
		ArrayList<TaskSteal> tasksteal = new ArrayList<>();
		boolean taskfound = false;
		boolean secondroundneeded = false;
		boolean followup = false;
		long newtime = 0L;
		long maxsteal = 0L;
		long missingtime = 0L;
		SingleTask actualtask = null;
		
		// First round 
		for(SingleTask st: techtasks) {
			
			// Is it our task?
			if(st.getTaskname().equals(currenttask)) {
				S3eUtil.logg(S3eConstants.logDebug, "Task " + currenttask + " found.", util, rparams);
				taskfound = true;
				actualtask = st;
				// Calculate how much would be the new time
				newtime = st.getUsedtime() + millistoadd;
				S3eUtil.logg(S3eConstants.logDebug, "New used time: " + newtime, util, rparams);
				S3eUtil.logg(S3eConstants.logDebug, "Max time: " + st.getMaxtime(), util, rparams);
				S3eUtil.logg(S3eConstants.logDebug, "Alert time: " + st.getAlerttime(), util, rparams);
				// Is it less than the max time, then we are fine
				if(newtime <= st.getMaxtime()) {
					st.setUsedtime(newtime);
					// But maybe alerttime problem?
					if(newtime > st.getAlerttime()) {
						S3eUtil.logg(S3eConstants.logDebug, "Warning: alert time exceeded", util, rparams);
						// We need a warning
						ret.setLocation(location + "." + rparams.mdfnTasksSection
								+ "." + st.getNodename());
						ret.setMessage("Task " + currenttask + " alert time " 
								+ S3eUtil.convertMillisToDurationString(st.getAlerttime()) 
								+ " exceeded, new used time is " 
								+ S3eUtil.convertMillisToDurationString(newtime));
						ret.setProblemtype(S3eConstants.problemtypeTASKTIMEEXC);
						ret.setSeverity(ExtConstants.alertseverityWARNING);
						ret.setStatus(rparams.gentextStatusWarning);
						ret.setTaskname(currenttask);
					}
				}
				else {
					// Collect how much time we can steal from the following tasks
					followup = true;
					missingtime = newtime - st.getMaxtime();
					S3eUtil.logg(S3eConstants.logDebug, "We miss " + missingtime + " milliseconds", util, rparams);
				}
			}
			else { 
				// Not our task, check if we can steal some time
				if(followup) {
					secondroundneeded = true;
					// Only if not repeatable and not used yet
					if(!(st.getRepeatable()) && st.getUsedtime() == 0L && missingtime > 0L) {
						TaskSteal tst = new TaskSteal();
						tst.name = st.getTaskname();
						tst.steal = st.getMaxtime() - st.getMintime();
						if(tst.steal > missingtime) {
							tst.steal = missingtime;
							missingtime = 0;
						}
						else missingtime = missingtime - tst.steal;
						maxsteal = maxsteal + tst.steal;
						S3eUtil.logg(S3eConstants.logDebug, "Stealing " + tst.steal 
								+ " milliseconds from " + tst.name, util, rparams);
						
						tasksteal.add(tst);
						
					}
				}
				
			}
			
		}
		if(!(secondroundneeded)) return ret;
		if(!(taskfound) || actualtask == null) return ret;
		
		// Now for sure we exceeded the max time, let's see how much we can steal
		S3eUtil.logg(S3eConstants.logDebug, "Time from the follow up tasks: " + maxsteal, util, rparams);
		actualtask.setMaxtime(actualtask.getMaxtime() + maxsteal);
		actualtask.setAlerttime(actualtask.getAlerttime() + maxsteal);
		S3eUtil.logg(S3eConstants.logDebug, "Changed the max time to: " 
				+ actualtask.getMaxtime(), util, rparams);
		// After it we must set the used time
		actualtask.setUsedtime(newtime);
		
		// Alert time check
		if(actualtask.getUsedtime() > actualtask.getAlerttime()) {
			S3eUtil.logg(S3eConstants.logDebug, "Warning: alert time exceeded", util, rparams);
			// We need a warning
			ret.setLocation(location + "." + rparams.mdfnTasksSection
					+ "." + actualtask.getNodename());
			ret.setMessage("Task " + currenttask + " alert time " 
					+ S3eUtil.convertMillisToDurationString(actualtask.getAlerttime()) 
					+ " exceeded, new used time is " 
					+ S3eUtil.convertMillisToDurationString(newtime));
			ret.setProblemtype(S3eConstants.problemtypeTASKTIMEEXC);
			ret.setSeverity(ExtConstants.alertseverityWARNING);
			ret.setStatus(rparams.gentextStatusWarning);
			ret.setTaskname(currenttask);
		}
		// Still can have problem if we could not steal enough
		if(actualtask.getUsedtime() > actualtask.getMaxtime()) {
			// We need an error
			S3eUtil.logg(S3eConstants.logDebug, "We still have error: " + maxsteal, util, rparams);
			ret.setLocation(location + "." + rparams.mdfnTasksSection
					+ "." + actualtask.getNodename());
			ret.setMessage("Task " + currenttask + " max time " 
					+ S3eUtil.convertMillisToDurationString(actualtask.getMaxtime()) 
					+ " exceeded, new used time is " 
					+ S3eUtil.convertMillisToDurationString(newtime));
			ret.setProblemtype(S3eConstants.problemtypeTASKTIMEEXC);
			ret.setSeverity(ExtConstants.alertseverityERROR);
			ret.setStatus(rparams.gentextStatusError);
			ret.setTaskname(currenttask);
		}
		
		// We need to really steal the time from the follow-up tasks
		for(TaskSteal tst: tasksteal) {
			for(SingleTask st: techtasks) {
				if(st.getTaskname().equals(tst.name)) {
					st.setMaxtime(st.getMaxtime() - tst.steal);
					if(st.getAlerttime() > st.getMaxtime()) st.setAlerttime(st.getMaxtime());
					S3eUtil.logg(S3eConstants.logDebug, 
							"Task " + st.getTaskname() 
							+ " new maxt = " + st.getMaxtime()
							+ ", new alt = " + st.getAlerttime(), util, rparams);
				}
			}
		}
		
		return ret;
	}
	
	private static void copyTaskResultToTaskNode(Attr tasknode, SingleTask st,PluginUtilInterface util,
			RuntimeParameters rparams, HashMap<String, ArrayList<PotType>> pottypes) {
		if(tasknode == null || st == null || util == null) return;
		
		S3eUtil.logg(S3eConstants.logDebug, "Writing task values:", util, rparams);
		S3eUtil.logg(S3eConstants.logDebug, "Node:" + st.getNodename(), util, rparams);
		S3eUtil.logg(S3eConstants.logDebug, "Maxtime:" + st.getMaxtime(), util, rparams);
		S3eUtil.logg(S3eConstants.logDebug, "Usedtime:" + st.getUsedtime(), util, rparams);
		S3eUtil.logg(S3eConstants.logDebug, "Old values:", util, rparams);
		ArrayList<NodeValue> nvs = util.nodeValueList(tasknode, "", false);
		for(NodeValue nv: nvs) {
			S3eUtil.logg(S3eConstants.logDebug, nv.getNode() + " = " + nv.getValue(), util, rparams);
		}
		
		util.setNodeValue(tasknode, String.valueOf(st.getMaxtime()), rparams.mdfnRuleDetailsMaxTime);
		util.setNodeValue(tasknode, String.valueOf(st.getUsedtime()), rparams.mdfnRuleDetailsUsedTime);
		util.setNodeValue(tasknode, String.valueOf(st.getAlerttime()), rparams.mdfnRuleDetailsAlertTime);
	}
	
	private static SingleTask convertNodeToSingleTask(String nodename, String lastnodename, Attr tasknode,
			PluginUtilInterface util,
			RuntimeParameters rparams, HashMap<String, ArrayList<PotType>> pottypes) {
		
		// This returns null, if the input is not a valid single task
		boolean hasalerttime = false;
		boolean hasmaxtime = false;
		boolean hasusedtime = false;
		boolean hastaskname = false;
		
		SingleTask ret = new SingleTask();
		ret.setNodename(nodename);
		ret.setLastnodename(lastnodename);
		
		ArrayList<NodeValue> nodes = util.nodeValueList(tasknode, "", false);
		if(nodes == null || nodes.size() == 0) return null;
		
		S3eUtil.logg(S3eConstants.logDebug, "Single node nodes:", util, rparams);
		for(NodeValue nv: nodes) {
			S3eUtil.logg(S3eConstants.logDebug, nv.getNode() + " = " + nv.getValue(), util, rparams);
			if(nv.getNode().equals(rparams.mdfnRuleDetailsAlertTime)) {
				ret.setAlerttime(S3eUtil.convertLongStringToMillis(nv.getValue(), rparams, util));
				hasalerttime = true;
			}
			if(nv.getNode().equals(rparams.mdfnRuleDetailsMaxTime)) {
				ret.setMaxtime(S3eUtil.convertLongStringToMillis(nv.getValue(), rparams, util));
				hasmaxtime = true;
			}
			if(nv.getNode().equals(rparams.mdfnRuleDetailsMinTime)) {
				ret.setMintime(S3eUtil.convertLongStringToMillis(nv.getValue(), rparams, util));
			}
			if(nv.getNode().equals(rparams.mdfnTasksFieldTaskName)) {
				ret.setTaskname(nv.getValue());
				hastaskname = true;
			}
			if(nv.getNode().equals(rparams.mdfnRuleDetailsUsedTime)) {
				ret.setUsedtime(S3eUtil.convertLongStringToMillis(nv.getValue(), rparams, util));
				hasusedtime = true;
			}
			if(nv.getNode().equals(rparams.mdfnTasksFieldTaskRepeatable)) {
				if(nv.getValue().equals(S3eConstants.textTrue)) ret.setRepeatable(true);
			}
		}
		
		if(!(hasalerttime && hasmaxtime && hastaskname && hasusedtime)) return null;
		
		
		return ret;
	}
	
	private static void setStatus(Pot pot, Attr charattr, MeasurementCalcResult mcr, 
			PluginUtilInterface util, RuntimeParameters rparams) {
		
		// We change the status for task error only if parameter true
		if(mcr.getProblemtype().equals(S3eConstants.problemtypeTASKTIMEEXC)) {
			if(!(rparams.tasksetsstatus)) return;
		}
		
		String currentstatus = util.getNodeValue(charattr, rparams.gentextStatus);
		// If already error, do not change
		if(currentstatus.equals(rparams.gentextStatusError)) return;
		// If warning, change only if the new is error
		if(currentstatus.equals(rparams.gentextStatusWarning)) {
			if(mcr.getStatus().equals(rparams.gentextStatusError)) {
				util.setNodeValue(charattr, rparams.gentextStatusError,
						rparams.gentextStatus);
				pot.setMatchkey4(rparams.gentextStatus);
				pot.setMatchval4(rparams.gentextStatusError);
			}
			return;
		}
		// Otherwise just set to new
		util.setNodeValue(charattr, mcr.getStatus(),
				rparams.gentextStatus);
		pot.setMatchkey4(rparams.gentextStatus);
		pot.setMatchval4(mcr.getStatus());
	}
	
	
	private static void handleMeasurementResult(Pot pot, Ball ball, SingleTelemetry st, MeasurementCalcResult mcr, PluginUtilInterface util,
			RuntimeParameters rparams, HashMap<String, ArrayList<PotType>> pottypes) {
		
		// Maybe the severity was not filled
		if(mcr.getSeverity() == null || mcr.getSeverity().equals("")) {
			if(mcr.getStatus().equals(rparams.gentextStatusWarning)) 
				mcr.setSeverity(ExtConstants.alertseverityWARNING);
			if(mcr.getStatus().equals(rparams.gentextStatusError)) 
				mcr.setSeverity(ExtConstants.alertseverityERROR);
		}
		
		// Log it
		S3eUtil.logg(S3eConstants.logDebug, "Calculation result: "
				+ pot.getMatchkey0() + " = " + pot.getMatchval0() 
				+ " severity " + mcr.getSeverity() + " - "
				+ mcr.getMessage(), util, rparams);
		
		// Alert but only if new status
		String oldstatus = util.getNodeValue(pot.getFlexi(), rparams.gentextStatus);
		if(!(oldstatus.equals(rparams.gentextStatusError))) {
			if((oldstatus.equals(rparams.gentextStatusOk) && 
					(mcr.getStatus().equals(rparams.gentextStatusError) 
							|| mcr.getStatus().equals(rparams.gentextStatusWarning)))
					|| (oldstatus.equals(rparams.gentextStatusWarning) && 
							mcr.getStatus().equals(rparams.gentextStatusError)
					))
			S3eUtil.sendAlert(mcr.getSeverity(), 
					pot.getType(), 
					pot.getMatchkey0(), 
					pot.getMatchval0(), 
					"Telemetry processing", 
					mcr.getMessage() + System.lineSeparator()
					+ "Problem type: " + mcr.getProblemtype() + System.lineSeparator()
					+ "Location: " + mcr.getLocation() + System.lineSeparator()
					+ "Task: " + mcr.getTaskname() + System.lineSeparator()
					+ "Characteristic: " + st.getC12c() + System.lineSeparator()
					+ "Value: " + st.getValue() + " " + st.getUom() + System.lineSeparator()
					+ "Master data: " + mcr.getBudgetname()
					, rparams, util);
		}
		
		
		// We put all necessary information into a plugin data transfer and call an
		// external plugin to process it.
		if(!(rparams.outpluginCalculationProblem.equals(""))) {
			PluginData data = new PluginData();
			data.pot = pot;
			data.ball = ball;
			data.transfer = new ArrayList<Object>();
			data.transfer.add(mcr);
			data.transfer.add(st);
			data.transfer.add(oldstatus);
			PluginInterface pi = util.getPlugin(rparams.outpluginCalculationProblem);
			if(!(pi == null)) {
				pi.execute(data, util);
			}
		}
		
		// We also need to set global status and last messages
		Attr flexi = pot.getFlexi();
		setStatus(pot, flexi, mcr, util, rparams);
		pot.setMatchkey4(rparams.gentextStatus);
		pot.setMatchval4(S3eMergeUtil.getUnitStatus(pot, rparams, util));
		if(mcr.getStatus().equals(rparams.gentextStatusWarning) 
				|| mcr.getStatus().equals(rparams.gentextStatusError)
				|| ( !(rparams.tasksetsstatus)
						&& mcr.getProblemtype().equals(S3eConstants.problemtypeTASKTIMEEXC))) {
			util.setNodeValue(flexi, mcr.getSeverity(), 
					rparams.gentextLastMessage + "." + rparams.gentextMessageSeverity);
			util.setNodeValue(flexi, mcr.getMessage() + " (" + mcr.getLocation() + ")", 
					rparams.gentextLastMessage + "." + rparams.gentextMessageText);
			util.setNodeValue(flexi, S3eUtil.convertInstantToTimestampString(Instant.now(), util), 
					rparams.gentextLastMessage + "." + rparams.gentextMessageTime);
		}
		if(mcr.getStatus().equals(rparams.gentextStatusError)) {
			util.setNodeValue(flexi, mcr.getMessage() + " (" + mcr.getLocation() + ")", 
					rparams.gentextLastError + "." + rparams.gentextMessageText);
			util.setNodeValue(flexi, S3eUtil.convertInstantToTimestampString(Instant.now(), util), 
					rparams.gentextLastError + "." + rparams.gentextMessageTime);
		}
		
	}
	
	private static SingleInterval findInterval(Attr techint, double value, RuntimeParameters rparams, PluginUtilInterface util) {
		SingleInterval ret = new SingleInterval();
		
		ArrayList<SingleInterval> sis = S3eUtil.readIntervalListNode(techint, util);
		if(sis == null || sis.size() == 0) return ret;
		for(SingleInterval si: sis) {
			if(value >= si.getFrom() && value <= si.getTo()) return si;
		}
		
		return ret;
	}
	
	private static MeasurementCalcResult isValueCovered(Attr budgets, double value, RuntimeParameters rparams, PluginUtilInterface util) {
		MeasurementCalcResult ret = new MeasurementCalcResult(rparams.gentextStatusOk);
		
		S3eUtil.logg(S3eConstants.logDebug, "Start: check coverage of value " + value, util, rparams);
		
		// We get the list of budgets as Attr. ALL budgets must have the value
		// covered in an interval, otherwise error.
		ArrayList<NodeNode> blist = util.nodeNodeList(budgets, "", false);
		if(!(blist == null) && blist.size() > 0) {
			
			for(NodeNode nn: blist) {
				
				S3eUtil.logg(S3eConstants.logDebug, 
						"Checking master data: " + nn.getNode(), util, rparams);
				
				// Get the technical node for interval
				Attr techint = util.getNode(nn.getSubnode(), rparams.mdfnRuleTechIntField);
				// Convert to intervals
				ArrayList<SingleInterval> sis = S3eUtil.readIntervalListNode(techint, util);
				if(!(sis == null) && sis.size() > 0) {
					boolean found = false;
					for(SingleInterval si: sis) {
						if(value >= si.getFrom() && value <= si.getTo()) found = true;
					}
					
					// Problem
					if(!found) {
						ret.setStatus(rparams.gentextStatusError);
						ret.setLocation(nn.getNode());
						ret.setSeverity(ExtConstants.alertseverityERROR);
						ret.setProblemtype(S3eConstants.problemtypeVALUENOTCOVERED);
						ret.setMessage("Value " + value + " is not covered in the master data.");
						return ret;
					}
					
				}
				
			}
			
		}
		
		
		return ret;
	}
	
}
