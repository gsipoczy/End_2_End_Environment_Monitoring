package sygr3em.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.mariuszgromada.math.mxparser.*;

import sygr.pots.extensions.Attr;
import sygr.pots.extensions.BusinessParameter;
import sygr.pots.extensions.ExtAlert;
import sygr.pots.extensions.ExtConstants;
import sygr.pots.extensions.NodeNode;
import sygr.pots.extensions.NodeValue;
import sygr.pots.extensions.PluginUtilInterface;
import sygr.pots.extensions.Pot;
import sygr.pots.extensions.PotType;
import sygr.pots.extensions.ValueObject;
import sygr3em.model.MultiProperty;
import sygr3em.model.MultiTelemetry;
import sygr3em.model.RuntimeParameters;
import sygr3em.model.SingleCharacteristic;
import sygr3em.model.SingleConversion;
import sygr3em.model.SingleInterval;
import sygr3em.model.SingleLocMessage;
import sygr3em.model.SingleProperty;
import sygr3em.model.SingleTelemetry;

public class S3eUtil {

	/**********************************************************************
	 * Log
	 ***********************************************************************/
	public static void logg(int level, String text, PluginUtilInterface util, RuntimeParameters rparams) {
		if(text == null || util == null) return;
		if(rparams == null) {
			util.log(S3eConstants.LOGPREFIX + text);
			return;
		}
		
		// Only if the log is the correct level
		if(rparams.logLevel < level) return;
		
		// In case of error, extend the text
		if(level == S3eConstants.logError) text = "ERROR: " + text;
		
		// Add log
		util.log(S3eConstants.LOGPREFIX + text);
	}
	
