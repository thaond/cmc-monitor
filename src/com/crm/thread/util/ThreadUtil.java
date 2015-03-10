package com.crm.thread.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import com.crm.kernel.queue.QueueFactory;
import com.crm.thread.DispatcherThread;
import com.crm.util.StringPool;
import com.fss.thread.ManageableThread;
import com.fss.thread.ParameterType;
import com.fss.thread.ParameterUtil;
import com.fss.util.AppException;
import com.fss.util.StringUtil;

public class ThreadUtil
{
	public static SimpleDateFormat							logTimeFormat			= new SimpleDateFormat("dd/MM HH:mm:ss:SSS");

	public static int										intervalCheckInstance	= 1000;
	public static int										intervalUpdateLog		= 500;

	public static void sleep(DispatcherThread thread) throws AppException
	{
		try
		{
			Thread.sleep(thread.getDelayTime());
		}
		catch (Exception e)
		{

		}
	}

	public static String logTimestamp(Date date) 
	{
		return (date == null) ? "" : logTimeFormat.format(date);
	}

	public static String getString(ManageableThread thread, String parameter, boolean mandatory, String defaultValue)
			throws AppException
	{
		if (mandatory)
		{
			return thread.loadMandatory(parameter);
		}
		else
		{
			Object value = thread.getParameter(parameter);

			return (value == null) ? defaultValue : (String) value;
		}
	}

	public static Boolean getBoolean(ManageableThread thread, String parameter, boolean defaultValue)
			throws AppException
	{
		Object objValue = thread.getParameter(parameter);

		if (objValue == null)
		{
			return defaultValue;
		}

		String value = (String) objValue;

		value = value.toUpperCase();

		return (value == null) ? defaultValue : value.equals("YES") || value.equals("Y");
	}

	public static String getNumeric(ManageableThread thread, String parameter, String defaultValue) throws AppException
	{
		String value = getString(thread, parameter, false, defaultValue);

		if (!value.equals(""))
		{
			value = value.replaceAll(",", "");
		}

		return value;
	}

	public static int getInt(ManageableThread thread, String parameter, int defaultValue) throws AppException
	{
		try
		{
			return Integer.valueOf(getNumeric(thread, parameter, String.valueOf(defaultValue)));
		}
		catch (Exception e)
		{
			return defaultValue;
		}
	}

	public static long getLong(ManageableThread thread, String parameter, long defaultValue) throws AppException
	{
		try
		{
			return Long.valueOf(getNumeric(thread, parameter, String.valueOf(defaultValue)));
		}
		catch (Exception e)
		{
			return defaultValue;
		}
	}

	public static double getDouble(ManageableThread thread, String parameter, double defaultValue) throws AppException
	{
		try
		{
			return Double.valueOf(getNumeric(thread, parameter, String.valueOf(defaultValue)));
		}
		catch (Exception e)
		{
			return defaultValue;
		}
	}

	public static Date getDate(ManageableThread thread, String parameter, Date defaultValue) throws AppException
	{
		return null;
	}

	public static int getBatchSize(ManageableThread thread) throws AppException
	{
		return getInt(thread, "batchSize", 5000);
	}

	public static String getImportDir(ManageableThread thread) throws AppException
	{
		return thread.loadDirectory("importDir", true, true);
	}

	public static String getErrorDir(ManageableThread thread) throws AppException
	{
		return thread.loadDirectory("errorDir", true, true);
	}

	public static String getTempDir(ManageableThread thread) throws AppException
	{
		return thread.loadDirectory("tempDir", true, true);
	}

	public static String getRejectDir(ManageableThread thread) throws AppException
	{
		return thread.loadDirectory("rejectDir", true, false);
	}

	public static String getExportDir(ManageableThread thread) throws AppException
	{
		return thread.loadDirectory("exportDir", true, true);
	}

	public static String getBackupDir(ManageableThread thread) throws AppException
	{
		return thread.loadDirectory("backupDir", true, false);
	}

	public static String getWildcard(ManageableThread thread) throws AppException
	{
		return getString(thread, "wildcard", true, "*.*");
	}

	public static String getDelimiter(ManageableThread thread) throws AppException
	{
		return getString(thread, "delimter", false, StringPool.SEMICOLON);
	}

	@SuppressWarnings("rawtypes")
	public static Vector createTextParameter(String parameter, int length, String description)
	{
		return ParameterUtil.createParameterDefinition(parameter, "", ParameterType.PARAM_TEXTBOX_MAX, length, description);
	}

	@SuppressWarnings("rawtypes")
	public static Vector createMaskParameter(String parameter, String mask, String description)
	{
		return ParameterUtil.createParameterDefinition(parameter, "", ParameterType.PARAM_TEXTBOX_MASK, mask, description);
	}

	@SuppressWarnings("rawtypes")
	public static Vector createNumericParameter(String parameter, String description)
	{
		return createMaskParameter(parameter, "999999999", description);
	}

