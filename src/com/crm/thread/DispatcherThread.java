package com.crm.thread;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import java.util.concurrent.RejectedExecutionException;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.Session;

import org.apache.log4j.Logger;

import com.crm.kernel.message.AlarmMessage;
import com.crm.kernel.message.Constants;
import com.crm.kernel.queue.LocalQueue;
import com.crm.kernel.queue.QueueFactory;
import com.crm.kernel.sql.Database;
import com.crm.provisioning.message.CommandMessage;
import com.crm.thread.util.ThreadUtil;
import com.fss.thread.ManageableThread;
import com.fss.thread.ThreadConstant;
import com.fss.thread.ThreadManager;
import com.fss.util.AppException;

public class DispatcherThread extends ManageableThread
{
	protected Object		mutex					= "";
	public int				snapshotInterval		= 1000;
	public int				statisticInterval		= 1000;
	public int				overloadWaitTime		= 100;
	public long				lastStatistic			= 0;
	public long				lastSnapshot			= 0;

	// //////////////////////////////////////////////////////
	// Thread parameters
	// //////////////////////////////////////////////////////
	public boolean			loadCacheEnable			= false;
	public boolean			alarmEnable				= false;

	public boolean			instanceEnable			= false;
	public String			instanceClass			= "";
	public int				instanceSize			= 0;
	public long				instanceCheckTime		= 0;

	public boolean			neverExpire				= true;

	public boolean			queueDispatcherEnable	= false;
	public int				queueMode				= Constants.QUEUE_MODE_MANUAL;
	public String			queueName				= "";
	public String			queueLocalName			= "";
	public int				queueLocalSize			= 0;

	public boolean			displayDebug			= false;
	public String			logClass				= "";
	public Logger			log						= Logger.getLogger(DispatcherThread.class);

	public InstanceFactory	instances				= null;
	public Date				instanceLastExecute		= null;

	// //////////////////////////////////////////////////////
	// Queue variables
	// //////////////////////////////////////////////////////
	public QueueConnection	queueConnection			= null;
	public Queue			queueWorking			= null;

	// //////////////////////////////////////////////////////
	// result of process variables
	// //////////////////////////////////////////////////////
	public int				totalCount				= 0;
	public int				successCount			= 0;
	public int				errorCount				= 0;
	public int				bypassCount				= 0;
	public int				insertCount				= 0;
	public int				updateCount				= 0;
	public int				exportCount				= 0;
	public String			minStamp				= "";
	public String			maxStamp				= "";

	// //////////////////////////////////////////////////////
	// batch variables
	// //////////////////////////////////////////////////////
	public int				batchSize				= 1;
	public int				batchCount				= 0;
	public boolean			batchCommit				= true;

	// //////////////////////////////////////////////////////
	// error variables
	// //////////////////////////////////////////////////////
	public Exception		error					= null;
	public String			lastError				= "";

	// //////////////////////////////////////////////////////
	// other variables
	// //////////////////////////////////////////////////////
	public long				sequenceValue			= 0;

	public ThreadManager getThreadManager()
	{
		return mmgrMain;
	}

	// //////////////////////////////////////////////////////
	// Override
	// //////////////////////////////////////////////////////
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Vector getDispatcherDefinition()
	{
		Vector vtReturn = new Vector();

		vtReturn.addAll(ThreadUtil.createQueueParameter(this));
		vtReturn.addAll(ThreadUtil.createInstanceParameter(this));
		vtReturn.addAll(ThreadUtil.createLogParameter(this));

		return vtReturn;
	}

	// //////////////////////////////////////////////////////
	// Override
	// //////////////////////////////////////////////////////
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Vector getParameterDefinition()
	{
		Vector vtReturn = getDispatcherDefinition();

		vtReturn.addElement(ThreadUtil.createTextParameter("LogDir", 256, ""));
		vtReturn.addElement(ThreadUtil.createIntegerParameter("DelayTime", "Delay time between sessions (in second)"));

		return vtReturn;
	}

