package com.crm.thread;

import java.io.File;
import java.text.NumberFormat;
import java.util.Vector;

import javax.jms.Queue;
import javax.jms.QueueSession;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.crm.kernel.queue.LocalQueue;
import com.crm.kernel.queue.QueueFactory;
import com.crm.kernel.sql.Database;
import com.crm.thread.util.ThreadUtil;
import com.fss.util.AppException;
import com.fss.util.StringUtil;

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright:
 * </p>
 * <p>
 * Company:
 * </p>
 * 
 * @author: HungPQ
 * @version 1.0
 */

public class QueueMonitorThread extends DispatcherThread
{
	protected	String[]			externalQueues		= new String[0];

	protected	int					maxSize				= 10000;
	protected	int					warningSize			= 1000;
	protected	String				warningDiskPath		= "";
	protected	int					warningDiskPercent	= 10;

	protected	int					sysInfoInterval		= 1000;

	protected	long				lastSend			= System.currentTimeMillis();
	protected	long				lastSystemInfo		= System.currentTimeMillis();

	protected	StringBuilder		systemDump			= new StringBuilder();
	public		CloseableHttpClient	httpClient			= HttpClients.createDefault();

	// //////////////////////////////////////////////////////
	// Override
	// //////////////////////////////////////////////////////
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Vector getDispatcherDefinition()
	{
		Vector vtReturn = new Vector();

		// data source connection
		vtReturn.addElement(ThreadUtil.createIntegerParameter("queueMaxPending", ""));

		if (QueueFactory.queueServerEnable)
		{
			vtReturn.addElement(
					ThreadUtil.createBooleanParameter(
							"queueDispatcherEnable", "init queue connection when start dispatcher"));
			vtReturn.addElement(ThreadUtil.createTextParameter("queuePrefix", 100, ""));
			vtReturn.addElement(
					ThreadUtil.createTextParameter("queueList", 4000, "list of external queue"));
		}
		
		vtReturn.addElement(
				ThreadUtil.createIntegerParameter("warningSize", "send alert when queue size is over this value"));
		vtReturn.addElement(
				ThreadUtil.createIntegerParameter("maxSize", "send alert when queue size is over this value"));

		vtReturn.addElement(
				ThreadUtil.createTextParameter("diskPath", 4000, "Path of warning disk"));
		vtReturn.addElement(
				ThreadUtil.createIntegerParameter(
						"warningDiskPercent", "Percent of in used disk space need to warning if reach."));

		vtReturn.addElement(
				ThreadUtil.createIntegerParameter(
						"sysInfoInterval", "interval for dummy system status (ms)"));

		vtReturn.addElement(ThreadUtil.createBooleanParameter("alarmEnable", "send to alarm thread"));

//		vtReturn.addAll(ThreadUtil.createInstanceParameter(this));
		vtReturn.addAll(ThreadUtil.createLogParameter(this));

		return vtReturn;
	}

	// //////////////////////////////////////////////////////
	// Override
	// //////////////////////////////////////////////////////
	public void fillParameter() throws AppException
	{
		try
		{
			super.fillParameter();

			neverExpire = true;
			
			sysInfoInterval = ThreadUtil.getInt(this, "sysInfoInterval", 1000);

			maxSize = ThreadUtil.getInt(this, "maxSize", 10000);
			warningSize = ThreadUtil.getInt(this, "warningSize", 1000);

			externalQueues = StringUtil.toStringArray(ThreadUtil.getString(this, "queueList", false, ""), ";");

			warningDiskPath = ThreadUtil.getString(this, "diskPath", false, "/");
			warningDiskPercent = ThreadUtil.getInt(this, "warningDiskPercent", 10);
		}
		catch (AppException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new AppException(e.getMessage());
		}
	}

	// //////////////////////////////////////////////////////
	/**
	 * Write log to file and show to user (with date in prefix)
	 * 
	 * @param strLog
	 *            Log content
	 * @author Thai Hoang Hiep
	 */
	// //////////////////////////////////////////////////////
	public void logMonitor(String strLog)
	{
		logMonitor(strLog, false);
	}

