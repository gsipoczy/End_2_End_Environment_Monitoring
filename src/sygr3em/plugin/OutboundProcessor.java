package sygr3em.plugin;

import java.util.ArrayList;
import java.util.HashMap;

import sygr.pots.extensions.Attr;
import sygr.pots.extensions.ExtConstants;
import sygr.pots.extensions.PluginData;
import sygr.pots.extensions.PluginInterface;
import sygr.pots.extensions.PluginUtilInterface;
import sygr.pots.extensions.Pot;
import sygr.pots.extensions.PotType;
import sygr.pots.extensions.SyncResponse;
import sygr3em.model.*;
import sygr3em.service.S3eConstants;
import sygr3em.service.S3eImsgUtil;
import sygr3em.service.S3eMdUtil;
import sygr3em.service.S3eUtil;

public class OutboundProcessor implements PluginInterface {
	
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
		String command = initialChecks(data, util);
		if(command == null || command.equals("")) return;
		
		// Wait if updating
		if(updating) {
			try {
				Thread.sleep(updatesleep);
			} catch(Exception e) {}
		}
				
		S3eUtil.logg(S3eConstants.logDebug, "START processing outgoing message", util, rparams);
		
		// Test Master Data
		if(command.equals(rparams.imsgcommandTestMasterData)) {
			processTestMasterData(data, util);
			return;
		}
		
		// Test Master Data Link
		if(command.equals(rparams.imsgcommandTestMasterDataLink)) {
			processTestMasterDataLink(data, util);
			return;
		}
		
		// Get Pot data for editing
		if(command.equals(rparams.imsgcommandGetUnitForEdit)) {
			processGetUnitForEdit(data, util);
			return;
		}
		
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
	
	private void processGetUnitForEdit(PluginData data, PluginUtilInterface util) {
		
		// For this we need authorization check
		if(data.webid == null) {
			data.response.setAnswer("No authorization information is available.");
			data.response.setStatus(401);
			return;
		}
		
		SingleTransfer foundst = null;
		// Get the info we need
		for(Object ob: data.transfer ) {
			if(ob instanceof SingleTransfer) {
				SingleTransfer st = ( SingleTransfer ) ob;
				if(st.getType().equals(S3eConstants.DATA)) {
					foundst = st;
				}
			}
		}
		
		if(foundst == null) {
			data.response.setAnswer("No ID arrived.");
			data.response.setStatus(401);
			return;
		}
		
		// Get the pot
		ArrayList<SinglePotIdWithStoretype> potids 
			= S3eImsgUtil.getExistingPots(foundst.getKey(), foundst.getValue(), rparams, util);
		if(potids == null || potids.size() == 0) {
			data.response.setAnswer("Unit not found.");
			data.response.setStatus(401);
			return;
		}
		
		if(potids.size() > 1) {
			data.response.setAnswer("Multiple units found, some must be deleted first.");
			data.response.setStatus(401);
			return;
		}
		
		SinglePotIdWithStoretype ourpot = null;
		try {
			ourpot = potids.get(0);
		} catch(Exception e) {}
		if(ourpot == null) {
			data.response.setAnswer("Null data received for Unit.");
			data.response.setStatus(401);
			return;
		}
		
		// Get the Pot
		Pot pot = util.getPot(ourpot.getStoretype(), ourpot.getPotid());
		if(pot == null) {
			data.response.setAnswer("Null data received from util.getPot.");
			data.response.setStatus(401);
			return;
		}
		if(!(util.fits(data.webid, ExtConstants.authMAINTAINPT, pot.getType()))) {
			data.response.setAnswer("No authorization " + ExtConstants.authMAINTAINPT + " " + pot.getType());
			data.response.setStatus(401);
			return;
		}
		if(!(rparams.autheditunit.equals(""))) {
			if(!(util.fits(data.webid, ExtConstants.authMAINTAINPT, rparams.autheditunit))) {
				data.response.setAnswer("No authorization " + ExtConstants.authMAINTAINPT + " " + rparams.autheditunit);
				data.response.setStatus(401);
				return;
			}
		}
		
		// Create the answer
		InboundMessageForEdit imsg = new InboundMessageForEdit();
		imsg.setCommand(rparams.imsgcommandUpdateUnit);
		SingleProperty sp = new SingleProperty();
		sp.setKey(pot.getMatchkey0());
		sp.setValue(pot.getMatchval0());
		imsg.setId(sp);
		imsg.setFixed(pot.getFixed());
		imsg.setFlexi(pot.getFlexi());
		
		// Convert to JSON
		String res = "";
		try {
			res = data.jsonMapper.writeValueAsString(imsg);
		} catch (Exception e) {
			data.response.setAnswer("Error converting message to JSON");
			data.response.setStatus(401);
			return;
		}
		
		data.response.setAnswer(res);
		data.response.setStatus(200);
		
		
	}
	
