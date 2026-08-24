package sygr3em.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import sygr.pots.extensions.Attr;
import sygr.pots.extensions.Creator;
import sygr.pots.extensions.ExtConstants;
import sygr.pots.extensions.ExtraMatch;
import sygr.pots.extensions.PluginData;
import sygr.pots.extensions.PluginUtilInterface;
import sygr.pots.extensions.Pot;
import sygr.pots.extensions.PotType;
import sygr.pots.extensions.Updater;
import sygr.pots.extensions.Uuid;
import sygr3em.model.InboundMessage;
import sygr3em.model.MultiProperty;
import sygr3em.model.MultiTelemetry;
import sygr3em.model.RuntimeParameters;
import sygr3em.model.SingleCharacteristic;
import sygr3em.model.SingleMerge;
import sygr3em.model.SinglePotIdWithStoretype;
import sygr3em.model.SingleProperty;
import sygr3em.model.SingleTelemetry;

public class S3eImsgUtil {
	
	public static void processUpdateUnit(InboundMessage msg, PluginUtilInterface util,
			RuntimeParameters rparams, HashMap<String, ArrayList<PotType>> pottypes,
			PluginData data) {
		addUpdaterUpdateUnit(msg.getCommand(), msg, util, rparams, data);
	}
	
	public static void processMerge(InboundMessage msg, PluginUtilInterface util,
			RuntimeParameters rparams, HashMap<String, ArrayList<PotType>> pottypes,
			PluginData data, boolean checkexistence) {
		
		addUpdaterMerge(msg.getCommand(), msg, util, rparams, data, checkexistence);
		
	}
	
	public static void processSplit(InboundMessage msg, PluginUtilInterface util,
			RuntimeParameters rparams, HashMap<String, ArrayList<PotType>> pottypes,
			PluginData data, boolean checkexistence) {
		
		addUpdaterSplit(msg.getCommand(), msg, util, rparams, data, checkexistence);
		
	}
	
	public static void processIds(InboundMessage msg, PluginUtilInterface util,
			RuntimeParameters rparams, HashMap<String, ArrayList<PotType>> pottypes,
			PluginData data, boolean checkexistence) {
		
		addUpdaterIds(msg.getCommand(), msg, util, rparams, data, checkexistence);
		
	}
	
	public static void processSetPotStatus(InboundMessage msg, PluginUtilInterface util,
			RuntimeParameters rparams, HashMap<String, ArrayList<PotType>> pottypes,
			PluginData data) {
		
		addUpdaterPotStatusChange(msg.getCommand(), msg, util, rparams, data, "");
		
	}
	
	public static void processSetTask(InboundMessage msg, PluginUtilInterface util,
			RuntimeParameters rparams, HashMap<String, ArrayList<PotType>> pottypes,
			PluginData data) {
		
		addUpdaterSetTask(msg.getCommand(), msg, util, rparams, data);
		
	}
	
	public static void processUpdateSecAttr(InboundMessage msg, PluginUtilInterface util,
			RuntimeParameters rparams, HashMap<String, ArrayList<PotType>> pottypes,
			PluginData data) {
		
		addUpdaterUpdateSecAttr(msg, util, rparams, data);
		
	}
	