	/**********************************************************************
	 * Refresh buffers
	 ***********************************************************************/
	public static RuntimeParameters readBusinessParams(PluginUtilInterface util) {
		
		RuntimeParameters rparams = new RuntimeParameters();
		if(util == null) return rparams;
		
		// Fill the primitive element
		rparams.pe[0] = new PrimitiveElement(0);
		rparams.pe[1] = new PrimitiveElement(1);
		rparams.pe[2] = new PrimitiveElement(2);
		
		// Get current business parameters 
		ArrayList<BusinessParameter> bps = util.getBusinessParameters(S3eConstants.bpApplication, "", "", "");
		ArrayList<BusinessParameter> xbps = util.getBusinessParameters(S3eConstants.bpXApplication, "", "", "");
		if(bps == null) bps = new ArrayList<BusinessParameter>();
		if(xbps == null) xbps = new ArrayList<BusinessParameter>();
		bps.addAll(xbps);
		if(bps.size() == 0) return rparams;
		logg(S3eConstants.logDebug, "Reloading Business Parameters", util, rparams);
		
		// Loop through and fill the parameters
		for(BusinessParameter bp: bps) {
			
			switch(bp.getKey0()) {
			
			// *****************************************************************
			// UOM conversions
			// *****************************************************************
			case S3eConstants.bpK0Conversions:
				SingleConversion conversion = new SingleConversion();
				if(rparams.conversions == null) rparams.conversions = new ArrayList<SingleConversion>();
				conversion.setC12c(bp.getKey1());
				conversion.setUom(bp.getKey2());
				conversion.setFormula(bp.getValue());
				rparams.conversions.add(conversion);
				
				break;
			
			// *****************************************************************
			// General settings
			// *****************************************************************
			case S3eConstants.bpK0General:
				switch(bp.getKey1()) {
				
				case S3eConstants.bpK1Authorization:
					if(bp.getKey2().equals(S3eConstants.bpK2EditUnit)) {
						if(!(bp.getValue() == null) && !(bp.getValue().equals(""))) {
							rparams.autheditunit = bp.getValue();
						}
					}
					break;
				
				// Outbound plugins
				case S3eConstants.bpK1OutPlugin:
					switch(bp.getKey2()) {
					case S3eConstants.bpK2TestMasterData:
						if(!(bp.getValue() == null) && !(bp.getValue().equals(""))) {
							rparams.outpluginTestMasterData = bp.getValue();
						}
						break;
					case S3eConstants.bpK2TestMasterDataLink:
						if(!(bp.getValue() == null) && !(bp.getValue().equals(""))) {
							rparams.outpluginTestMasterDataLink = bp.getValue();
						}
						break;
					case S3eConstants.bpK2CalculationProblem:
						if(!(bp.getValue() == null) && !(bp.getValue().equals(""))) {
							rparams.outpluginCalculationProblem = bp.getValue();
						}
						break;
					default: break;
					}
					
					break;
				
				// Log level
				case S3eConstants.bpK1LogLevel:
					int loglevel = convertStringToInteger(bp.getValue(), util);
					if(!(loglevel == S3eConstants.NOTANINT)) {
						if(loglevel < S3eConstants.minLogLevel) loglevel = S3eConstants.minLogLevel;
						if(loglevel > S3eConstants.maxLogLevel) loglevel = S3eConstants.maxLogLevel;
						rparams.logLevel = loglevel;
	
					}
					break;
					
				// Update sleep
				case S3eConstants.bpK1UpdateSleep:
					long sleep = convertLongStringToMillis(bp.getValue(), rparams,util);
					if(!(sleep == S3eConstants.NOTADURATION)) {
						if(sleep < 0L) sleep = 0L;
						rparams.updatesleep = sleep;
	
					}
					break;
					
					// Use rule
				case S3eConstants.bpK1UseRules:
					Boolean userules = convertStringToBoolean(bp.getValue(), util);
					if(!(userules == null))	rparams.userules = userules; 
					break;
					
				case S3eConstants.bpK1TaskSetsStatus:
					Boolean tasksetsstatus = convertStringToBoolean(bp.getValue(), util);
					if(!(tasksetsstatus == null)) rparams.tasksetsstatus = tasksetsstatus;
					break;
					
				case S3eConstants.bpK1AllowUseWarning:
					Boolean allowusewarning = convertStringToBoolean(bp.getValue(), util);
					if(!(allowusewarning == null)) rparams.allowusewarning = allowusewarning; 
					break;
					
				case S3eConstants.bpK1AllowUseError:
					Boolean allowuseerror = convertStringToBoolean(bp.getValue(), util);
					if(!(allowuseerror == null)) rparams.allowuseerror = allowuseerror;
					break;
					
				case S3eConstants.bpK1CollectSources:
					Boolean collectsources = convertStringToBoolean(bp.getValue(), util);
					if(!(collectsources == null)) rparams.collectsources = collectsources;
					break;
					
				case S3eConstants.bpK1MergeWaitMs:
					 long mwms = convertLongStringToMillis(bp.getValue(), rparams, util);
					 if(!(mwms == S3eConstants.NOTADURATION)) rparams.mergewaitms = mwms;
					break;
					
					// Task sets status
					
				// Comamnds
				case S3eConstants.bpK1Command:
					switch(bp.getKey2()) {
					case S3eConstants.bpK2CommandCreateNew: 
						rparams.imsgcommandCreateNew = bp.getValue(); break;
					case S3eConstants.bpK2CommandCreateReplace: 
						rparams.imsgcommandCreateReplace = bp.getValue(); break;
					case S3eConstants.bpK2CommandSetDeleted: 
						rparams.imsgcommandSetDeleted = bp.getValue(); break;
					case S3eConstants.bpK2CommandSetActive: 
						rparams.imsgcommandSetActive = bp.getValue(); break;
					case S3eConstants.bpK2CommandSetInactive: 
						rparams.imsgcommandSetInactive = bp.getValue(); break;
					case S3eConstants.bpK2CommandSetPaused: 
						rparams.imsgcommandSetPaused = bp.getValue(); break;
					case S3eConstants.bpK2CommandSetDeletedForce: 
						rparams.imsgcommandSetDeletedForce = bp.getValue(); break;
					case S3eConstants.bpK2CommandTestMasterData: 
						rparams.imsgcommandTestMasterData = bp.getValue(); break;
					case S3eConstants.bpK2CommandTestMasterDataLink: 
						rparams.imsgcommandTestMasterDataLink = bp.getValue(); break;
					case S3eConstants.bpK2CommandTelemetry: 
						rparams.imsgcommandTelemetry = bp.getValue(); break;
					case S3eConstants.bpK2CommandSetTask: 
						rparams.imsgcommandSetTask = bp.getValue(); break;
					case S3eConstants.bpK2CommandMergeExisting: 
						rparams.imsgcommandMergeExisting = bp.getValue(); break;
					case S3eConstants.bpK2CommandMergeNew: 
						rparams.imsgcommandMergeNew = bp.getValue(); break;
					case S3eConstants.bpK2CommandGetUnitForEdit: 
						rparams.imsgcommandGetUnitForEdit = bp.getValue(); break;
					case S3eConstants.bpK2CommandUpdateUnit: 
						rparams.imsgcommandUpdateUnit = bp.getValue(); break;
					case S3eConstants.bpK2CommandSplit: 
						rparams.imsgcommandSplit = bp.getValue(); break;
					case S3eConstants.bpK2CommandAddIds: 
						rparams.imsgcommandAddIds = bp.getValue(); break;
					case S3eConstants.bpK2CommandDeleteIds: 
						rparams.imsgcommandDeleteIds = bp.getValue(); break;
					case S3eConstants.bpK2CommandUpdateSecAttr: 
						rparams.imsgcommandUpdateSecAttr = bp.getValue(); break;
					default: break;
					}
					
					break;
					
				// Store types
				case S3eConstants.bpK1PotTypes:
					if(bp.getApplication().equals(S3eConstants.bpXApplication))	{
						if(rparams.pottypes == null) rparams.pottypes = new ArrayList<String>();
						if(!(bp.getKey2() == null) && !(bp.getKey2().equals(""))) {
							boolean found = false;
							for(String pt: rparams.pottypes) {
								if(bp.getKey2().equals(pt)) found = true;
							}
							if(!found) rparams.pottypes.add(bp.getKey2());
						}
					}
					break;
					
				// Targets
				case S3eConstants.bpK1Targets:
					// Delete current
					if(!(rparams.targets == null)) rparams.targets.clear();
					else rparams.targets = new HashMap<String, ArrayList<String>>();
					// Key2 is either a pot type or default
					ArrayList<String> ars = new ArrayList<>();
					if(!(bp.getValue() == null) && !(bp.getValue().equals("")))
						ars.add(bp.getValue());
					if(!(bp.getValue2() == null) && !(bp.getValue2().equals("")))
						ars.add(bp.getValue2());
					if(!(bp.getValue3() == null) && !(bp.getValue3().equals("")))
						ars.add(bp.getValue3());
					if(!(bp.getValue4() == null) && !(bp.getValue4().equals("")))
						ars.add(bp.getValue4());
					if(!(bp.getValue5() == null) && !(bp.getValue5().equals("")))
						ars.add(bp.getValue5());
					rparams.targets.put(bp.getKey2(), ars);
					
					break;
					
				// Texts
				case S3eConstants.bpK1Texts:
					if(bp.getApplication().equals(S3eConstants.bpXApplication)) {
						switch(bp.getKey2()) {
						case S3eConstants.bpK2FlexiMain:
							if(!(bp.getValue() == null) && !(bp.getValue().equals("")))
								rparams.gentextStatus = bp.getValue();
							if(!(bp.getValue2() == null) && !(bp.getValue2().equals("")))
								rparams.gentextLastMessage = bp.getValue2();
							if(!(bp.getValue3() == null) && !(bp.getValue3().equals("")))
								rparams.gentextLastError = bp.getValue3();
							break;
						case S3eConstants.bpK2FixedMain:
							if(!(bp.getValue() == null) && !(bp.getValue().equals("")))
								rparams.gentextFixedStatus = bp.getValue();
							if(!(bp.getValue2() == null) && !(bp.getValue2().equals("")))
								rparams.gentextFixedLastValue = bp.getValue2();
							if(!(bp.getValue3() == null) && !(bp.getValue3().equals("")))
								rparams.gentextFixedLastTime = bp.getValue3();
							if(!(bp.getValue4() == null) && !(bp.getValue4().equals("")))
								rparams.gentextFixedBudget = bp.getValue4();
							break;
						case S3eConstants.bpK2FixedMain2:
							if(!(bp.getValue() == null) && !(bp.getValue().equals("")))
								rparams.gentextFixedCurrentTask = bp.getValue();
							if(!(bp.getValue2() == null) && !(bp.getValue2().equals("")))
								rparams.gentextFixedLastLocation = bp.getValue2();
							if(!(bp.getValue3() == null) && !(bp.getValue3().equals("")))
								rparams.gentextFixedTaskStarted = bp.getValue3();
							break;
						case S3eConstants.bpK2Sources:
							if(!(bp.getValue() == null) && !(bp.getValue().equals("")))
								rparams.gentextSources = bp.getValue();
							break;
						case S3eConstants.bpK2Statuses:
							if(!(bp.getValue() == null) && !(bp.getValue().equals("")))
								rparams.gentextStatusOk = bp.getValue();
							if(!(bp.getValue2() == null) && !(bp.getValue2().equals("")))
								rparams.gentextStatusWarning = bp.getValue2();
							if(!(bp.getValue3() == null) && !(bp.getValue3().equals("")))
								rparams.gentextStatusError = bp.getValue3();
							break;
						case S3eConstants.bpK2Messages:
							if(!(bp.getValue() == null) && !(bp.getValue().equals("")))
								rparams.gentextMessageSeverity = bp.getValue();
							if(!(bp.getValue2() == null) && !(bp.getValue2().equals("")))
								rparams.gentextMessageText = bp.getValue2();
							if(!(bp.getValue3() == null) && !(bp.getValue3().equals("")))
								rparams.gentextMessageTime = bp.getValue3();
							break;
						default: break;
						}
					}
					break;
					
				// Create New Object parameters
				case S3eConstants.bpK1CreateNew:
					switch(bp.getKey2()) {
					case S3eConstants.bpK2CreateNewPotType:
						rparams.createnewPotType = bp.getValue(); break;
					case S3eConstants.bpK2CreateNewErrorPotType:
						rparams.createnewErrorPotType = bp.getValue(); break;
					case S3eConstants.bpK2CreateNewBallType:
						rparams.createnewBallType = bp.getValue(); break;
					case S3eConstants.bpK2CreateNewInitPlugin:
						rparams.createnewInitPlugin = bp.getValue(); break;
					case S3eConstants.bpK2CreateNewFnameParams:
						rparams.createnewFnameParams = bp.getValue(); break;
					case S3eConstants.bpK2CreateNewFnamePrimAtt:
						rparams.createnewFnamePrimAtt = bp.getValue(); break;
					case S3eConstants.bpK2CreateNewFnameSecAtt:
						rparams.createnewFnameSecAtt = bp.getValue(); break;
					case S3eConstants.bpK2CreateNewFnameTargetPt:
						rparams.createnewFnameTargetPt = bp.getValue(); break;
					case S3eConstants.bpK2CreateNewDecisionPt:
						rparams.createnewDecisionPt = bp.getValue();
						rparams.createnewDecisionPtSeparator = bp.getValue2(); break;
					default: break;
					}
					break;
					
				// Ball types
				case S3eConstants.bpK1BallTypes:
					switch(bp.getKey2()) {
					case S3eConstants.bpK2Command:
						if(!(bp.getValue() == null) && !(bp.getValue().equals("")))
							rparams.balltypeCommand = bp.getValue();
						break;
					default: break;
					}
					
				// Ball report plugin
				case S3eConstants.bpK1BallRepPlugin:
					if(!(bp.getValue() == null) && !(bp.getValue().equals("")))
						rparams.ballreportplugin = bp.getValue();
					break;
					
				default: break;
				}
				
				break;
				
			// *****************************************************************
			// Characteristics
			// *****************************************************************
			case S3eConstants.bpK0Characteristics:
				if(bp.getApplication().equals(S3eConstants.bpXApplication)) {
					if(rparams.characteristics == null) rparams.characteristics = new ArrayList<SingleCharacteristic>();
					SingleCharacteristic sch = new SingleCharacteristic();
					sch.setName(bp.getValue());
					sch.setUom(bp.getValue2());
					sch.setMdfield(bp.getValue3());
					sch.setMdpottype(bp.getValue4());
					sch.setDisplayname(bp.getKey2());
					sch.setMinfrequency(convertLongStringToMillis(bp.getValue5(), rparams, util));
					if(sch.getMinfrequency() < 0) sch.setMinfrequency(0L);
					boolean found = false;
					for(SingleCharacteristic sc: rparams.characteristics) {
						if(sch.getName().equals(sc.getName())) found = true;
					}
					if(!found) rparams.characteristics.add(sch);
				}
				break;
				
			// *****************************************************************
			// Master Data
			// *****************************************************************
			case S3eConstants.bpK0MasterData:
				switch(bp.getKey1()) {
				case S3eConstants.bpK1FieldNames:
					if(bp.getApplication().equals(S3eConstants.bpXApplication)) {
						switch(bp.getKey2()) {
						case S3eConstants.bpK2MainSections:
							if(!(bp.getValue() == null) && !(bp.getValue().equals("")))
								rparams.mdfnSectionCrossings = bp.getValue();
							if(!(bp.getValue2() == null) && !(bp.getValue2().equals("")))
								rparams.mdfnSectionIntervals = bp.getValue2();
							if(!(bp.getValue3() == null) && !(bp.getValue3().equals("")))
								rparams.mdfnSectionSettings = bp.getValue3();
							break;
						case S3eConstants.bpK2Crossings:
							if(!(bp.getValue() == null) && !(bp.getValue().equals("")))
								rparams.mdfnCrossingsBottomValue = bp.getValue();
							if(!(bp.getValue2() == null) && !(bp.getValue2().equals("")))
								rparams.mdfnCrossingsTopValue = bp.getValue2();
							if(!(bp.getValue3() == null) && !(bp.getValue3().equals("")))
								rparams.mdfnCrossingsMaxCross = bp.getValue3();
							if(!(bp.getValue4() == null) && !(bp.getValue4().equals("")))
								rparams.mdfnCrossingsAlertCross = bp.getValue4();
							if(!(bp.getValue5() == null) && !(bp.getValue5().equals("")))
								rparams.mdfnCrossingsCurrentCross = bp.getValue5();
							break;
						case S3eConstants.bpK2Rules:
							if(!(bp.getValue() == null) && !(bp.getValue().equals("")))
								rparams.mdfnRuleOrPattern = bp.getValue();
							if(!(bp.getValue2() == null) && !(bp.getValue2().equals("")))
								rparams.mdfnRuleAndPattern = bp.getValue2();
							if(!(bp.getValue3() == null) && !(bp.getValue3().equals("")))
								rparams.mdfnRuleIntervalPattern = bp.getValue3();
							if(!(bp.getValue4() == null) && !(bp.getValue4().equals("")))
								rparams.mdfnRuleTechIntField = bp.getValue4();
							break;
						case S3eConstants.bpK2RuleDetails:
							if(!(bp.getValue() == null) && !(bp.getValue().equals("")))
								rparams.mdfnRuleDetailsMinValue = bp.getValue();
							if(!(bp.getValue2() == null) && !(bp.getValue2().equals("")))
								rparams.mdfnRuleDetailsMaxValue = bp.getValue2();
							if(!(bp.getValue3() == null) && !(bp.getValue3().equals("")))
								rparams.mdfnRuleDetailsMaxTime = bp.getValue3();
							if(!(bp.getValue4() == null) && !(bp.getValue4().equals("")))
								rparams.mdfnRuleDetailsAlertTime = bp.getValue4();
							if(!(bp.getValue5() == null) && !(bp.getValue5().equals("")))
								rparams.mdfnRuleDetailsUsedTime = bp.getValue5();
							break;
						case S3eConstants.bpK2Tasks:
							if(!(bp.getValue() == null) && !(bp.getValue().equals("")))
								rparams.mdfnTasksSection = bp.getValue();
							if(!(bp.getValue2() == null) && !(bp.getValue2().equals("")))
								rparams.mdfnTasksGroupPattern = bp.getValue2();
							if(!(bp.getValue3() == null) && !(bp.getValue3().equals("")))
								rparams.mdfnTasksTaskPattern = bp.getValue3();
							if(!(bp.getValue4() == null) && !(bp.getValue4().equals("")))
								rparams.mdfnTasksFieldTaskName = bp.getValue4();
							if(!(bp.getValue5() == null) && !(bp.getValue5().equals("")))
								rparams.mdfnRuleDetailsMinTime = bp.getValue5();
							break;
						case S3eConstants.bpK2Tasks2:
							if(!(bp.getValue() == null) && !(bp.getValue().equals("")))
								rparams.mdfnTasksFieldTaskRepeatable = bp.getValue();
							break;
						case S3eConstants.bpK2Settings:
							if(!(bp.getValue() == null) && !(bp.getValue().equals("")))
								rparams.mdfnSettingsOverlap = bp.getValue();
							if(!(bp.getValue2() == null) && !(bp.getValue2().equals("")))
								rparams.mdfnSettingsRounding = bp.getValue2();
							if(!(bp.getValue3() == null) && !(bp.getValue3().equals("")))
								rparams.mdfnSettingsTimeSplit = bp.getValue3();
							if(!(bp.getValue4() == null) && !(bp.getValue4().equals("")))
								rparams.mdfnSettingsAllowOverlap = bp.getValue4();
							break;
						default: break;
						}
					}
					break;
					
				case S3eConstants.bpK1Settings:
					switch(bp.getKey2()) {
					case S3eConstants.bpK2Defaults:
						if(!(bp.getValue() == null) && !(bp.getValue().equals("")))
							rparams.mdfnSettingsDefaultOverlap = bp.getValue();
						if(!(bp.getValue2() == null) && !(bp.getValue2().equals("")))
							rparams.mdfnSettingsDefaultRounding = bp.getValue2();
						if(!(bp.getValue3() == null) && !(bp.getValue3().equals("")))
							rparams.mdfnSettingsDefaultTimeSplit = bp.getValue3();
						if(!(bp.getValue4() == null) && !(bp.getValue4().equals("")))
							rparams.mdfnSettingsDefaultAllowOverlap = bp.getValue4();
						break;
					case S3eConstants.bpK2Overlap:
						if(bp.getApplication().equals(S3eConstants.bpXApplication)) {
							if(!(bp.getValue() == null) && !(bp.getValue().equals("")))
								rparams.mdfnSettingsOverlapLow = bp.getValue();
							if(!(bp.getValue2() == null) && !(bp.getValue2().equals("")))
								rparams.mdfnSettingsOverlapHigh = bp.getValue2();
						}
						break;
					case S3eConstants.bpK2AllowOverlap:
						if(bp.getApplication().equals(S3eConstants.bpXApplication)) {
							if(!(bp.getValue() == null) && !(bp.getValue().equals("")))
								rparams.mdfnSettingsAllowOverlapTrue = bp.getValue();
							if(!(bp.getValue2() == null) && !(bp.getValue2().equals("")))
								rparams.mdfnSettingsAllowOverlapFalse = bp.getValue2();
						}
						break;
					case S3eConstants.bpK2TimeSplit:
						if(bp.getApplication().equals(S3eConstants.bpXApplication)) {
							if(!(bp.getValue() == null) && !(bp.getValue().equals("")))
								rparams.mdfnSettingsTimeSplitOld = bp.getValue();
							if(!(bp.getValue2() == null) && !(bp.getValue2().equals("")))
								rparams.mdfnSettingsTimeSplitNew = bp.getValue2();
							if(!(bp.getValue3() == null) && !(bp.getValue3().equals("")))
								rparams.mdfnSettingsTimeSplitHalf = bp.getValue3();
							if(!(bp.getValue4() == null) && !(bp.getValue4().equals("")))
								rparams.mdfnSettingsTimeSplitLinear = bp.getValue4();
						}
						break;
					default: break;
					}
					break;
					
				default: break;
				}
				break;
				
			default: break;
			
			}
			readBpLog(bp, util, rparams);
		}
		
		return rparams;
	}
	