	@SuppressWarnings("rawtypes")
	public static Vector createSmallParameter(String parameter, String description)
	{
		return createMaskParameter(parameter, "999", description);
	}

	@SuppressWarnings("rawtypes")
	public static Vector createIntegerParameter(String parameter, String description)
	{
		return createMaskParameter(parameter, "999999", description);
	}

	@SuppressWarnings("rawtypes")
	public static Vector createLongParameter(String parameter, String description)
	{
		return createMaskParameter(parameter, "999999999", description);
	}

	@SuppressWarnings("rawtypes")
	public static Vector createFloatParameter(String parameter, String description)
	{
		return createMaskParameter(parameter, "999999999", description);
	}

	@SuppressWarnings("rawtypes")
	public static Vector createPasswordParameter(String parameter, String description)
	{
		return ParameterUtil.createParameterDefinition(parameter, "", ParameterType.PARAM_PASSWORD, "", description);
	}

	@SuppressWarnings("rawtypes")
	public static Vector createComboParameter(String parameter, String values, String description)
	{
		return ParameterUtil.createParameterDefinition(parameter, ""
				, ParameterType.PARAM_COMBOBOX, StringUtil.toStringVector(values, StringPool.COMMA), description);
	}

	@SuppressWarnings("rawtypes")
	public static Vector createBooleanParameter(String parameter, String description)
	{
		return createComboParameter(parameter, "Yes,No", description);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Vector createInstanceParameter(ManageableThread thread)
	{
		Vector vtReturn = new Vector();

		vtReturn.addElement(createBooleanParameter("neverExpire", "never end of session"));
		vtReturn.addElement(createBooleanParameter("loadCacheEnable", "reload cache when start session"));
		vtReturn.addElement(createBooleanParameter("alarmEnable", "send to alarm thread"));
		vtReturn.addElement(createBooleanParameter("instanceEnable", "set true to support multi-thread"));
		vtReturn.addElement(createTextParameter("instanceClass", 100, ""));
		vtReturn.addElement(createIntegerParameter("instanceSize", "number of threads"));

		return vtReturn;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Vector createLogParameter(ManageableThread thread)
	{
		Vector vtReturn = new Vector();

		vtReturn.addElement(createBooleanParameter("displayDebug", "display trace message in monitor"));
		vtReturn.addElement(createTextParameter("logClass", 100, "log category"));
		vtReturn.addElement(createIntegerParameter("snapshotInterval", "Interval in milisecond to refresh snapshot"));
		vtReturn.addElement(createIntegerParameter("statisticInterval", "statistic interval in second"));

		return vtReturn;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Vector createFTPParameter(ManageableThread thread)
	{
		Vector vtReturn = new Vector();

		vtReturn.add(createTextParameter("ftpUse", 400, "The billing runing ftp mode, that must put cdr file in remote server"));
		vtReturn.add(createTextParameter("ftpAddress", 400, ""));
		vtReturn.add(createIntegerParameter("ftpPort", ""));
		vtReturn.add(createTextParameter("ftpUser", 400, ""));
		vtReturn.add(createTextParameter("ftpPass", 400, ""));
		vtReturn.add(createComboParameter("ftpMode", "upload,download,manual", ""));
		vtReturn.add(createTextParameter("ftpRemoteDir", 400, ""));
		vtReturn.add(createTextParameter("ftpBackupDir", 400, ""));
		vtReturn.add(createTextParameter("ftpTempDir", 400, ""));

		vtReturn.addElement(createTextParameter("importDir", 300, ""));
		vtReturn.addElement(createTextParameter("exportDir", 300, ""));
		vtReturn.addElement(createTextParameter("tempDir", 300, ""));
		vtReturn.addElement(createTextParameter("errorDir", 300, ""));
		vtReturn.addElement(createTextParameter("rejectDir", 300, ""));
		vtReturn.addElement(createTextParameter("backupDir", 300, ""));

		vtReturn.addElement(createTextParameter("wildcard", 300, ""));
		
		return vtReturn;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Vector createQueueParameter(ManageableThread thread)
	{
		Vector vtReturn = new Vector();

		if (QueueFactory.queueServerEnable)
		{
			vtReturn.addElement(
					createBooleanParameter("queueDispatcherEnable", "init queue connection when start dispatcher"));
			vtReturn.addElement(createTextParameter("queueName", 100, "jndi queue name"));
		}
		vtReturn.addElement(createComboParameter("queueMode", "manual,consumer,producer", "jndi queue name"));
		vtReturn.addElement(createTextParameter("queueLocalName", 100, "jndi queue name"));
		vtReturn.addElement(createIntegerParameter("queueLocalSize", "Max local queue size"));

		return vtReturn;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Vector createDatasourceParameter(ManageableThread thread)
	{
		Vector vtReturn = new Vector();

		vtReturn.addElement(createTextParameter("validateClass", 100, "validate class"));
		vtReturn.addElement(createTextParameter("fieldList", 100, ""));
		vtReturn.addElement(createTextParameter("indicatorField", 100, ""));
		vtReturn.addElement(createTextParameter("stampField", 100, ""));
		vtReturn.addElement(createIntegerParameter("batchSize", "batch of size"));

		return vtReturn;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Vector createProvisioningParameter(ManageableThread thread, boolean poolable)
	{
		Vector vtReturn = new Vector();

		vtReturn.addElement(createTextParameter("provisioning", 100, ""));
		vtReturn.addElement(createTextParameter("host", 100, ""));
		vtReturn.addElement(createIntegerParameter("port", ""));
		vtReturn.addElement(createTextParameter("userName", 100, ""));
		vtReturn.addElement(createPasswordParameter("password", ""));

		// pooling parameters
		vtReturn.addElement(createTextParameter("provisioningClass", 100, ""));
		vtReturn.addElement(createIntegerParameter("timeout", "wait response in miliseconds"));
		vtReturn.addElement(createIntegerParameter("baseCostTime", "average cost time when execute command"));

		if (poolable)
		{
		vtReturn.addAll(createThreadPoolParameter(thread));
		}

		return vtReturn;
	}

	@SuppressWarnings("rawtypes")
	public static Vector createProvisioningParameter(ManageableThread thread)
	{
		return createProvisioningParameter(thread, true);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Vector createThreadPoolParameter(ManageableThread thread)
	{
		Vector vtReturn = new Vector();

		// pooling parameters
		vtReturn.addElement(createIntegerParameter("maxActive", "maximum number of objects that can be allocated in the pool "));
		vtReturn.addElement(createIntegerParameter("maxWait", "maximum amount of time (in milliseconds)"));
		vtReturn.addElement(createIntegerParameter("minIdle", "minimum number of objects allowed in the pool before evictor"));

		vtReturn.addElement(createBooleanParameter("testOnBorrow", "init queue connection when start instance"));
		vtReturn.addElement(createBooleanParameter("testOnReturn", "init queue connection when start instance"));

		vtReturn.addElement(
				createIntegerParameter(
						"timeBetweenEvictionRunsMillis",
						"indicates how long the eviction thread should sleep before examining idle objects"));

		vtReturn.addElement(
				createIntegerParameter(
						"numTestsPerEvictionRun", "number of objects examined in each run of the idle object evictor"));

		vtReturn.addElement(
				createIntegerParameter(
						"minEvictableIdleTimeMillis",
						"minimum amount of time that an object may sit idle in the pool before it is eligible for eviction"));

		vtReturn.addElement(createBooleanParameter("testWhileIdle", "indicates whether or not idle objects should be validated"));

		vtReturn.addElement(
				createIntegerParameter(
						"softMinEvictableIdleTimeMillis",
						"minimum amount of time an object may sit idle in the pool before it is eligible for eviction"));

		vtReturn.addElement(
				createBooleanParameter(
						"lifo", "determines whether or not the pool returns idle objects in last-in-first-out order"));

		return vtReturn;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Vector createSMPPParameter(ManageableThread thread)
	{
		Vector vtSMPP = new Vector();

		Vector vtBindMode = new Vector();
		vtBindMode.addElement("transmitter");
		vtBindMode.addElement("receiver");
		vtBindMode.addElement("transciever");

		vtSMPP.addElement(createComboParameter("bindMode", "transmitter,receiver,transciever", ""));
		vtSMPP.addElement(createBooleanParameter("asynchronous", ""));
		vtSMPP.addElement(createBooleanParameter("use-concatenated", "Use concatenated message if is long message"));
		vtSMPP.addElement(createTextParameter("system-type", 30, ""));
		vtSMPP.addElement(createIntegerParameter("receive-timeout", ""));

		vtSMPP.addElement(createTextParameter("addr-ton", 30, ""));
		vtSMPP.addElement(createTextParameter("addr-npi", 30, ""));
		vtSMPP.addElement(createTextParameter("address-range", 100, ""));

		vtSMPP.addElement(createTextParameter("source-ton", 30, ""));
		vtSMPP.addElement(createTextParameter("source-npi", 30, ""));
		vtSMPP.addElement(createTextParameter("source-address", 30, ""));

		vtSMPP.addElement(createTextParameter("destination-ton", 30, ""));
		vtSMPP.addElement(createTextParameter("destination-npi", 30, ""));
		vtSMPP.addElement(createTextParameter("destination-address", 30, ""));

		vtSMPP.addElement(createTextParameter("registeredDelivery", 30, ""));
		vtSMPP.addElement(createIntegerParameter("enquireInterval", "enquire interval"));

		return vtSMPP;
	}
}