	public static void processTelemetry(InboundMessage msg, PluginUtilInterface util,
			RuntimeParameters rparams, HashMap<String, ArrayList<PotType>> pottypes,
			PluginData data) {
		
		if(msg == null || util == null || rparams == null || pottypes == null
				|| data == null || data.messageOut == null) return;
		if(msg.getId() == null) {
			S3eUtil.logg(S3eConstants.logError, "Telemetry: no ID", util, rparams);
			S3eUtil.sendAlert(ExtConstants.alertseverityERROR, "", "", "", "Telemetry without ID", 
					"Telemetry message arrived without ID.", rparams, util);
			return;
		}
		SingleProperty id = msg.getId();
		
		if(msg.getMeasurements() == null || msg.getMeasurements().size() == 0) {
			S3eUtil.logg(S3eConstants.logError, "Telemetry no measurements", util, rparams);
			S3eUtil.sendAlert(ExtConstants.alertseverityERROR, "", id.getKey(), id.getValue(), "Telemetry without measurements", 
					"Telemetry message arrived without measurements.", rparams, util);
			return;
		}
		ArrayList<MultiTelemetry> measurements = msg.getMeasurements();
		
		// LOG
		S3eUtil.logg(S3eConstants.logDebug, "Telemetry for: " + id.getKey() + " = " + id.getValue(), util, rparams);
		for(MultiTelemetry multi: measurements) {
			S3eUtil.logg(S3eConstants.logDebug, "   Time: " + multi.getTimestamp().toString(), util, rparams);
			if(multi.getList() == null || multi.getList().size() == 0) {
				S3eUtil.logg(S3eConstants.logDebug, "      No measurements.", util, rparams);
			}
			else {
				for(SingleTelemetry st: multi.getList()) {
					S3eUtil.logg(S3eConstants.logDebug, "      " 
							+ st.getC12c() + ": " 
							+ st.getValue() + " " + st.getUom(), util, rparams);
				}
			}
		}
		
		// Create Updater
		addUpdaterTelemetry(msg, util, rparams, data);
		
	}