	private static void readBpLog(BusinessParameter bp, PluginUtilInterface util, RuntimeParameters rparams) {
		if(bp == null || util == null || rparams == null) return;
		
		logg(S3eConstants.logDebug, 
				"BP " + bp.getApplication() + "/" + bp.getKey0() + "/" + bp.getKey1() + "/" + bp.getKey2() + " = " 
					+ bp.getValue() + "/" + bp.getValue2() + "/" + bp.getValue3()
					+ bp.getValue4() + bp.getValue5(), 
				util, rparams);
	}
	
	public static HashMap<String, ArrayList<PotType>> readPotTypes(PluginUtilInterface util, RuntimeParameters rparams) {
		
		// Initial empty return value
		ArrayList<PotType> apts = new ArrayList<>();
		HashMap<String, ArrayList<PotType>> hpts = new HashMap<>();
		hpts.put("", apts);
		
		// If no useful input, return the empty value
		if(util == null || rparams == null || rparams.characteristics == null) return hpts;
		
		logg(S3eConstants.logDebug, "Reloading Pot Types", util, rparams);
		
		// Fetch the Master Data pot types for every environment characteristic
		for(SingleCharacteristic sc: rparams.characteristics) {
			logg(S3eConstants.logDebug, "Load Pot Types for " + sc.getDisplayname(), util, rparams);
			
			if(sc.getMdpottype() == null || sc.getMdpottype().equals("")) {
				logg(S3eConstants.logDebug, "ERROR: No master data pot type pattern!", util, rparams);
				continue;
			}
			
			// Read the Pot Types
			ArrayList<PotType> mdpts = util.findPotTypes(sc.getMdpottype(), "");
			if(!(mdpts == null) && mdpts.size() > 0) {
				logg(S3eConstants.logDebug, "Found " + mdpts.size() + " Pot Types", util, rparams);
				hpts.put(sc.getName(), mdpts);
			}
			else logg(S3eConstants.logDebug, "No Pot Types found", util, rparams);			
		}
		
		// Read the decision pot types, too
		ArrayList<PotType> dpts = util.findPotTypes(rparams.createnewDecisionPt, "");
		if(!(dpts == null) && dpts.size() > 0) {
			logg(S3eConstants.logDebug, "MD determination: Found " + dpts.size() + " Pot Types", util, rparams);
			hpts.put(S3eConstants.MDDETHMKEY, dpts);
		}
		else logg(S3eConstants.logDebug, "No Pot Types found for MD determination", util, rparams);
		
		return hpts;
	}
	
