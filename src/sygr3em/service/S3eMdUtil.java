package sygr3em.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import sygr.pots.extensions.Attr;
import sygr.pots.extensions.ExtConstants;
import sygr.pots.extensions.NodeNode;
import sygr.pots.extensions.NodeValue;
import sygr.pots.extensions.PluginUtilInterface;
import sygr.pots.extensions.PotType;
import sygr.pots.extensions.ValueObject;
import sygr.pots.extensions.WebId;
import sygr3em.model.RuntimeParameters;
import sygr3em.model.SingleCharacteristic;
import sygr3em.model.SingleInterval;
import sygr3em.model.SingleLocMessage;

public class S3eMdUtil {
	
	public static ArrayList<SingleLocMessage> testMasterDataLink(String pottype,
			HashMap<String, ArrayList<PotType>> pottypes, 
			RuntimeParameters rparam, PluginUtilInterface util, WebId webid){
		ArrayList<SingleLocMessage> ret = new ArrayList<>();
		
		S3eUtil.logg(S3eConstants.logDebug, "START: test master data link pot type " 
				+ pottype, util, rparam);
		
		SingleLocMessage reterror = new SingleLocMessage();
		reterror.setLocation("ERROR");
		if(pottype == null || pottype.equals("")) {
			S3eUtil.logg(S3eConstants.logDebug, "Pot type is invalid.", util, rparam);
			reterror.setText("Invalid Pot Type.");
			ret.add(reterror);
			return ret;
		}
		
		// Check authorization
		if(!(util.fits(webid, ExtConstants.authMAINTAINPT, pottype))) {
			SingleLocMessage slm = new SingleLocMessage();
			slm.setLocation(S3eConstants.UNAUTHORIZED);
			slm.setText(pottype);
			ret.add(slm);
			return ret;
		}
		
		// Read the pot type
		ArrayList<PotType> pts = pottypes.get(S3eConstants.MDDETHMKEY);
		if(pts == null || pts.size() == 0) {
			SingleLocMessage slm = new SingleLocMessage();
			slm.setLocation("ERROR");
			slm.setText("No master data links defined at all.");
			ret.add(slm);
			return ret;
		}
		PotType pt = null;
		for(PotType pp: pts) {
			if(pp.getType().equals(pottype)) {
				pt = pp;
			}
		}
		if(pt == null || pt.getFixed() == null) {
			SingleLocMessage slm = new SingleLocMessage();
			slm.setLocation("ERROR");
			slm.setText("Pot type does not exist as master data link.");
			ret.add(slm);
			return ret;
		}
		
		// Get the root nodes
		ArrayList<NodeNode> nns = util.nodeNodeList(pt.getFixed(), "", false);
		if(nns == null || nns.size() == 0) {
			SingleLocMessage slm = new SingleLocMessage();
			slm.setLocation(rparam.gentextStatusWarning);
			slm.setText("Pot type does not contain data.");
			ret.add(slm);
			return ret;
		}
		
		// Check one by one
		for(NodeNode nn: nns) {
			S3eUtil.logg(S3eConstants.logDebug, "Checking root node " + nn.getNode(), util, rparam);
			testSingleMasterDataLink(nn, rparam, util, ret, pottypes);
		}
		
		return ret;
		
		
	}
	
