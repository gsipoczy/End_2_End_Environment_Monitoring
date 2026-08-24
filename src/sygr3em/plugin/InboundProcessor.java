package sygr3em.plugin;

import java.util.ArrayList;
import java.util.HashMap;

import com.fasterxml.jackson.core.JsonProcessingException;

import sygr.pots.extensions.*;
import sygr3em.model.InboundMessage;
import sygr3em.model.RuntimeParameters;
import sygr3em.model.SingleTransfer;
import sygr3em.service.S3eConstants;
import sygr3em.service.S3eImsgUtil;
import sygr3em.service.S3eUtil;

public class InboundProcessor  implements PluginInterface{
	
	private RuntimeParameters rparams = null;
	private HashMap<String, ArrayList<PotType>> pottypes = null;
	private volatile boolean updating = false;
	private long updatesleep = 0L;

	@Override
	public void execute(PluginData data, PluginUtilInterface util) {
		
		if(util == null) return;
		
		// If not done yet, initialize buffers
		if(rparams == null) rparams = S3eUtil.readBusinessParams(util);
		updatesleep = rparams.updatesleep;
		if(pottypes == null) pottypes = S3eUtil.readPotTypes(util, rparams);
		
		// Initial correctness check
		if(!(initialChecks(data, util))) return;		
		
		// Wait if updating
		if(updating) {
			try {
				Thread.sleep(updatesleep);
			} catch(Exception e) {}
		}
		
		S3eUtil.logg(S3eConstants.logDebug, "START processing incoming message", util, rparams);
		
		// Understand the message
		InboundMessage msg = understandMessage(data, util);
		// If null, it was not understandable
		if(msg == null || msg.getCommand() == null) return;
		
		S3eUtil.logg(S3eConstants.logDebug, "Correct message received", util, rparams);
		
		// From now the processing depends on the command
		
		// Create New Pot Object
		if(msg.getCommand().equals(rparams.imsgcommandCreateNew) ||
				msg.getCommand().equals(rparams.imsgcommandCreateReplace)) {
			S3eImsgUtil.processCreateNew(msg, util, rparams, pottypes, data);
			return;
		}
		
		// Telemetry
		if(msg.getCommand().equals(rparams.imsgcommandTelemetry)) {
			S3eImsgUtil.processTelemetry(msg, util, rparams, pottypes, data);
			return;
		}
		
		// Set task
		if(msg.getCommand().equals(rparams.imsgcommandSetTask)) {
			S3eImsgUtil.processSetTask(msg, util, rparams, pottypes, data);
			return;
		}
		
		// Update unit
		if(msg.getCommand().equals(rparams.imsgcommandUpdateUnit)) {
			S3eImsgUtil.processUpdateUnit(msg, util, rparams, pottypes, data);
			return;
		}
		
		// Update Secondary Attributes
		if(msg.getCommand().equals(rparams.imsgcommandUpdateSecAttr)) {
			S3eImsgUtil.processUpdateSecAttr(msg, util, rparams, pottypes, data);
			return;
		}
		
		// Test Master Data / Get data for update
		// id.key = envvar type (like "temperature"), id.value = master data name
		if(msg.getCommand().equals(rparams.imsgcommandTestMasterData)
				|| msg.getCommand().equals(rparams.imsgcommandTestMasterDataLink)
				|| msg.getCommand().equals(rparams.imsgcommandGetUnitForEdit)) {
			if(data.transfer == null) {
				S3eUtil.logg(S3eConstants.logDebug, "transfer is null! Stop.", util, rparams);
				return;
			}
			// Create command
			SingleTransfer cm = new SingleTransfer();
			cm.setType(S3eConstants.COMMAND);
			cm.setValue(msg.getCommand());
			data.transfer.add(cm);
			
			// Transfer the data
			SingleTransfer st = new SingleTransfer();
			st.setType(S3eConstants.DATA);
			st.setKey(msg.getId().getKey());
			st.setValue(msg.getId().getValue());
			data.transfer.add(st);
			return;
		}
		
		// Change Pot Status
		if(msg.getCommand().equals(rparams.imsgcommandSetActive) || 
				msg.getCommand().equals(rparams.imsgcommandSetDeleted) ||
				msg.getCommand().equals(rparams.imsgcommandSetDeletedForce) ||
				msg.getCommand().equals(rparams.imsgcommandSetInactive) ||
				msg.getCommand().equals(rparams.imsgcommandSetPaused)) {
			S3eImsgUtil.processSetPotStatus(msg, util, rparams, pottypes, data);
			return;
		}
		
		// Add/remove IDs
		if(msg.getCommand().equals(rparams.imsgcommandAddIds) || 
				msg.getCommand().equals(rparams.imsgcommandDeleteIds)) {
			S3eImsgUtil.processIds(msg, util, rparams, pottypes, data, false);
			return;
		}
		
		// Merge
		if(msg.getCommand().equals(rparams.imsgcommandMergeExisting)) {
			S3eImsgUtil.processMerge(msg, util, rparams, pottypes, data, true);
			return;
		}
		
		// Split
		if(msg.getCommand().equals(rparams.imsgcommandSplit)) {
			S3eImsgUtil.processSplit(msg, util, rparams, pottypes, data, true);
			return;
		}
		
		if(msg.getCommand().equals(rparams.imsgcommandMergeNew)) {
			// First create the new
			S3eImsgUtil.processCreateNew(msg, util, rparams, pottypes, data);
			// Wait until done
			data.messageOut.setMsbeforeupdaters(rparams.mergewaitms);
			// Then merge
			S3eImsgUtil.processMerge(msg, util, rparams, pottypes, data, false);
			return;
		}
		
		// Invalid command
		String text = "Invalid inbound command arrived: " + System.lineSeparator()  + System.lineSeparator();
		text = text + "ID: " + msg.getId().getKey() + " = " + msg.getId().getValue()
				 + System.lineSeparator()  + System.lineSeparator();
		text = text + "Command: " + msg.getCommand();
		S3eUtil.sendAlert(ExtConstants.alertseverityERROR, "", 
				msg.getId().getKey(), msg.getId().getValue(), 
				"Invalid command in inbound message", text, rparams, util);
		
	}

