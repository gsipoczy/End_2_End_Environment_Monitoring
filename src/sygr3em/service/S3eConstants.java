package sygr3em.service;

import java.time.Instant;

public class S3eConstants {

	// Log types
	public static final int logInfo = 1;
	public static final int logError = 0;
	public static final int logDebug = 2;
	public static final int minLogLevel = 0;
	public static final int maxLogLevel = 2;
	
	// Business parameters
	public static final String bpApplication = "sygr3em";
	public static final String bpXApplication = ":XSP:sygr3em";
	
	public static final String bpK0General = "general";
	
	public static final String bpK1LogLevel = "loglevel";
	public static final String bpK1UpdateSleep = "updatesleep";
	public static final String bpK1UseRules = "userules";
	public static final String bpK1Authorization = "authorization";
	public static final String bpK1TaskSetsStatus = "tasksetstatus";
	public static final String bpK1MergeWaitMs = "mergewaitms";
	public static final String bpK1AllowUseWarning = "allowusewarning";
	public static final String bpK1AllowUseError = "allowuseerror";
	public static final String bpK1CollectSources = "collectsources";
	
	public static final String bpK1OutPlugin = "outplugin";
	public static final String bpK2TestMasterData = "test_master_data";
	public static final String bpK2TestMasterDataLink = "test_md_link";
	public static final String bpK2CalculationProblem = "calculation_problem";
	public static final String bpK2EditUnit = "editunit";
	
	public static final String bpK1Targets = "targets";
	public static final String bpK2DefaultTarget = "default";
	
	public static final String bpK1Texts = "texts";
	public static final String bpK2FlexiMain = "flexi_main";
	public static final String bpK2FixedMain = "fixed_main";
	public static final String bpK2FixedMain2 = "fixed_main_2";
	public static final String bpK2Sources = "sources";
	public static final String bpK2Statuses = "statuses";
	public static final String bpK2Messages = "messages";
	
	public static final String bpK1BallTypes = "balltypes";
	public static final String bpK2Command = "command";
	public static final String bpK1BallRepPlugin = "ballreportplugin";
	
	public static final String bpK1CreateNew = "createnew";
	public static final String bpK2CreateNewPotType = "pottype";
	public static final String bpK2CreateNewErrorPotType = "errorpottype";
	public static final String bpK2CreateNewBallType = "balltype";
	public static final String bpK2CreateNewInitPlugin = "initplugin";
	public static final String bpK2CreateNewFnameParams = "fnameparams";
	public static final String bpK2CreateNewFnamePrimAtt = "fnameprimatt";
	public static final String bpK2CreateNewFnameSecAtt = "fnamesecatt";
	public static final String bpK2CreateNewFnameTargetPt = "fnametargetpt";
	public static final String bpK2CreateNewDecisionPt = "decisionpt";
	
	public static final String bpK1Command = "command";
	public static final String bpK2CommandCreateNew = "createnew";
	public static final String bpK2CommandCreateReplace = "createreplace";
	public static final String bpK2CommandSetDeleted = "setdeleted";
	public static final String bpK2CommandSetActive = "setactive";
	public static final String bpK2CommandSetInactive = "setinactive";
	public static final String bpK2CommandSetPaused = "setpaused";
	public static final String bpK2CommandSetDeletedForce = "setdeletedforce";
	public static final String bpK2CommandTestMasterData = "test_master_data";
	public static final String bpK2CommandTestMasterDataLink = "test_md_link";
	public static final String bpK2CommandTelemetry = "telemetry";
	public static final String bpK2CommandSetTask = "settask";
	public static final String bpK2CommandMergeExisting = "mergeexisting";
	public static final String bpK2CommandMergeNew = "mergenew";
	public static final String bpK2CommandGetUnitForEdit = "getunitforedit";
	public static final String bpK2CommandUpdateUnit = "updateunit";
	public static final String bpK2CommandSplit = "split";
	public static final String bpK2CommandAddIds = "addids";
	public static final String bpK2CommandDeleteIds = "deleteids";
	public static final String bpK2CommandUpdateSecAttr = "updatesecattr";
	
	public static final String bpK1PotTypes = "pottypes";
	