	private static void testSingleMasterDataLink(NodeNode rootnode, 
			RuntimeParameters rparam, PluginUtilInterface util, ArrayList<SingleLocMessage> msgs,
			HashMap<String, ArrayList<PotType>> pottypes){
		
		/*
		 * The single link looks like
		 * 
		 * Material,Plant	
		 * 		mat01,plant01
		 * 			md-temperature	Temperature master 01
		 * 			pottype	sygr3emHu01
		 * 		mat01,plant02
		 * 			...
		 */
		
		// Check how many elements the name has
		int defattrnum = 0;
		try {
			String[] pts = rootnode.getNode().split(rparam.createnewDecisionPtSeparator);
			defattrnum = pts.length;
		} catch (Exception e) {}
		if(defattrnum == 0) {
			SingleLocMessage slm = new SingleLocMessage();
			slm.setLocation(rootnode.getNode());
			slm.setText("no check attributes defined.");
			msgs.add(slm);
			return;
		}
		
		// Now get the subnodes
		ArrayList<NodeNode> nns = util.nodeNodeList(rootnode.getSubnode(), "", false);
		if(nns == null || nns.size() == 0) {
			SingleLocMessage slm = new SingleLocMessage();
			slm.setLocation(rootnode.getNode());
			slm.setText("no subnodes defined.");
			msgs.add(slm);
			return;
		}
		
		// Check one by one
		for(NodeNode nn: nns) {
			S3eUtil.logg(S3eConstants.logDebug, "Checking subnode " + nn.getNode(), util, rparam);
			
			// Must have the same number of attributes as the title
			int attrnum = 0;
			try {
				String[] pts = nn.getNode().split(rparam.createnewDecisionPtSeparator);
				attrnum = pts.length;
			} catch (Exception e) {}
			if(attrnum != defattrnum) {
				SingleLocMessage slm = new SingleLocMessage();
				slm.setLocation(rootnode.getNode() + " - " + nn.getNode());
				slm.setText("Different number of attributes than defined.");
				msgs.add(slm);
			}
			
			// And now check the actual md values
			ArrayList<NodeValue> nvs = util.nodeValueList(nn.getSubnode(), "", false);
			if(nvs == null || nvs.size() == 0) {
				SingleLocMessage slm = new SingleLocMessage();
				slm.setLocation(rootnode.getNode() + " - " + nn.getNode());
				slm.setText("Definition has no subnodes.");
				msgs.add(slm);
			}
			else {
				boolean ptfound = false;
				boolean mdfound = false;
				for(NodeValue nv: nvs) {
					S3eUtil.logg(S3eConstants.logDebug, "Checking md node " 
							+ nv.getNode() + " - " + nv.getValue(), util, rparam);
					if(nv.getNode().equals(rparam.createnewFnameTargetPt)) {
						S3eUtil.logg(S3eConstants.logDebug, "Pot type node found, check if valid.", util, rparam);
						ptfound = true;
						// But still must check that it's correct
						boolean ptvalid = false;
						for(String dpt: rparam.pottypes) {
							if(dpt.equals(nv.getValue())) ptvalid = true;
						}
						if(!ptvalid) {
							S3eUtil.logg(S3eConstants.logDebug, "No, pot type is not valid.", util, rparam);
							SingleLocMessage slm = new SingleLocMessage();
							slm.setLocation(rootnode.getNode() + " - " + nn.getNode());
							slm.setText("Pot type is invalid.");
							msgs.add(slm);
						}
						else {
							S3eUtil.logg(S3eConstants.logDebug, "Yes, pot type is valid.", util, rparam);
						}
					}
					else { // must be something valid md- definition
						S3eUtil.logg(S3eConstants.logDebug, "MD node, check if valid characteristic.", util, rparam);
						SingleCharacteristic foundsc = null;
						for(SingleCharacteristic sc: rparam.characteristics) {
							if(sc.getMdfield().equals(nv.getNode())) foundsc = sc;
						}
						if(!(foundsc == null)) {
							S3eUtil.logg(S3eConstants.logDebug, "Yes, characteristic is valid, check that the master data exists", util, rparam);
							// Check that there is such master data
							S3eUtil.logg(S3eConstants.logDebug, "Try to read the pot type group for " + foundsc.getName(), util, rparam);
							ArrayList<PotType> mdpts = pottypes.get(foundsc.getName());
							if(mdpts == null || mdpts.size() == 0) {
								S3eUtil.logg(S3eConstants.logDebug, "Pot type group not found.", util, rparam);
								SingleLocMessage slm = new SingleLocMessage();
								slm.setLocation(rootnode.getNode() + " - " + nn.getNode() 
									+ " - " + nv.getNode());
								slm.setText("No master data pot types exist.");
								msgs.add(slm);
							}
							else {
								S3eUtil.logg(S3eConstants.logDebug, "Pot type group found.", util, rparam);
								boolean masterfound = false;
								for(PotType pt: mdpts) {
									S3eUtil.logg(S3eConstants.logDebug, "Searching in pot type " + pt.getType(), util, rparam);
									Attr pta = pt.getFixed();
									if(pta == null) {
										S3eUtil.logg(S3eConstants.logDebug, "Pot type has no master data.", util, rparam);
										SingleLocMessage slm = new SingleLocMessage();
										slm.setLocation(rootnode.getNode() + " - " + nn.getNode() 
											+ " - " + nv.getNode());
										slm.setText("Pot type has no master data.");
										msgs.add(slm);
									}
									else {
										ArrayList<NodeValue> nnvvs = util.nodeValueList(pta, "", false);
										if(nnvvs == null) {
											S3eUtil.logg(S3eConstants.logDebug, "Pot type md has no nodes.", util, rparam);
											SingleLocMessage slm = new SingleLocMessage();
											slm.setLocation(rootnode.getNode() + " - " + nn.getNode() 
												+ " - " + nv.getNode());
											slm.setText("Pot type has no master data.");
											msgs.add(slm);
										}
										else {
											S3eUtil.logg(S3eConstants.logDebug, "Searching value " + nv.getValue() 
												+ " in the pot type:" + pt.getType(), util, rparam);
											for(NodeValue nnvv: nnvvs) {
												S3eUtil.logg(S3eConstants.logDebug, "Is it " + nnvv.getNode() + "?", util, rparam);
												if(nnvv.getNode().equals(nv.getValue())) {
													S3eUtil.logg(S3eConstants.logDebug, "Yes, it is!", util, rparam);
													masterfound = true;
													mdfound = true;
												}
												else {
													S3eUtil.logg(S3eConstants.logDebug, "No, it is not.", util, rparam);
												}
											}
										}
									}
								}
								if(!(masterfound)) {
									SingleLocMessage slm = new SingleLocMessage();
									slm.setLocation(rootnode.getNode() + " - " + nn.getNode() 
										+ " - " + nv.getNode());
									slm.setText("Master data " + nv.getValue() + " not found in any master data pot types.");
									msgs.add(slm);
								}
							}
						}
						else {
							SingleLocMessage slm = new SingleLocMessage();
							slm.setLocation(rootnode.getNode() + " - " + nn.getNode() 
								+ " - " + nv.getNode());
							slm.setText("Invalid characteristic master data field.");
							msgs.add(slm);
						}
						
					}
				}
				
				if(!(ptfound)) {
					if(rparam.pottypes.size() > 1) {
						SingleLocMessage slm = new SingleLocMessage();
						slm.setLocation(rootnode.getNode() + " - " + nn.getNode());
						slm.setText("Multiple pot types defined in the system, pot type obligatory.");
						msgs.add(slm);
					}
				}
				
				if(!(mdfound)) {
					SingleLocMessage slm = new SingleLocMessage();
					slm.setLocation(rootnode.getNode() + " - " + nn.getNode());
					slm.setText("No master data was defined for any characteristic.");
					msgs.add(slm);
				}
			}
			
		}
		
	}

	
	public static ArrayList<SingleLocMessage> testMasterData(String envvar, String mdname,
			HashMap<String, ArrayList<PotType>> pottypes, 
			RuntimeParameters rparam, PluginUtilInterface util, WebId webid){
		ArrayList<SingleLocMessage> ret = new ArrayList<>();
		
		S3eUtil.logg(S3eConstants.logDebug, "START: test master data " 
				+ mdname + " for " + envvar, util, rparam);
		
		SingleLocMessage reterror = new SingleLocMessage();
		reterror.setLocation("ERROR");
		if(envvar == null || envvar.equals("")) {
			S3eUtil.logg(S3eConstants.logDebug, "Environment variable is invalid.", util, rparam);
			reterror.setText("Invalid Environment Variable.");
			ret.add(reterror);
			return ret;
		}
		if(mdname == null || mdname.equals("")) {
			S3eUtil.logg(S3eConstants.logDebug, "Master data is invalid", util, rparam);
			reterror.setText("Invalid Master Data Name.");
			ret.add(reterror);
			return ret;
		}
		
		boolean envvardefined = false;
		boolean evhaspottypes = false;
		boolean mdfound = false;
		for(SingleCharacteristic sc: rparam.characteristics) {
			if(envvar.equals(sc.getName())) {
				envvardefined = true;
				S3eUtil.logg(S3eConstants.logDebug, "Environmental variable is defined.", util, rparam);
				
				// OK, it is defined, so check if we have master data for it.
				ArrayList<PotType> pts = pottypes.get(sc.getName());
				if(!(pts == null) && pts.size() > 0) {
					evhaspottypes = true;
					S3eUtil.logg(S3eConstants.logDebug, "Environmental variable has pot types.", util, rparam);
					
					// Try to find the master data by the name in nv
					for(PotType pt: pts) {
						// Check authorization
						if(!(util.fits(webid, ExtConstants.authMAINTAINPT, pt.getType()))) {
							SingleLocMessage slm = new SingleLocMessage();
							slm.setLocation(S3eConstants.UNAUTHORIZED);
							slm.setText(pt.getType());
							ret.add(slm);
							continue;
						}
						
						// Do the master data check
						HashMap<String,Attr> mdlist = util.getNodes(pt.getFixed(), "");
						if(!(mdlist == null) && mdlist.size() > 0) {
							Attr mdtemplate = mdlist.get(mdname);
							if(!(mdtemplate == null)) {
								S3eUtil.logg(S3eConstants.logDebug, "Master data is defined and found, start test.", util, rparam);
								mdfound = true;
								// Make a copy, not to destroy the pot type data
								Attr md = util.deepCopyAttr(mdtemplate);
								ArrayList<SingleLocMessage> ret1 = S3eMdUtil.checkAndEnhanceMd(md, rparam, util);
								S3eUtil.logg(S3eConstants.logDebug, "Test returned errors: " + ret1.size(), util, rparam);
								ret.addAll(ret1);
							}
						}
					}
				}						
			}
		}
		
		if(!(envvardefined)) {
			S3eUtil.logg(S3eConstants.logDebug, "Environmental variable is not defined.", util, rparam);
			reterror.setText("Environment variable " + envvar + " is not defined in the system.");
			ret.add(reterror);
			return ret;
		}
		
		if(!(evhaspottypes)) {
			S3eUtil.logg(S3eConstants.logDebug, "Environmental variable has no master data pot types.", util, rparam);
			reterror.setText("Environment variable " + envvar + " has no master data pot types.");
			ret.add(reterror);
			return ret;
		}
		
		if(!(mdfound)) {
			S3eUtil.logg(S3eConstants.logDebug, "Master data not found.", util, rparam);
			reterror.setText("Environment variable " + envvar 
					+ " master data " + mdname + " is not found.");
			ret.add(reterror);
			return ret;
		}
		
		return ret;
		
		
	}