	private void processTestMasterDataLink(PluginData data, PluginUtilInterface util) {
		
		// For this we need authorization check
		if(data.webid == null) {
			data.response.setAnswer("No authorization information is available.");
			data.response.setStatus(401);
			return;
		}
		
		String pottype = "";
		// Get the info we need
		for(Object ob: data.transfer ) {
			if(ob instanceof SingleTransfer) {
				SingleTransfer st = ( SingleTransfer ) ob;
				if(st.getType().equals(S3eConstants.DATA)) {
					pottype = st.getValue();
				}
			}
		}
		
		// We create a new transfer collection
		data.transfer.clear();
		
		// Check if we have an external plugin
		PluginInterface pi = null;
		if(!(rparams.outpluginTestMasterDataLink == null) 
				&& !(rparams.outpluginTestMasterDataLink.equals(""))) {
			// Get the plugin
			pi = util.getPlugin(rparams.outpluginTestMasterDataLink);
		}
		
		// Get the list of errors
		ArrayList<SingleLocMessage> errors = S3eMdUtil.testMasterDataLink(pottype, pottypes, rparams, util, data.webid);
		if(errors == null) {
			if(pi == null) {
				data.response.setAnswer("Technical error: cannot execute test.");
				data.response.setStatus(406);
				return;
			}
			else {
				SingleTransfer st = new SingleTransfer();
				st.setType(S3eConstants.HTTPSTATUS);
				st.setValue("406");
				data.transfer.add(st);
				pi.execute(data, util);
				return;
			}
		}
		
		// OK, we have result, set the status good.
		SingleTransfer st = new SingleTransfer();
		st.setType(S3eConstants.HTTPSTATUS);
		st.setValue("200");
		data.transfer.add(st);
		st = new SingleTransfer();
		st.setType(S3eConstants.ID);
		st.setKey("");
		st.setValue(pottype);
		data.transfer.add(st);
		
		if(errors.size() == 0) {
			if(pi == null) {
				data.response.setAnswer("Master data link pot type " + pottype 
					+ " is perfect.");
				data.response.setStatus(200);
				return;
			}
		}
		
		if(pi == null) {
			String ans = "The following errors were found for master data link pot type " 
					+ pottype + ":"
					+ System.lineSeparator() + System.lineSeparator();
			for(SingleLocMessage slm: errors) {
				ans = ans + slm.getLocation() + ": " + slm.getText() + System.lineSeparator();
			}
		
			data.response.setAnswer(ans);
			data.response.setStatus(200);
			return;
		}
		else {
			for(SingleLocMessage slm: errors) {
				st = new SingleTransfer();
				st.setType(S3eConstants.DATA);
				st.setKey(slm.getLocation());
				st.setValue(slm.getText());
				data.transfer.add(st);
			}
			pi.execute(data, util);
			return;
		}
		
	}
	
