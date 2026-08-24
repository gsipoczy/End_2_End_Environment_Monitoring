package sygr3em.plugin;

import java.util.ArrayList;
import java.util.HashMap;

import sygr.pots.extensions.Attr;
import sygr.pots.extensions.Ball;
import sygr.pots.extensions.ExtConstants;
import sygr.pots.extensions.NodeNode;
import sygr.pots.extensions.PluginData;
import sygr.pots.extensions.PluginInterface;
import sygr.pots.extensions.PluginUtilInterface;
import sygr.pots.extensions.Pot;
import sygr.pots.extensions.PotType;
import sygr.pots.extensions.ReportingListHeader;
import sygr.pots.extensions.ReportingListItem;
import sygr3em.model.MultiTelemetry;
import sygr3em.model.RuntimeParameters;
import sygr3em.model.SingleTelemetry;
import sygr3em.service.S3eConstants;
import sygr3em.service.S3eUtil;

public class ReportBallShort  implements PluginInterface{

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
		
		S3eUtil.logg(S3eConstants.logDebug, "START processing short balls report", util, rparams);
		
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
		if(data.ball == null) {
			S3eUtil.logg(S3eConstants.logDebug, "No ball.", util, rparams);
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
		if(data.plugintype == null || !(data.plugintype.equals(ExtConstants.pluginusageREPBALL))) {
			S3eUtil.logg(S3eConstants.logError, "Wrong plugin type, stop.", util, rparams);
			if(!(data.plugintype == null)) {
				String text = "Expected: " + ExtConstants.pluginusageREPBALL + ", got: " + data.plugintype + ".";
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
		
		hd.setH00("ID");
		hd.setH01("Time stamp");
		hd.setH02("Command");
		hd.setH10("Time stamp");
		
		// Add here the column numbers the report has to be sorted by, in the sort order.
		// Negative number means descending sort.
		ArrayList<Integer> sortcolumns = new ArrayList<>();
		sortcolumns.add(10);
		hd.setSortcolumns(sortcolumns);
		
		// Must set how many columns we use in the report (max. is 30 (0-29)).
		hd.setMaxcol(10);
		
	}
	private void prepareItem(PluginData data, PluginUtilInterface util) {
		ArrayList<ReportingListItem> rlis = data.reportItems;
		if(rlis == null) return;
		Ball ball = data.ball;
		if(ball == null) return;
		String storetype = data.storeType;
		if(storetype == null || storetype.equals("")) return;
		
		Attr attr = ball.getAttr();
		if(attr == null) return;
		String command = util.getNodeValue(attr, S3eConstants.attrlocCommand);
		if(command == null || command.equals("")) return;
		
		// There are 2 possibilities:
		// We are in a good pot. A good pot must have a flexi node "testid" with something value.
		// If no, we just return a simple line
		ReportingListItem rli = new ReportingListItem();
		rli.setC00(ball.getMatchkey() + " " + ball.getMatchval());
		rli.setC01(util.convertInstantToStringSimple(ball.getEventtime()));
		rli.setC02(command);
		rli.setC10(S3eUtil.convertInstantToNumericString(ball.getEventtime()));
		
		if(command.equals(rparams.imsgcommandTelemetry)) {
			rlis.add(rli);
			ArrayList<ReportingListItem> rlis2 = getTelemetryDetails(attr, util, rli.getC10());
			rlis.addAll(rlis2);
			return;
		}
		if(command.equals(rparams.imsgcommandSetTask)) {
			rli.setC03(util.getNodeValue(attr, S3eConstants.attrlocTask));
			rlis.add(rli);
			return;
		}
		if(command.equals(rparams.imsgcommandMergeExisting) ||
				command.equals(rparams.imsgcommandMergeNew)) {
			rlis.add(rli);
			ArrayList<ReportingListItem> rlis2 = getMergeDetails(attr, util, rli.getC10(), storetype);
			rlis.addAll(rlis2);
			return;
		}
		
		rlis.add(rli);
		
				
	}
	
	private ArrayList<ReportingListItem> getMergeDetails(Attr attr, 
			PluginUtilInterface util, String timestamp, String storetype) {
		ArrayList<ReportingListItem> ret = new ArrayList<>();
		ArrayList<NodeNode> nns = util.nodeNodeList(attr, "", false);
		if(nns == null || nns.size() == 0) return ret;
		
		ReportingListItem itemhead = new ReportingListItem();
		itemhead.setC03("Key");
		itemhead.setC04("Value");
		itemhead.setC05("Set status to");
		itemhead.setC10(timestamp);
		ret.add(itemhead);
		
		for(NodeNode nn: nns) {
			if(nn.getNode().startsWith(S3eConstants.MERGESOURCENODE)) {
				String potid = util.getNodeValue(nn.getSubnode(), S3eConstants.POTID);
				if(!(potid == null) && !(potid.equals(""))) {
					Pot pot = util.getPot(storetype, potid);
					ReportingListItem item = new ReportingListItem();
					item.setC03(pot.getMatchkey0());
					item.setC04(pot.getMatchval0());
					item.setC05(util.getNodeValue(nn.getSubnode(), S3eConstants.STATUS));
					item.setC10(timestamp);
					ret.add(item);
				}
			}
		}
		
		return ret;
	}
	
	private ArrayList<ReportingListItem> getTelemetryDetails(Attr attr, PluginUtilInterface util, String timestamp) {
		ArrayList<ReportingListItem> ret = new ArrayList<>();
		Attr msattr = util.getNode(attr, S3eConstants.MEASUREMENTS);
		if(msattr == null) return ret;
		ArrayList<MultiTelemetry> mts = S3eUtil.copyAttrToMeasurements(msattr, util);
		if(mts == null || mts.size() == 0) return ret;
		
		ReportingListItem itemhead = new ReportingListItem();
		itemhead.setC03("Time");
		itemhead.setC04("Characteristic");
		itemhead.setC05("Value");
		itemhead.setC06("UOM");
		itemhead.setC10(timestamp);
		ret.add(itemhead);
		
		for(MultiTelemetry mt: mts) {
			String time = util.convertInstantToStringSimple(mt.getTimestamp());
			for(SingleTelemetry st: mt.getList()) {
				ReportingListItem item = new ReportingListItem();
				item.setC03(time);
				item.setC04(st.getC12c());
				item.setC05(String.valueOf(st.getValue()));
				item.setC06(st.getUom());
				item.setC10(timestamp);
				ret.add(item);
			}
		}
		
		return ret;
	}
}