	public static ArrayList<SingleLocMessage> checkAndEnhanceMd(Attr md,
			RuntimeParameters rparam, PluginUtilInterface util){
		ArrayList<SingleLocMessage> ret = new ArrayList<>();
		
		S3eUtil.logg(S3eConstants.logDebug, "START checking masted data", util, rparam);
		
		if(md == null) {
			S3eUtil.logg(S3eConstants.logError, "Master data is null.", util, rparam);
			addError(ret, "", "Master Data is null");
			return ret;
		}
		
		int globalrounding = 0;
		
		/*
		 * the master data
		 * - can have a top node settings. If no, create with default values
		 * - MUST have a top node crossings OR intervals or both.
		 */
		
		// settings
		String locoverlap = rparam.mdfnSectionSettings + "." + rparam.mdfnSettingsOverlap;
		String overlap = util.getNodeValue(md, locoverlap); 
		if(overlap.equals("")) {
			util.setNodeValue(md, rparam.mdfnSettingsDefaultOverlap, locoverlap);
		}
		else {
			// Can have only the predefined
			if(!(overlap.equals(rparam.mdfnSettingsOverlapLow)) &&
					!(overlap.equals(rparam.mdfnSettingsOverlapHigh))) {
				util.setNodeValue(md, rparam.mdfnSettingsDefaultOverlap, locoverlap);
			}
		}
		
		S3eUtil.logg(S3eConstants.logDebug, "Check overlap allowance", util, rparam);
		String locallowoverlap = rparam.mdfnSectionSettings + "." + rparam.mdfnSettingsAllowOverlap;
		S3eUtil.logg(S3eConstants.logDebug, "location should be: " + locallowoverlap, util, rparam);
		String allowoverlap = util.getNodeValue(md, locallowoverlap); 
		if(allowoverlap.equals("")) {
			S3eUtil.logg(S3eConstants.logDebug, "Not found, set to " + rparam.mdfnSettingsDefaultAllowOverlap, util, rparam);
			util.setNodeValue(md, rparam.mdfnSettingsDefaultAllowOverlap, locallowoverlap);
		}
		else {
			S3eUtil.logg(S3eConstants.logDebug, "Found: " + allowoverlap, util, rparam);
			// Can have only the predefined
			if(!(allowoverlap.equals(rparam.mdfnSettingsAllowOverlapTrue)) &&
					!(allowoverlap.equals(rparam.mdfnSettingsAllowOverlapFalse))) {
				S3eUtil.logg(S3eConstants.logDebug, "Change, because only allowed: " 
					+ rparam.mdfnSettingsAllowOverlapTrue + " and " 
					+ rparam.mdfnSettingsAllowOverlapFalse, util, rparam);
				util.setNodeValue(md, rparam.mdfnSettingsDefaultAllowOverlap, locallowoverlap);
			}
		}
		// remember it
		boolean ballowoverlap = false;
		String finalallowoverlap = util.getNodeValue(md, locallowoverlap);
		if(finalallowoverlap.equals(rparam.mdfnSettingsAllowOverlapTrue)) ballowoverlap = true;
		
		String locrounding = rparam.mdfnSectionSettings + "." + rparam.mdfnSettingsRounding;
		String rounding = util.getNodeValue(md, locrounding); 
		if(util.getNodeValue(md, locrounding).equals("")) {
			util.setNodeValue(md, rparam.mdfnSettingsDefaultRounding, locrounding);
		}
		else {
			// the value must be an integer >= 0
			ValueObject valrounding = util.convertStringToValueObject(rounding);
			if(!(valrounding.getType().equals(ExtConstants.valuetypeINTEGER))) {
				util.setNodeValue(md, rparam.mdfnSettingsDefaultRounding, locrounding);
			}
			if(valrounding.getIntegerval() < 0) {
				util.setNodeValue(md, "0", locrounding);
			}
			if(valrounding.getIntegerval() > 9) {
				util.setNodeValue(md, "9", locrounding);
			}
		}
		ValueObject valrounding = util.convertStringToValueObject(util.getNodeValue(md, locrounding));
		globalrounding = valrounding.getIntegerval();
		
		String loctimesplit = rparam.mdfnSectionSettings + "." + rparam.mdfnSettingsTimeSplit;
		String timesplit = util.getNodeValue(md, loctimesplit);
		if(util.getNodeValue(md, loctimesplit).equals("")) {
			util.setNodeValue(md, rparam.mdfnSettingsDefaultTimeSplit, loctimesplit);
		}
		else {
			// Can have only the predefined
			if(!(timesplit.equals(rparam.mdfnSettingsTimeSplitOld)) &&
					!(timesplit.equals(rparam.mdfnSettingsTimeSplitNew)) &&
					!(timesplit.equals(rparam.mdfnSettingsTimeSplitHalf)) &&
					!(timesplit.equals(rparam.mdfnSettingsTimeSplitLinear))) {
				util.setNodeValue(md, rparam.mdfnSettingsDefaultTimeSplit, loctimesplit);
			}
		}
		
		// Remove all other stuff from settings
		ArrayList<NodeValue> snvs = util.nodeValueList(md, rparam.mdfnSectionSettings, false);
		for(NodeValue nv: snvs) {
			if(!(nv.getNode().equals(rparam.mdfnSettingsOverlap)) &&
					!(nv.getNode().equals(rparam.mdfnSettingsRounding)) &&
					!(nv.getNode().equals(rparam.mdfnSettingsTimeSplit)) &&
					!(nv.getNode().equals(rparam.mdfnSettingsAllowOverlap))) {
				util.deleteNode(md, rparam.mdfnSectionSettings + "." + nv.getNode());
			}
		}
		
		// Get the intervals and the crossings
		Attr intervals = util.getNode(md, rparam.mdfnSectionIntervals);
		Attr crossings = util.getNode(md, rparam.mdfnSectionCrossings);
		// Both cannot be empty
		if(intervals.getNodes().size() == 0 && crossings.getNodes().size() == 0) {
			addError(ret, "", "Both intervals and crossings are empty.");
			S3eUtil.logg(S3eConstants.logError, "Master data has no crossings, neither intervals.", util, rparam);
			return ret;
		}
		
		// Handle the crossings one by one
		if(crossings.getNodes().size() > 0) {
			HashMap<String,Attr> crosslist = util.getNodes(crossings, "");
			if(!(crosslist == null) && !(crosslist.entrySet() == null)) {
				Iterator<Entry<String, Attr>> it = crosslist.entrySet().iterator();
				if(!(it == null)) {
					while (it.hasNext()) {
						HashMap.Entry<String, Attr> pair = (HashMap.Entry<String, Attr>)it.next();
						if(!(pair == null) && !(pair.getKey() == null) && !(pair.getValue() == null)) {
							String key = ( String ) pair.getKey();
							Attr value = ( Attr ) pair.getValue();
							checkCrossing(rparam.mdfnSectionCrossings + "." + key, 
									value, ret, rparam, util, globalrounding);
						}
					}
				}
			}
		}
		
		// handle intervals
		if(intervals.getNodes().size() > 0) {
			// The top interval node itself is an AND rule
			ArrayList<SingleInterval> ivs = new ArrayList<>();
			checkInterval(S3eConstants.mdiAnd, rparam.mdfnSectionIntervals, intervals,
					ret, rparam, util, globalrounding, ivs);
			Attr ivlist = createIntervalListNode(ivs, util);
			util.setNode(md, ivlist, rparam.mdfnRuleTechIntField);
			
			if(!(ballowoverlap)) {
				checkIntervalOverlap(ivs, ret, rparam, util);
			}
		}
		
		// Remove everything else
		ArrayList<NodeValue> nvs = util.nodeValueList(md, "", false);
		for(NodeValue nv: nvs) {
			if(!(nv.getNode().equals(rparam.mdfnSectionSettings)) &&
					!(nv.getNode().equals(rparam.mdfnSectionIntervals)) &&
					!(nv.getNode().equals(rparam.mdfnSectionCrossings)) &&
					!(nv.getNode().equals(rparam.mdfnRuleTechIntField))) {
				util.deleteNode(md, nv.getNode());
			}
		}
		
		util.setNodeValue(md, "", rparam.gentextFixedLastLocation);
		
		return ret;
	}
	
