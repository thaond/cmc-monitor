package com.crm.provisioning.thread.osa;

import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueSession;

import com.crm.kernel.message.Constants;
import com.crm.kernel.queue.QueueFactory;
import com.crm.thread.DispatcherInstance;
import com.crm.provisioning.message.CommandMessage;
import com.crm.provisioning.message.OSACallbackMessage;

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

public class OSACallbackInstance extends DispatcherInstance
{
	public QueueSession		session		= null;
	public MessageProducer	producer	= null;

	// //////////////////////////////////////////////////////
	// Queue variables
	// //////////////////////////////////////////////////////
	public OSACallbackInstance() throws Exception
	{
		super();
	}

	public OSACallbackThread getDispatcher()
	{
		return (OSACallbackThread) dispatcher;
	}

	// //////////////////////////////////////////////////////
	// process session
	// Author : ThangPV
	// Created Date : 16/09/2004
	// //////////////////////////////////////////////////////
	public void beforeProcessSession() throws Exception
	{
		super.beforeProcessSession();

		if (QueueFactory.queueServerEnable && dispatcher.queueDispatcherEnable)
		{
			Queue sendQueue = QueueFactory.getQueue(dispatcher.queueName);

			session = dispatcher.getQueueSession();
			producer = QueueFactory.createProducer(session, sendQueue, getDispatcher().timeout);
		}
	}

	// //////////////////////////////////////////////////////
	// process session
	// Author : ThangPV
	// Created Date : 16/09/2004
	// //////////////////////////////////////////////////////
	public void afterProcessSession() throws Exception
	{
		QueueFactory.closeQueue(producer);
		QueueFactory.closeQueue(session);

		super.afterProcessSession();
	}

	public int processMessage(Object message) throws Exception
	{
		int action = Constants.BIND_ACTION_NONE;

		if (message instanceof OSACallbackMessage)
		{
			action = processMessage((OSACallbackMessage) message);
		}
		else 
		{
			logMonitor("ignore message with class " + message.getClass().getName());
		}

		return action;
	}

	// //////////////////////////////////////////////////////
	// process session
	// Author : ThangPV
	// Created Date : 16/09/2004
	// //////////////////////////////////////////////////////
	public int processMessage(OSACallbackMessage response) throws Exception
	{
		try
		{
			String correlationId = "osa.callback." + response.getSessionId();

			Object waitingRequest = QueueFactory.callbackListerner.get(correlationId);

			if (waitingRequest != null)
			{
				if (waitingRequest instanceof CommandMessage)
				{
					response.setIsdn(((CommandMessage)waitingRequest).getIsdn());
				}
				
				synchronized (waitingRequest)
				{
					QueueFactory.callbackOrder.put(correlationId, response);

					waitingRequest.notifyAll();
				}
			}
			else if (dispatcher.queueDispatcherEnable)
			{
				Message message = QueueFactory.createObjectMessage(session, response);
				message.setJMSCorrelationID(response.getSessionId());

				producer.send(message);
			}
			else
			{
				logMonitor("request listener not found: " + response.getCause() + " - " + response.getSessionId());
			}
			
			if ((response != null) && dispatcher.displayDebug)
			{
				StringBuilder sbLog = new StringBuilder();
				
				sbLog.append("local attached: ");
				sbLog.append(response.getSessionId());
				sbLog.append(", ");
				sbLog.append(response.getCause());
				
				if (!response.getIsdn().equals(""))
				{
					sbLog.append(", ");
					sbLog.append(response.getIsdn());
				}
				
				debugMonitor(sbLog.toString());
			}
		}
		catch (Exception e)
		{
			QueueFactory.attachLocal(dispatcher.queueLocalName, response);

			throw e;
		}
		
		return Constants.BIND_ACTION_SUCCESS;
	}
}