	public static int convertStringToInteger(String text, PluginUtilInterface util) {
		if(text == null || util == null) return S3eConstants.NOTANINT;
		
		ValueObject vo = util.convertStringToValueObject(text);
		if(!(vo.getType().equals(ExtConstants.valuetypeINTEGER))) return S3eConstants.NOTANINT;
		
		return vo.getIntegerval();
	}
	
	public static String convertInstantToTimestampString(Instant inst, PluginUtilInterface util) {
		if(inst == null || util == null) return "";
		return util.convertInstantToStringSimple(inst);
	}
	
	public static Instant convertTimestampStringToInstant(String text, PluginUtilInterface util) {
		if(text == null || text.equals("")) return S3eConstants.NOTATIMESTAMP; 
		ValueObject vo = util.convertStringToValueObject(text);
		if(vo.getType().equals(ExtConstants.valuetypeINSTANT)) return vo.getInstantval();
		return S3eConstants.NOTATIMESTAMP;
	}

	public static String convertInstantToNumericString(Instant inst) {
		if(inst == null) return "";
		String instasec = String.valueOf(inst.getLong(ChronoField.INSTANT_SECONDS));
		String instanano = String.valueOf(inst.getLong(ChronoField.NANO_OF_SECOND));
		return instasec + "." + instanano;
	}
	
	public static Instant convertNumericStringToInstant(String text) {
		if(text == null || text.equals("")) return S3eConstants.NOTATIMESTAMP;
		long insecs = 0L;
		long innanos = 0L;
		try {
			String[] pts = text.split("\\.");
			if(pts.length < 2) {
				return S3eConstants.NOTATIMESTAMP;
			}
			else {
				try {
					insecs = Long.parseLong(pts[0]);
				} catch(Exception e) {
					return S3eConstants.NOTATIMESTAMP;
				}
				try {
					innanos = Long.parseLong(pts[1]);
				} catch(Exception e) {
					return S3eConstants.NOTATIMESTAMP;
				}
			}
		} catch(Exception e) {
			return S3eConstants.NOTATIMESTAMP;
		}
		try {
			return Instant.ofEpochSecond(insecs, innanos);
		} catch(Exception e) {
			return S3eConstants.NOTATIMESTAMP;
		}
	}
	