	private static double checkInterval(int ivtype, String location, Attr interval, ArrayList<SingleLocMessage> errors,
			RuntimeParameters rparam, PluginUtilInterface util, int rounding, ArrayList<SingleInterval> ivs) {
		
		double ret = 0D;
		
		// If no nodes, for sure error.
		if(interval == null || interval.getNodes() == null || interval.getNodes().size() == 0) {
			addError(errors, location, "Empty definition.");
			return ret;
		}
		
		// Now check by node (rule) type
		switch(ivtype) {
		case S3eConstants.mdiOr:
		case S3eConstants.mdiAnd: checkAndOr(location, interval, errors, rparam, util, rounding, ivs);
			return ret;
		case S3eConstants.mdiInt: checkInt(location, interval, errors, rparam, util, rounding, ivs);
			return ret;
		case S3eConstants.mdiTasks: ret = checkTasks(location, interval, errors, rparam, util, rounding, ivs);
			return ret;
		case S3eConstants.mdiTaskGroup: ret = checkTaskGroup(location, interval, errors, rparam, util, rounding, ivs);
			return ret;
		case S3eConstants.mdiSingleTask: ret = checkSingleTask(location, interval, errors, rparam, util);
			return ret;
		default: return ret;
		}
		
	}
	
	private static void checkAndOr(String location, Attr interval, ArrayList<SingleLocMessage> errors,
			RuntimeParameters rparam, PluginUtilInterface util, int rounding, ArrayList<SingleInterval> ivs) {
		
		// An Or-rule can contain several Or-, And- and/or Int-rules and nothing else.
		HashMap<String,Attr> hmtop = util.getNodes(interval, "");		
		if(!(hmtop == null) && !(hmtop.entrySet() == null)) {
			Iterator<Entry<String, Attr>> it = hmtop.entrySet().iterator();
			if(!(it == null)) {
				while (it.hasNext()) {
					HashMap.Entry<String, Attr> pair = (HashMap.Entry<String, Attr>)it.next();
					if(!(pair == null) && !(pair.getKey() == null) && !(pair.getValue() == null)) {
						String key = ( String ) pair.getKey();
						Attr value = ( Attr ) pair.getValue();
						String currloc = location + "." + key;
						
						// Check the type
						int nodetype = getIntNodeType(key, rparam, util);
						if(nodetype == S3eConstants.mdiOr ||
								nodetype == S3eConstants.mdiAnd ||
								nodetype == S3eConstants.mdiInt) {
							checkInterval(nodetype, currloc, value, errors, rparam, util, rounding, ivs);
						}
						else {
							addError(errors, currloc, "Invalid node type in the rule.");
							return;
						}
						
					}
				}
			}
		}
	}
	
