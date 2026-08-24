package sygr3em.service;

import java.time.Instant;
import java.util.ArrayList;

import sygr.pots.extensions.Attr;
import sygr.pots.extensions.Ball;
import sygr.pots.extensions.ExtConstants;
import sygr.pots.extensions.NewBall;
import sygr.pots.extensions.NodeNode;
import sygr.pots.extensions.NodeValue;
import sygr.pots.extensions.PluginData;
import sygr.pots.extensions.PluginUtilInterface;
import sygr.pots.extensions.Pot;
import sygr3em.model.RuntimeParameters;
import sygr3em.model.SingleCharacteristic;
import sygr3em.model.SinglePotWithStatus;

public class S3eMergeUtil {
	
	public static void executeSplit(PluginData data, RuntimeParameters rparam, PluginUtilInterface util) {
		
		S3eUtil.logg(S3eConstants.logDebug, "START executeSplit", util, rparam);
		S3eUtil.logg(S3eConstants.logDebug, 
				"Split from " + data.pot.getMatchkey0() + " = " 
				+ data.pot.getMatchval0(), util, rparam);
		
		if(data == null || data.pot == null || data.ball == null 
				|| data.pot.getFixed() == null || data.ball.getAttr() == null) {
			S3eUtil.logg(S3eConstants.logDebug, "No Pot/Ball for splitting.", util, rparam);
			return;
		}

		// Get the targets
		ArrayList<NodeNode> nns = util.nodeNodeList(data.ball.getAttr(), "", false);
		if(nns == null || nns.size() == 0) return;
		
		for(NodeNode nn: nns) {
			String key = util.getNodeValue(nn.getSubnode(), S3eConstants.KEY);
			String value = util.getNodeValue(nn.getSubnode(), S3eConstants.VALUE);
			if(key.equals("") || value.equals("")) continue;
			
			Pot newpot = util.deepCopyPot(data.pot);
			newpot.setMatchkey0(key);
			newpot.setMatchval0(value);
			collectSources(data.pot, newpot, rparam, util);
			data.newPots.add(newpot);
		}
		
		// Set source status
		String newstatus = util.getNodeValue(data.ball.getAttr(), S3eConstants.STATUS);
		if(!(newstatus.equals(""))) {
			SinglePotWithStatus sps = new SinglePotWithStatus();
			sps.setNewstatus(newstatus);
			sps.setPot(data.pot);
			S3eUtil.logg(S3eConstants.logDebug, 
					"Change source status to " + newstatus, util, rparam);
			changeStatus(data, sps, rparam, util);
		}
		
		
		
	}
	
	public static void executeMerge(PluginData data, RuntimeParameters rparam, PluginUtilInterface util) {
		
		S3eUtil.logg(S3eConstants.logDebug, "START executeMerge", util, rparam);
		S3eUtil.logg(S3eConstants.logDebug, 
				"Merge into " + data.pot.getMatchkey0() + " = " 
				+ data.pot.getMatchval0(), util, rparam);
		
		if(data == null || data.pot == null || data.ball == null 
				|| data.pot.getFixed() == null || data.ball.getAttr() == null) {
			S3eUtil.logg(S3eConstants.logDebug, "No Pot/Ball for merging.", util, rparam);
			return;
		}

		// Get the source pots
		ArrayList<SinglePotWithStatus> sourcepots = getSourcePots(data, rparam, util);
		if(sourcepots == null || sourcepots.size() == 0) {
			S3eUtil.logg(S3eConstants.logDebug, "Merge: no source pots found.", util, rparam);
			return;
		}
		
		// Process one by one
		for(SinglePotWithStatus sps: sourcepots) {
			
			// Check the status
			String status = getUnitStatus(sps.getPot(), rparam, util);
			if(!(status.equals(rparam.gentextStatusOk))) {
				if(status.equals(rparam.gentextStatusWarning) && !rparam.allowusewarning) {
					S3eUtil.logg(S3eConstants.logDebug, 
							"Source " + sps.getPot().getMatchkey0() + " = " 
							+ sps.getPot().getMatchval0() 
							+ "is in warning status, cannot use for merge.", util, rparam);
					continue;
				}
				if(status.equals(rparam.gentextStatusError) && !rparam.allowuseerror) {
					S3eUtil.logg(S3eConstants.logDebug, 
							"Source " + sps.getPot().getMatchkey0() + " = " 
							+ sps.getPot().getMatchval0() 
							+ "is in error status, cannot use for merge.", util, rparam);
					continue;
				}
			}
			
			// Process
			S3eUtil.logg(S3eConstants.logDebug, 
					"Process source " + sps.getPot().getMatchkey0() + " = " 
					+ sps.getPot().getMatchval0(), util, rparam);
			// Merge the budgets
			mixFixed(sps.getPot(), data.pot, rparam, util);
			mixFlexi(sps.getPot(), data.pot, rparam, util);
			collectSources(sps.getPot(), data.pot, rparam, util);
			// If has a new status, create the corresponding newball
			if(!(sps.getNewstatus().equals(""))) {
				S3eUtil.logg(S3eConstants.logDebug, 
						"Change source status to " + sps.getNewstatus(), util, rparam);
				changeStatus(data, sps, rparam, util);
			}
		}
		
	}
	
