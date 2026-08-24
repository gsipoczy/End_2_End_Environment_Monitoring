package sygr3em.plugin;

import java.util.ArrayList;
import java.util.HashMap;

import sygr.pots.extensions.ExtConstants;
import sygr.pots.extensions.NodeValue;
import sygr.pots.extensions.PluginData;
import sygr.pots.extensions.PluginInterface;
import sygr.pots.extensions.PluginUtilInterface;
import sygr.pots.extensions.PotType;
import sygr3em.model.RuntimeParameters;
import sygr3em.service.S3eCalculationUtil;
import sygr3em.service.S3eCommandUtil;
import sygr3em.service.S3eConstants;
import sygr3em.service.S3eMergeUtil;
import sygr3em.service.S3eUtil;

public class RuleExecuteCommand implements PluginInterface {
	
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
		
		S3eUtil.logg(S3eConstants.logDebug, "START processing command Ball", util, rparams);
		
		// Get the command from the ball
		String command = "";
		ArrayList<NodeValue> nvs = util.nodeValueList(data.ball.getAttr(), "", false);
		for(NodeValue nv: nvs) {
			if(nv.getNode().equals(S3eConstants.attrlocCommand)) command = nv.getValue();
		}
		if(command == null || command.equals("")) {
			S3eUtil.logg(S3eConstants.logDebug, "Ball did not contain command.", util, rparams);
			String subject = "Missing command";
			String text = "A message arrived without command.";
			S3eUtil.sendAlert(ExtConstants.alertseverityERROR,
					"",
					data.ball.getMatchkey(),
					data.ball.getMatchval(),
					subject,
					text,
					rparams,
					util);
			return;
		}
		
		// Change Pot Status
		if(command.equals(rparams.imsgcommandSetDeleted) ||
				command.equals(rparams.imsgcommandSetDeletedForce) ||
				command.equals(rparams.imsgcommandSetActive) ||
				command.equals(rparams.imsgcommandSetInactive) ||
				command.equals(rparams.imsgcommandSetPaused)) {
			S3eUtil.logg(S3eConstants.logDebug, "Processing status change of", util, rparams);
			S3eUtil.logg(S3eConstants.logDebug, "key = " + data.ball.getMatchkey(), util, rparams);
			S3eUtil.logg(S3eConstants.logDebug, "value = " + data.ball.getMatchval(), util, rparams);
			S3eUtil.logg(S3eConstants.logDebug, "command = " + command, util, rparams);
			boolean force = command.equals(rparams.imsgcommandSetDeletedForce)? true : false;
			if(command.equals(rparams.imsgcommandSetDeleted) ||
				command.equals(rparams.imsgcommandSetDeletedForce))
				S3eCommandUtil.executeSetPotStatus(ExtConstants.potstatusDELETED, data, rparams, util, nvs, force);
			if(command.equals(rparams.imsgcommandSetActive))
				S3eCommandUtil.executeSetPotStatus(ExtConstants.potstatusACTIVE, data, rparams, util, nvs, force);
			if(command.equals(rparams.imsgcommandSetInactive))
				S3eCommandUtil.executeSetPotStatus(ExtConstants.potstatusINACTIVE, data, rparams, util, nvs, force);
			if(command.equals(rparams.imsgcommandSetPaused))
				S3eCommandUtil.executeSetPotStatus(ExtConstants.potstatusPAUSED, data, rparams, util, nvs, force);
		}
		
		// Set Task
		if(command.equals(rparams.imsgcommandSetTask)) {
			S3eUtil.logg(S3eConstants.logDebug, "Setting the task of", util, rparams);
			S3eUtil.logg(S3eConstants.logDebug, "key = " + data.ball.getMatchkey(), util, rparams);
			S3eUtil.logg(S3eConstants.logDebug, "value = " + data.ball.getMatchval(), util, rparams);
			S3eCommandUtil.executeSetTask(data, rparams, util, pottypes);
		}
		
		// Update unit
		if(command.equals(rparams.imsgcommandUpdateUnit)) {
			S3eUtil.logg(S3eConstants.logDebug, "Updating master and transactional data of", util, rparams);
			S3eUtil.logg(S3eConstants.logDebug, "key = " + data.ball.getMatchkey(), util, rparams);
			S3eUtil.logg(S3eConstants.logDebug, "value = " + data.ball.getMatchval(), util, rparams);
			S3eCommandUtil.executeUpdateUnit(data, rparams, util);
		}
		
