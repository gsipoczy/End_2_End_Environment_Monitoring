package sygr3em.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;

import sygr.pots.extensions.*;
import sygr3em.model.*;

public class S3eCommandUtil {
	
	public static void executeIds(String command, PluginData data, RuntimeParameters rparam, PluginUtilInterface util) {
		if(data == null || data.pot == null || data.ball == null 
				|| data.ball.getAttr() == null) {
			S3eUtil.logg(S3eConstants.logDebug, "No Pot/Ball for updating Pot.", util, rparam);
			return;
		}
		
		ArrayList<ExtraMatch> ems = data.pot.getExtramatches();
		if(ems == null) ems = new ArrayList<ExtraMatch>(); 
		
		// Get IDs from ball
		ArrayList<NodeNode> nns = util.nodeNodeList(data.ball.getAttr(), "", false);
		if(nns == null || nns.size() == 0) return;
		
		for(NodeNode nn: nns) {
			String key = util.getNodeValue(nn.getSubnode(), S3eConstants.KEY);
			String value = util.getNodeValue(nn.getSubnode(), S3eConstants.VALUE);
			if(key.equals("") || value.equals("")) continue;
			
			// Check if exists
			boolean exists = false;
			ExtraMatch existingem = null;
			for(ExtraMatch em: ems) {
				if(em.getMatchkey().equals(key) && em.getMatchval().equals(value)) {
					exists = true;
					existingem = em;
				}
			}
			
			if(command.equals(rparam.imsgcommandAddIds) && !exists) {
				ExtraMatch em = new ExtraMatch();
				em.setMatchkey(key);
				em.setMatchval(value);
				ems.add(em);
			}
			else { // deletion, but only if exists
				if(exists && !(existingem == null)) {
					ems.remove(existingem);
				}
			}
		}
		
		data.pot.setExtramatches(ems);
		data.pot.setUpdateExtraMatches(true);
		
	}
	
	public static void executeUpdateUnit(PluginData data, RuntimeParameters rparam, PluginUtilInterface util) {
		if(data == null || data.pot == null || data.ball == null 
				|| data.ball.getAttr() == null) {
			S3eUtil.logg(S3eConstants.logDebug, "No Pot/Ball for updating Pot.", util, rparam);
			return;
		}
		
		// Get fix and flexi from Ball
		Attr fixed = util.getNode(data.ball.getAttr(), S3eConstants.FIXED);
		if(fixed == null) {
			S3eUtil.logg(S3eConstants.logDebug, "No master data arrived in Ball", util, rparam);
		}
		else data.pot.setFixed(fixed);
		
		Attr flexi = util.getNode(data.ball.getAttr(), S3eConstants.FLEXI);
		if(flexi == null) {
			S3eUtil.logg(S3eConstants.logDebug, "No transactional data arrived in Ball", util, rparam);
		}
		else data.pot.setFlexi(flexi);
	}
	
	public static void executeUpdateSecAttr(PluginData data, RuntimeParameters rparam, PluginUtilInterface util) {
		if(data == null || data.pot == null || data.ball == null 
				|| data.ball.getAttr() == null) {
			S3eUtil.logg(S3eConstants.logDebug, "No Pot/Ball for updating Pot.", util, rparam);
			return;
		}
		
		// Get attr from Ball
		Attr attr = data.ball.getAttr();
		if(attr == null) {
			S3eUtil.logg(S3eConstants.logDebug, "No data arrived in Ball", util, rparam);
		}
		
		// Get flexi from Pot
		Attr flexi = data.pot.getFlexi();
		if(flexi == null) {
			S3eUtil.logg(S3eConstants.logDebug, "No flexi in Pot", util, rparam);
		}
		
		// Check if delete the existing
		Boolean deleteexisting = S3eUtil.convertStringToBoolean(
				util.getNodeValue(attr, S3eConstants.DELETE), util);
		if(deleteexisting == null) deleteexisting = false;
		
		// Delete existing secondary attributes if needed
		if(deleteexisting) {
			Attr empty = new Attr();
			util.setNode(flexi, empty, rparam.createnewFnameSecAtt);
		}
		
		// Get the secondary attributes node
		Attr bsa = util.getNode(attr, rparam.createnewFnameSecAtt);
		if(bsa == null) {
			S3eUtil.logg(S3eConstants.logDebug, "No secodary attributes.", util, rparam);
			return;
		}
		
		// Loop and add the values
		ArrayList<NodeValue> nvs = util.nodeValueList(bsa, "", false);
		if(nvs == null || nvs.size() == 0) {
			S3eUtil.logg(S3eConstants.logDebug, "No secodary attributes.", util, rparam);
			return;
		}
		for(NodeValue nv: nvs) {
			util.setNodeValue(flexi, nv.getValue(), rparam.createnewFnameSecAtt + "." + nv.getNode());
		}
		
		
	}

