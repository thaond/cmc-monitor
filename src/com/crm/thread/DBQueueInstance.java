package com.crm.thread;

import java.sql.PreparedStatement;

import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueSession;

import com.crm.kernel.message.Constants;
import com.crm.kernel.queue.QueueFactory;
import com.crm.provisioning.message.CommandMessage;

public class DBQueueInstance extends DispatcherInstance
{
	protected PreparedStatement	stmtRemove	= null;

	protected long lastRunTime = System.currentTimeMillis();
	
	public DBQueueInstance() throws Exception
	{
		super();
	}

	public DBQueueThread getDispatcher()
	{
		return (DBQueueThread) super.getDispatcher();
	}

	public int processMessage(QueueSession session, MessageProducer producer, Object request) throws Exception
	{
		if (request == null)
		{
			return Constants.BIND_ACTION_NONE;
		}

		try
		{
			if (dispatcher.queueDispatcherEnable)
			{
				Message message = QueueFactory.createObjectMessage(session, request);

				producer.send(message);

				synchronized (dispatcher.mutex)
				{
					getDispatcher().totalServerPending++;
				}
			}
			else
			{
				QueueFactory.attachLocal(dispatcher.queueName, request);
			}

			long requestId = 0;
			String debugContent = "";
			
			if (request instanceof CommandMessage)
			{
				requestId = ((CommandMessage) request).getRequestId();
				debugContent = ((CommandMessage) request).toSubscriptionLog();
			}

			if (requestId != Constants.DEFAULT_ID)
			{
				getDispatcher().removeRequest(requestId);
			}

			if (dispatcher.displayDebug)
			{
				debugMonitor(debugContent);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			
			getDispatcher().sendInstanceAlarm(e, Constants.ERROR);

			throw e;
		}
		finally
		{
			getDispatcher().removeCounter();
		}

		return Constants.BIND_ACTION_SUCCESS;
	}

	// //////////////////////////////////////////////////////
	// process session
	// Author : ThangPV
	// Created Date : 16/09/2004
	// //////////////////////////////////////////////////////
	public void doProcessSession() throws Exception
	{
		Queue sendQueue = null;
		QueueSession session = null;
		MessageProducer producer = null;

		try
		{
			if (dispatcher.queueDispatcherEnable)
			{
				sendQueue = QueueFactory.getQueue(dispatcher.queueName);
				session = dispatcher.getQueueSession();
				producer = QueueFactory.createProducer(session, sendQueue);
			}

			while (isAvailable())
			{
				Object request = detachMessage();
	
				if (request != null)
				{
					processMessage(session, producer, request);
				}

				Thread.sleep(1);
			}
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			QueueFactory.closeQueue(session);
		}
	}
}