	private static void checkInt(String location, Attr interval, ArrayList<SingleLocMessage> errors,
			RuntimeParameters rparam, PluginUtilInterface util, int rounding, ArrayList<SingleInterval> ivs) {
		
		/*
		 * Rules for int:
		 * - must contain a "min_value" and it has to be numeric (int or double)
		 * - must contain a "max_value" and it has to be numeric (int or double)
		 * - min_value < max_value
		 * - must contain a "max_time" which has to be the "d:h:m:s" format
		 * - CAN contain an "alert_time". If yes, it has to be the format "d:h:m:s"
		 * 		and cannot be bigger than "max_time"
		 * - CAN contain a "used_time", but anyhow it will be created and set to 0
		 * 
		 * - CAN contain a "tasks" - it will be evaluated separate
		 * - everything else will be removed.
		 */
		
		ArrayList<NodeValue> nvs = util.nodeValueList(interval, "", false); // never returns null
		
		boolean min_value_exists = false;
		boolean max_value_exists = false;
		boolean max_time_exists = false;
		boolean alert_time_exists = false;
		
		double minval = S3eConstants.NOTNUMERIC;
		double maxval = S3eConstants.NOTNUMERIC;
		long maxtime = S3eConstants.NOTADURATION;
		long alerttime = S3eConstants.NOTADURATION;
		String maxtimetextval = "";
		String alerttimetextval = "";
		
		double taskstime = 0D;
		
		for(NodeValue nv: nvs) {
			boolean valid = false;
			double numval = S3eUtil.convertStringToNumeric(nv.getValue(), util);
			long durval = S3eUtil.convertDurationStringToMillis(nv.getValue(), rparam, util);
			String currloc = location + "." + nv.getNode();
			
			// min_value
			if(nv.getNode().equals(rparam.mdfnRuleDetailsMinValue)) {
				valid = true;
				minval = S3eUtil.round(numval, rounding);
				util.setNodeValue(interval, String.valueOf(minval), rparam.mdfnRuleDetailsMinValue);
				if(minval == S3eConstants.NOTNUMERIC) {
					addError(errors, currloc, "Not numeric: " + nv.getValue());
					return;
				}
				min_value_exists = true;
			}
			
			// max_value
			if(nv.getNode().equals(rparam.mdfnRuleDetailsMaxValue)) {
				valid = true;
				maxval = S3eUtil.round(numval, rounding);
				util.setNodeValue(interval, String.valueOf(maxval), rparam.mdfnRuleDetailsMaxValue);
				if(maxval == S3eConstants.NOTNUMERIC) {
					addError(errors, currloc, "Not numeric: " + nv.getValue());
					return;
				}
				max_value_exists = true;
			}
			
			// max_time
			if(nv.getNode().equals(rparam.mdfnRuleDetailsMaxTime)) {
				valid = true;
				maxtime = durval;
				//maxtimetextval = nv.getValue();
				maxtimetextval = String.valueOf(maxtime);
				util.setNodeValue(interval, maxtimetextval, rparam.mdfnRuleDetailsMaxTime);
				if(maxtime == S3eConstants.NOTADURATION) {
					addError(errors, currloc, "Not a duration: " + nv.getValue());
					return;
				}
				max_time_exists = true;
			}
			
			// alert_time
			if(nv.getNode().equals(rparam.mdfnRuleDetailsAlertTime)) {
				valid = true;
				alerttime = durval;
				alerttimetextval = String.valueOf(alerttime);
				util.setNodeValue(interval, alerttimetextval, rparam.mdfnRuleDetailsAlertTime);
				if(alerttime == S3eConstants.NOTADURATION) {
					addError(errors, currloc, "Not a duration: " + nv.getValue());
					return;
				}
				alert_time_exists = true;
			}
			
			// used_time
			if(nv.getNode().equals(rparam.mdfnRuleDetailsUsedTime)) {
				valid = true;
			}
			
			// tasks
			if(nv.getNode().equals(rparam.mdfnTasksSection)) {
				valid = true;
				// It has to be analysed in itself
				Attr tasksnode = util.getNode(interval, rparam.mdfnTasksSection);
				taskstime = checkInterval(S3eConstants.mdiTasks, currloc, tasksnode, errors, rparam,  util, rounding, ivs);
			}
			
			if(!valid) {
				util.deleteNode(interval, nv.getNode());
			}
		}
		
		if(!(min_value_exists)) {
			addError(errors, location, "Minimum value is missing.");
			return;
		}
		if(!(max_value_exists)) {
			addError(errors, location, "Maximum value is missing.");
			return;
		}
		if(minval > maxval) {
			addError(errors, location, "Maximum value is less than minimum value.");
			return;
		}
		if(!(max_time_exists)) {
			addError(errors, location, "Maximum time is missing.");
			return;
		}
		if(alert_time_exists && alerttime > maxtime) {
			addError(errors, location, "Alert time must be less than max time.");
			return;
		}
		if(!(alert_time_exists)) {
			util.setNodeValue(interval, maxtimetextval, location + "." + rparam.mdfnRuleDetailsAlertTime);
		}
		
		if(maxtime < taskstime) {
			addError(errors, location, "Tasks allocated time is bigger than the maximum time.");
			return;
		}
		
		util.setNodeValue(interval, "0", rparam.mdfnRuleDetailsUsedTime);
		
		// Create a  new interval entry
		SingleInterval iv = new SingleInterval();
		iv.setFrom(minval);
		iv.setTo(maxval);
		iv.setLocation(location);
		if(!(ivs == null)) ivs.add(iv);
		

	}
	
	private static double checkTasks(String location, Attr interval, ArrayList<SingleLocMessage> errors,
			RuntimeParameters rparam, PluginUtilInterface util, int rounding, ArrayList<SingleInterval> ivs) {
		
		double ret = 0D;
		
		S3eUtil.logg(S3eConstants.logDebug, "Start checking tasks list at " + location, util, rparam);
		
		// tasks can contain nothing else just list of task groups
		HashMap<String,Attr> hmtop = util.getNodes(interval, "");		
		if(!(hmtop == null) && !(hmtop.entrySet() == null)) {
			Iterator<Entry<String, Attr>> it = hmtop.entrySet().iterator();
			if(!(it == null)) {
				while (it.hasNext()) {
					HashMap.Entry<String, Attr> pair = (HashMap.Entry<String, Attr>)it.next();
					if(!(pair == null) && !(pair.getKey() == null) && !(pair.getValue() == null)) {
						String key = ( String ) pair.getKey();
						Attr value = ( Attr ) pair.getValue();
						String currloc = location + "." + key;
						S3eUtil.logg(S3eConstants.logDebug, "Node at " + currloc, util, rparam);
						
						// Check the type
						int nodetype = getIntNodeType(key, rparam, util);
						if(nodetype == S3eConstants.mdiTaskGroup) {
							S3eUtil.logg(S3eConstants.logDebug, "It's a task group, call tg check." + location, util, rparam);
							ret = ret + checkInterval(nodetype, currloc, value, errors, rparam, util, rounding, ivs);
						}
						else {
							S3eUtil.logg(S3eConstants.logDebug, "Not a task group, tasks list error." + location, util, rparam);
							addError(errors, currloc, "Invalid node type in the tasks rule.");
							return ret;
						}
						
					}
				}
			}
		}
		
		return ret;
		
	}
	
	private static double checkTaskGroup(String location, Attr interval, ArrayList<SingleLocMessage> errors,
			RuntimeParameters rparam, PluginUtilInterface util, int rounding, ArrayList<SingleInterval> ivs) {
		
		/*
		 * This is a bit tricky, because it can be either a single task
		 * 
		 * 	alert_time
		 *  max_time
		 *  task_name
		 *  (used_time)
		 *  
		 * OR a collection of single tasks
		 * 
		 * 	task-...
		 * 	task-...
		 * 
		 * BUT not both!
		 * 
		 * Hint: if it turns out to be a single task, we just pass the checking to the single task check
		 * 
		 */
		
		double ret = 0D;
		
		ArrayList<NodeValue> nvs = util.nodeValueList(interval, "", false); // never returns null
		
		// First find out which one. If both, error.
		boolean itsahard = false;
		boolean itsasoft = false;
		for(NodeValue nv: nvs) {
			boolean valid = false;
			
			if(nv.getNode().equals(rparam.mdfnRuleDetailsAlertTime) ||
					nv.getNode().equals(rparam.mdfnRuleDetailsMaxTime) ||
					nv.getNode().equals(rparam.mdfnTasksFieldTaskName) ||
					nv.getNode().equals(rparam.mdfnRuleDetailsUsedTime)) {
				valid = true;
				itsahard = true;
			}
			
			int nodetype = getIntNodeType(nv.getNode(), rparam, util);
			if(nodetype == S3eConstants.mdiSingleTask) {
				valid = true;
				itsasoft = true;
			}
			
			if(!valid) {
				util.deleteNode(interval, nv.getNode());
			}
		}
		
		// Now, if both, that's error.
		if(itsahard && itsasoft) {
			addError(errors, location, "Task group contains both tasks and task elements.");
			return ret;
		}
		
		// If hard, then it itself is a single task, so check as one
		if(itsahard) {
			ret = checkInterval(S3eConstants.mdiSingleTask, location, interval, errors, rparam, util, rounding, ivs);
			return ret;
		}
		
		// If soft, loop and check one by one.
		HashMap<String,Attr> hmtop = util.getNodes(interval, "");		
		if(!(hmtop == null) && !(hmtop.entrySet() == null)) {
			Iterator<Entry<String, Attr>> it = hmtop.entrySet().iterator();
			if(!(it == null)) {
				while (it.hasNext()) {
					HashMap.Entry<String, Attr> pair = (HashMap.Entry<String, Attr>)it.next();
					if(!(pair == null) && !(pair.getKey() == null) && !(pair.getValue() == null)) {
						String key = ( String ) pair.getKey();
						Attr value = ( Attr ) pair.getValue();
						String currloc = location + "." + key;
						
						// Check the type
						int nodetype = getIntNodeType(key, rparam, util);
						if(nodetype == S3eConstants.mdiSingleTask) {
							ret = ret + checkInterval(nodetype, currloc, value, errors, rparam, util, rounding, ivs);
						}
						else {
							addError(errors, currloc, "Invalid node type in the tasks rule.");
							return ret;
						}
						
					}
				}
			}
		}
		
		return ret;
		
	}
	