	@Override
	public void reloadConfig(ArrayList<String> changes, PluginUtilInterface util) {
		if(changes == null || util == null) return;
		
		synchronized(this) { updating = true;}
		
		// Have a look what was modified and refresh the buffers accordingly
		for(String change: changes) {
			if(!(change == null)) {
				if(change.equals(ExtConstants.configchangeBUSINESSPARAM)) {
					rparams = S3eUtil.readBusinessParams(util);
				}
				if(change.equals(ExtConstants.configchangePOTTYPE)) {
					pottypes = S3eUtil.readPotTypes(util, rparams);
				}
			}
		}
		
		synchronized(this) { updating = false;}
		updatesleep = rparams.updatesleep;
		
	}
	
	private InboundMessage understandMessage(PluginData data, PluginUtilInterface util) {
		InboundMessage msg = null;
		
		// Try the transfer first
		if(!(data.transfer == null) && data.transfer.size() > 0) {
			for(Object oo: data.transfer) {
				if(oo instanceof InboundMessage) {
					S3eUtil.logg(S3eConstants.logDebug, "Inbound message arrived in data.transfer", util, rparams);
					msg = ( InboundMessage ) oo;
					return msg;
				}
			}
		}
		
		// If no, do the traditional way
		try {
			msg = data.jsonMapper.readValue(data.messageIn.getContent(), InboundMessage.class);
		} catch (JsonProcessingException e) {
			S3eUtil.logg(S3eConstants.logDebug, "incoming message is not our JSON", util, rparams);
			
			try {
				if(data.messageIn.getContent().contains("InboundMessage")) {
					msg = data.xmlMapper.readValue(data.messageIn.getContent(), InboundMessage.class);
					if(msg == null) {
						S3eUtil.logg(S3eConstants.logDebug, "incoming message is not our XML", util, rparams);
						return null;
					}
				}
				else {
					S3eUtil.logg(S3eConstants.logDebug, "incoming message is not our XML", util, rparams);
					return null;
				}
		    } catch (Exception e1) { 
		    	S3eUtil.logg(S3eConstants.logDebug, "incoming message is not our XML", util, rparams);
				return null;
		    }
		}
		if(msg == null) {
			S3eUtil.logg(S3eConstants.logDebug, "incoming message is not our XML", util, rparams);
			return null;
		}
		
		return msg;
		
	}
	
	private boolean initialChecks(PluginData data, PluginUtilInterface util) {
		
		// First make sure that we have util, otherwise even cannot send logs
		if(util == null) return false;
				
		// Then that the data is not null
		if(data == null) {
			S3eUtil.logg(S3eConstants.logError, "Data is null, stop.", util, rparams);
			return false;
		}
				
		// Check that we are called as the correct plugin type (CHANGE!!!)
		if(data.plugintype == null || !(data.plugintype.equals(ExtConstants.pluginusageMSGCONV))) {
			S3eUtil.logg(S3eConstants.logError, "Wrong plugin type, stop.", util, rparams);
			if(!(data.plugintype == null)) {
				String text = "Expected: " + ExtConstants.pluginusageMSGCONV + ", got: " + data.plugintype + ".";
				S3eUtil.logg(S3eConstants.logError, text, util, rparams);
			}
			return false;
		}
		
		// Additional checks
		
		// We need to have an input message, otherwise nothing to do
		if(data.messageIn == null || data.messageIn.getContent() == null) {
			S3eUtil.logg(S3eConstants.logError, "data.messageIn is null, stop.", util, rparams);
			return false;
		}
				
		// Also need an XML and JSON parser.
		if(data.jsonMapper == null) {
			S3eUtil.logg(S3eConstants.logError, "data.jsonMapper is null, stop.", util, rparams);
			util.log("data.jsonMapper is null, stop");
			return false;
		}
		if(data.xmlMapper == null) {
			S3eUtil.logg(S3eConstants.logError, "data.xmlMapper is null, stop.", util, rparams);
			util.log("data.xmlMapper is null, stop");
			return false;
		}
		
		
		// everything fine
		return true;
	}

}