	// //////////////////////////////////////////////////////
	public void logMonitor(String strLog, boolean bSendMail)
	{
		if (bSendMail)
		{
			alertByMail(strLog);
		}

		final java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("dd/MM HH:mm:ss:SSS");
		strLog = fmt.format(new java.util.Date()) + " " + strLog + "\r\n";

		log(strLog);
	}

	// //////////////////////////////////////////////////////
	/**
	 * Write log to file and show to user (without date in prefix)
	 * 
	 * @param strLog
	 *            Log content
	 * @author Thai Hoang Hiep
	 */
	// //////////////////////////////////////////////////////
	public void log(String strLog)
	{
		if (!strLog.endsWith("\n"))
		{
			strLog += "\n";
		}

		logToUser(strLog);
		logToFile(strLog);
	}

	// //////////////////////////////////////////////////////
	// process session
	// Author : ThangPV
	// Created Date : 16/09/2004
	// Modified by NamTA
	// Modified Date 27/08/2012
	// //////////////////////////////////////////////////////
	protected String queueWarning(Queue checkQueue, String name, int size) throws Exception
	{
		if (size > warningSize)
		{
			String queueWarning = "";

			if (size >= maxSize)
			{
				queueWarning = "FATAL: queue " + name + " is reach to limitation (" + size + "/" + maxSize + ")\r\n";
			}
			else
			{
				queueWarning = "WARNING: queue " + name + " may be reach to limitation (" + size + "/" + maxSize + ")\r\n";
			}

			logMonitor(queueWarning);
			// alarmMessage += warningMessage;

			return queueWarning;
		}
		return "";
	}

	// //////////////////////////////////////////////////////
	// process session
	// Author : ThangPV
	// Created Date : 16/09/2004
	// Modified by NamTA
	// Modified Date 27/08/2012
	// //////////////////////////////////////////////////////
	protected boolean memoryLogging() throws Exception
	{
		Runtime runtime = Runtime.getRuntime();

		// write log
		NumberFormat format = NumberFormat.getInstance();

		long maxMemory = runtime.maxMemory();
		long allocatedMemory = runtime.totalMemory();
		long freeMemory = runtime.freeMemory();

		File file = new File(warningDiskPath);

		long totalDiskSize = file.getTotalSpace();
		long usableDiskSize = file.getUsableSpace();
		long usedDiskSize = totalDiskSize - usableDiskSize;
		long percentUsedDisk = 100 * (usedDiskSize) / totalDiskSize;

		systemDump.append("\r\n");
		systemDump.append("Memory information:\r\n");
		systemDump.append("\t :: Free memory            : ");
		systemDump.append(format.format(freeMemory / 1024 / 1024));
		systemDump.append(" MB\r\n");
		systemDump.append("\t :: Allocated memory       : ");
		systemDump.append(format.format(allocatedMemory / 1024 / 1024));
		systemDump.append(" MB\r\n");
		systemDump.append("\t :: Max memory             : ");
		systemDump.append(format.format(maxMemory / 1024 / 1024));
		systemDump.append(" MB\r\n");
		systemDump.append("\t :: Total free memory      : ");
		systemDump.append(format.format((freeMemory + (maxMemory - allocatedMemory)) / 1024 / 1024));
		systemDump.append(" MB\r\n");
		systemDump.append("\t :: Total free memory      : ");
		systemDump.append(format.format((100 * (freeMemory + (maxMemory - allocatedMemory)) / maxMemory)));
		systemDump.append(" (%)\r\n");
		systemDump.append("\t :: Disk in used           : ");
		systemDump.append(format.format(usedDiskSize / 1024 / 1024));
		systemDump.append("/");
		systemDump.append(format.format(totalDiskSize / 1024 / 1024));
		systemDump.append(" MB, Used ");
		systemDump.append(format.format(percentUsedDisk));
		systemDump.append(" (%) (");
		systemDump.append(warningDiskPath);
		systemDump.append(")\r\n");
		systemDump.append("\t :: Total running thread   : ");
		systemDump.append(Thread.activeCount());
		systemDump.append("\r\n");

		return (percentUsedDisk >= warningDiskPercent);
	}