	public static String getUnitStatus(Pot pot, RuntimeParameters rparam, PluginUtilInterface util) {
		if(pot == null || pot.getFlexi() == null) return "";
		return util.getNodeValue(pot.getFlexi(), rparam.gentextStatus);
	}
	
	private static void changeStatus(PluginData data, SinglePotWithStatus sps, RuntimeParameters rparam, PluginUtilInterface util) {
		
		// Check that the status is really new
		if(sps.getNewstatus().equals(sps.getPot().getPotstatus())) return;
		
		String command = "";
		switch(sps.getNewstatus()) {
		case ExtConstants.potstatusACTIVE: command = rparam.imsgcommandSetActive; break;
		case ExtConstants.potstatusINACTIVE: command = rparam.imsgcommandSetInactive; break;
		case ExtConstants.potstatusPAUSED: command = rparam.imsgcommandSetPaused; break;
		case ExtConstants.potstatusDELETED: command = rparam.imsgcommandSetDeletedForce; break;
		default: break;
		}
		if(command.equals("")) return; // wrong status arrived
		
		Attr attr = new Attr();
		util.setNodeValue(attr, command, S3eConstants.attrlocCommand);
		util.setNodeValue(attr, sps.getPot().getMatchkey0(), S3eConstants.attrlocKey);
		util.setNodeValue(attr, sps.getPot().getMatchval0(), S3eConstants.attrlocValue);
		util.setNodeValue(attr, "", S3eConstants.attrlocExceptionId);
		
		Ball ball = new Ball();
		ball.setType(rparam.balltypeCommand);
		ball.setMatchkey(sps.getPot().getMatchkey0());
		ball.setMatchval(sps.getPot().getMatchval0());
		
		if(command.equals(rparam.imsgcommandSetActive)) ball.setUsage(ExtConstants.ballusageWAKEUP);
		else ball.setUsage(ExtConstants.ballusageSINGLE);
		
		ball.setAttr(attr);
		
		NewBall nb = new NewBall();
		nb.setBall(ball);
		data.newBalls.add(nb);
	}
	
	private static void collectSources(Pot source, Pot target, RuntimeParameters rparam, PluginUtilInterface util) {
		if(!(rparam.collectsources)) return;
		if(target.getFlexi() == null) return;
		ArrayList<NodeNode> nns = util.nodeNodeList(source.getFlexi(), rparam.gentextSources, false);
		util.setNodeValue(target.getFlexi(), "", 
			rparam.gentextSources + "." + source.getMatchkey0() + " " + source.getMatchval0());
		for(NodeNode nn: nns) {
			util.setNodeValue(target.getFlexi(), "", 
					rparam.gentextSources + "." + nn.getNode());
		}
	}
	