	private static double checkSingleTask(String location, Attr interval, ArrayList<SingleLocMessage> errors,
			RuntimeParameters rparam, PluginUtilInterface util) {
		
		/*
		 * Single task:
		 * - must have a "max_time", duration, >0
		 * - CAN have an "alert_time", duration, >0, < "max_time"
		 * - MUST have a "task_name", cannot be empty, we set the node value to it.
		 * 	(except for if the node itself has a value, that is fine for task name)
		 * - CAN have a "used_time", but anyway we set to "0".
		 */
		
		ArrayList<NodeValue> nvs = util.nodeValueList(interval, "", false); // never returns null
		
		boolean max_time_exists = false;
		boolean alert_time_exists = false;
		boolean min_time_exists = false;
		boolean task_name_exists = false;
		boolean repeatable_exists = false;
		
		long maxtime = S3eConstants.NOTADURATION;
		long alerttime = S3eConstants.NOTADURATION;
		long mintime = S3eConstants.NOTADURATION;
		String maxtimetextval = "";
		String mintimetextval = "";
		String alerttimetextval = "";
		
		if(!(interval.getValue().equals(""))) {
			task_name_exists = true;
		}
		
		for(NodeValue nv: nvs) {
			boolean valid = false;
			long durval = S3eUtil.convertDurationStringToMillis(nv.getValue(), rparam, util);
			String currloc = location + "." + nv.getNode();
			
			// max_time
			if(nv.getNode().equals(rparam.mdfnRuleDetailsMaxTime)) {
				valid = true;
				maxtime = durval;
				//maxtimetextval = nv.getValue();
				maxtimetextval = String.valueOf(maxtime);
				util.setNodeValue(interval, maxtimetextval, rparam.mdfnRuleDetailsMaxTime);
				if(maxtime == S3eConstants.NOTADURATION) {
					addError(errors, currloc, "Not a duration: " + nv.getValue());
					return 0D;
				}
				max_time_exists = true;
			}
			
			// alert_time
			if(nv.getNode().equals(rparam.mdfnRuleDetailsAlertTime)) {
				valid = true;
				alerttime = durval;
				alerttimetextval = String.valueOf(alerttime);
				util.setNodeValue(interval, alerttimetextval, rparam.mdfnRuleDetailsAlertTime);
				if(alerttime == S3eConstants.NOTADURATION) {
					addError(errors, currloc, "Not a duration: " + nv.getValue());
					return 0D;
				}
				alert_time_exists = true;
			}
			
			// min_time
			if(nv.getNode().equals(rparam.mdfnRuleDetailsMinTime)) {
				valid = true;
				mintime = durval;
				mintimetextval = String.valueOf(mintime);
				util.setNodeValue(interval, mintimetextval, rparam.mdfnRuleDetailsMinTime);
				if(mintime == S3eConstants.NOTADURATION) {
					addError(errors, currloc, "Not a duration: " + nv.getValue());
					return 0D;
				}
				min_time_exists = true;
			}
			
			// used_time
			if(nv.getNode().equals(rparam.mdfnRuleDetailsUsedTime)) {
				valid = true;
			}
			
			// task_name
			if(nv.getNode().equals(rparam.mdfnTasksFieldTaskName)) {
				valid = true;
				if(nv.getValue().equals("")) {
					addError(errors, currloc, "Task does not have a task name.");
					return 0D;
				}
				interval.setValue(nv.getValue());
				task_name_exists = true;
			}
			
			// task_name
			if(nv.getNode().equals(rparam.mdfnTasksFieldTaskRepeatable)) {
				valid = true;
				if(nv.getValue().equals(S3eConstants.textTrue) 
						|| nv.getValue().equals(S3eConstants.textFalse)) {
					repeatable_exists = true;
				}
			}
			
			if(!valid) {
				util.deleteNode(interval, nv.getNode());
			}
		}
		
		if(!(max_time_exists)) {
			addError(errors, location, "Maximum time is missing.");
			return 0D;
		}
		if(alert_time_exists && alerttime > maxtime) {
			addError(errors, location, "Alert time must be less than max time.");
			return 0D;
		}
		if(!(alert_time_exists)) {
			util.setNodeValue(interval, maxtimetextval, rparam.mdfnRuleDetailsAlertTime);
		}
		if(min_time_exists && mintime > maxtime) {
			addError(errors, location, "Minimum time must be less than max time.");
			return 0D;
		}
		if(!(min_time_exists)) {
			util.setNodeValue(interval, "0", rparam.mdfnRuleDetailsMinTime);
		}
		if(!(repeatable_exists)) {
			util.setNodeValue(interval, S3eConstants.textFalse, rparam.mdfnTasksFieldTaskRepeatable);
		}
		
		if(!(task_name_exists)) {
			addError(errors, location, "Task name missing.");
			return 0D;
		}
		
		util.setNodeValue(interval, "0", rparam.mdfnRuleDetailsUsedTime);
		
		return maxtime;

	}
	
	public static int getIntNodeType(String nodename, RuntimeParameters rparam, PluginUtilInterface util) {
		if(nodename == null || nodename.equals("")) return S3eConstants.mdiNodeError;
		
		String regex = "";
		// Try the different nodes one by one
		regex = util.escapeRegex(rparam.mdfnRuleOrPattern);
		if(nodename.matches(regex)) return S3eConstants.mdiOr;
		
		regex = util.escapeRegex(rparam.mdfnRuleAndPattern);
		if(nodename.matches(regex)) return S3eConstants.mdiAnd;
		
		regex = util.escapeRegex(rparam.mdfnRuleIntervalPattern);
		if(nodename.matches(regex)) return S3eConstants.mdiInt;
		
		regex = util.escapeRegex(rparam.mdfnTasksSection);
		if(nodename.matches(regex)) return S3eConstants.mdiTasks;
		
		regex = util.escapeRegex(rparam.mdfnTasksGroupPattern);
		if(nodename.matches(regex)) return S3eConstants.mdiTaskGroup;
		
		regex = util.escapeRegex(rparam.mdfnTasksTaskPattern);
		if(nodename.matches(regex)) return S3eConstants.mdiSingleTask;
		
		return S3eConstants.mdiNodeError;
	}
	