	public static void processCreateNew(InboundMessage msg, PluginUtilInterface util,
			RuntimeParameters rparams, HashMap<String, ArrayList<PotType>> pottypes,
			PluginData data) {
		
		Creator creator = new Creator();
		
		String newPotSecondId = Uuid.getUuid();
		
		if(msg == null || util == null || rparams == null || pottypes == null
				|| data == null || data.messageOut == null) return;
		
		// First of all check whether a Pot exists already with the same primary ID.
		ArrayList<SinglePotIdWithStoretype> allpotids 
			= getExistingPots(msg.getId().getKey(), msg.getId().getValue(), rparams, util);
		
		// If found existing, it will depend on the command
		if(!(allpotids == null) && allpotids.size() > 0) {
			S3eUtil.logg(S3eConstants.logDebug, "Found active Pots: " + allpotids.size(), util, rparams);
			// If create new command we DO NOT create new pot, but alert
			if(msg.getCommand().equals(rparams.imsgcommandCreateNew)) {
				S3eUtil.logg(S3eConstants.logDebug, "Command: new, send alert.", util, rparams);
				S3eUtil.alertPotExistsAlready(msg.getId().getKey(), 
						msg.getId().getValue(), rparams, util);
				return;
			}
			// If create/replace, we need to delete the existing ones
			if(msg.getCommand().equals(rparams.imsgcommandCreateReplace)) {
				addUpdaterPotStatusChange(rparams.imsgcommandSetDeleted, msg, util, rparams, data, newPotSecondId);
				S3eUtil.logg(S3eConstants.logDebug, "Command: replace, create deletion Ball.", util, rparams);
			}
		}
		
		S3eUtil.logg(S3eConstants.logDebug, "START creating Creator", util, rparams);
		
		// Basic info
		creator.setType(rparams.createnewPotType);
		S3eUtil.logg(S3eConstants.logDebug, "Pot type = " + rparams.createnewPotType, util, rparams);
		creator.setInitplugin(rparams.createnewInitPlugin);
		S3eUtil.logg(S3eConstants.logDebug, "Init plugin = " + rparams.createnewInitPlugin, util, rparams);
		
		// Main identifier
		if(msg.getId() == null || msg.getId().getKey() == null || msg.getId().getValue() == null) return;
		creator.setMatchkey0(msg.getId().getKey());
		creator.setMatchval0(msg.getId().getValue());
		
		// Set the signature and protection ID
		creator.setProp0(S3eConstants.SIGNATURE);
		creator.setProp1(newPotSecondId);
		
		// Secondary identifiers will be matchcodes
		if(!(msg.getSecondaryIds() == null) && !(msg.getSecondaryIds().getList() == null) 
				&& msg.getSecondaryIds().getList().size() > 0) {
			ArrayList<ExtraMatch> ems = new ArrayList<>();
			for(SingleProperty secid: msg.getSecondaryIds().getList()) {
				ExtraMatch em = new ExtraMatch();
				em.setMatchkey(secid.getKey());
				em.setMatchval(secid.getValue());
				ems.add(em);
			}
			creator.setExtramatches(ems);
		}
		
		// Create the transactional data
		Attr attr = new Attr();
		
		// For sure we need an Attr for parameters, and we prefill
		// Also prepare the master data decision
		Attr pattr = new Attr();
		MultiProperty mddec = new MultiProperty();

		// Target Pot Type
		util.setNodeValue(pattr, "", rparams.createnewFnameTargetPt);
		SingleProperty sp0 = new SingleProperty();
		sp0.setKey(rparams.createnewFnameTargetPt);
		mddec.getList().add(sp0);
		
		// And need one target budget type for every environmental variable
		if(!(rparams.characteristics == null) && rparams.characteristics.size() > 0) {
			for(SingleCharacteristic sc: rparams.characteristics) {
				util.setNodeValue(pattr, "", sc.getMdfield());
				SingleProperty sp1 = new SingleProperty();
				sp1.setKey(sc.getMdfield());
				mddec.getList().add(sp1);
			}
		}
		// Then let's see what other parameters we have
		if(!(msg.getParameters() == null) && !(msg.getParameters().getList() == null)
				&& msg.getParameters().getList().size() > 0) {
			for(SingleProperty sc: msg.getParameters().getList()) {
				util.setNodeValue(pattr, sc.getValue(), sc.getKey());
				// If it's one of our stuff, modify the value
				for(SingleProperty sp: mddec.getList()) {
					if(sc.getKey().equals(sp.getKey())) {
						sp.setValue(sc.getValue());
					}
				}
			}
		}
		
		// HERE comes the trick: we try to determine the pot type and master data
		// based on decision Pot Type.
		if(S3eUtil.determineMdFromPt(mddec, msg.getPrimaryAttributes(), rparams, pottypes, util)) {
						
			// Take the results
			for(SingleProperty sp: mddec.getList()) {
				util.setNodeValue(pattr, sp.getValue(), sp.getKey());
			}			
			
			// Send note to the Pot Init Plugin that it can be completed
			creator.setMatchkey1(S3eConstants.FINAL);
		}
		
		// And add it to the main Attr
		util.setNode(attr, pattr, rparams.createnewFnameParams);
		
		// Create primary attributes
		Attr prattr = S3eUtil.createPropertyAttr(msg.getPrimaryAttributes(), rparams, util);
		util.setNode(attr, prattr, rparams.createnewFnamePrimAtt);
		
		// Create secondary attributes
		Attr scattr = S3eUtil.createPropertyAttr(msg.getSecondaryAttributes(), rparams, util);;
		util.setNode(attr, scattr, rparams.createnewFnameSecAtt);
		 
		creator.setAttr(attr);
		data.messageOut.addCreator(creator);
	}
	
	public static void addUpdaterPotStatusChange(String command, InboundMessage msg, PluginUtilInterface util,
	RuntimeParameters rparams, 	PluginData data, String exceptionid) {
		
		Attr attr = new Attr();
		util.setNodeValue(attr, command, S3eConstants.attrlocCommand);
		util.setNodeValue(attr, msg.getId().getKey(), S3eConstants.attrlocKey);
		util.setNodeValue(attr, msg.getId().getValue(), S3eConstants.attrlocValue);
		util.setNodeValue(attr, exceptionid, S3eConstants.attrlocExceptionId);
		
		Updater updater = new Updater();
		updater.setType(rparams.balltypeCommand);
		updater.setMatchkey(msg.getId().getKey());
		updater.setMatchval(msg.getId().getValue());
		
		if(command.equals(rparams.imsgcommandSetActive)) updater.setUsage(ExtConstants.ballusageWAKEUP);
		
		updater.setAttr(attr);
		data.messageOut.addUpdater(updater);
	}
	