	private static void mixFixed(Pot source, Pot target, RuntimeParameters rparam, PluginUtilInterface util) {
		
		S3eUtil.logg(S3eConstants.logDebug, "START mixFixed", util, rparam);
		
		Attr sourceattr = source.getFixed();
		if(sourceattr == null) {
			S3eUtil.logg(S3eConstants.logDebug, "Source has no Fixed, stop", util, rparam);
			return;
		}
		if(target.getFixed() == null) target.setFixed(new Attr());
		Attr targetattr = target.getFixed();
		
		
		// Loop on telemetries
		S3eUtil.logg(S3eConstants.logDebug, "Loop on telemetries", util, rparam);
		ArrayList<NodeNode> snns = util.nodeNodeList(sourceattr, "", false);
		ArrayList<NodeNode> tnns = util.nodeNodeList(targetattr, "", false);
		if(snns == null || snns.size() == 0) return;
		for(NodeNode snn: snns) {
			// Check that it's a telemetry
			boolean yesitis = false;
			for(SingleCharacteristic sch: rparam.characteristics) {
				if(snn.getNode().equals(sch.getName())) yesitis = true;
			}
			if(yesitis) {
				S3eUtil.logg(S3eConstants.logDebug, "Processing: " + snn.getNode(), util, rparam);
				// Check that the target has the same characteristic
				boolean haschar = false;
				Attr targetchar = null;
				for(NodeNode tnn: tnns) {
					if(tnn.getNode().equals(snn.getNode())) {
						haschar = true;
						targetchar = tnn.getSubnode();
					}
				}
				// If does not have easy, just add from the source
				if(!(haschar)) {
					S3eUtil.logg(S3eConstants.logDebug, "Target has no this, add from source", util, rparam);
					util.setNode(targetattr, snn.getSubnode(), snn.getNode());
				}
				else {
					mixCharacteristic(snn.getSubnode(), targetchar, rparam, util);
				}
			}
		}
		
		
	}
	
	private static void mixCharacteristic(Attr source, Attr target, RuntimeParameters rparam, PluginUtilInterface util) {
		
		S3eUtil.logg(S3eConstants.logDebug, "START mixCharacteristic", util, rparam);
		
		/*
		 * The characteristic nodes look like
		 * 		budget
		 * 			MasterData1
		 * 			MasterData2
		 * 		last_time
		 * 		last_value
		 * 		status
		 */
		boolean updlastlocation = false;
		Instant slt = S3eUtil.convertNumericStringToInstant(
				util.getNodeValue(source, rparam.gentextFixedLastTime));
		if(slt.equals(S3eConstants.NOTATIMESTAMP)) slt = Instant.ofEpochMilli(0);
		Instant tlt = S3eUtil.convertNumericStringToInstant(
				util.getNodeValue(target, rparam.gentextFixedLastTime));
		if(tlt.equals(S3eConstants.NOTATIMESTAMP)) tlt = Instant.ofEpochMilli(0);
		double slv = S3eUtil.convertStringToNumeric(
				util.getNodeValue(source, rparam.gentextFixedLastValue), util);
		if(slt.isAfter(tlt)) {
			S3eUtil.logg(S3eConstants.logDebug, 
					"Source last telemetry is later than target, updating target", util, rparam);
			updlastlocation = true;
			util.setNodeValue(target, 
					util.getNodeValue(source, rparam.gentextFixedLastTime), 
					rparam.gentextFixedLastTime);
			if(slv != S3eConstants.NOTNUMERIC) {
				util.setNodeValue(target, 
						util.getNodeValue(source, rparam.gentextFixedLastValue), 
						rparam.gentextFixedLastValue);
			}
		}
		String sst = util.getNodeValue(source, rparam.gentextStatus);
		String tst = util.getNodeValue(source, rparam.gentextStatus);
		if(sst.equals(rparam.gentextStatusError)) {
			S3eUtil.logg(S3eConstants.logDebug, "Setting target telemetry status to error", util, rparam);
			util.setNodeValue(target, rparam.gentextStatusError, rparam.gentextStatus);
		}
		else {
			if(sst.equals(rparam.gentextStatusWarning) && (!tst.equals(rparam.gentextStatusError))) {
				S3eUtil.logg(S3eConstants.logDebug, "Setting target telemetry status to warning", util, rparam);
				util.setNodeValue(target, rparam.gentextStatusWarning, rparam.gentextStatus);
			}
		}
		
		// Now take the budgets
		Attr sourcebudget = util.getNode(source, rparam.gentextFixedBudget);
		Attr targetbudget = util.getNode(target, rparam.gentextFixedBudget);
		ArrayList<NodeNode> snns = util.nodeNodeList(sourcebudget, "", false);
		ArrayList<NodeNode> tnns = util.nodeNodeList(targetbudget, "", false);
		if(snns == null || snns.size() == 0) {
			S3eUtil.logg(S3eConstants.logDebug, "Source telemetry has no budget, stop", util, rparam);
			return; // nothing to metrge
		}
		if(tnns == null || tnns.size() == 0) {
			S3eUtil.logg(S3eConstants.logDebug, "Target telemetry has no budget, copy from source", util, rparam);
			util.setNode(target, sourcebudget, rparam.gentextFixedBudget);
			return;
		}
		
		// Loop on the source master data
		S3eUtil.logg(S3eConstants.logDebug, "Loop on master data", util, rparam);
		for(NodeNode snn: snns) {
			S3eUtil.logg(S3eConstants.logDebug, "Processing md: " + snn.getNode(), util, rparam);
			boolean hasalready = false;
			Attr md = null;
			for(NodeNode tnn: tnns) {
				if(snn.getNode().equals(tnn.getNode())) {
					hasalready = true;
					md = tnn.getSubnode();
				}
			}
			
			// If does not have this budget, simply add
			if(!hasalready) {
				S3eUtil.logg(S3eConstants.logDebug, "Target has no this master data, add from source", util, rparam);
				util.setNode(targetbudget, snn.getSubnode(), snn.getNode());
			}
			else {
				mixMaster(snn.getSubnode(), md, rparam, util, updlastlocation);
			}
			
		}
		
	}
	