	private static void checkCrossing(String location, Attr crossing, ArrayList<SingleLocMessage> errors,
			RuntimeParameters rparam, PluginUtilInterface util, int rounding){
		
		/*
		 * A crossing must look like this:
		 * 		alert_cross		integer, >0
		 * 		max_cross		integer, >= alert_cross
		 * 		top_value		integer or double
		 * 		bottom_value	integer or double, <= top_value
		 * 		current_cross	normally it's missing from a master data definition,
		 * 			but if it's there, must set to 0
		 * 		min_time		long (millisec), >= 0
		 * 		last_time		long (millisec), set to 0
		 * 
		 * Fixes:
		 * 		max_cross missing: ERROR
		 * 		max_cross not integer or not > 0: ERROR
		 * 		alert_cross missing: create with = max_cross
		 * 		alert_cross not integer or not > 0 or > max_cross: set to = max_cross
		 * 
		 * 		top_value missing AND bottom value missing: ERROR
		 * 		top_value missing: create = bottom_value
		 * 		bottom_value missing: create = top_value
		 * 		bottom_value > top_value: set to = top_value
		 * 		any of them not numeric: ERROR
		 * 		min_time error if not long, if missing, add as 0
		 * 
		 *  	current_cross: set value to 0
		 *  	
		 *  	any other node: delete
		 */
		
		ArrayList<NodeValue> nvs = util.nodeValueList(crossing, "", false);
		
		if(nvs == null || nvs.size() == 0) {
			addError(errors, location, "Definition is empty.");
			return;
		}
		
		boolean max_cross_exists = false;
		boolean alert_cross_exists = false;
		boolean top_value_exists = false;
		boolean bottom_value_exists = false;
		boolean min_time_exists = false;
		
		int maxcval = S3eConstants.NOTANINT;
		int alertcval = S3eConstants.NOTANINT;
		double topval = S3eConstants.NOTNUMERIC;
		double bottomval = S3eConstants.NOTNUMERIC;
		long min_time = S3eConstants.NOTADURATION;
		
		for(NodeValue nv: nvs) {
			boolean valid = false;
			int intval = S3eUtil.convertStringToInteger(nv.getValue(), util);
			double numval = S3eUtil.convertStringToNumeric(nv.getValue(), util);
			long msval = S3eUtil.convertDurationStringToMillis(nv.getValue(), rparam, util);
			String currloc = location + "." + nv.getNode();
			
			if(nv.getNode().equals(rparam.mdfnRuleDetailsMinTime)) {
				valid = true;
				min_time = msval;
				if(min_time == S3eConstants.NOTADURATION) {
					addError(errors, currloc, "Not a duration: " + nv.getValue());
					return;
				}
				if(min_time < 0) {
					addError(errors, currloc, "Must be >= 0: " + nv.getValue());
					return;
				}
				min_time_exists = true;
			}
			
			if(nv.getNode().equals(rparam.mdfnCrossingsMaxCross)) {
				valid = true;
				maxcval = intval;
				if(maxcval == S3eConstants.NOTANINT) {
					addError(errors, currloc, "Not an integer: " + nv.getValue());
					return;
				}
				if(maxcval < 1) {
					addError(errors, currloc, "Must be >= 1: " + nv.getValue());
					return;
				}
				max_cross_exists = true;
			}
			
			if(nv.getNode().equals(rparam.mdfnCrossingsAlertCross)) {
				valid = true;
				alertcval = intval;
				alert_cross_exists = true;
			}
			
			if(nv.getNode().equals(rparam.mdfnCrossingsTopValue)) {
				valid = true;
				topval = S3eUtil.round(numval, rounding);
				util.setNodeValue(crossing, String.valueOf(topval), rparam.mdfnCrossingsTopValue);
				top_value_exists = true;
			}
			
			if(nv.getNode().equals(rparam.mdfnCrossingsBottomValue)) {
				valid = true;
				bottomval = S3eUtil.round(numval, rounding);
				util.setNodeValue(crossing, String.valueOf(bottomval), rparam.mdfnCrossingsBottomValue);
				bottom_value_exists = true;
			}
			
			if(nv.getNode().equals(rparam.mdfnCrossingsCurrentCross)) {
				valid = true;
			}
			
			if(!valid) {
				util.deleteNode(crossing, nv.getNode());
			}
		}
		
		if(!(max_cross_exists)) {
			addError(errors, location, "Maximum allowed crossings is missing.");
			return;
		}
		
		if(!(alert_cross_exists) || alertcval == S3eConstants.NOTANINT 
				|| alertcval < 1 || alertcval > maxcval) {
			util.setNodeValue(crossing, String.valueOf(maxcval), rparam.mdfnCrossingsAlertCross);
		}
		
		if(!(min_time_exists)) {
			util.setNodeValue(crossing, "0", rparam.mdfnRuleDetailsMinTime);
		}
		
		util.setNodeValue(crossing, "0", rparam.mdfnCrossingsCurrentCross);
		util.setNodeValue(crossing, "", rparam.gentextFixedLastTime);
		
		if(!top_value_exists && !bottom_value_exists) {
			addError(errors, location, "Top and bottom values missing.");
			return;
		}
		
		if(top_value_exists && topval == S3eConstants.NOTNUMERIC) {
			addError(errors, location, "Top value is not numeric.");
			return;
		}
		
		if(bottom_value_exists && bottomval == S3eConstants.NOTNUMERIC) {
			addError(errors, location, "Bottom value is not numeric.");
			return;
		}
		if(!top_value_exists) {
			util.setNodeValue(crossing, String.valueOf(bottomval), rparam.mdfnCrossingsTopValue);
		}
		if(!bottom_value_exists) {
			util.setNodeValue(crossing, String.valueOf(topval), rparam.mdfnCrossingsBottomValue);
		}
		if(bottomval > topval) {
			util.setNodeValue(crossing, String.valueOf(topval), rparam.mdfnCrossingsBottomValue);
			util.setNodeValue(crossing, String.valueOf(bottomval), rparam.mdfnCrossingsTopValue);
		}
		
	}
	