	public static void addUpdaterSetTask(String command, InboundMessage msg, PluginUtilInterface util,
			RuntimeParameters rparams, 	PluginData data) {
				
		// If we did not receive timestamp, suppose it's now
		if(msg.getTimestamp().equals(Instant.ofEpochMilli(0))) msg.setTimestamp(Instant.now());
		
		Attr attr = new Attr();
		util.setNodeValue(attr, command, S3eConstants.attrlocCommand);
		util.setNodeValue(attr, msg.getId().getKey(), S3eConstants.attrlocKey);
		util.setNodeValue(attr, msg.getId().getValue(), S3eConstants.attrlocValue);
		util.setNodeValue(attr, msg.getTask(), S3eConstants.attrlocTask);
		util.setNodeValue(attr, 
				S3eUtil.convertInstantToNumericString(msg.getTimestamp()), S3eConstants.attrlocTimestamp);
				
		Updater updater = new Updater();
		updater.setType(rparams.balltypeCommand);
		updater.setMatchkey(msg.getId().getKey());
		updater.setMatchval(msg.getId().getValue());
				
		updater.setAttr(attr);
		data.messageOut.addUpdater(updater);
	}
	
	public static void addUpdaterTelemetry(InboundMessage msg, PluginUtilInterface util,
			RuntimeParameters rparams, 	PluginData data) {
				
		Attr attr = new Attr();
		util.setNodeValue(attr, rparams.imsgcommandTelemetry, S3eConstants.attrlocCommand);
		util.setNodeValue(attr, msg.getId().getKey(), S3eConstants.attrlocKey);
		util.setNodeValue(attr, msg.getId().getValue(), S3eConstants.attrlocValue);
		
		// Make a node for the measurements
		Attr msm = copyMeasurementsToAttr(msg.getMeasurements(), util);
		if(!(msm == null)) {
			util.setNode(attr, msm, S3eConstants.MEASUREMENTS);
		}
		
		// Add the secondary attributes
		Attr scattr = S3eUtil.createPropertyAttr(msg.getSecondaryAttributes(), rparams, util);
		util.setNode(attr, scattr, rparams.createnewFnameSecAtt);
		
		Updater updater = new Updater();
		updater.setType(rparams.balltypeCommand);
		updater.setMatchkey(msg.getId().getKey());
		updater.setMatchval(msg.getId().getValue());
		
		updater.setAttr(attr);
		data.messageOut.addUpdater(updater);
	}
	
	public static void addUpdaterUpdateSecAttr(InboundMessage msg, PluginUtilInterface util,
			RuntimeParameters rparams, 	PluginData data) {
				
		Attr attr = new Attr();
		util.setNodeValue(attr, msg.getCommand(), S3eConstants.attrlocCommand);
		util.setNodeValue(attr, msg.getId().getKey(), S3eConstants.attrlocKey);
		util.setNodeValue(attr, msg.getId().getValue(), S3eConstants.attrlocValue);
		
		// Add the secondary attributes
		Attr scattr = S3eUtil.createPropertyAttr(msg.getSecondaryAttributes(), rparams, util);
		util.setNode(attr, scattr, rparams.createnewFnameSecAtt);
		
		// Add the deletion flag
		if(msg.getSecondaryAttributes().getDeleteexising()) {
			util.setNodeValue(attr, S3eConstants.textTrue, S3eConstants.DELETE);
		}
		else {
			util.setNodeValue(attr, S3eConstants.textFalse, S3eConstants.DELETE);
		}
		
		Updater updater = new Updater();
		updater.setType(rparams.balltypeCommand);
		updater.setMatchkey(msg.getId().getKey());
		updater.setMatchval(msg.getId().getValue());
		
		updater.setAttr(attr);
		data.messageOut.addUpdater(updater);
	}
	
