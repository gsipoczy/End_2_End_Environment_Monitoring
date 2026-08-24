package sygr3em.plugin;

import java.util.ArrayList;
import java.util.HashMap;

import sygr.pots.extensions.Ball;
import sygr.pots.extensions.ExtConstants;
import sygr.pots.extensions.NewBall;
import sygr.pots.extensions.PluginData;
import sygr.pots.extensions.PluginInterface;
import sygr.pots.extensions.PluginUtilInterface;
import sygr.pots.extensions.Pot;
import sygr.pots.extensions.PotType;
import sygr3em.model.RuntimeParameters;
import sygr3em.service.S3eConstants;
import sygr3em.service.S3eUtil;

public class CreateNewInitPlugin implements PluginInterface{
	
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
				
		S3eUtil.logg(S3eConstants.logDebug, "START Pot Init plugin processing", util, rparams);
		
		if(data.pot.getMatchkey1().equals(S3eConstants.FINAL)) {
			S3eUtil.logg(S3eConstants.logDebug, "Pot is FINAL, no rule execution", util, rparams);
			finalizePot(data.pot, util);
		}
		else {
			if(rparams.userules) {
				// create the new ball and add to the return
				S3eUtil.logg(S3eConstants.logDebug, "Pot is not final, need Ball for Rule execution", util, rparams);
				NewBall nb = createBall(data, util);
				if(nb.getBall() == null) return;
				data.newBalls.add(nb);
			}
			else {
				// Must create alert, could not find master data.
				S3eUtil.logg(S3eConstants.logError, "Pot not final, no rules to use, send alert.", util, rparams);
				S3eUtil.alertNoMdDetermined(data.pot, rparams, util);
				data.pot.setType(rparams.createnewErrorPotType);
			}
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
			return false;
		}
				
		// Check that we are called as the correct plugin type (CHANGE!!!)
		if(data.plugintype == null || !(data.plugintype.equals(ExtConstants.pluginusagePOTINIT))) {
			S3eUtil.logg(S3eConstants.logError, "Wrong plugin type, stop.", util, rparams);
			if(!(data.plugintype == null)) {
				String text = "Expected: " + ExtConstants.pluginusagePOTINIT + ", got: " + data.plugintype + ".";
				S3eUtil.logg(S3eConstants.logError, text, util, rparams);
			}
			return false;
		}
		
		// Additional checks
		
		// We need to have the pot to be initialized
		if(data.pot == null) {
			S3eUtil.logg(S3eConstants.logError, "pot is null, stop.", util, rparams);
			return false;
		}
		
		// Also will need the place for new balls
		if(data.newBalls == null) {
			S3eUtil.logg(S3eConstants.logError, "newballs is null, stop.", util, rparams);
			return false;
		}
				
		// everything fine
		return true;
	}
	
	private NewBall createBall(PluginData data, PluginUtilInterface util) {
		NewBall nb = new NewBall();
		
		if(rparams == null) return nb;
		
		// Create the ball:
		// - machkey = pot.matchkey0, matchval = pot.matchval0
		// - usage: single
		// otherwise we do not need anything
		Ball ball = new Ball();
		ball.setType(rparams.createnewBallType);
		ball.setMatchkey(data.pot.getMatchkey0());
		ball.setMatchval(data.pot.getMatchval0());
		ball.setUsage(ExtConstants.ballusageSINGLE);
		nb.setBall(ball);
		
		// Add 1 second delay (if it's < 10 millisec, the ball will be triggered not as separate thread,
		// so it will run before the pot was saved -> BIG problem...)
		nb.setDelay(1000);
		
		return nb;
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