		// Add/remove secondary IDs
		if(command.equals(rparams.imsgcommandAddIds) 
				|| command.equals(rparams.imsgcommandDeleteIds)) {
			S3eUtil.logg(S3eConstants.logDebug, "Updating master and transactional data of", util, rparams);
			S3eUtil.logg(S3eConstants.logDebug, "key = " + data.ball.getMatchkey(), util, rparams);
			S3eUtil.logg(S3eConstants.logDebug, "value = " + data.ball.getMatchval(), util, rparams);
			S3eCommandUtil.executeIds(command, data, rparams, util);
		}
		
		// Merge
		if(command.equals(rparams.imsgcommandMergeNew) || command.equals(rparams.imsgcommandMergeExisting)) {
			S3eUtil.logg(S3eConstants.logDebug, "Merging into", util, rparams);
			S3eUtil.logg(S3eConstants.logDebug, "key = " + data.ball.getMatchkey(), util, rparams);
			S3eUtil.logg(S3eConstants.logDebug, "value = " + data.ball.getMatchval(), util, rparams);
			S3eMergeUtil.executeMerge(data, rparams, util);
		}
		
		// Split
		if(command.equals(rparams.imsgcommandSplit)) {
			S3eUtil.logg(S3eConstants.logDebug, "Splitting", util, rparams);
			S3eUtil.logg(S3eConstants.logDebug, "key = " + data.ball.getMatchkey(), util, rparams);
			S3eUtil.logg(S3eConstants.logDebug, "value = " + data.ball.getMatchval(), util, rparams);
			S3eMergeUtil.executeSplit(data, rparams, util);
		}
		
		// Update secondary attributes
		if(command.equals(rparams.imsgcommandUpdateSecAttr)) {
			S3eUtil.logg(S3eConstants.logDebug, "Updating secondary attributes", util, rparams);
			S3eUtil.logg(S3eConstants.logDebug, "key = " + data.ball.getMatchkey(), util, rparams);
			S3eUtil.logg(S3eConstants.logDebug, "value = " + data.ball.getMatchval(), util, rparams);
			S3eCommandUtil.executeUpdateSecAttr(data, rparams, util);
		}
		
		// Telemetry
		if(command.equals(rparams.imsgcommandTelemetry)) {
			S3eUtil.logg(S3eConstants.logDebug, "Telemetry arrived for", util, rparams);
			S3eUtil.logg(S3eConstants.logDebug, 
					"key = " + data.ball.getMatchkey() + " = " + data.pot.getMatchkey0()
					, util, rparams);
			S3eUtil.logg(S3eConstants.logDebug, 
					"value = " + data.ball.getMatchval() + " = " + data.pot.getMatchval0()
					, util, rparams);
			S3eCalculationUtil.processTelemetry(data, util, rparams, pottypes);
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
	
	private boolean initialChecks(PluginData data, PluginUtilInterface util) {
		
		// First make sure that we have util, otherwise even cannot send logs
		if(util == null) return false;
				
		// Then that the data is not null
		if(data == null) {
			S3eUtil.logg(S3eConstants.logError, "Data is null, stop.", util, rparams);
			util.log("ERROR: data is null, stop");
			return false;
		}
				
		// Check that we are called as the correct plugin type
		if(data.plugintype == null || !(data.plugintype.equals(ExtConstants.pluginusageRULE))) {
			S3eUtil.logg(S3eConstants.logError, "Wrong plugin type, stop.", util, rparams);
			if(!(data.plugintype == null)) {
				String text = "Expected: " + ExtConstants.pluginusageRULE + ", got: " + data.plugintype + ".";
				S3eUtil.logg(S3eConstants.logError, text, util, rparams);
			}
			return false;
		}
		
		// Need the pot
		if(data.pot == null) {
			S3eUtil.logg(S3eConstants.logError, "data.pot is null, stop.", util, rparams);
			return false;
		}
		
		// Need the ball
		if(data.ball == null || data.ball.getAttr() == null) {
			S3eUtil.logg(S3eConstants.logError, "data.ball is null, stop.", util, rparams);
			return false;
		}
		
		// everything fine
		return true;
	}

}