	public static void executeSetTask(PluginData data, RuntimeParameters rparam, PluginUtilInterface util,
			HashMap<String, ArrayList<PotType>> pottypes) {
		
		if(data == null || data.pot == null || data.ball == null 
				|| data.pot.getFixed() == null || data.ball.getAttr() == null) {
			S3eUtil.logg(S3eConstants.logDebug, "No Pot/Ball for task setting.", util, rparam);
			return;
		}
		
		S3eUtil.logg(S3eConstants.logDebug, "START chaning task", util, rparam);
		
		// Get the task and the timestamp
		String newtask = util.getNodeValue(data.ball.getAttr(), S3eConstants.attrlocTask);
		String newtimestamp = util.getNodeValue(data.ball.getAttr(), S3eConstants.attrlocTimestamp);
		Instant currenttime = S3eUtil.convertNumericStringToInstant(newtimestamp);
		if(currenttime.equals(S3eConstants.NOTATIMESTAMP)) {
			S3eUtil.logg(S3eConstants.logDebug, "No time stamp for new task start, stop.", util, rparam);
			return;
		}
		
		// Get the current task
		String oldtask = util.getNodeValue(data.pot.getFixed(), rparam.gentextFixedCurrentTask);
		String oldtimestamp = util.getNodeValue(data.pot.getFixed(), rparam.gentextFixedTaskStarted);
		S3eUtil.logg(S3eConstants.logDebug, "Old task: " + oldtask + " from " + oldtimestamp, util, rparam);
		S3eUtil.logg(S3eConstants.logDebug, "New task: " + newtask + " from " + newtimestamp, util, rparam);
		Instant oldtime = S3eUtil.convertNumericStringToInstant(oldtimestamp);
		if(oldtime.equals(S3eConstants.NOTATIMESTAMP)) {
			oldtime = Instant.ofEpochMilli(0);
			oldtimestamp = "";
		}
		
		// Cannot go back in time
		if(oldtime.isAfter(currenttime)) {
			S3eUtil.logg(S3eConstants.logDebug, "Time stamp too old for new task, stop.", util, rparam);
			return;
		}
		
		// If the old task is the same as the new, we have nothing to do
		if(oldtask.equals(newtask)) {
			S3eUtil.logg(S3eConstants.logDebug, "Old and new task are the same, stop.", util, rparam);
			return;
		}
		
		// If there was an old task, we need to handle - close it correctly
		if(!(oldtask.equals(""))) {
			closeCurrentTask(data, oldtask, oldtimestamp, currenttime, rparam, util, pottypes);
		}
		
		// And just set in the pot the new
		util.setNodeValue(data.pot.getFixed() , newtask, rparam.gentextFixedCurrentTask);
		util.setNodeValue(data.pot.getFixed() , newtimestamp, rparam.gentextFixedTaskStarted);
		
	}
	