	public static void addMdToFixed(Attr fixed, String envvar, String mdname, Attr md,
			RuntimeParameters rparam, PluginUtilInterface util) {
		if(fixed == null || envvar == null || envvar.equals("") ||
				mdname == null || mdname.equals("") || md == null) return;
		
		/*
		 * The fixed structure look like this:
		 * 
		 * 	current_task
		 * 	[envvar 1]
		 * 		status	[status value]
		 * 		last_value	[last value]
		 * 		last_time	[last time]
		 * 		budget
		 * 			[budget name 1]
		 * 				last_location
		 * 				crossings
		 * 				intervals
		 * 				settings
		 * 			[budget name 2]
		 * 				...
		 * 	[envvar 2]
		 * 		...
		 * 
		 * The master data (md) looks like
		 * 	crossings
		 * 	intervals
		 * 	settings
		 */
		
		S3eUtil.logg(S3eConstants.logDebug, "START adding master data to Pot.", util, rparam);
		
		// Get the list of the environment variables already existing
		HashMap<String,Attr> envvars = util.getNodes(fixed, "");
		boolean envvarExists = false;
		if(!(envvars == null) && !(envvars.entrySet() == null)) {
			Iterator<Entry<String, Attr>> itEnvvar = envvars.entrySet().iterator();
			if(!(itEnvvar == null)) {
				while (itEnvvar.hasNext()) {
					HashMap.Entry<String, Attr> pair = (HashMap.Entry<String, Attr>)itEnvvar.next();
					if(!(pair == null) && !(pair.getKey() == null) && !(pair.getValue() == null)) {
						String key = ( String ) pair.getKey();
						Attr value = ( Attr ) pair.getValue();
						if(key == null || value == null) continue;
						// Check if this is our envvar
						if(key.equals(envvar)) {
							S3eUtil.logg(S3eConstants.logDebug, "Ev " + envvar + " md " + mdname + " exists, merging.", util, rparam);
							addMdToEnvvar(value, mdname, md, rparam, util);
							envvarExists = true;
						}
					}
				}
			}
		}
		
		// If did not exist yet, must create as new
		if(!(envvarExists)) {
			S3eUtil.logg(S3eConstants.logDebug, "Ev " + envvar + " md " + mdname + " does not exist, create new.", util, rparam);
			Attr newEnvvar = new Attr();
			addMdToEnvvar(newEnvvar, mdname, md, rparam, util);
			util.setNodeValue(fixed, "", envvar);
			util.setNode(fixed, newEnvvar, envvar);
		}
		
		// We need a field "current_task" in the root
		util.setNodeValue(fixed, "", rparam.gentextFixedCurrentTask);
		util.setNodeValue(fixed, "", rparam.gentextFixedTaskStarted);
	}
	
	public static void addMdToEnvvar(Attr envvar, String mdname, Attr md,
			RuntimeParameters rparam, PluginUtilInterface util) {
		/*
		 * The envvar structure look like this:
		 * 
		 * 	
		 * 		status	[status value]
		 * 		last_value	[last value]
		 * 		last_time	[last time]
		 * 		budget
		 * 			[budget name 1]
		 * 				last_location
		 * 				crossings
		 * 				intervals
		 * 				settings
		 * 			[budget name 2]
		 * 				...
		 * 	[envvar 2]
		 * 		...
		 * 
		 * The master data (md) looks like
		 * 	crossings
		 * 	intervals
		 * 	settings
		 */
		
		// Get the top level nodes and check that all the necessary fields are there
		HashMap<String,Attr> topnodes = util.getNodes(envvar, "");
		if(topnodes == null) topnodes = new HashMap<String,Attr>();
		
		// status
		Attr node_status = topnodes.get(rparam.gentextFixedStatus);
		if(node_status == null) {
			S3eUtil.logg(S3eConstants.logDebug, "Adding main status field", util, rparam);
			util.setNodeValue(envvar, rparam.gentextStatusOk, rparam.gentextFixedStatus);
		}
		
		// last value
		Attr node_lastval = topnodes.get(rparam.gentextFixedLastValue);
		if(node_lastval == null) {
			S3eUtil.logg(S3eConstants.logDebug, "Adding last value field", util, rparam);
			util.setNodeValue(envvar, "", rparam.gentextFixedLastValue);
		}
		
		// last time
		Attr node_lasttime = topnodes.get(rparam.gentextFixedLastTime);
		if(node_lasttime == null) {
			S3eUtil.logg(S3eConstants.logDebug, "Adding last time field", util, rparam);
			util.setNodeValue(envvar, "", rparam.gentextFixedLastTime);
		}
		
		// budget
		Attr budget = topnodes.get(rparam.gentextFixedBudget);
		if(budget == null) {
			S3eUtil.logg(S3eConstants.logDebug, "Adding budget field", util, rparam);
			util.setNodeValue(envvar, "", rparam.gentextFixedBudget);
			budget = new Attr();
			// util.setNodeValue(budget, "", rparam.gentextFixedLastLocation);
			util.setNode(envvar, budget, rparam.gentextFixedBudget);
		}
		
		// Now we need to see whether we have this budget already or not
		Attr targetbudget = util.getNode(budget, mdname);
		// unfortunately getNode never gives back a null, max. an empty node
		if(targetbudget == null || targetbudget.getNodes() == null ||
				targetbudget.getNodes().size() == 0) {
			// New, create
			S3eUtil.logg(S3eConstants.logDebug, "Master data " + mdname + " does not exist, add as new.", util, rparam);
			util.setNode(budget, md, mdname);
		}
		else {
			// The budget existed already, so must mix
			S3eUtil.logg(S3eConstants.logDebug, "Master data " + mdname + " exist, mixing.", util, rparam);
			mixTwoBudget(targetbudget, md, rparam, util);
		}
		
	}
	
	private static void mixTwoBudget(Attr target, Attr source,
			RuntimeParameters rparam, PluginUtilInterface util) {
		/*
		 * The budgets looks like
		 * 	crossings
		 * 	intervals
		 * 	settings
		 * 
		 * out of these the settings must be the same (same budget type),
		 * but the crossings values and the intervals values can be different
		 * 
		 * We need to adjust the target and leave intact the source.
		 * 
		 */
		S3eMergeUtil.mixMaster(source, target, rparam, util, false);
	}
	
	private static void addError(ArrayList<SingleLocMessage> coll, String loc, String text) {
		if(coll == null) return;
		if(loc == null) loc = "";
		if(text == null) text = "";
		SingleLocMessage slm = new SingleLocMessage();
		slm.setLocation(loc);
		slm.setText(text);
		coll.add(slm);
	}
	
	private static Attr createIntervalListNode(ArrayList<SingleInterval> ivs, PluginUtilInterface util) {
		Attr ret = new Attr();
		if(ivs == null || ivs.size() == 0) return ret;
		
		// First must sort the intervals by bottom value
		try {
			Collections.sort(ivs);
		} catch(Exception e) { return ret; }
		
		// Now create a new attr for all
		int counter = 10000;
		
		for(SingleInterval iv: ivs) {
			counter++;
			String nodename = S3eConstants.IVLISTNODE + counter;
			Attr line = new Attr();
			util.setNodeValue(line, String.valueOf(iv.getFrom()), S3eConstants.IVLISTFROM);
			util.setNodeValue(line, String.valueOf(iv.getTo()), S3eConstants.IVLISTTO);
			util.setNodeValue(line, iv.getLocation(), S3eConstants.IVLISTLOC);
			util.setNode(ret, line, nodename);
		}
		
		
		
		return ret;
	}
	
	private static void checkIntervalOverlap(ArrayList<SingleInterval> ivs, ArrayList<SingleLocMessage> errors,
			RuntimeParameters rparam, PluginUtilInterface util) {
		
		if(ivs == null || ivs.size() == 0 || errors == null || rparam == null || util == null) return;
		
		// Assumption: ivs is sorted by "from" value
		
		SingleInterval previv = null;
		for(SingleInterval iv: ivs) {
			if(!(previv == null)) {
				if(previv.getTo() > iv.getFrom()) {
					addError(errors, "Interval overlapping: ", 
							previv.getLocation() + " - " + iv.getLocation());
				}
			}
			previv = iv;
		}
		
	}
	
}
