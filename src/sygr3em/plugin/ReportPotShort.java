package sygr3em.plugin;

import java.util.ArrayList;
import java.util.HashMap;

import sygr.pots.extensions.ExtConstants;
import sygr.pots.extensions.PluginData;
import sygr.pots.extensions.PluginInterface;
import sygr.pots.extensions.PluginUtilInterface;
import sygr.pots.extensions.Pot;
import sygr.pots.extensions.PotType;
import sygr.pots.extensions.ReportingListHeader;
import sygr.pots.extensions.ReportingListItem;
import sygr3em.model.RuntimeParameters;
import sygr3em.service.S3eConstants;
import sygr3em.service.S3eReportUtil;
import sygr3em.service.S3eUtil;

public class ReportPotShort  implements PluginInterface{

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
		
		S3eUtil.logg(S3eConstants.logDebug, "START processing short pots report", util, rparams);
		
		if(!(data.reportHeader == null)) {
			S3eUtil.logg(S3eConstants.logDebug, "Processing header", util, rparams);
			prepareHeader(data, util);
			return;
		}
		
		S3eUtil.logg(S3eConstants.logDebug, "Processing item", util, rparams);
		if(data.reportItems == null) {
			S3eUtil.logg(S3eConstants.logDebug, "No items.", util, rparams);
			return;
		}
		if(data.pot == null) {
			S3eUtil.logg(S3eConstants.logDebug, "No pot.", util, rparams);
			return;
		}
		if(data.storeType == null) {
			S3eUtil.logg(S3eConstants.logDebug, "No store type.", util, rparams);
			return;
		}
		prepareItem(data, util);
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
		if(data.plugintype == null || !(data.plugintype.equals(ExtConstants.pluginusageREPPOT))) {
			S3eUtil.logg(S3eConstants.logError, "Wrong plugin type, stop.", util, rparams);
			if(!(data.plugintype == null)) {
				String text = "Expected: " + ExtConstants.pluginusageREPPOT + ", got: " + data.plugintype + ".";
				S3eUtil.logg(S3eConstants.logError, text, util, rparams);
			}
			return false;
		}
		
		// everything fine
		return true;
	}
	
	private void prepareHeader(PluginData data, PluginUtilInterface util) {
		ReportingListHeader hd = data.reportHeader;
		if(hd == null) return;
		
		hd.setH00("");
		hd.setH01("Status");
		hd.setH02("ID Key");
		hd.setH03("ID Value");
		hd.setH04("Last Msg Time");
		hd.setH05("Last Message");
		hd.setPlugin(rparams.ballreportplugin);
		
		// Add here the column numbers the report has to be sorted by, in the sort order.
		// Negative number means descending sort.
		ArrayList<Integer> sortcolumns = new ArrayList<>();
		sortcolumns.add(20);
		sortcolumns.add(21);
		sortcolumns.add(22);
		hd.setSortcolumns(sortcolumns);
		
		// Must set how many columns we use in the report (max. is 30 (0-29)).
		hd.setMaxcol(6);
		
	}
	private void prepareItem(PluginData data, PluginUtilInterface util) {
		ArrayList<ReportingListItem> rlis = data.reportItems;
		if(rlis == null) return;
		Pot pot = data.pot;
		if(pot == null) return;
		String storetype = data.storeType;
		if(storetype == null || storetype.equals("")) return;
		
		// There are 2 possibilities:
		// We are in a good pot. A good pot must have a flexi node "testid" with something value.
		// If no, we just return a simple line
		ReportingListItem rli = new ReportingListItem();
		
		// Get the status
		rli.setC01(S3eReportUtil.getPotStatus(pot, rparams, util));
		rli.setC00(S3eReportUtil.getPotStatusIcon(rli.getC01(), rparams));
		rli.setC20(rli.getC00());
		
		// Get the IDs
		rli.setC02(pot.getMatchkey0());
		rli.setC03(pot.getMatchval0());
		rli.setC21(rli.getC02());
		rli.setC22(rli.getC03());
		
		// Get the messages
		rli.setC04(S3eReportUtil.getLastMessageTime(pot, rparams, util));
		rli.setC05(S3eReportUtil.getLastMessageText(pot, rparams, util));
		
		rlis.add(rli);
				
	}
	
	
}