	private static void closeCurrentTask(PluginData data, 
			String oldtask, String oldtimestamp, Instant currenttime,
			RuntimeParameters rparam, PluginUtilInterface util,
			HashMap<String, ArrayList<PotType>> pottypes) {
		
		// Here we have 2 tasks:
		// - generate a new Telemetry message for all characteristics which
		//		has this task, with the current telemetry value but with the new timestamp
		//		(do only if the new timestamp is later than the old, however in the other
		//		case it will be anyway rejected).
		// - take the remaining time from all these places and give to the next task
		//		(add to the max_time).
		
		// First of all, get what has to be closed, so what telemetry to create
		ArrayList<MultiTaskClosing> mtcs = getTaskClosingList(data, 
				oldtask, oldtimestamp, currenttime, rparam, util);
		if(mtcs == null || mtcs.size() == 0) {
			S3eUtil.logg(S3eConstants.logDebug, "Old task is not in any telemetry, no need to close.", util, rparam);
			return;
		}
		
		// OK, now we must make a telemetry ball and call telemetry processing
		// We have only 1 telemetry (1 timestamp) with possibly several characteristics
		ArrayList<MultiTelemetry> mts = new ArrayList<>();
		MultiTelemetry mt = new MultiTelemetry();
		mt.setTimestamp(currenttime);
		
		S3eUtil.logg(S3eConstants.logDebug, "Tasks found to be handled:", util, rparam);
		for(MultiTaskClosing mtc: mtcs) {
			S3eUtil.logg(S3eConstants.logDebug, "   Telemetry: " + mtc.getCharacteristic(), util, rparam);
			S3eUtil.logg(S3eConstants.logDebug, "   Current value: " + mtc.getValue(), util, rparam);
			if(mtc.getTcs() == null || mtc.getTcs().size() == 0) {
				S3eUtil.logg(S3eConstants.logDebug, "      No actual usage.", util, rparam);
			}
			else {
				for(SingleTaskClosing stc: mtc.getTcs()) {
					S3eUtil.logg(S3eConstants.logDebug, "      Old: " + stc.getOldtasklocation(), util, rparam);
					S3eUtil.logg(S3eConstants.logDebug, "         New: " + stc.getNexttasklocation(), util, rparam);
				}
				SingleTelemetry st = new SingleTelemetry();
				st.setC12c(mtc.getCharacteristic());
				st.setValue(mtc.getValue());
				mt.getList().add(st);
			}
		}
		mts.add(mt);
		
		// Create a temporary ball for telemetry execution
		Attr tmattr = S3eImsgUtil.copyMeasurementsToAttr(mts, util);
		Ball tmball = new Ball();
		util.setNodeValue(tmball.getAttr(), rparam.imsgcommandTelemetry, S3eConstants.attrlocCommand);
		util.setNodeValue(tmball.getAttr(), data.ball.getMatchkey(), S3eConstants.attrlocKey);
		util.setNodeValue(tmball.getAttr(), data.ball.getMatchval(), S3eConstants.attrlocValue);
		util.setNode(tmball.getAttr(), tmattr, S3eConstants.MEASUREMENTS);
		// We create a new data with the real pot (which we want to change,
		// but with the temporary telemetry ball
		PluginData tmpdata = new PluginData();
		tmpdata.pot = data.pot;
		tmpdata.ball = tmball;
		// And just execute the telemetry processing
		S3eCalculationUtil.processTelemetry(tmpdata, util, rparam, pottypes);
		
		// But not ready yet. 
		forwardRemainingTaskTimes(mtcs, data.pot.getFixed(), rparam, util);
	}
	
	private static void forwardRemainingTaskTimes(ArrayList<MultiTaskClosing> mtcs,
			Attr fixed, RuntimeParameters rparam, PluginUtilInterface util) {
		
		S3eUtil.logg(S3eConstants.logDebug, "START forwarding remaining times.", util, rparam);
		if(mtcs == null || mtcs.size() == 0) return;
		
		for(MultiTaskClosing mtc: mtcs) {
			if(mtc.getTcs() == null || mtc.getTcs().size() == 0) continue;
			
			for(SingleTaskClosing stc: mtc.getTcs()) {
				S3eUtil.logg(S3eConstants.logDebug, "Node: " + stc.getOldtasklocation(), util, rparam);
				if(stc.getNexttasklocation().equals("")) {
					S3eUtil.logg(S3eConstants.logDebug, "No next task, skip.", util, rparam);
					continue;
				}
				// Get the task node
				Attr tnode = util.getNode(fixed, stc.getOldtasklocation());
				// Get if it is repeatable
				Boolean repeatable = S3eUtil.convertStringToBoolean(
						util.getNodeValue(tnode, rparam.mdfnTasksFieldTaskRepeatable), util);
				if(repeatable == null) {
					S3eUtil.logg(S3eConstants.logDebug, "Repeatable parameter missing, skip.", util, rparam);
					continue;
				}
				if(repeatable.equals(Boolean.TRUE)) {
					S3eUtil.logg(S3eConstants.logDebug, "Task is repeatable, skip.", util, rparam);
					continue;
				}
				
				// OK, this is a non-repeatable task and have a next taks.
				// Let's get how much time remained
				long max_time = S3eUtil.convertLongStringToMillis(
						util.getNodeValue(tnode, rparam.mdfnRuleDetailsMaxTime), 
						rparam, util);
				long used_time = S3eUtil.convertLongStringToMillis(
						util.getNodeValue(tnode, rparam.mdfnRuleDetailsUsedTime), 
						rparam, util);
				if(max_time == S3eConstants.NOTADURATION || used_time == S3eConstants.NOTADURATION) {
					S3eUtil.logg(S3eConstants.logDebug, "No max time or used time, skip.", util, rparam);
					continue;
				}
				long remaining_time = max_time - used_time;
				if(remaining_time <= 0) {
					S3eUtil.logg(S3eConstants.logDebug, "Task has no remaining time, skip.", util, rparam);
					continue;
				}
				// OK, have remaining time, get the next task
				S3eUtil.logg(S3eConstants.logDebug, "Next node: " + stc.getNexttasklocation(), util, rparam);
				Attr nextnode = util.getNode(fixed, stc.getNexttasklocation());
				long next_max_time = S3eUtil.convertLongStringToMillis(
						util.getNodeValue(nextnode, rparam.mdfnRuleDetailsMaxTime), 
						rparam, util);
				if(next_max_time == S3eConstants.NOTADURATION) {
					S3eUtil.logg(S3eConstants.logDebug, "Next task has no max time", util, rparam);
					continue;
				}
				S3eUtil.logg(S3eConstants.logDebug, "Next task current max time = " + next_max_time , util, rparam);
				S3eUtil.logg(S3eConstants.logDebug, "Adding from previus task = " + remaining_time , util, rparam);
				next_max_time = next_max_time + remaining_time;
				S3eUtil.logg(S3eConstants.logDebug, "Next task new max time = " + next_max_time , util, rparam);
				util.setNodeValue(nextnode, String.valueOf(next_max_time), rparam.mdfnRuleDetailsMaxTime);
				// In the same time set the remaining time to 0 in the old task
				util.setNodeValue(tnode, String.valueOf(used_time), rparam.mdfnRuleDetailsMaxTime);
			}
			
		}
		
	}
	
