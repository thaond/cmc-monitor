package com.crm.provisioning.thread.osa;

import javax.jms.Queue;
import javax.jms.QueueSession;

import com.crm.kernel.message.Constants;
import com.crm.kernel.queue.QueueFactory;
import com.crm.provisioning.message.CommandMessage;
import com.crm.provisioning.thread.CommandInstance;
import com.crm.thread.util.ThreadUtil;

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

public class OSACommandInstance extends CommandInstance
{
	// //////////////////////////////////////////////////////
	// Queue variables
	// //////////////////////////////////////////////////////
	public QueueSession	session			= null;
	public Queue		queueCallback	= null;

	public OSACommandInstance() throws Exception
	{
		super();
	}

	public OSAThread getDispatcher()
	{
		return (OSAThread) dispatcher;
	}

	// //////////////////////////////////////////////////////
	// process session
	// Author : ThangPV
	// Created Date : 16/09/2004
	// //////////////////////////////////////////////////////
	public void beforeProcessSession() throws Exception
	{
		super.beforeProcessSession();

		try
		{
			if (dispatcher.queueDispatcherEnable)
			{
				session = dispatcher.getQueueSession();
				queueCallback = QueueFactory.getQueue(getDispatcher().queueCallback);
			}
		}
		catch (Exception e)
		{
			throw e;
		}
	}

	// //////////////////////////////////////////////////////
	// process session
	// Author : ThangPV
	// Created Date : 16/09/2004
	// //////////////////////////////////////////////////////
	public void afterProcessSession() throws Exception
	{
		try
		{
			QueueFactory.closeQueue(session);
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			super.afterProcessSession();
		}
	}

	public void displayCommand(CommandMessage request, long startTime, boolean debug) throws Exception
	{
		int baseCostTime = getDispatcher().baseCostTime;
		long executeCost = (System.currentTimeMillis() - startTime);

		boolean highCost = ((baseCostTime > 0) && (executeCost > baseCostTime));

		if (dispatcher.displayDebug || highCost || (request.getStatus() == Constants.ORDER_STATUS_DENIED))
		{
			StringBuilder sbLog = new StringBuilder();

			sbLog.append("orderId = ");
			sbLog.append(request.getOrderId());
			sbLog.append(", orderDate = ");
			sbLog.append(ThreadUtil.logTimestamp(request.getOrderDate()));
			sbLog.append(", sessionId = ");
			sbLog.append(request.getParameters().getString("sessionId"));
			sbLog.append(", isdn = ");
			sbLog.append(request.getIsdn());
			sbLog.append(", cause = ");
			sbLog.append(request.getCause());
			sbLog.append(", description = ");
			sbLog.append(request.getDescription());
			/*
			sbLog.append(", callback = ");
			sbLog.append(request.getParameters().getString("callbackTimestamp"));
			sbLog.append(", callbackReceiveDate = ");
			sbLog.append(request.getParameters().getString("callbackReceiveDate"));
			sbLog.append(", callbackParseDate = ");
			sbLog.append(request.getParameters().getString("callbackParseDate"));
			*/
			sbLog.append(", cost = ");
			sbLog.append(executeCost);

			if (!request.getParameters().getString("cost").equals(""))
			{
				sbLog.append(",");
				sbLog.append(request.getParameters().getString("cost"));
			}

			if (highCost || (request.getStatus() == Constants.ORDER_STATUS_DENIED))
			{
				logMonitor(sbLog.toString());
			}
			else
			{
				debugMonitor(sbLog.toString());
			}
		}
	}

}