	// //////////////////////////////////////////////////////
	// Override
	// //////////////////////////////////////////////////////
	public void fillDispatcherParameter() throws Exception
	{
		snapshotInterval = ThreadUtil.getInt(this, "snapshotInterval", 100);
		statisticInterval = ThreadUtil.getInt(this, "statisticInterval", 15000);
		overloadWaitTime = ThreadUtil.getInt(this, "overloadWaitTime", 10);

		loadCacheEnable = ThreadUtil.getBoolean(this, "loadCacheEnable", false);
		alarmEnable = ThreadUtil.getBoolean(this, "alarmEnable", false);
		neverExpire = ThreadUtil.getBoolean(this, "neverExpire", true);

		// get instance setting
		instanceEnable = ThreadUtil.getBoolean(this, "instanceEnable", false);

		if (instanceEnable)
		{
			instanceClass = ThreadUtil.getString(this, "instanceClass", true, "");
			instanceSize = ThreadUtil.getInt(this, "instanceSize", 1);

			if (instanceSize <= 0)
			{
				throw new AppException("Instance size must greater than zero");
			}
		}

		// get log setting
		displayDebug = ThreadUtil.getBoolean(this, "displayDebug", false);
		logClass = ThreadUtil.getString(this, "logClass", true, "");
		log = Logger.getLogger(logClass);
		
		// get queue setting
		queueDispatcherEnable = QueueFactory.queueServerEnable;

		if (queueDispatcherEnable)
		{
			queueDispatcherEnable = ThreadUtil.getBoolean(this, "queueDispatcherEnable", false);
			queueName = ThreadUtil.getString(this, "queueName", false, "");
		}
		queueLocalName = ThreadUtil.getString(this, "queueLocalName", false, "");
		queueLocalSize = ThreadUtil.getInt(this, "queueLocalSize", 0);

		if (queueLocalName.equals(""))
		{
			queueLocalName = queueName;
		}
		if (queueDispatcherEnable)
		{
			String mode = ThreadUtil.getString(this, "queueMode", false, "consumer");

			if (mode.equalsIgnoreCase("manual"))
			{
				queueMode = Constants.QUEUE_MODE_MANUAL;
			}
			else if (mode.equalsIgnoreCase("consumer"))
			{
				queueMode = Constants.QUEUE_MODE_CONSUMER;
			}
			else if (mode.equalsIgnoreCase("producer"))
			{
				queueMode = Constants.QUEUE_MODE_PRODUCER;
			}
			else
			{
				throw new AppException("unknow-queue-mode");
			}
		}
	}

	// //////////////////////////////////////////////////////
	// Override
	// //////////////////////////////////////////////////////
	public void fillParameter() throws AppException
	{
		super.fillParameter();

		try
		{
			fillDispatcherParameter();
		}
		catch (AppException e)
		{
			logMonitor(e);

			throw e;
		}
		catch (Exception e)
		{
			logMonitor(e);

			throw new AppException(e.getMessage());
		}
	}

	public QueueSession getQueueSession() throws JMSException
	{
		if (queueConnection == null)
		{
			return null;
		}
		
		return queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
	}
	
	public synchronized void addInstance() throws Exception
	{
		if (!instanceEnable || (instances == null))
		{
			return;
		}
		else if (instances.getActiveTaskCount() >= instanceSize)
		{
			return;
		}

		DispatcherInstance instance = (DispatcherInstance) Class.forName(instanceClass).newInstance();
		instance.setDispatcher(this);

		try
		{
			instances.execute(instance);
		}
		catch (RejectedExecutionException e)
		{
			// logMonitor(e);
		}
	}

	public int getDelayTime()
	{
		return miDelayTime;
	}

	public Logger getLog()
	{
		return log;
	}

	public boolean isAvailable()
	{
		return (miThreadCommand != ThreadConstant.THREAD_STOP && !mmgrMain.isServerLocked());
	}

	// ////////////////////////////////////////////////////////////////////////
	// load directory parameters
	// Author: ThangPV
	// Modify DateTime: 19/02/2003
	// /////////////////////////////////////////////////////////////////////////
	public void logMonitor(String strLog, boolean bSendMail)
	{
		if (bSendMail)
		{
			alertByMail(strLog);
		}

		SimpleDateFormat fmt = new SimpleDateFormat("dd/MM HH:mm:ss:SSS");

		strLog = fmt.format(new java.util.Date()) + " " + strLog + "\r\n";

		log(strLog);
		
		strLog = null;
	}

	// ////////////////////////////////////////////////////////////////////////
	// load directory parameters
	// Author: ThangPV
	// Modify DateTime: 19/02/2003
	// /////////////////////////////////////////////////////////////////////////
	public void logMonitor(Object message)
	{
		String logContent = "";

		try
		{
			if (message instanceof Exception)
			{
				Exception exception = (Exception) message;

				if (exception != null)
				{
					if (!(exception instanceof AppException))
					{
						exception.printStackTrace();
					}

					if (exception instanceof InvocationTargetException)
					{
						Throwable target = ((InvocationTargetException) exception).getTargetException();

						logContent = target.getMessage();
					}
					else if (exception instanceof AppException)
					{
						AppException appException = (AppException) exception;

						if (appException.getContext() != null)
						{
							logContent = appException.getMessage() + " : " + appException.getContext();
						}
						else
						{
							logContent = appException.getMessage();
						}
					}
					else
					{
						logContent = exception.getClass().getName() + ": " + exception.getMessage();
					}
				}
			}
			else
			{
				logContent = message.toString();
			}

			logMonitor(logContent, false);
		}
		catch (Exception e)
		{
		}
	}