	private static ArrayList<MultiTaskClosing> getTaskClosingList(PluginData data, 
			String oldtask, String oldtimestamp, Instant currenttime,
			RuntimeParameters rparam, PluginUtilInterface util) {
		ArrayList<MultiTaskClosing> ret = new ArrayList<>();
		
		// Get the list of the telemetries within the unit
		ArrayList<NodeNode> nns = util.nodeNodeList(data.pot.getFixed(), "", false);
		if(nns == null || nns.size() == 0) {
			S3eUtil.logg(S3eConstants.logDebug, "No telemetry found.", util, rparam);
			return ret;
		}
		for(NodeNode nn: nns) {
			S3eUtil.logg(S3eConstants.logDebug, "Processing root level: " + nn.getNode(), util, rparam);
			// Check that it is a defined telemetry
			boolean itsachar = false;
			for(SingleCharacteristic sc: rparam.characteristics) {
				if(sc.getName().equals(nn.getNode())) itsachar = true;
			}
			if(!(itsachar)) {
				S3eUtil.logg(S3eConstants.logDebug, "It's not a characteristic, skip.", util, rparam);
				continue;
			}
			
			// So we have a characteristic node in nn, start a MultiTaskClosing entry.
			MultiTaskClosing mtc = getTaskClosingSingle(nn.getNode(), nn.getSubnode(),
					oldtask, oldtimestamp, currenttime, rparam, util);
			if(mtc.getTcs() == null || mtc.getTcs().size() == 0) {
				S3eUtil.logg(S3eConstants.logDebug, "Characteristic has not this task, skip.", util, rparam);
				continue;
			}
			else ret.add(mtc);
			
		}
		
		return ret;
	}
	
