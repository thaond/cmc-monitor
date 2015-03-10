package com.crm.provisioning.thread;

import java.util.NoSuchElementException;

import javax.jms.Message;

import com.crm.kernel.message.Constants;
import com.crm.kernel.queue.QueueFactory;
import com.crm.provisioning.message.CommandMessage;
import com.crm.provisioning.util.CommandUtil;

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2003
 * </p>
 * <p>
 * Company: FPT
 * </p>
 * 
 * @author DatPX
 * @version 1.0 Purpose : Compute file : R4
 */

public class ReceiverInstance extends ProvisioningInstance
{
	// public Queue queueIncomeMessage = null;

	public ReceiverInstance() throws Exception
	{
		super();
	}

	@Override
	public void initQueue() throws Exception
	{

		super.initQueue();
	}

	/**
	 * process message
	 */
	public int processMessage(Message message) throws Exception
	{
		//QueueFactory.sendMessage(queueSession, queueWorking, message, message.getJMSExpiration());

		return Constants.BIND_ACTION_NONE;
	}

	@Override
	public Message detachMessage() throws Exception
	{
		Message message = null;
		CommandMessage request = null;
		try
		{
			request = (CommandMessage) QueueFactory.detachLocal(getDispatcher().queueLocalName);
		}
		catch (NoSuchElementException e)
		{
			debugMonitor("Can not get idle object in pool due to instanceSize > maxActive in pool or can not make connection to server.");
		}
		catch (Exception e)
		{
			debugMonitor(e);
			throw e;
		}

		try
		{
			if (request != null)
			{
				request.setTimeout(((SMPPThread) getDispatcher()).orderTimeout * 1000);

				try
				{
					message = QueueFactory.sendMessage(getDispatcher().getQueueSession(), request,
							QueueFactory.getQueue(getDispatcher().queueName), request.getCorrelationID(),
							null, null, request.getTimeout());
				}
				catch (Exception ex)
				{
					logMonitor("Can not send to route, notify error: " + request.toShortString());
					CommandUtil.sendSMS(this, request, "He thong dang ban, quy khach vui long thu lai sau.");
				}
			}
		}
		catch (Exception e)
		{
			debugMonitor(e);
			throw e;
		}

		request  = null;
		
		return message;
	}

}
