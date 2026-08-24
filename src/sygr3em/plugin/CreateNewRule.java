package sygr3em.plugin;

import java.util.ArrayList;
import java.util.HashMap;

import sygr.pots.extensions.ExtConstants;
import sygr.pots.extensions.PluginData;
import sygr.pots.extensions.PluginInterface;
import sygr.pots.extensions.PluginUtilInterface;
import sygr.pots.extensions.Pot;
import sygr.pots.extensions.PotType;
import sygr3em.model.RuntimeParameters;
import sygr3em.service.S3eConstants;
import sygr3em.service.S3eUtil;

public class CreateNewRule implements PluginInterface {
	
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
		
		S3eUtil.logg(S3eConstants.logDebug, "START processing Master Data Matching ball", util, rparams);
		
		// Simply try to finalize the Pot
		finalizePot(data.pot, util);
		
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
		
		// everything fine
		return true;
	}
	
	private void finalizePot(Pot pot, PluginUtilInterface util) {
		S3eUtil.logg(S3eConstants.logDebug, "Start finalizing the new Pot.", util, rparams);
		boolean success = S3eUtil.makeFinalNewPot(pot, rparams, pottypes, util);
		if(!success) {
			S3eUtil.logg(S3eConstants.logError, "Error creating final Pot from template.", util, rparams);
			S3eUtil.alertNoMdDetermined(pot, rparams, util);
			pot.setType(rparams.createnewErrorPotType);
		}
	}
}