	private static MultiTaskClosing getTaskClosingSingle(String toplocation,
			Attr c12cnn,
			String oldtask, String oldtimestamp, Instant currenttime,
			RuntimeParameters rparam, PluginUtilInterface util) {
		MultiTaskClosing ret = new MultiTaskClosing();
		
		/* So here:
		 * toplocation = a c12c name, like "temperature"
		 * c21cnn is the node of this, like
		 * 	budget ...
		 * 	last_time
		 * 	last_value
		 * 	status
		 */
		
		S3eUtil.logg(S3eConstants.logDebug, "Create MultiTaskClosing for " + toplocation, util, rparam);
		
		// The characteristic will be the toplocation
		ret.setCharacteristic(toplocation);
		// The value we need to take from the "last_value" field, which is a double
		ret.setValue(S3eUtil.convertStringToNumeric(
				util.getNodeValue(c12cnn, rparam.gentextFixedLastValue), 
				util));
		S3eUtil.logg(S3eConstants.logDebug, "Current value: " + ret.getValue(), util, rparam);
		
		// Now take the master data list from the budget node
		ArrayList<NodeNode> nns = util.nodeNodeList(c12cnn, rparam.gentextFixedBudget, false);
		if(nns == null || nns.size() == 0) {
			S3eUtil.logg(S3eConstants.logDebug, "No master data in telemetry found.", util, rparam);
			return ret;
		}
		for(NodeNode nn: nns) {
			
			/*
			 * nn.getNode() = the master data name
			 * nn.getSubnode = the master data, but from this we do not need everything, only
			 * the intervals part.
			 * 
			 */
			
			// Compose a top level location
			String ivlocation = toplocation + "." 
					+ rparam.gentextFixedBudget + "."
					+ nn.getNode() + "." + rparam.mdfnSectionIntervals;
			Attr ivnode = util.getNode(c12cnn, rparam.gentextFixedBudget + "."
					+ nn.getNode() + "." + rparam.mdfnSectionIntervals);
			S3eUtil.logg(S3eConstants.logDebug, "Location: " + ivlocation, util, rparam);
			ArrayList<SingleTaskClosing> stcs = getSingleTaskClosings(ivlocation,
					ivnode, oldtask, oldtimestamp, currenttime, rparam, util);
			if(!(stcs == null) && stcs.size() > 0) {
				S3eUtil.logg(S3eConstants.logDebug, "Got " + stcs.size() + " task nodes.", util, rparam);
				ret.getTcs().addAll(stcs);
			}
		}
		
		return ret;
	}
	
	private static ArrayList<SingleTaskClosing> getSingleTaskClosings(String ivlocation,
			Attr ivnode,
			String oldtask, String oldtimestamp, Instant currenttime,
			RuntimeParameters rparam, PluginUtilInterface util) {
		ArrayList<SingleTaskClosing> ret = new ArrayList<>();
		
		ArrayList<NodeNode> nns = util.nodeNodeList(ivnode, "", false);
		if(nns == null || nns.size() == 0) return ret;
		for(NodeNode nn: nns) {
			String newlocation = ivlocation + "." + nn.getNode();
			S3eUtil.logg(S3eConstants.logDebug, "Checking node: " + newlocation, util, rparam);
			// Get the node type
			int nodetype = S3eMdUtil.getIntNodeType(nn.getNode(), rparam, util);
			// If it's not an interval, need to dig deeper
			if(nodetype != S3eConstants.mdiInt) {
				ArrayList<SingleTaskClosing> stcs = getSingleTaskClosings(newlocation,
						nn.getSubnode(),
						oldtask, oldtimestamp, currenttime, rparam, util);
				if(!(stcs == null) && stcs.size() > 0) ret.addAll(stcs);
			}
			else { // This is an Interval!!!
				String taskslocation = newlocation + "." + rparam.mdfnTasksSection;
				SingleTaskClosing stc = getStcFromInterval(taskslocation,
						util.getNode(nn.getSubnode(), rparam.mdfnTasksSection),
						oldtask, oldtimestamp, currenttime,
						rparam, util);
				if(!(stc.getOldtasklocation().equals(""))) {
					S3eUtil.logg(S3eConstants.logDebug, "Valid node, adding: " + stc.getOldtasklocation(), util, rparam);
					ret.add(stc);
				}
				else {
					S3eUtil.logg(S3eConstants.logDebug, "Empty node, skip.", util, rparam);
				}
			}
			
		}
		
		return ret;
	}
	