	// ////////////////////////////////////////////////////////////////////////
	// load directory parameters
	// Author: ThangPV
	// Modify DateTime: 19/02/2003
	// /////////////////////////////////////////////////////////////////////////
	public void debugMonitor(Object message)
	{
		try
		{
			if (displayDebug || (message instanceof Exception))
			{
				logMonitor(message);
			}
			else if (log.isDebugEnabled())
			{
				log.debug(message);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void sendInstanceAlarm(Exception e, String cause, String detail) throws Exception
	{
		if (alarmEnable)
		{
			AlarmMessage alarm = new AlarmMessage();

			if (cause.equals(Constants.ERROR) && e != null && e instanceof SQLException)
			{
				String format = "%05d";
				cause = "ORA-" + String.format(format, ((SQLException) e).getErrorCode());
			}
			alarm.setCause(cause);
	
			String content = detail + "\r\n";
			if (e != null && e.getStackTrace() != null)
			{
				StackTraceElement[] stackTraces = e.getStackTrace();
				for (StackTraceElement stackTrace : stackTraces)
				{
					content += stackTrace.toString() + "\r\n";
				}
				alarm.setDescription(e.getMessage());
			}

			alarm.setContent(content);

			sendInstanceAlarm(alarm);
		}
	}
	
	public void sendInstanceAlarm(Exception e, String cause) throws Exception
	{
		sendInstanceAlarm(e, cause, "");
	}
	
	public void sendInstanceAlarm(String cause, String detail) throws Exception
	{
		sendInstanceAlarm(null, cause, detail);
	}
	
	public void sendInstanceAlarm(Exception error, CommandMessage request, String detail) throws Exception
	{
		if (request.getCause() != null && request.getCause().equals(Constants.ERROR)
				&& request.getDescription() != null && !request.getDescription().equals(""))
		{
			sendInstanceAlarm(error, request.getDescription(), detail);
		}
		else
		{
			sendInstanceAlarm(error, request.getCause(), detail);
		}
	}
	
	public void sendInstanceAlarm(AlarmMessage request) throws Exception
	{
		try
		{
			QueueFactory.attachLocal(QueueFactory.ALARM_QUEUE, request);
		}
		catch (Exception e)
		{
			throw e;
		}
	}

	// //////////////////////////////////////////////////////
	// reset counter
	// Author : ThangPV
	// Created Date : 16/09/2004
	// //////////////////////////////////////////////////////
	public void resetCounter() throws Exception
	{
		totalCount = 0;
		successCount = 0;
		errorCount = 0;
		bypassCount = 0;
		insertCount = 0;
		updateCount = 0;

		minStamp = "";
		maxStamp = "";
	}

	// //////////////////////////////////////////////////////
	// after process session
	// Author : ThangPV
	// Created Date : 16/09/2004
	// //////////////////////////////////////////////////////
	public void loadCache() throws Exception
	{

	}

	/**
	 * Init queue Author : ThangPV Created Date : 16/09/2004 Edited by NamTA
	 * 
	 * @throws Exception
	 */
	public void initQueue() throws Exception
	{
		if (queueDispatcherEnable)
		{
			queueConnection = QueueFactory.createQueueConnection();
			queueWorking = queueName.equals("") ? null : QueueFactory.getQueue(queueName);
		}
		
		if (!queueLocalName.equals(""))
		{
			QueueFactory.getLocalQueue(queueLocalName).setDispathcher(this); 
			QueueFactory.getLocalQueue(queueLocalName).setMaxSize(queueLocalSize);
		}
	}

	// //////////////////////////////////////////////////////
	// after process session
	// Author : ThangPV
	// Created Date : 16/09/2004
	// //////////////////////////////////////////////////////
	public void beforeProcessSession() throws Exception
	{
		try
		{
			logMonitor("Starting process session");

			if (loadCacheEnable)
			{
				loadCache();
			}

			resetCounter();
			initQueue();

			if (instanceEnable && (instances == null || instances.isShutdown()))
			{
				instances = new InstanceFactory(instanceSize);
			}
		}
		catch (Exception e)
		{
			throw e;
		}
	}

	// //////////////////////////////////////////////////////
	// after process session
	// Author : ThangPV
	// Created Date : 16/09/2004
	// //////////////////////////////////////////////////////
	public void afterProcessSession() throws Exception
	{
		try
		{
			QueueFactory.closeQueue(queueConnection);
			
			if (instances != null)
			{
				instances.shutdown();
			}
		}
		catch (Exception e)
		{
			logMonitor(e);
		}
		finally
		{
			instances = null;
			Database.closeObject(mcnMain);

			logMonitor("End of process session");
		}
	}

	public boolean isAutoLoop()
	{
		return neverExpire;
	}

	// //////////////////////////////////////////////////////
	// process session
	// Author : ThangPV
	// Created Date : 16/09/2004
	// //////////////////////////////////////////////////////
	public void checkInstance() throws Exception
	{
		if (!instanceEnable || queueLocalName.equals("")
				|| ((System.currentTimeMillis() - instanceCheckTime) < ThreadUtil.intervalCheckInstance))
		{
			return;
		}

		instanceCheckTime = System.currentTimeMillis();

		LocalQueue localQueue = QueueFactory.getLocalQueue(queueLocalName);

		int added = 0;
		while ((instances.getActiveTaskCount() < instanceSize) && (localQueue.getSize() > 0))
		{
			added++;

			try
			{
				addInstance();
			}
			catch (RejectedExecutionException e)
			{
				// logMonitor(e);
			}
			catch (Exception e)
			{
				throw e;
			}

			if (added > instanceSize)
			{
				break;
			}
		}

		instanceCheckTime = System.currentTimeMillis();
	}

	// //////////////////////////////////////////////////////
	// process session
	// Author : ThangPV
	// Created Date : 16/09/2004
	// //////////////////////////////////////////////////////
	public Object detachMessage(MessageConsumer consumer) throws Exception
	{
		if (consumer != null)
		{
			return consumer.receiveNoWait();
		}
		return null;
	}

	public boolean isOverload()
	{
		return QueueFactory.getLocalQueue(queueName).isOverload();
	}

	// //////////////////////////////////////////////////////
	// process session
	// Author : ThangPV
	// Created Date : 16/09/2004
	// //////////////////////////////////////////////////////
	public void processOverload(MessageConsumer consumer) throws Exception
	{
		Thread.sleep(overloadWaitTime);
	}

	public void updateSnapshot() throws Exception
	{
	}

	public void updateStatistic() throws Exception
	{
	}

	// //////////////////////////////////////////////////////
	// process session, default apply for consumer dispatcher
	// Author : ThangPV
	// Created Date : 16/09/2004
	// //////////////////////////////////////////////////////
	public void doProcessSession() throws Exception
	{
		QueueSession session = null;
		MessageConsumer consumer = null;

		try
		{
			if (QueueFactory.queueServerEnable && queueDispatcherEnable && (queueMode == Constants.QUEUE_MODE_CONSUMER))
			{
				session = getQueueSession();
				consumer = session.createConsumer(QueueFactory.getQueue(queueName));
			}

			while (isAvailable())
			{
				
				checkInstance();

				if ((System.currentTimeMillis() - lastSnapshot) > snapshotInterval)
				{
					updateSnapshot();
					lastSnapshot = System.currentTimeMillis();
				}
				if ((System.currentTimeMillis() - lastStatistic) > statisticInterval)
				{
					updateStatistic();
					lastStatistic = System.currentTimeMillis();
				}

				if (isOverload())
				{
					processOverload(consumer);
				}
				else
				{
					Object message = detachMessage(consumer);
					if (message != null)
					{
						QueueFactory.attachLocal(queueName, message);
					}
					else
					{
						Thread.sleep(5);
					}
				}

				// Thread.sleep(1);
			}
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			QueueFactory.closeQueue(consumer);
			QueueFactory.closeQueue(session);
		}
	}

	// //////////////////////////////////////////////////////
	// process session
	// Author : ThangPV
	// Created Date : 16/09/2004
	// //////////////////////////////////////////////////////
	public void processSession() throws Exception
	{
		try
		{
			beforeProcessSession();

			lastStatistic = System.currentTimeMillis();

			while (isAvailable())
			{
				checkInstance();

				doProcessSession();

				if (!isAutoLoop())
				{
					break;
				}
				else if (isAvailable())
				{
					Thread.sleep(getDelayTime());
				}
			}
		}
		catch (Exception e)
		{
			// sendAlarmMessage(e);

			logMonitor(e);
		}
		finally
		{
			afterProcessSession();
		}
	}
}