	public static Attr copyMeasurementsToAttr(ArrayList<MultiTelemetry> mts, PluginUtilInterface util) {
		Attr ret = new Attr();
		if(mts == null || mts.size() == 0) return ret;
		
		// Sort by timestamp
		try {
			Collections.sort(mts);
		} catch(Exception e) {}
		
		int counter = 10000;
		// Create one subnode per timestamp
		for(MultiTelemetry mt: mts) {
			Attr tnode = new Attr();
			util.setNodeValue(tnode, 
					S3eUtil.convertInstantToNumericString(mt.getTimestamp()), 
					S3eConstants.TIMESTAMP);
			// then add one line per data. We put the uom to the same value in form
			// 12345,67;C
			if(!(mt.getList() == null) && mt.getList().size() > 0) {
				for(SingleTelemetry st: mt.getList()) {
					util.setNodeValue(tnode, 
							st.getValue() + S3eConstants.UOMSEPARATOR 
							+ st.getUom() + S3eConstants.UOMSEPARATOR, st.getC12c());
				}
				String nname = S3eConstants.TMLISTNODE + counter++;
				util.setNode(ret, tnode, nname);
			}
			
		}
		
		return ret;
	}
	
	public static ArrayList<SinglePotIdWithStoretype> getExistingPots(String key, String value, RuntimeParameters rparams, PluginUtilInterface util) {
		ArrayList<SinglePotIdWithStoretype> allpotids = new ArrayList<>();
		
		if(rparams.pottypes == null || rparams.pottypes.size() == 0) {
			S3eUtil.logg(S3eConstants.logError, "No pot types defined.", util, rparams);
			S3eUtil.alertSetupIncorrect("No pot types defined for the system.", rparams, util);
			return allpotids;
		}
		S3eUtil.logg(S3eConstants.logDebug, "Start finding existing Pots", util, rparams);
		S3eUtil.logg(S3eConstants.logDebug, "Key = " + key, util, rparams);
		S3eUtil.logg(S3eConstants.logDebug, "Value = " + value, util, rparams);
		
		S3eUtil.logg(S3eConstants.logDebug, "POTTYPE LIST:" + value, util, rparams);
		for(String pt: rparams.pottypes) {
			S3eUtil.logg(S3eConstants.logDebug, "PT = " + pt, util, rparams);
		}
		
		S3eUtil.logg(S3eConstants.logDebug, "And now start to search" + value, util, rparams);
		for(String pt: rparams.pottypes) {
			S3eUtil.logg(S3eConstants.logDebug, "Search Pot Type " + pt, util, rparams);
			
			// Here must be very careful:
			// We need only pots, that has prop0 = the signature.
			// But if we select directly, we get everything, because it's a matching, so
			// props are selected by OR.
			ArrayList<String> potids = util.findPotIds(util.getStoreTypeOfPotType(pt), pt, key, 
					value,
					//S3eConstants.SIGNATURE, "", "", "", "", "", "", "", "", "",
					"", "", "", "", "", "", "", "", "", "",
					 ExtConstants.potstatusACTIVE);
			if(!(potids == null) && potids.size() > 0) {
				S3eUtil.logg(S3eConstants.logDebug, "Found pots: " + potids.size(), util, rparams);
				S3eUtil.logg(S3eConstants.logDebug, "Try to find the real pots", util, rparams);
				for(String potid: potids) {
					Pot suspiciouspot = util.getPot(util.getStoreTypeOfPotType(pt), potid);
					S3eUtil.logg(S3eConstants.logDebug, "Pot ID: " + potid, util, rparams);
					S3eUtil.logg(S3eConstants.logDebug, "Pot Type: " + pt, util, rparams);
					S3eUtil.logg(S3eConstants.logDebug, "Store type: " + util.getStoreTypeOfPotType(pt), util, rparams);
					if(!(suspiciouspot == null) && suspiciouspot.getProp0().equals(S3eConstants.SIGNATURE)) {
						S3eUtil.logg(S3eConstants.logDebug, "Found, fine", util, rparams);
						SinglePotIdWithStoretype spst = new SinglePotIdWithStoretype();
						spst.setPotid(potid);
						spst.setStoretype(util.getStoreTypeOfPotType(pt));
						allpotids.add(spst);
					}
					else {
						S3eUtil.logg(S3eConstants.logDebug, "NOT FOUND", util, rparams);
					}
				}
			}
		}
		S3eUtil.logg(S3eConstants.logDebug, "Total Pots found: " + allpotids.size(), util, rparams);
		return allpotids;
		
	}
	