	private static SingleTaskClosing getStcFromInterval(String taskslocation,
			Attr tasksnode,
			String oldtask, String oldtimestamp, Instant currenttime,
			RuntimeParameters rparam, PluginUtilInterface util) {
		SingleTaskClosing ret = new SingleTaskClosing();
		
		/*
		 * The ivlocation = "temperature. ... .int-03.tasks"
		 * The tasksnode is
		 * 		tg-xxx
		 * 		tg-yyy
		 * 			task-xxx
		 * 			task-yyy
		 */
		
		// Get the task groups sorted and collect what we have
		ArrayList<NodeNode> nns = util.nodeNodeList(tasksnode, "", true);
		if(nns == null || nns.size() == 0) return ret;
		
		ArrayList<SingleProperty> locs = new ArrayList<>();
		for(NodeNode nn: nns) {
			S3eUtil.logg(S3eConstants.logDebug, "Checking task node: " + nn.getNode(), util, rparam);
			
			String tglocation = taskslocation + "." + nn.getNode();
			// This node can be a single task or a collection of single tasks
			String nodeval = util.getNodeValue(nn.getSubnode(), rparam.mdfnTasksFieldTaskName);
			if(!(nodeval.equals(""))) {
				S3eUtil.logg(S3eConstants.logDebug, "   Has task name: " + nodeval, util, rparam);
				SingleProperty sp = new SingleProperty();
				sp.setKey(tglocation);
				sp.setValue(nodeval);
				locs.add(sp);
			}
			else { // We have a tasks group, dig deeper
				S3eUtil.logg(S3eConstants.logDebug, "   No task name, go to tasks.", util, rparam);
				ArrayList<NodeNode> nns2 = util.nodeNodeList(nn.getSubnode(), "", true);
				if(nns2 == null || nns2.size() == 0) continue;
				for(NodeNode nn2: nns2) {
					S3eUtil.logg(S3eConstants.logDebug, "Checking task node: " + nn2.getNode(), util, rparam);
					String tasklocation = tglocation + "." + nn2.getNode();
					String nodeval2 = util.getNodeValue(nn2.getSubnode(), rparam.mdfnTasksFieldTaskName);
					if(!(nodeval2.equals(""))) {
						S3eUtil.logg(S3eConstants.logDebug, "   Has task name: " + nodeval2, util, rparam);
						SingleProperty sp = new SingleProperty();
						sp.setKey(tasklocation);
						sp.setValue(nodeval2);
						locs.add(sp);
					}
					else {
						S3eUtil.logg(S3eConstants.logDebug, "   No task name, HOW???.", util, rparam);
					}
				}
				
				
			}
			
		}
		// Log a little:
		S3eUtil.logg(S3eConstants.logDebug, "Total tasks found: ", util, rparam);
		boolean getnext = false;
		for(SingleProperty sp: locs) {
			S3eUtil.logg(S3eConstants.logDebug, sp.getKey() + " = " + sp.getValue(), util, rparam);
			S3eUtil.logg(S3eConstants.logDebug, "Old task = ." + oldtask + ".", util, rparam);
			S3eUtil.logg(S3eConstants.logDebug, "Currentcheck = ." + sp.getValue() + ".", util, rparam);
			if(sp.getValue().equals(oldtask)) {
				S3eUtil.logg(S3eConstants.logDebug, "Equal, set old location", util, rparam);
				ret.setOldtasklocation(sp.getKey());
				getnext = true;
			}
			else {
				S3eUtil.logg(S3eConstants.logDebug, "Not equal.", util, rparam);
				if(getnext) {
					S3eUtil.logg(S3eConstants.logDebug, "But it is the next task.", util, rparam);
					ret.setNexttasklocation(sp.getKey());
					getnext = false;
				}
			}
		}
		
		return ret;
	}
	
	
	public static void executeSetPotStatus(String newstatus, PluginData data, RuntimeParameters rparam, PluginUtilInterface util,
			ArrayList<NodeValue> nvs, boolean deleteall) {
		
		if(data == null || data.pot == null || newstatus == null) {
			S3eUtil.logg(S3eConstants.logDebug, "No Pot.", util, rparam);
			return;
		}
		if(rparam == null || util == null || nvs == null) return;
		
		if(!(deleteall)) {
			// Check if the Pot belongs to S3e
			if(!(data.pot.getProp0().equals(S3eConstants.SIGNATURE))) {
				S3eUtil.logg(S3eConstants.logDebug, "Pot has no signature.", util, rparam);
				return;
			}
		
			// Check if we have an exception ID
			for(NodeValue nv: nvs) {
				if(nv.getNode().equals(S3eConstants.attrlocExceptionId)) {
					if(nv.getValue().equals(data.pot.getProp1())) {
						S3eUtil.logg(S3eConstants.logDebug, "Pot is on the exception list, cannot change status.", util, rparam);
						return;
					}
				}
			}
		}
		
		// Also we delete only by the mathch0
		if(!(data.pot.getMatchkey0().equals(data.ball.getMatchkey())) 
				|| !(data.pot.getMatchval0().equals(data.ball.getMatchval()))) {
			S3eUtil.logg(S3eConstants.logDebug, "Pot status can be changed only by primary ID.", util, rparam);
			return;
		}
		
		// Set deleted
		S3eUtil.logg(S3eConstants.logDebug, "Setting the Pot status to " + newstatus, util, rparam);
		data.pot.setPotstatus(newstatus);
		
	}
	
}