	public static double convertStringToNumeric(String text, PluginUtilInterface util) {
		if(text == null || util == null) return S3eConstants.NOTNUMERIC;
		
		ValueObject vo = util.convertStringToValueObject(text);
		if(vo.getType().equals(ExtConstants.valuetypeINTEGER)) return vo.getIntegerval();
		if(vo.getType().equals(ExtConstants.valuetypeDOUBLE)) return vo.getDoubleval();
		
		return S3eConstants.NOTNUMERIC;
	}
	
	public static String convertMillisToDurationString(long millis) {
		String ret = "";
		
		long days = Math.floorDiv(millis, 86400000L);
		millis = millis - days * 86400000L;
		long hours = Math.floorDiv(millis, 3600000L);
		millis = millis - hours * 3600000L;
		long minutes = Math.floorDiv(millis, 60000L);
		millis = millis - minutes * 60000L;
		long seconds = Math.floorDiv(millis, 1000L);
		millis = millis - seconds * 1000L;
		
		ret = String.valueOf(millis);
		ret = S3eConstants.durationSeparatorRegex + ret;
		ret = String.valueOf(seconds) + ret;
		ret = S3eConstants.durationSeparatorRegex + ret;
		ret = String.valueOf(minutes) + ret;
		ret = S3eConstants.durationSeparatorRegex + ret;
		ret = String.valueOf(hours) + ret;
		ret = S3eConstants.durationSeparatorRegex + ret;
		ret = String.valueOf(days) + ret;
		
		return ret;
	}
	
	public static long convertLongStringToMillis(String text, 
			RuntimeParameters rparam, PluginUtilInterface util) {
		long ret = S3eConstants.NOTADURATION;
		
		if(text == null || text.equals("")) return 0L;
		
		try {
			ret = Long.parseLong(text);
		} catch(Exception e) {
			return ret;
		}
		
		return ret;
	}
	
	public static long convertDurationStringToMillis(String text, 
			RuntimeParameters rparam, PluginUtilInterface util) {
		if(text == null || text.equals("") || util == null) {
			S3eUtil.logg(S3eConstants.logDebug, "Duration conversion: can not start at all...", util, rparam);
			return S3eConstants.NOTADURATION;
		}
		
		// The duration is a tricky thing. It expects a String in format
		// "days"
		// "days:hours"
		// "days:hours:minutes"
		// "days:hours:minutes:seconds"
		// "days:hours:minutes:seconds:millis"
		
		S3eUtil.logg(S3eConstants.logDebug, "START converting duration: " + text, util, rparam);
		
		// So first split the text by ":"
		try {
			S3eUtil.logg(S3eConstants.logDebug, "Start splitting at "
					+ S3eConstants.durationSeparatorRegex, util, rparam);
			String[] pts = text.split(S3eConstants.durationSeparatorRegex);
			S3eUtil.logg(S3eConstants.logDebug, "Splitted.", util, rparam);
			if(pts == null || pts.length == 0) {
				S3eUtil.logg(S3eConstants.logDebug, "... but split is null or empty.", util, rparam);
				return S3eConstants.NOTADURATION;
			}
			
			S3eUtil.logg(S3eConstants.logDebug, "START parsing the elements, parts: " + pts.length, util, rparam);
			double seconds = 0L;
			if(pts.length > 0) {
				S3eUtil.logg(S3eConstants.logDebug, "Try parsing " + pts[0], util, rparam);
				double days = convertStringToNumeric(pts[0], util);
				if(days == S3eConstants.NOTNUMERIC) return S3eConstants.NOTADURATION;
				else seconds = seconds + days * 86400;
			}
			if(pts.length > 1) {
				S3eUtil.logg(S3eConstants.logDebug, "Try parsing " + pts[1], util, rparam);
				double hours = convertStringToNumeric(pts[1], util);
				if(hours == S3eConstants.NOTNUMERIC) return S3eConstants.NOTADURATION;
				else seconds = seconds + hours * 3600;
			}
			if(pts.length > 2) {
				S3eUtil.logg(S3eConstants.logDebug, "Try parsing " + pts[2], util, rparam);
				double minutes = convertStringToNumeric(pts[2], util);
				if(minutes == S3eConstants.NOTNUMERIC) return S3eConstants.NOTADURATION;
				else seconds = seconds + minutes * 60;
			}
			if(pts.length > 3) {
				S3eUtil.logg(S3eConstants.logDebug, "Try parsing " + pts[3], util, rparam);
				double secs = convertStringToNumeric(pts[3], util);
				if(secs == S3eConstants.NOTNUMERIC) return S3eConstants.NOTADURATION;
				else seconds = seconds + secs;
			}
			if(pts.length > 4) {
				S3eUtil.logg(S3eConstants.logDebug, "Try parsing " + pts[4], util, rparam);
				double millis = convertStringToNumeric(pts[4], util);
				if(millis == S3eConstants.NOTNUMERIC) return S3eConstants.NOTADURATION;
				else seconds = seconds + millis / 1000;
			}
			
			// Now multipy seconds by 1000 to get millis and convert to long
			seconds = seconds * 1000;
			
			
			return Double.valueOf(seconds).longValue();
			
		} catch (Exception e) {
			S3eUtil.logg(S3eConstants.logDebug, "Exception happened", util, rparam);
			return S3eConstants.NOTADURATION;
		}
		
	}
	
	public static Boolean convertStringToBoolean(String text, PluginUtilInterface util) {
		if(text == null || util == null) return false;
		
		ValueObject vo = util.convertStringToValueObject(text);
		if(!(vo.getType().equals(ExtConstants.valuetypeBOOLEAN))) return null;
		
		return vo.getBooleanval();
	}
	
	public static boolean determineMdFromPt(MultiProperty params, MultiProperty attributes,
			RuntimeParameters rparam, HashMap<String, ArrayList<PotType>> pottypes,
			PluginUtilInterface util) {
		
		// rules:
		// - do not change a parameter if have already value
		// - return true if the pot type and AT LEAST ONE characteristic md found
		
		logg(S3eConstants.logDebug, "START determining master data from pot types", util, rparam);
		
		// Check, maybe we are already fine
		if(isParamsFinal(params, rparam, util)) {
			logg(S3eConstants.logDebug, "Not needed, we have everything", util, rparam);
			return true;
		}
		
		logg(S3eConstants.logDebug, "Determination is based on the following properties:", util, rparam);
		for(SingleProperty sp: attributes.getList()) {
			logg(S3eConstants.logDebug, sp.getKey() + " = " + sp.getValue(), util, rparam);
		}
		
		// Get the MD determination pot types
		logg(S3eConstants.logDebug, "Get the master data determination pot types", util, rparam);
		ArrayList<PotType> mdpts = pottypes.get(S3eConstants.MDDETHMKEY);
		if(mdpts == null || mdpts.size() == 0) {
			logg(S3eConstants.logDebug, "No such pot type, cancel.", util, rparam);
			return false;
		}
		
		// Check the Pot Types one by one
		logg(S3eConstants.logDebug, "Loop on the MD determination pot types", util, rparam);
		for(PotType pt: mdpts) {
			logg(S3eConstants.logDebug, "Try pot type " + pt.getType(), util, rparam);
			if(determineMdFromOnePt(params, attributes, rparam, pt, util)) return true;
		}
		
		return false;
	}
	