	public static void mixMaster(Attr source, Attr target, RuntimeParameters rparam, 
			PluginUtilInterface util, boolean updlastlocation) {
		
		S3eUtil.logg(S3eConstants.logDebug, "START mixMaster", util, rparam);
		
		/*
		 * The master data Attrs look like
		 * 
		 * 		TECH-INT		must regenerate after update
		 * 		crossings		mix
		 * 		intervals		mix
		 * 		settings		do not touch
		 * 		last_location	update if required
		 * 
		 */
		
		// Handle crossings
		Attr scross = util.getNode(source, rparam.mdfnSectionCrossings);
		Attr tcross = util.getNode(target, rparam.mdfnSectionCrossings);
		mixCrossings(scross, tcross, rparam, util);
		
		// Handle intervals
		Attr sints = util.getNode(source, rparam.mdfnSectionIntervals);
		Attr tints = util.getNode(target, rparam.mdfnSectionIntervals);
		mixIntervals(sints, tints, rparam, util);
		
		// Handle technical intervals
		S3eUtil.logg(S3eConstants.logDebug, "Updating technical intervals", util, rparam);
		Attr stech = util.getNode(source, rparam.mdfnRuleTechIntField);
		Attr ttech = util.getNode(target, rparam.mdfnRuleTechIntField);
		// only leave in target which exists in source
		ArrayList<NodeNode> snns = util.nodeNodeList(stech, "", false);
		ArrayList<NodeNode> tnns = util.nodeNodeList(ttech, "", false);
		if(snns == null || snns.size() == 0) ttech = new Attr();
		if(tnns == null || tnns.size() == 0) ttech = new Attr();
		
		// Loop on the source master data
		for(NodeNode tnn: tnns) {
			boolean found = false;
			for(NodeNode snn: snns) {
				if(tnn.getNode().equals(snn.getNode())) found = true;
			}
			if(!found) util.deleteNode(ttech, tnn.getNode());
		}
		
		// Handle last location
		if(updlastlocation) {
			S3eUtil.logg(S3eConstants.logDebug, "Updating last location", util, rparam);
			String ll = util.getNodeValue(source, rparam.gentextFixedLastLocation);
			if(!(ll.equals(""))) {
				util.setNodeValue(target, ll, rparam.gentextFixedLastLocation);
			}
		}
		
	}
	
