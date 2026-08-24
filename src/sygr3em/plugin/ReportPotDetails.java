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

public class ReportPotDetails  implements PluginInterface{

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
		hd.setH01("ID Key");
		hd.setH02("ID Value");
		hd.setH05("Name");
		hd.setH06("From");
		hd.setH07("To");
		hd.setH08("Max");
		hd.setH09("Alert");
		hd.setH10("Current");
		hd.setH11("Last Msg Time");
		hd.setH12("Last Message");
		hd.setPlugin(rparams.ballreportplugin);
		
		// Add here the column numbers the report has to be sorted by, in the sort order.
		// Negative number means descending sort.
		ArrayList<Integer> sortcolumns = new ArrayList<>();
		sortcolumns.add(20);
		sortcolumns.add(21);
		sortcolumns.add(22);
		sortcolumns.add(23);
		hd.setSortcolumns(sortcolumns);
		
		// Must set how many columns we use in the report (max. is 30 (0-29)).
		hd.setMaxcol(13);
		
	}
	private void prepareItem(PluginData data, PluginUtilInterface util) {
		ArrayList<ReportingListItem> rlis = data.reportItems;
		if(rlis == null) return;
		Pot pot = data.pot;
		if(pot == null) return;
		String storetype = data.storeType;
		if(storetype == null || storetype.equals("")) return;
		
		if(pot.getType().equals(rparams.createnewErrorPotType)) return;
		boolean typeok = false;
		for(String oktype: rparams.pottypes) {
			if(pot.getType().equals(oktype)) typeok = true;
		}
		if(!typeok) return;
		
		// There are 2 possibilities:
		// We are in a good pot. A good pot must have a flexi node "testid" with something value.
		// If no, we just return a simple line
		ReportingListItem rli = new ReportingListItem();
		
		// Get the status
		rli.setC00(S3eReportUtil.getPotStatusIcon(S3eReportUtil.getPotStatus(pot, rparams, util), rparams));
		rli.setC20(rli.getC00());
		
		// Get the IDs
		rli.setC01(pot.getMatchkey0());
		rli.setC02(pot.getMatchval0());
		rli.setC21(rli.getC01());
		rli.setC22(rli.getC02());
		
		// Get the messages
		rli.setC11(S3eReportUtil.getLastMessageTime(pot, rparams, util));
		rli.setC12(S3eReportUtil.getLastMessageText(pot, rparams, util));
		
		// Set the counter
		int counter = 10001;
		rli.setC23("C10001");
		rlis.add(rli);
		
		// Get the details
		ArrayList<ReportingListItem> addit = S3eReportUtil.getPotDetails(pot, rparams, util, counter,
				rli.getC20(), rli.getC21(), rli.getC22());
		if(!(addit == null)) rlis.addAll(addit); 
		
		// Make an empty line
		ReportingListItem rli1 = new ReportingListItem();
		rli1.setC20(rli.getC20());
		rli1.setC21(rli.getC21());
		rli1.setC22(rli.getC22());
		rli1.setC23("C99999");
		rlis.add(rli1);
				
	}
	
	
}