	private static boolean determineMdFromOnePt(MultiProperty params, MultiProperty attributes,
			RuntimeParameters rparam, PotType pottype,
			PluginUtilInterface util) {
		
		// We need the master data of the Pot Type
		Attr mdattr = pottype.getFixed();
		if(mdattr == null) return false;
		
		// Get the list of the highest level nodes. They are the attr names,
		// like "Active Ingredient,Source Country,Destination Country"
		HashMap<String,Attr> hmtop = util.getNodes(mdattr, "");
		
		logg(S3eConstants.logDebug, "Iterate the property keys.", util, rparam);
		// Iterate. The 2nd level is the actual property values,
		// like "api01,sc01,dc01"
		if(!(hmtop == null) && !(hmtop.entrySet() == null)) {
			Iterator<Entry<String, Attr>> it = hmtop.entrySet().iterator();
			if(!(it == null)) {
				while (it.hasNext()) {
					HashMap.Entry<String, Attr> pair = (HashMap.Entry<String, Attr>)it.next();
					if(!(pair == null) && !(pair.getKey() == null) && !(pair.getValue() == null)) {
						String key = ( String ) pair.getKey();
						Attr value = ( Attr ) pair.getValue();
						logg(S3eConstants.logDebug, "Key: " + key, util, rparam);
						if(determineMdFromOnePtNode(params, attributes, rparam, 
								key, value, util)) return true;
					}
				}
			}
		}
		
		return false;
	}
	
	private static boolean determineMdFromOnePtNode(MultiProperty params, MultiProperty attributes,
			RuntimeParameters rparam, String propkey, Attr prattr,
			PluginUtilInterface util) {
		
		// So here propkey is like "Active Ingredient,Source Country,Destination Country"
		// which correspond to lines in "attributes" key.
		// prattr top levels are like "api01,sc01,dc01",
		// and under them the values
		
		// We need to split the key
		String propstring = getPropertyString(attributes, rparam, propkey, util);
		if(propstring.equals("")) {
			logg(S3eConstants.logDebug, "No data for this property key.", util, rparam);
			return false;
		}
		logg(S3eConstants.logDebug, "Property string = " + propstring, util, rparam);
		
		// Now get the top level Attrs and iterate
		HashMap<String,Attr> prtop = util.getNodes(prattr, "");
		
		logg(S3eConstants.logDebug, "Iterate the property values", util, rparam);
		// Iterate. The 2nd level are the pot type and master data types
		if(!(prtop == null) && !(prtop.entrySet() == null)) {
			Iterator<Entry<String, Attr>> it = prtop.entrySet().iterator();
			if(!(it == null)) {
				while (it.hasNext()) {
					HashMap.Entry<String, Attr> pair = (HashMap.Entry<String, Attr>)it.next();
					if(!(pair == null) && !(pair.getKey() == null) && !(pair.getValue() == null)) {
						String key = ( String ) pair.getKey();
						Attr value = ( Attr ) pair.getValue();
						
						logg(S3eConstants.logDebug, "Current node: " + key, util, rparam);
						
						// We are fine if the key = propstring
						if(key.equals(propstring)) {
							
							logg(S3eConstants.logDebug, "Equals, take the parameters", util, rparam);
							
							// Then we need to get the nodes
							ArrayList<NodeValue> anvs = util.nodeValueList(value, "", false);
							if(!(anvs == null)) {
								for(NodeValue nv: anvs) {
									for(SingleProperty sp: params.getList()) {
										if(nv.getNode().equals(sp.getKey()))
											sp.setValue(nv.getValue());
									}
								}
							}
							
							// Check if we are fine
							if(isParamsFinal(params, rparam, util)) return true;
							else logg(S3eConstants.logDebug, "... but not enough parameters were given", util, rparam);
							
						}
						
					}
				}
			}
		}
		
		
		return false;
	}
	
	private static String getPropertyString(MultiProperty attributes, 
			RuntimeParameters rparam, String propkey, PluginUtilInterface util) {
		
		// Split the key
		ArrayList<String> keyparts = new ArrayList<>();
		if(propkey.contains(rparam.createnewDecisionPtSeparator)) {
			try {
				String[] pts = propkey.split(rparam.createnewDecisionPtSeparator);
				Collections.addAll(keyparts, pts);
			} catch (Exception e) {
				return "";
			}
		}
		else keyparts.add(propkey);
		
		// Now try to make our property string
		String propstring = "";
		for(String onekey: keyparts) {
			boolean found = false;
			for(SingleProperty sc: attributes.getList()) {
				if(onekey.equals(sc.getKey())) {
					found = true;
					if(propstring.equals("")) {
						propstring = sc.getValue();
					}
					else {
						propstring = propstring + rparam.createnewDecisionPtSeparator + sc.getValue(); 
					}
				}
			}
			if(!found) return "";
		}
		
		return propstring;
	}
	
	private static boolean isParamsFinal(MultiProperty params, RuntimeParameters rparam, 
			PluginUtilInterface util) {
		if(params == null || params.getList() == null) return false;
		
		logg(S3eConstants.logDebug, "START Check if every parameter have", util, rparam);
		
		boolean pottypeok = false;
		boolean mdok = false;
		
		logg(S3eConstants.logDebug, "Loop on the parameters", util, rparam);
		for(SingleProperty sp: params.getList()) {
			logg(S3eConstants.logDebug, "Check " + sp.getKey() + " = " + sp.getValue(), util, rparam);
			// Pot type
			logg(S3eConstants.logDebug, "Is it the pot type, key " + rparam.createnewFnameTargetPt, util, rparam);
			if(sp.getKey().equals(rparam.createnewFnameTargetPt)) {
				logg(S3eConstants.logDebug, "Yes, and the value is " + sp.getValue(), util, rparam);
				if(!(sp.getValue().equals(""))) pottypeok = true; 
			}
			
			// characteristics
			logg(S3eConstants.logDebug, "Check the characteristics", util, rparam);
			for(SingleCharacteristic sc: rparam.characteristics) {
				logg(S3eConstants.logDebug, "Characteristic = " + sp.getKey(), util, rparam);
				logg(S3eConstants.logDebug, "MD field = " + sc.getMdfield(), util, rparam);
				if(sp.getKey().equals(sc.getMdfield()) && !(sp.getValue().equals(""))) {
					logg(S3eConstants.logDebug, "Cool, found.", util, rparam);
					mdok = true;
				}
			}
		}
		
		// It is possible that pot type was not in the master data.
		// In case there is only 1 pot type defined in the rparam, we will use that.
		if(!(pottypeok)) {
			if(!(rparam.pottypes == null) && rparam.pottypes.size() == 1) {
				try {
					String pt = rparam.pottypes.get(0);
					SingleProperty sp = new SingleProperty();
					sp.setKey(rparam.createnewFnameTargetPt);
					sp.setValue(pt);
					params.add(sp);
					pottypeok = true;
				} catch (Exception e) {}
			}
		}
		
		logg(S3eConstants.logDebug, "Pot type found = " + pottypeok, util, rparam);
		logg(S3eConstants.logDebug, "Master data type found = " + mdok, util, rparam);
		
		return pottypeok && mdok;
	}
	
	public static Attr convertRawAttrToFinalAttr(Attr attr, 
			RuntimeParameters rparam, HashMap<String, ArrayList<PotType>> pottypes,
			PluginUtilInterface util) {
		return util.deepCopyAttr(attr);
	}
	