	private static void mixCrossings(Attr source, Attr target, RuntimeParameters rparam, 
			PluginUtilInterface util) {
		
		S3eUtil.logg(S3eConstants.logDebug, "START mixCrossings", util, rparam);
		
		ArrayList<NodeNode> snns = util.nodeNodeList(source, "", false);
		ArrayList<NodeNode> tnns = util.nodeNodeList(target, "", false);
		if(snns == null || snns.size() == 0) {
			S3eUtil.logg(S3eConstants.logDebug, "Source has no crossings, stop", util, rparam);
			return; // nothing to metrge
		}
		if(tnns == null || tnns.size() == 0) {
			S3eUtil.logg(S3eConstants.logDebug, "Target has no crossings, copy from source", util, rparam);
			target = util.deepCopyAttr(source);
			return;
		}
		
		// Loop on the source master data
		for(NodeNode snn: snns) {
			S3eUtil.logg(S3eConstants.logDebug, "Process crossing: " + snn.getNode(), util, rparam);
			boolean hasalready = false;
			Attr cross = null;
			for(NodeNode tnn: tnns) {
				if(snn.getNode().equals(tnn.getNode())) {
					hasalready = true;
					cross = tnn.getSubnode();
				}
			}
			
			// If does not have this crossing, simply add
			if(!hasalready) {
				S3eUtil.logg(S3eConstants.logDebug, "Target has no this crossing, add from source", util, rparam);
				util.setNode(target, snn.getSubnode(), snn.getNode());
			}
			else {
				S3eUtil.logg(S3eConstants.logDebug, "Target has this crossing, compare", util, rparam);
				// Need to compare the max crossings
				int sm = S3eUtil.convertStringToInteger(
						util.getNodeValue(snn.getSubnode(), rparam.mdfnCrossingsCurrentCross), util);
				int tm = S3eUtil.convertStringToInteger(
						util.getNodeValue(cross, rparam.mdfnCrossingsCurrentCross), util);
				if(sm > tm) {
					S3eUtil.logg(S3eConstants.logDebug, "Source has more crossings, update target", util, rparam);
					util.setNodeValue(cross, util.getNodeValue(snn.getSubnode(), rparam.mdfnCrossingsCurrentCross), 
							rparam.mdfnCrossingsCurrentCross);
				}
			}
			
		}
	}
	
	private static void mixIntervals(Attr source, Attr target, RuntimeParameters rparam, 
			PluginUtilInterface util) {
		
		S3eUtil.logg(S3eConstants.logDebug, "START mixIntervals", util, rparam);
		
		// Loop on the subnodes and process according their type
		ArrayList<NodeNode> snns = util.nodeNodeList(source, "", false);
		ArrayList<NodeNode> tnns = util.nodeNodeList(target, "", false);
		
		// If there is no source nodes, empty the target node
		if(snns == null || snns.size() == 0) {
			S3eUtil.logg(S3eConstants.logDebug, "Source has no intervals, delete from target", util, rparam);
			target = new Attr();
			return;
		}
		
		// If no target node, do not touch
		if(tnns == null || tnns.size() == 0) {
			S3eUtil.logg(S3eConstants.logDebug, "Target has no intervals, stop.", util, rparam);
			return;
		}
		
		// Loop on the target nodes
		for(NodeNode tnn: tnns) {
			S3eUtil.logg(S3eConstants.logDebug, "Process target node: " + tnn.getNode(), util, rparam);
			boolean hasinsource = false;
			Attr snode = null;
			for(NodeNode snn: snns) {
				if(snn.getNode().equals(tnn.getNode())) {
					hasinsource = true;
					snode = snn.getSubnode();
				}
			}
			
			// If does not have this crossing, simply add
			if(!hasinsource) {
				S3eUtil.logg(S3eConstants.logDebug, "Source has no this node, delete from target", util, rparam);
				util.deleteNode(target, tnn.getNode());
			}
			else {
				S3eUtil.logg(S3eConstants.logDebug, "Source has the same node, compare", util, rparam);
				// Process according to the node type
				int nodetype = S3eMdUtil.getIntNodeType(tnn.getNode(), rparam, util);
				// If or or and rule, recursively check
				if(nodetype == S3eConstants.mdiOr || nodetype == S3eConstants.mdiAnd) {
					S3eUtil.logg(S3eConstants.logDebug, "Node is And/Or, check the subnodes", util, rparam);
					mixIntervals(snode, tnn.getSubnode(), rparam, util);
				}
				// If interval, send to separate processing
				if(nodetype == S3eConstants.mdiInt) {
					S3eUtil.logg(S3eConstants.logDebug, "Node is Interval, handle", util, rparam);
					handleSingleInt(snode, tnn.getSubnode(), rparam, util);
				}
			}
			
		}
		
	}
	