	public static void addUpdaterMerge(String command, InboundMessage msg, PluginUtilInterface util,
			RuntimeParameters rparams, 	PluginData data, boolean checkexistence) {
		
		// Check that the target exists
		
		if(checkexistence) {
			ArrayList<SinglePotIdWithStoretype> targetpots = getExistingPots(msg.getId().getKey(), msg.getId().getValue(), rparams, util);
			if(targetpots == null || targetpots.size() != 1) {
				String text = "Merge target " + msg.getId().getKey() + 
						" = " + msg.getId().getValue() +
						" does not exists, cannot merge."; 
				S3eUtil.logg(S3eConstants.logError, 
						text, util, rparams);
				S3eUtil.sendAlert(ExtConstants.alertseverityERROR, "", 
						msg.getId().getKey(), msg.getId().getValue(), 
						"Merge target missing", text, rparams, util);
				return;
			}
		}
				
		Attr attr = new Attr();
		util.setNodeValue(attr, command, S3eConstants.attrlocCommand);
		util.setNodeValue(attr, msg.getId().getKey(), S3eConstants.attrlocKey);
		util.setNodeValue(attr, msg.getId().getValue(), S3eConstants.attrlocValue);
				
		// Put the sources into the Attr too
		if(msg.getMerges() == null || msg.getMerges().getList() == null 
				|| msg.getMerges().getList().size() == 0) {
			String text = "Merge target " + msg.getId().getKey() + 
					" = " + msg.getId().getValue() +
					" no source units arrived."; 
			S3eUtil.logg(S3eConstants.logError, 
					text, util, rparams);
			S3eUtil.sendAlert(ExtConstants.alertseverityERROR, "", 
					msg.getId().getKey(), msg.getId().getValue(), 
					"Merge sources missing", text, rparams, util);
			return;
		}
		int count = 10000;
		for(SingleMerge sm: msg.getMerges().getList()) {
			ArrayList<SinglePotIdWithStoretype> sources = getExistingPots(sm.getKey(), sm.getValue(), rparams, util);
			if(!(sources == null) && sources.size() == 1) {
				try {
					String potid = sources.get(0).getPotid();
					String nid = S3eConstants.MERGESOURCENODE + count++ + ".";
					util.setNodeValue(attr, potid, nid + S3eConstants.POTID);
					util.setNodeValue(attr, sm.getStatus(), nid + S3eConstants.STATUS);
				} catch(Exception e) {}
			}
		}
		
		Updater updater = new Updater();
		updater.setType(rparams.balltypeCommand);
		updater.setMatchkey(msg.getId().getKey());
		updater.setMatchval(msg.getId().getValue());
				
		updater.setAttr(attr);
		data.messageOut.addUpdater(updater);
	}
	