	public static final String bpK0Characteristics = "characteristics";
	public static final String bpK0Conversions = "conversions";
	
	public static final String bpK0MasterData = "master_data";
	
	public static final String bpK1FieldNames = "field_names";
	public static final String bpK2MainSections = "main_sections";
	public static final String bpK2Crossings = "crossings";
	public static final String bpK2Rules = "rules";
	public static final String bpK2RuleDetails = "rule_details";
	public static final String bpK2Tasks = "tasks";
	public static final String bpK2Tasks2 = "tasks_2";
	public static final String bpK2Settings = "settings";
	
	public static final String bpK1Settings = "settings";
	public static final String bpK2Defaults = "defaults";
	public static final String bpK2Overlap = "overlap";
	public static final String bpK2TimeSplit = "time_split";
	public static final String bpK2AllowOverlap = "allow_overlap";
	
	// Master data intervals node types
	public static final int mdiOr = 0;
	public static final int mdiAnd = 1;
	public static final int mdiInt = 2;
	public static final int mdiTasks = 3;
	public static final int mdiTaskGroup = 4;
	public static final int mdiSingleTask = 5;
	public static final int mdiNodeError = 6;
	
	// Fix Attr locations
	public static final String attrlocCommand = "command";
	public static final String attrlocKey = "key";
	public static final String attrlocValue = "value";
	public static final String attrlocExceptionId = "except for";
	public static final String attrlocTask = "task";
	public static final String attrlocTimestamp = "timestamp";
	
	// Others
	public static final int NOTANINT = -99999999;
	public static final double NOTNUMERIC= -999999999999.9D;
	public static final long NOTADURATION = -999999999999L;
	public static final Instant NOTATIMESTAMP = Instant.ofEpochMilli(0);
	public static final String durationSeparatorRegex = ":";
	public static final String nodeSeparatorRegex = "\\.";
	
	public static final String FINAL = "FINAL";
	public static final String MDDETHMKEY = "TECHMDDET";
	public static final String LOGPREFIX = "S3e: ";
	
	public static final String SIGNATURE = "sygr3em";
	public static final String COMMAND = "command";
	public static final String TIMESTAMP = "TECH-TS";
	public static final String IVLISTNODE = "IV";
	public static final String IVLISTFROM = "from";
	public static final String IVLISTTO = "to";
	public static final String IVLISTLOC = "location";
	public static final String UOMSEPARATOR = ";";
	public static final String TMLISTNODE = "TM";
	public static final String MEASUREMENTS = "measurements";
	public static final String FORMULAX = "x";
	public static final String textTrue = "true";
	public static final String textFalse = "false";
	public static final String POTID = "POTID";
	public static final String STATUS = "STATUS";
	public static final String MERGESOURCENODE = "MS";
	public static final String SPLITTARGETNODE = "ST";
	public static final String IDNODE = "ID";
	public static final String KEY = "KEY";
	public static final String VALUE = "VALUE";
	public static final String FIXED = "FIXED";
	public static final String FLEXI = "FLEXI";
	public static final String DELETE = "DELETE";
	
	// Data transfer
	public static final String DATA = "data";
	public static final String HTTPSTATUS = "httpstatus";
	public static final String ID = "id";
	public static final String UNAUTHORIZED = "---UNAUTHORIZED---";
	
	// Problem types
	public static final String problemtypeVALUENOTCOVERED = "VALUE_NOT_COVERED";
	public static final String problemtypeALERTTIMEEXC = "ALERT_TIME_EXCEEDED";
	public static final String problemtypeMAXTIMEEXC = "MAX_TIME_EXCEEDED";
	public static final String problemtypeTASKTIMEEXC = "TASK_TIME_EXCEEDED";
	public static final String problemtypeALERTCROSSEXC = "ALERT_CROSS_EXCEEDED";
	public static final String problemtypeMAXCROSSEXC = "MAX_CROSS_EXCEEDED";
	
	// HTML lamps
	public static final String lampGREY = "&#x2609;";
	public static final String lampGREEN = "&#x1F7E2;";
	public static final String lampYELLOW = "&#x1F7E1;";
	public static final String lampRED = "&#x1F534;";
	
}