	private static void handleSingleInt(Attr source, Attr target, RuntimeParameters rparam, 
			PluginUtilInterface util) {
		
		S3eUtil.logg(S3eConstants.logDebug, "START handleSingleInt", util, rparam);
		
		/*
		 * Interval Attrs look like
		 * 
		 * 		alert_time		leave the smaller
		 * 		max_time		leave the smaller
		 * 		max_value		N/A
		 * 		min_value		N/A
		 * 		used_time		leave the bigger
		 * 		tasks			handle separate
		 */
		
		ArrayList<NodeValue> snns = util.nodeValueList(source, "", false);
		ArrayList<NodeValue> tnns = util.nodeValueList(target, "", false);
		if(snns == null || snns.size() == 0) {
			S3eUtil.logg(S3eConstants.logDebug, "Source interval empty, clear target", util, rparam);
			target = new Attr();
			return;
		}
		
		// If no target node, do not touch
		if(tnns == null || tnns.size() == 0) {
			S3eUtil.logg(S3eConstants.logDebug, "Target empty, stop", util, rparam);
			return;
		}
		
		long sval = 0L;
		long tval = 0L;
		for(NodeValue snn: snns) {
			S3eUtil.logg(S3eConstants.logDebug, "Compare: " + snn.getNode(), util, rparam);
			for(NodeValue tnn: tnns) {
				if(snn.getNode().equals(tnn.getNode())) {
					if(snn.getNode().equals(rparam.mdfnRuleDetailsAlertTime) 
							|| snn.getNode().equals(rparam.mdfnRuleDetailsMaxTime)) {
						sval = S3eUtil.convertLongStringToMillis(snn.getValue(), rparam, util);
						tval = S3eUtil.convertLongStringToMillis(tnn.getValue(), rparam, util);
						if(sval < tval) {
							S3eUtil.logg(S3eConstants.logDebug, "Source < target, update", util, rparam);
							util.setNodeValue(target, snn.getValue(), tnn.getNode());
						}
					}
					if(snn.getNode().equals(rparam.mdfnRuleDetailsUsedTime)) {
						sval = S3eUtil.convertLongStringToMillis(snn.getValue(), rparam, util);
						tval = S3eUtil.convertLongStringToMillis(tnn.getValue(), rparam, util);
						if(sval > tval) {
							S3eUtil.logg(S3eConstants.logDebug, "Source > target, update", util, rparam);
							util.setNodeValue(target, snn.getValue(), tnn.getNode());
						}
					}
				}
			}
		}
		
		// Tasks handled separate
		Attr stasks = util.getNode(source, rparam.mdfnTasksSection);
		Attr ttasks = util.getNode(target, rparam.mdfnTasksSection);
		handleTasks(stasks, ttasks, rparam, util);
		
	}
	