	private void processTestMasterData(PluginData data, PluginUtilInterface util) {
		
		// For this we need authorization check
		if(data.webid == null) {
			data.response.setAnswer("No authorization information is available.");
			data.response.setStatus(401);
			return;
		}
		
		String envvar = "";
		String testmd = "";
		// Get the info we need
		for(Object ob: data.transfer ) {
			if(ob instanceof SingleTransfer) {
				SingleTransfer st = ( SingleTransfer ) ob;
				if(st.getType().equals(S3eConstants.DATA)) {
					envvar = st.getKey();
					testmd = st.getValue();
				}
			}
		}
		
		// We create a new transfer collection
		data.transfer.clear();
		
		// Check if we have an external plugin
		PluginInterface pi = null;
		if(!(rparams.outpluginTestMasterData == null) 
				&& !(rparams.outpluginTestMasterData.equals(""))) {
			// Get the plugin
			pi = util.getPlugin(rparams.outpluginTestMasterData);
		}
		
		// Get the list of errors
		ArrayList<SingleLocMessage> errors = S3eMdUtil.testMasterData(envvar, 
				testmd, pottypes, rparams, util, data.webid);
		if(errors == null) {
			if(pi == null) {
				data.response.setAnswer("Technical error: cannot execute test.");
				data.response.setStatus(406);
				return;
			}
			else {
				SingleTransfer st = new SingleTransfer();
				st.setType(S3eConstants.HTTPSTATUS);
				st.setValue("406");
				data.transfer.add(st);
				pi.execute(data, util);
				return;
			}
		}
		
		// OK, we have result, set the status good.
		SingleTransfer st = new SingleTransfer();
		st.setType(S3eConstants.HTTPSTATUS);
		st.setValue("200");
		data.transfer.add(st);
		st = new SingleTransfer();
		st.setType(S3eConstants.ID);
		st.setKey(envvar);
		st.setValue(testmd);
		data.transfer.add(st);
		
		if(errors.size() == 0) {
			if(pi == null) {
				data.response.setAnswer("Master data " + testmd 
					+ " for environmental variable " + envvar + " is perfect.");
				data.response.setStatus(200);
				return;
			}
		}
		
		if(pi == null) {
			String ans = "The following errors were found for master data " 
					+ testmd
					+ " for environmental variable " + envvar + ":"
					+ System.lineSeparator() + System.lineSeparator();
			for(SingleLocMessage slm: errors) {
				ans = ans + slm.getLocation() + ": " + slm.getText() + System.lineSeparator();
			}
		
			data.response.setAnswer(ans);
			data.response.setStatus(200);
			return;
		}
		else {
			for(SingleLocMessage slm: errors) {
				st = new SingleTransfer();
				st.setType(S3eConstants.DATA);
				st.setKey(slm.getLocation());
				st.setValue(slm.getText());
				data.transfer.add(st);
			}
			pi.execute(data, util);
			return;
		}
		
	}
	
	private String initialChecks(PluginData data, PluginUtilInterface util) {
		
		String ret = "";
		
		// First make sure that we have util, otherwise even cannot send logs
		if(util == null) return ret;
				
		// Then that the data is not null
		if(data == null) {
			S3eUtil.logg(S3eConstants.logError, "Data is null, stop.", util, rparams);
			return ret;
		}
				
		// Check that we are called as the correct plugin type (CHANGE!!!)
		if(data.plugintype == null || !(data.plugintype.equals(ExtConstants.pluginusageOUTSYNC))) {
			S3eUtil.logg(S3eConstants.logError, "Wrong plugin type, stop.", util, rparams);
			if(!(data.plugintype == null)) {
				String text = "Expected: " + ExtConstants.pluginusageOUTSYNC + ", got: " + data.plugintype + ".";
				S3eUtil.logg(S3eConstants.logError, text, util, rparams);
			}
			return ret;
		}
		
		// Additional checks
		
		// We need data.response
		if(data.response == null) {
			S3eUtil.logg(S3eConstants.logError, "data.response, stop.", util, rparams);
			return ret;
		}
		
		/*
		// need at least one result pot from the message converter
		if(data.result == null || data.result.pots == null || data.result.pots.size() == 0) {
			S3eUtil.logg(S3eConstants.logError, "data.result.pots not given, stop.", util, rparams);
			util.log("data.jsonMapper is null, stop");
			return false;
		}
		*/
		
		// We need the transfer and at least a command in it
		if(data.transfer == null || data.transfer.size() == 0) {
			S3eUtil.logg(S3eConstants.logError, "data.transfer empty, stop.", util, rparams);
			return ret;
		}
		for(Object ob: data.transfer ) {
			if(ob instanceof SingleTransfer) {
				SingleTransfer st = ( SingleTransfer ) ob;
				if(st.getType().equals(S3eConstants.COMMAND)) {
					ret = st.getValue();
				}
			}
		}
		if(ret.equals("")) {
			S3eUtil.logg(S3eConstants.logError, "No command transferred, stop.", util, rparams);
			S3eUtil.logg(S3eConstants.logError, "Content of transfer:", util, rparams);
			for(Object ob: data.transfer ) {
				if(ob instanceof SingleTransfer) {
					SingleTransfer st = ( SingleTransfer ) ob;
					S3eUtil.logg(S3eConstants.logError, 
							st.getType() + " - " + st.getKey() + " - " + st.getValue(),
							util, rparams);
				}
			}
			return ret;
		}
		
		// everything fine
		return ret;
	}

}
