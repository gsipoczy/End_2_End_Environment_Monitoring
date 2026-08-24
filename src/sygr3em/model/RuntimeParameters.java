package sygr3em.model;

import java.util.ArrayList;
import java.util.HashMap;
import org.mariuszgromada.math.mxparser.*;

public class RuntimeParameters {

	// Log level, default: errors only
	public int logLevel = 0;
	
	// Wait time in millisec when plugins updating
	public long updatesleep = 5000L;
	
	// Inbound message commands
	public String imsgcommandCreateNew = "CreateNew";
	public String imsgcommandCreateReplace = "CreateReplace";
	public String imsgcommandSetDeleted = "SetDeleted";
	public String imsgcommandSetDeletedForce = "SetDeletedForce";
	public String imsgcommandSetActive = "SetActive";
	public String imsgcommandSetInactive = "SetInactive";
	public String imsgcommandSetPaused = "SetPaused";
	public String imsgcommandTestMasterData = "TestMasterData";
	public String imsgcommandTestMasterDataLink = "TestMasterDataLink";
	public String imsgcommandTelemetry = "Telemetry";
	public String imsgcommandSetTask = "SetTask";
	public String imsgcommandMergeExisting = "MergeExisting";
	public String imsgcommandMergeNew = "MergeNew";
	public String imsgcommandGetUnitForEdit = "GetUnitForEdit";
	public String imsgcommandUpdateUnit = "UpdateUnit";
	public String imsgcommandSplit = "Split";
	public String imsgcommandAddIds = "AddIds";
	public String imsgcommandDeleteIds = "DeleteIds";
	public String imsgcommandUpdateSecAttr = "UpdateSecAttr";
	
	// Create New parameters
	public String createnewPotType = "sygr3emCreateTemplate";
	public String createnewErrorPotType = "sygr3emERROR";
	public String createnewBallType = "sygr3emCreateTrigger";
	public String createnewInitPlugin = "sygr3eminitcreate";
	public String createnewFnameParams = "parameters";
	public String createnewFnamePrimAtt = "primary attributes";
	public String createnewFnameSecAtt = "secondary attributes";
	public String createnewFnameTargetPt = "pottype";
	public String createnewDecisionPt = "sygr3emDet*";
	public String createnewDecisionPtSeparator = ",";
	
	// Ball types
	public String balltypeCommand = "sygr3emCommand";
	
	// Characteristics
	public ArrayList<SingleCharacteristic> characteristics = new ArrayList<>();
	
	// Pot types
	public ArrayList<String> pottypes = new ArrayList<>();
	
	// Edit unit authorization (actually pot type)
	public String autheditunit = "";
	
	// Use internal rules for master data decision
	public boolean userules = false;
	// Task errors change HU status
	public boolean tasksetsstatus = false;
	// Merge to new: wait millisec after creating new Pot
	public long mergewaitms = 1000L;
	// Allow use error/warning status units for merge/split
	public boolean allowusewarning = false;
	public boolean allowuseerror = false;
	// Sources collection
	public boolean collectsources = false;
	public String gentextSources = "sources";
	
	// Targets
	public HashMap<String, ArrayList<String>> targets = new HashMap<>();
	
	// Texts generally
	public String gentextStatus = "status";
	public String gentextStatusOk = "ok";
	public String gentextStatusWarning = "warning";
	public String gentextStatusError = "error";
	public String gentextLastMessage = "last_message";
	public String gentextLastError = "last_error";
	public String gentextMessageSeverity = "severity";
	public String gentextMessageText = "text";
	public String gentextMessageTime = "time";
	public String gentextFixedStatus = "status";
	public String gentextFixedLastValue = "last_value";
	public String gentextFixedLastTime = "last_time";
	public String gentextFixedBudget = "budget";
	public String gentextFixedLastLocation = "last_location";
	public String gentextFixedCurrentTask = "current_task";
	public String gentextFixedTaskStarted = "task_started";
	
	// Master data structure
	public String mdfnSectionCrossings = "crossings";
	public String mdfnSectionIntervals = "intervals";
	public String mdfnSectionSettings = "settings";
	
	public String mdfnCrossingsBottomValue = "bottom_value";
	public String mdfnCrossingsTopValue = "top_value";
	public String mdfnCrossingsMaxCross = "max_cross";
	public String mdfnCrossingsAlertCross = "alert_cross";
	public String mdfnCrossingsCurrentCross = "current_cross";
	
	public String mdfnRuleOrPattern = "or*";
	public String mdfnRuleAndPattern = "and*";
	public String mdfnRuleIntervalPattern = "int*";
	public String mdfnRuleTechIntField = "TECH-INT";
	
	public String mdfnRuleDetailsMinValue = "min_value";
	public String mdfnRuleDetailsMaxValue = "max_value";
	public String mdfnRuleDetailsMaxTime = "max_time";
	public String mdfnRuleDetailsMinTime = "min_time";
	public String mdfnRuleDetailsAlertTime = "alert_time";
	public String mdfnRuleDetailsUsedTime = "used_time";
	
	public String mdfnTasksSection = "tasks";
	public String mdfnTasksGroupPattern = "tg*";
	public String mdfnTasksTaskPattern = "task-*";
	public String mdfnTasksFieldTaskName = "task_name";
	public String mdfnTasksFieldTaskRepeatable = "repeatable";
	
	public String mdfnSettingsOverlap = "overlap";
	public String mdfnSettingsRounding = "rounding";
	public String mdfnSettingsTimeSplit = "time_split";
	public String mdfnSettingsAllowOverlap = "allow_overlap";
	
	public String mdfnSettingsDefaultOverlap = "low";
	public String mdfnSettingsDefaultRounding = "2";
	public String mdfnSettingsDefaultTimeSplit = "half";
	public String mdfnSettingsDefaultAllowOverlap = "false";
	
	public String mdfnSettingsOverlapLow = "low";
	public String mdfnSettingsOverlapHigh = "high";
	
	public String mdfnSettingsAllowOverlapTrue = "true";
	public String mdfnSettingsAllowOverlapFalse = "false";
	
	public String mdfnSettingsTimeSplitOld = "old";
	public String mdfnSettingsTimeSplitNew = "new";
	public String mdfnSettingsTimeSplitHalf = "half";
	public String mdfnSettingsTimeSplitLinear = "linear";
	
	// UOM conversions
	public ArrayList<SingleConversion> conversions = new ArrayList<>();
	
	// Outbound plugins
	public String outpluginTestMasterData = "";
	public String outpluginTestMasterDataLink = "";
	public String outpluginCalculationProblem = "";
	
	// Reporting plugins
	public String ballreportplugin = "";
	
	// For mat calculations
	public PrimitiveElement[] pe = new PrimitiveElement[3];
	
}