	private static void handleTasks(Attr source, Attr target, RuntimeParameters rparam, 
			PluginUtilInterface util) {
		S3eUtil.logg(S3eConstants.logDebug, "START handleTasks", util, rparam);
		if(source == null || target == null) return;
		
		// Loop on the task groups
		ArrayList<NodeNode> snns = util.nodeNodeList(source, "", false);
		ArrayList<NodeNode> tnns = util.nodeNodeList(target, "", false);
		if(snns == null || snns.size() == 0) {
			S3eUtil.logg(S3eConstants.logDebug, "Source has no tasks, stop", util, rparam);
			return;
		}
		
		// If no target node, do not touch
		if(tnns == null || tnns.size() == 0) {
			S3eUtil.logg(S3eConstants.logDebug, "Target has no tasks, stop", util, rparam);
			return;
		}
		
		for(NodeNode snn: snns) {
			S3eUtil.logg(S3eConstants.logDebug, "Process node: " + snn.getNode(), util, rparam);
			for(NodeNode tnn: tnns) {
				if(snn.getNode().equals(tnn.getNode())) {
					// A task group can be a collection of single tasks
					// or itself a single task
					// It is single if has a "task_name" field with value
					String tname = util.getNodeValue(tnn.getSubnode(), rparam.mdfnTasksFieldTaskName);
					if(!(tname.equals(""))) {
						S3eUtil.logg(S3eConstants.logDebug, "It's a single task, process", util, rparam);
						handleSingleTask(snn.getSubnode(), tnn.getSubnode(), rparam, util);
					}
					else {
						S3eUtil.logg(S3eConstants.logDebug, "TG is list of tasks, process them", util, rparam);
						ArrayList<NodeNode> snns2 = util.nodeNodeList(snn.getSubnode(), "", false);
						ArrayList<NodeNode> tnns2 = util.nodeNodeList(tnn.getSubnode(), "", false);
						for(NodeNode snn2: snns2) {
							for(NodeNode tnn2: tnns2) {
								if(snn2.getNode().equals(tnn2.getNode())) {
									handleSingleTask(snn2.getSubnode(), tnn2.getSubnode(), rparam, util);
								}
							}
						}
					}
				}
			}
		}
		
	}
	
	private static void handleSingleTask(Attr source, Attr target, RuntimeParameters rparam, 
			PluginUtilInterface util) {
		
		S3eUtil.logg(S3eConstants.logDebug, "START handleSingleTask", util, rparam);
		/*
		 * Task Attrs look like
		 * 
		 * 		alert_time		leave the smaller
		 * 		max_time		leave the smaller
		 * 		min_time		leave the bigger
		 * 		repeatable		N/A
		 * 		task_name		N/A
		 * 		used_time		leave the bigger
		 */
		
		ArrayList<NodeValue> snns = util.nodeValueList(source, "", false);
		ArrayList<NodeValue> tnns = util.nodeValueList(target, "", false);
		if(snns == null || snns.size() == 0) {
			S3eUtil.logg(S3eConstants.logDebug, "Source has no this task, stop", util, rparam);
			return;
		}
		
		// If no target node, do not touch
		if(tnns == null || tnns.size() == 0) {
			S3eUtil.logg(S3eConstants.logDebug, "Target has no this task, stop", util, rparam);
			return;
		}
		
		long sval = 0L;
		long tval = 0L;
		for(NodeValue snn: snns) {
			S3eUtil.logg(S3eConstants.logDebug, "Processing node: " + snn.getNode(), util, rparam);
			for(NodeValue tnn: tnns) {
				if(snn.getNode().equals(tnn.getNode())) {
					if(snn.getNode().equals(rparam.mdfnRuleDetailsAlertTime) 
							|| snn.getNode().equals(rparam.mdfnRuleDetailsMaxTime)) {
						sval = S3eUtil.convertLongStringToMillis(snn.getValue(), rparam, util);
						tval = S3eUtil.convertLongStringToMillis(tnn.getValue(), rparam, util);
						if(sval < tval) {
							S3eUtil.logg(S3eConstants.logDebug, "Source < target, update", util, rparam);
							util.setNodeValue(target, snn.getValue(), tnn.getNode());
						}
					}
					if(snn.getNode().equals(rparam.mdfnRuleDetailsUsedTime) 
							|| snn.getNode().equals(rparam.mdfnRuleDetailsMinTime)) {
						sval = S3eUtil.convertLongStringToMillis(snn.getValue(), rparam, util);
						tval = S3eUtil.convertLongStringToMillis(tnn.getValue(), rparam, util);
						if(sval > tval) {
							S3eUtil.logg(S3eConstants.logDebug, "Source > target, update", util, rparam);
							util.setNodeValue(target, snn.getValue(), tnn.getNode());
						}
					}
				}
			}
		}
	}
	
