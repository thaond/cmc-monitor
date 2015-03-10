package com.crm.provisioning.thread.osa;

import java.util.Vector;

import com.crm.kernel.queue.QueueFactory;
import com.crm.thread.DispatcherThread;
import com.crm.thread.util.ThreadUtil;

import com.fss.util.AppException;

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

public class OSACallbackThread extends DispatcherThread
{
	public int				callbackPort	= 5000;
	public int				backLogSize		= 1000;
	public int				numThreads		= 50;
	public int				timeout			= 60000;
	public boolean			localSupported	= false;

	public CallbackServer	callbackServer	= null;

	// //////////////////////////////////////////////////////
	// Override
	// //////////////////////////////////////////////////////
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Vector getDispatcherDefinition()
	{
		Vector vtReturn = new Vector();

		vtReturn.addElement(ThreadUtil.createIntegerParameter("callbackPort", ""));
		vtReturn.addElement(ThreadUtil.createIntegerParameter("backLogSize", "Backlog size."));
		vtReturn.addElement(ThreadUtil.createIntegerParameter("numThreads", "Number of event loop group"));
		vtReturn.addElement(ThreadUtil.createIntegerParameter("timeout", "time to live of result before expired (ms)."));

		vtReturn.addAll(ThreadUtil.createQueueParameter(this));
		vtReturn.addAll(ThreadUtil.createInstanceParameter(this));
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

			queueName = QueueFactory.COMMAND_CALLBACK;
			queueLocalName = queueName;

			callbackPort = ThreadUtil.getInt(this, "callbackPort", 5000);
			backLogSize = ThreadUtil.getInt(this, "backLogSize", 1000);
			numThreads = ThreadUtil.getInt(this, "numThreads", 50);
			timeout = ThreadUtil.getInt(this, "timeout", 60000);
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

	// //////////////////////////////////////////////////////
	// after process session
	// Author : ThangPV
	// Created Date : 16/09/2004
	// //////////////////////////////////////////////////////
	public void beforeProcessSession() throws Exception
	{
		super.beforeProcessSession();

		callbackServer = new CallbackServer(this, callbackPort, backLogSize, numThreads);
		callbackServer.start();

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
			if (callbackServer != null)
			{
				callbackServer.shutdown();
			}
		}
		finally
		{
			super.afterProcessSession();
		}
	}
}