	// //////////////////////////////////////////////////////
	// process session
	// Author : ThangPV
	// Created Date : 16/09/2004
	// Modified by NamTA
	// Modified Date 27/08/2012
	// //////////////////////////////////////////////////////
	public void getSystemStatus() throws Exception
	{
		systemDump.delete(0, systemDump.length());

		boolean needAlarm = memoryLogging();

		if (Database.appDatasource != null)
		{
			systemDump.append("\r\n");
			systemDump.append("Database connection status: \r\n");
			systemDump.append("\t Number database connection\t\t\t: ");
			systemDump.append(Database.appDatasource.getNumConnections());
			systemDump.append("\r\n");
			systemDump.append("\t Number busy database connection\t: ");
			systemDump.append(Database.appDatasource.getNumBusyConnections());
			systemDump.append("\r\n");
			systemDump.append("\t Number idle database connection\t: ");
			systemDump.append(Database.appDatasource.getNumIdleConnections());
			systemDump.append("\r\n");
			systemDump.append("\t Number idle database connection\t: ");
			systemDump.append(Database.appDatasource.getNumUnclosedOrphanedConnections());
			systemDump.append("\r\n");
		}

		String queueWarningMessage = "";

		systemDump.append("\r\n");
		systemDump.append("Local queue status: \r\n");

		for (String key : QueueFactory.localQueues.keySet())
		{
			LocalQueue localQueue = QueueFactory.getLocalQueue(key);

			systemDump.append("Local queue (");
			systemDump.append(key);
			systemDump.append("): ");
			systemDump.append(localQueue.getSize());

			if (localQueue.getMaxSize() > 0)
			{
				systemDump.append("/");
				systemDump.append(localQueue.getMaxSize());
			}

			systemDump.append("\r\n");
		}
		if (QueueFactory.getTotalLocalPending() > 0)
		{
			systemDump.append("Total pending counter : ");
			systemDump.append(QueueFactory.getTotalLocalPending());
			systemDump.append("\r\n");
		}

		if (queueDispatcherEnable)
		{
			systemDump.append("\r\n");
			systemDump.append("Remote queue status: \r\n");

			QueueSession session = null;

			try
			{
				session = getQueueSession();

				for (int j = 0; j < externalQueues.length; j++)
				{
					if (externalQueues[j].equals(""))
					{
						continue;
					}

					String queueName = externalQueues[j];

					try
					{
						Queue checkQueue = QueueFactory.getQueue(queueName);

						int size = QueueFactory.getQueueSize(session, checkQueue);

						QueueFactory.queueSnapshot.put(queueName, new Integer(size));

						systemDump.append("Total command request for ");
						systemDump.append(queueName);
						systemDump.append(" : ");
						systemDump.append(size);
						systemDump.append("\r\n");

						queueWarningMessage += queueWarning(checkQueue, queueName, size);
					}
					catch (Exception e)
					{
						systemDump.append("Error occur when get size of queue ");
						systemDump.append(queueName);
						systemDump.append(": ");
						systemDump.append(e.getMessage());

						logMonitor(e);
					}
				}
			}
			catch (Exception e)
			{
				systemDump.append("Can not get remote queue size: ");
				systemDump.append(e.getMessage());
				systemDump.append("\r\n");

				logMonitor(e);
			}
			finally
			{
				QueueFactory.closeQueue(session);
			}
		}

		if (needAlarm)
		{
			systemDump.append("WARNING: Disk space is running low");
		}
		if (!queueWarningMessage.equals(""))
		{
			needAlarm = true;
			systemDump.append(queueWarningMessage);
		}

		logMonitor(systemDump);

		if (needAlarm)
		{
			sendInstanceAlarm("system-resource", systemDump.toString());
		}
	}

	public void doProcessSession() throws Exception
	{
		while (isAvailable())
		{
			checkInstance();

			if ((System.currentTimeMillis() - lastSystemInfo) > sysInfoInterval)
			{
				try
				{
					getSystemStatus();

					lastSystemInfo = System.currentTimeMillis();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}

			Thread.sleep(getDelayTime());
		}
	}	
}