	private static void mixFlexi(Pot source, Pot target, RuntimeParameters rparam, PluginUtilInterface util) {
		
		S3eUtil.logg(S3eConstants.logDebug, "START mixFlexi", util, rparam);
		
		Attr sattr = source.getFlexi();
		Attr tattr = target.getFlexi();
		
		// Handle status
		String sst = util.getNodeValue(sattr, rparam.gentextStatus);
		String tst = util.getNodeValue(tattr, rparam.gentextStatus);
		if(sst.equals(rparam.gentextStatusError)) {
			S3eUtil.logg(S3eConstants.logDebug, "Source status Error, update", util, rparam);
			util.setNodeValue(tattr, rparam.gentextStatusError, rparam.gentextStatus);
		}
		else {
			if(sst.equals(rparam.gentextStatusWarning) && (!tst.equals(rparam.gentextStatusError))) {
				S3eUtil.logg(S3eConstants.logDebug, "Source status Warning, update", util, rparam);
				util.setNodeValue(tattr, rparam.gentextStatusWarning, rparam.gentextStatus);
			}
		}
		
		// Handle last error
		Attr slerr = util.getNode(sattr, rparam.gentextLastError);
		Attr tlerr = util.getNode(tattr, rparam.gentextLastError);
		Instant serrt = S3eUtil.convertTimestampStringToInstant(
				util.getNodeValue(slerr, rparam.gentextMessageTime), util);
		Instant terrt = S3eUtil.convertTimestampStringToInstant(
				util.getNodeValue(tlerr, rparam.gentextMessageTime), util);
		if(serrt.isAfter(terrt)) {
			S3eUtil.logg(S3eConstants.logDebug, "Last error message source newer, update", util, rparam);
			util.setNodeValue(tlerr, 
					util.getNodeValue(slerr, rparam.gentextMessageTime), rparam.gentextMessageTime);
			util.setNodeValue(tlerr, 
					util.getNodeValue(slerr, rparam.gentextMessageText), rparam.gentextMessageText);
		}
		// Handle last message
		Attr slmsg = util.getNode(sattr, rparam.gentextLastMessage);
		Attr tlmsg = util.getNode(tattr, rparam.gentextLastMessage);
		Instant smsgt = S3eUtil.convertTimestampStringToInstant(
				util.getNodeValue(slmsg, rparam.gentextMessageTime), util);
		Instant tmsgt = S3eUtil.convertTimestampStringToInstant(
				util.getNodeValue(tlmsg, rparam.gentextMessageTime), util);
		if(smsgt.isAfter(tmsgt)) {
			S3eUtil.logg(S3eConstants.logDebug, "Last message source newer, update", util, rparam);
			util.setNodeValue(tlmsg, 
					util.getNodeValue(slmsg, rparam.gentextMessageTime), rparam.gentextMessageTime);
			util.setNodeValue(tlmsg, 
					util.getNodeValue(slmsg, rparam.gentextMessageText), rparam.gentextMessageText);
			util.setNodeValue(tlmsg, 
					util.getNodeValue(slmsg, rparam.gentextMessageSeverity), rparam.gentextMessageSeverity);
		}
		
	}
	
	private static ArrayList<SinglePotWithStatus> getSourcePots(PluginData data, RuntimeParameters rparam, PluginUtilInterface util) {
		ArrayList<SinglePotWithStatus> ret = new ArrayList<>();
		
		Attr attr = data.ball.getAttr(); // not null, checked already
		ArrayList<NodeNode> nns = util.nodeNodeList(attr, "", false);
		for(NodeNode nn: nns) {
			SinglePotWithStatus sps = new SinglePotWithStatus();
			boolean goodnode = false;
			ArrayList<NodeValue> nvs = util.nodeValueList(nn.getSubnode(), "", false);
			for(NodeValue nv: nvs) {
				if(nv.getNode().equals(S3eConstants.POTID)) {
					sps.setPot(util.getPot(nv.getValue()));
					goodnode = true;
				}
				if(nv.getNode().equals(S3eConstants.STATUS)) sps.setNewstatus(nv.getValue());
			}
			if(goodnode) ret.add(sps);
		}
		
		return ret;
	}

}