	public static void alertPotExistsAlready(String key, String value, RuntimeParameters rparam, PluginUtilInterface util) {
		
		String subject = "Creation attempt: object exists already.";
		if(key == null) key = "";
		if(value == null) value = "";
		
		String text = "The system received a " + rparam.imsgcommandCreateNew  + " command."
				+ System.lineSeparator();
		text = text + "At least one active object exists already with: " + System.lineSeparator();
		text = text + "ID key = " + key + System.lineSeparator();
		text = text + "ID value = " + value + System.lineSeparator() + System.lineSeparator();
		
		text = text + "If you want to replace it, please use the command " + rparam.imsgcommandCreateReplace;
		
		sendAlert(ExtConstants.alertseverityERROR,
				"",
				key,
				value,
				subject,
				text,
				rparam,
				util);
		
	}
	
	public static void alertSetupIncorrect(String msg, RuntimeParameters rparam, PluginUtilInterface util) {
		
		String subject = "Configuration error";
		if(msg == null || msg.equals("")) msg = subject;
		
		String text = "The system has the following configuration error:"
				+ System.lineSeparator() + System.lineSeparator();
		text = text + msg + System.lineSeparator() + System.lineSeparator();
		text = text + "Please correct the configuration." + System.lineSeparator() + System.lineSeparator();
		
		sendAlert(ExtConstants.alertseverityERROR,
				"",
				"",
				"",
				subject,
				text,
				rparam,
				util);
		
	}
	
	public static void alertMdIncorrect(ArrayList<SingleLocMessage> coll, 
			String pottype, 
			String envvar,
			String mdname,
			RuntimeParameters rparam, PluginUtilInterface util) {
		if(rparam == null || util ==  null) {
			logg(S3eConstants.logError, "Alerting: wrong call", util, rparam);
			return;
		}
		if(coll == null) coll = new ArrayList<SingleLocMessage>();
		if(pottype == null) pottype = "";
		if(envvar == null) envvar = "";
		if(mdname == null) mdname = "";
		
		String subject = "Master data error";
		
		String text = "The following master data definition contains errors:"
				+ System.lineSeparator() + System.lineSeparator();
		text = text + "in Pot Type = " + pottype + System.lineSeparator();
		text = text + "Environment characteristic = " + envvar + System.lineSeparator();
		text = text + "Master data name = " + mdname + System.lineSeparator() + System.lineSeparator();
		
		text = text + "The following locations are incorrect:" + System.lineSeparator();
		for(SingleLocMessage slm: coll) {
			text = text + slm.getLocation() + ": " + slm.getText() + System.lineSeparator();
		}
		text = text + System.lineSeparator();
		
		text = text + "Please fix the master data definition in the Pot Type.";
		
		sendAlert(ExtConstants.alertseverityERROR,
				pottype,
				envvar,
				mdname,
				subject,
				text,
				rparam,
				util);
		
	}
	
	public static void alertNoMdDetermined(Pot pot, RuntimeParameters rparam, PluginUtilInterface util) {
		
		if(pot == null || rparam == null || util ==  null) {
			logg(S3eConstants.logError, "Alerting: wrong call", util, rparam);
			return;
		}
		
		String subject = "No master data defined";
		
		String text = "For the following object no master data could be determined:"
			+ System.lineSeparator() + System.lineSeparator();
		text = text + pot.getMatchkey0() + " " + pot.getMatchval0()
			+ System.lineSeparator() + System.lineSeparator();
		text = text + "Primary attributes:"
			+ System.lineSeparator() + System.lineSeparator();
		
		ArrayList<NodeValue> pratts = util.nodeValueList(pot.getFlexi(), rparam.createnewFnamePrimAtt, false);
		if(!(pratts == null) && pratts.size() > 0) {
			for(NodeValue nv: pratts) {
				text = text + nv.getNode() + " = " + nv.getValue() + System.lineSeparator(); 
			}
		}
		
		text = text + System.lineSeparator(); 
		text = text + "The tracking object is saved as:" + System.lineSeparator() + System.lineSeparator();
		
		text = text + "Pot type = " + rparam.createnewErrorPotType + System.lineSeparator();
		text = text + "Match key 0 = " + pot.getMatchkey0() + System.lineSeparator();
		text = text + "Match value 0 = " + pot.getMatchval0() + System.lineSeparator();
		text = text + System.lineSeparator();
		text = text + "Please maintain the master data determination rules and resend the creation request.";
		
		
		sendAlert(ExtConstants.alertseverityERROR,
				pot.getType(),
				pot.getMatchkey0(),
				pot.getMatchval0(),
				subject,
				text,
				rparam,
				util);
	}
	
	public static void sendAlert(
			String severity,
			String pottype,
			String key,
			String value,
			String subject,
			String text,
			RuntimeParameters rparam,
			PluginUtilInterface util) {
		
		ExtAlert exa = new ExtAlert();
		exa.setAlertseverity(severity);
		exa.setAlerttext(text);
		exa.setAlertsubject(key + " " + value + " - " + subject);
		
		ArrayList<String> targets = null;
		// Have pottype, then by that
		if(!(pottype == null) && !(pottype.equals(""))) {
			targets = rparam.targets.get(pottype);
		}
		// If still null, try the default
		if(targets == null || targets.size() == 0) {
			targets = rparam.targets.get(S3eConstants.bpK2DefaultTarget);
		}
		// If still nothing, we cannot send alert, log it
		if(targets == null || targets.size() == 0) {
			logg(S3eConstants.logError, "ERROR: Cannot send alert, targets not configured.", util, rparam);
			return;
		}
		
		// Ok, send an alert to all targets defined
		for(String target: targets) {
			exa.setAlerttarget(target);
			logg(S3eConstants.logDebug, "Sending " + severity + " alert to target " + exa.getAlerttarget(), util, rparam);
			util.sendAlert(exa);
		}
		
	}
	