	public static void addUpdaterSplit(String command, InboundMessage msg, PluginUtilInterface util,
			RuntimeParameters rparams, 	PluginData data, boolean checkexistence) {
		
		// Check that the source exists
		
		if(checkexistence) {
			ArrayList<SinglePotIdWithStoretype> sourcepots = getExistingPots(msg.getId().getKey(), msg.getId().getValue(), rparams, util);
			if(sourcepots == null || sourcepots.size() != 1) {
				String text = "Split source " + msg.getId().getKey() + 
						" = " + msg.getId().getValue() +
						" does not exists, cannot split."; 
				S3eUtil.logg(S3eConstants.logError, 
						text, util, rparams);
				S3eUtil.sendAlert(ExtConstants.alertseverityERROR, "", 
						msg.getId().getKey(), msg.getId().getValue(), 
						"Split source missing", text, rparams, util);
				return;
			}
		}
				
		Attr attr = new Attr();
		util.setNodeValue(attr, command, S3eConstants.attrlocCommand);
		util.setNodeValue(attr, msg.getId().getKey(), S3eConstants.attrlocKey);
		util.setNodeValue(attr, msg.getId().getValue(), S3eConstants.attrlocValue);
				
		// Put the targets into the Attr too
		if(msg.getSplit() == null || msg.getSplit().getIds() == null
				|| msg.getSplit().getIds().getList() == null
				|| msg.getSplit().getIds().getList().size() == 0) {
			String text = "Split source " + msg.getId().getKey() + 
					" = " + msg.getId().getValue() +
					" no target units arrived."; 
			S3eUtil.logg(S3eConstants.logError, 
					text, util, rparams);
			S3eUtil.sendAlert(ExtConstants.alertseverityERROR, "", 
					msg.getId().getKey(), msg.getId().getValue(), 
					"Split targets missing", text, rparams, util);
			return;
		}
		
		util.setNodeValue(attr, msg.getSplit().getStatus(), S3eConstants.STATUS);
		
		int count = 10000;
		for(SingleProperty sp: msg.getSplit().getIds().getList()) {
			ArrayList<SinglePotIdWithStoretype> targets = getExistingPots(sp.getKey(), sp.getValue(), rparams, util);
			if(!(targets == null) && targets.size() > 0) {
				String text = "Split target " + sp.getKey() + 
						" = " + sp.getValue() +
						" exists already, cannot split into."; 
				S3eUtil.logg(S3eConstants.logError, 
						text, util, rparams);
				S3eUtil.sendAlert(ExtConstants.alertseverityERROR, "", 
						msg.getId().getKey(), msg.getId().getValue(), 
						"Split target exists already", text, rparams, util);
			}
			else {
				String nid = S3eConstants.SPLITTARGETNODE + count++ + ".";
				util.setNodeValue(attr, sp.getKey(), nid + S3eConstants.KEY);
				util.setNodeValue(attr, sp.getValue(), nid + S3eConstants.VALUE);
				
			}
		}
		
		Updater updater = new Updater();
		updater.setType(rparams.balltypeCommand);
		updater.setMatchkey(msg.getId().getKey());
		updater.setMatchval(msg.getId().getValue());
				
		updater.setAttr(attr);
		data.messageOut.addUpdater(updater);
	}
	