	public static boolean makeFinalNewPot(Pot pot, 
			RuntimeParameters rparam,
			HashMap<String, ArrayList<PotType>> pottypes,
			PluginUtilInterface util) {
		
		// If anything is null, error (should not happen)
		if(pot == null || rparam == null || pottypes == null || util == null) {
			S3eUtil.logg(S3eConstants.logError, "Something is null, cannot continue.", util, rparam);
			return false;
		}
		
		// Extract the parameters
		if(pot.getFlexi() == null) {
			S3eUtil.logg(S3eConstants.logError, "Pot flexi is null, cannot continue.", util, rparam);
			return false;
		}
		ArrayList<NodeValue> nvl = util.nodeValueList(pot.getFlexi(), rparam.createnewFnameParams, false);
		if(nvl == null || nvl.size() == 0) {
			S3eUtil.logg(S3eConstants.logError, "Pot flexi has no parameters, cannot continue.", util, rparam);
			return false;
		}
		
		// Before forget, clean the FINAL from the key 1
		pot.setMatchkey1("");
		
		// The pot has its primary and seconday IDs, we need to prepare 3 things:
		// - the final pot type
		// - the fixed attr
		// - the flexi attr
		String pottype = "";
		boolean hasAtLeastOneChar = false;
		Attr fixed = new Attr();
		
		// Loop on the params and process one by one
		for(NodeValue nv: nvl) {
			if(nv.getNode().equals(rparam.createnewFnameTargetPt)) {
				if(!(nv.getValue() == null && !(nv.getValue().equals("")))) {
					pottype = nv.getValue();
				}
			}
			else { // should be something environmental variable, try to find
				for(SingleCharacteristic sc: rparam.characteristics) {
					if(nv.getNode().equals(sc.getMdfield())) {
						
						// OK, it is defined, so check if we have master data for it.
						ArrayList<PotType> pts = pottypes.get(sc.getName());
						if(!(pts == null) && pts.size() > 0) {
							// Try to find the master data by the name in nv
							for(PotType pt: pts) {
								HashMap<String,Attr> mdlist = util.getNodes(pt.getFixed(), "");
								if(!(mdlist == null) && mdlist.size() > 0) {
									Attr mdtemplate = mdlist.get(nv.getValue());
									if(!(mdtemplate == null)) {
										// Make a copy, not to destroy the pot type data
										Attr md = util.deepCopyAttr(mdtemplate);
										ArrayList<SingleLocMessage> slms 
											= S3eMdUtil.checkAndEnhanceMd(md, rparam, util);
										if(!(slms == null) && slms.size() > 0) {
											S3eUtil.logg(S3eConstants.logError, "Master data is incorrect. Sending Alert.", util, rparam);
											alertMdIncorrect(slms, 
													pt.getType(),
													sc.getName(),
													nv.getValue(),
													rparam, util);
										}
										else { // Master data is fine
											
											/*
											 * So now:
											 * sc.getName() = the envvar (e.g. "temperature")
											 * nv.getValue() = the md name (e.g. "TempMaster 01")
											 * md = the master data itself, like
											 * 		crossing
											 * 			...
											 * 		intervals
											 * 			...
											 * 		settings
											 * 			...
											 * So just add to the fixed
											 */
											S3eUtil.logg(S3eConstants.logDebug, "Master data is correct, adding to Pot.", util, rparam);
											S3eMdUtil.addMdToFixed(fixed, 
													sc.getName(), nv.getValue(), md,
													rparam, util);
											hasAtLeastOneChar = true;
										}
									}
								}
							}
						}						
					}
				}
			}
		}
		
		
		// Finalize the flexi, add some field for later usage
		util.setNodeValue(pot.getFlexi(), rparam.gentextStatusOk, rparam.gentextStatus);
		util.setNodeValue(pot.getFlexi(), "", rparam.gentextLastMessage);
		util.setNodeValue(pot.getFlexi(), "", rparam.gentextLastMessage + "." + rparam.gentextMessageSeverity);
		util.setNodeValue(pot.getFlexi(), "", rparam.gentextLastMessage + "." + rparam.gentextMessageText);
		util.setNodeValue(pot.getFlexi(), "", rparam.gentextLastMessage + "." + rparam.gentextMessageTime);
		util.setNodeValue(pot.getFlexi(), "", rparam.gentextLastError);
		util.setNodeValue(pot.getFlexi(), "", rparam.gentextLastError + "." + rparam.gentextMessageText);
		util.setNodeValue(pot.getFlexi(), "", rparam.gentextLastError + "." + rparam.gentextMessageTime);
		
		// Finalize the pot
		if(pottype.equals("") || !(hasAtLeastOneChar)) {
			S3eUtil.logg(S3eConstants.logError, "No Pot Type or no characteristic, stop.", util, rparam);
			return false;
		}
		pot.setType(pottype);
		pot.setFixed(fixed);
		return true;
	}
	
	public static double round(double value, int places) {
		if(places < 0) return S3eConstants.NOTNUMERIC;
		BigDecimal bd = new BigDecimal(Double.toString(value));
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}
	
	public static ArrayList<SingleInterval> readIntervalListNode(Attr ivnode, PluginUtilInterface util) {
		ArrayList<SingleInterval> ret = new ArrayList<>();
		if(ivnode == null) return ret;
		
		ArrayList<NodeNode> nns = util.nodeNodeList(ivnode, "", true); // Get sorted by node name
		if(nns == null || nns.size() == 0) return ret;
		
		for(NodeNode nn: nns) {
			SingleInterval si = new SingleInterval();
			
			ArrayList<NodeValue> nnns = util.nodeValueList(nn.getSubnode(), "", false);
			if(nnns == null || nnns.size() == 0) continue;
			
			for(NodeValue nnn: nnns) {
				switch(nnn.getNode()) {
				case S3eConstants.IVLISTFROM: si.setFrom(convertStringToNumeric(nnn.getValue(), util)); break;
				case S3eConstants.IVLISTTO: si.setTo(convertStringToNumeric(nnn.getValue(), util)); break;
				case S3eConstants.IVLISTLOC: si.setLocation(nnn.getValue()); break;
				default: break;
				}
			}
			
			ret.add(si);
			
		}
		
		try {
			Collections.sort(ret);
		} catch(Exception e) {}
		
		return ret;
	}
	
	public static ArrayList<MultiTelemetry> copyAttrToMeasurements(Attr attr, PluginUtilInterface util) {
		ArrayList<MultiTelemetry> ret = new ArrayList<>();
		if(attr == null || util == null) return ret;
		
		ArrayList<NodeNode> nns = util.nodeNodeList(attr, "", true);
		if(nns == null || nns.size() == 0) return ret;
		
		// We have one node per timestamp, sorted.
		for(NodeNode nn: nns) {
			ArrayList<NodeValue> nvs = util.nodeValueList(nn.getSubnode(), "", false);
			if(!(nvs == null) && nvs.size() > 0) {
				MultiTelemetry mt = new MultiTelemetry();
				for(NodeValue nv: nvs) {
					if(nv.getNode().equals(S3eConstants.TIMESTAMP)) {
						mt.setTimestamp(convertNumericStringToInstant(nv.getValue()));
					}
					else {
						SingleTelemetry st = new SingleTelemetry();
						st.setC12c(nv.getNode());
						try {
							String[] pts = nv.getValue().split(S3eConstants.UOMSEPARATOR);
							if(pts == null || pts.length < 1) continue;
							st.setValue(convertStringToNumeric(pts[0], util));
							if(pts.length > 1) {
								st.setUom(pts[1]);
							}
						} catch(Exception e) {
							continue;
						}
						mt.add(st);
					}
				}
				ret.add(mt);
			}
		}
		
		return ret;
	}
	
	public static double convertUom(double value, String formula, RuntimeParameters rparam, PluginUtilInterface util) {
		if(formula == null || formula.equals("") || rparam.pe == null) 
			return Double.NaN;
		
		String dst = String.valueOf(value);
		try {
			formula = formula.replaceAll(S3eConstants.FORMULAX, dst);
		} catch(Exception e) { return Double.NaN; }
		
		S3eUtil.logg(S3eConstants.logDebug, "Executing UOM conversion using: " + formula, util, rparam);
		
		Expression exp = new Expression(formula);
		// .calculate always return a good double, or a NaN
		return exp.calculate();
	}
	
	public static Attr createPropertyAttr(MultiProperty mp, RuntimeParameters rparam, PluginUtilInterface util) {
		Attr ret = new Attr();
		
		if(!(mp == null) && !(mp.getList() == null)
				&& mp.getList().size() > 0) {
			for(SingleProperty sc: mp.getList()) {
				util.setNodeValue(ret, sc.getValue(), sc.getKey());
			}
		}
		
		return ret;
	}
	
}