	public static void addUpdaterIds(String command, InboundMessage msg, PluginUtilInterface util,
			RuntimeParameters rparams, 	PluginData data, boolean checkexistence) {
		
		// Check that the source exists
		
		if(checkexistence) {
			ArrayList<SinglePotIdWithStoretype> sourcepots = getExistingPots(msg.getId().getKey(), msg.getId().getValue(), rparams, util);
			if(sourcepots == null || sourcepots.size() != 1) {
				String text = "Add/remove ID source " + msg.getId().getKey() + 
						" = " + msg.getId().getValue() +
						" does not exists, cannot split."; 
				S3eUtil.logg(S3eConstants.logError, 
						text, util, rparams);
				S3eUtil.sendAlert(ExtConstants.alertseverityERROR, "", 
						msg.getId().getKey(), msg.getId().getValue(), 
						"Add/remove ID source missing", text, rparams, util);
				return;
			}
		}
				
		Attr attr = new Attr();
		util.setNodeValue(attr, command, S3eConstants.attrlocCommand);
		util.setNodeValue(attr, msg.getId().getKey(), S3eConstants.attrlocKey);
		util.setNodeValue(attr, msg.getId().getValue(), S3eConstants.attrlocValue);
				
		// Put the IDs into the Attr too
		if(msg.getSecondaryIds() == null || msg.getSecondaryIds().getList() == null
				|| msg.getSecondaryIds().getList().size() == 0) {
			String text = "Add/remove IDs source " + msg.getId().getKey() + 
					" = " + msg.getId().getValue() +
					" no IDs arrived."; 
			S3eUtil.logg(S3eConstants.logError, 
					text, util, rparams);
			S3eUtil.sendAlert(ExtConstants.alertseverityERROR, "", 
					msg.getId().getKey(), msg.getId().getValue(), 
					"Add/remove IDs missing", text, rparams, util);
			return;
		}
		
		int count = 10000;
		for(SingleProperty sp: msg.getSecondaryIds().getList()) {
			String nid = S3eConstants.IDNODE + count++ + ".";
			util.setNodeValue(attr, sp.getKey(), nid + S3eConstants.KEY);
			util.setNodeValue(attr, sp.getValue(), nid + S3eConstants.VALUE);
		}
		
		Updater updater = new Updater();
		updater.setType(rparams.balltypeCommand);
		updater.setMatchkey(msg.getId().getKey());
		updater.setMatchval(msg.getId().getValue());
				
		updater.setAttr(attr);
		data.messageOut.addUpdater(updater);
	}
	
	public static void addUpdaterUpdateUnit(String command, InboundMessage msg, PluginUtilInterface util,
			RuntimeParameters rparams, 	PluginData data) {
		
		// Get the pot
		ArrayList<SinglePotIdWithStoretype> potids 
			= S3eImsgUtil.getExistingPots(msg.getId().getKey(), msg.getId().getValue(), rparams, util);
		if(potids == null || potids.size() == 0) {
			return;
		}
				
		if(potids.size() > 1) {
			return;
		}
				
		SinglePotIdWithStoretype ourpot = null;
		try {
			ourpot = potids.get(0);
		} catch(Exception e) {}
		if(ourpot == null) {
			return;
		}
				
		// Get the Pot
		Pot pot = util.getPot(ourpot.getStoretype(), ourpot.getPotid());
		if(pot == null) {
			return;
		}
		if(!(util.fits(data.webid, ExtConstants.authMAINTAINPT, pot.getType()))) {
			return;
		}
		if(!(rparams.autheditunit.equals(""))) {
			if(!(util.fits(data.webid, ExtConstants.authMAINTAINPT, rparams.autheditunit))) {
				return;
			}
		}
		
		String subject = "Manual modification";
		String text = "Unit " + pot.getMatchkey0() + " = " + pot.getMatchval0() + " was manually modified." + System.lineSeparator();
		text = text + "User = " + data.webid.getUsername();
		S3eUtil.sendAlert(ExtConstants.alertseverityINFO, pot.getType(), pot.getMatchkey0(), pot.getMatchval0(), subject, 
					text, rparams, util);
						
		Attr attr = new Attr();
		util.setNodeValue(attr, command, S3eConstants.attrlocCommand);
		util.setNodeValue(attr, msg.getId().getKey(), S3eConstants.attrlocKey);
		util.setNodeValue(attr, msg.getId().getValue(), S3eConstants.attrlocValue);
		util.setNode(attr, msg.getFixed(), S3eConstants.FIXED);
		util.setNode(attr, msg.getFlexi(), S3eConstants.FLEXI);
				
		Updater updater = new Updater();
		updater.setType(rparams.balltypeCommand);
		updater.setMatchkey(msg.getId().getKey());
		updater.setMatchval(msg.getId().getValue());
				
		updater.setAttr(attr);
		data.messageOut.addUpdater(updater);
	}
	
}
