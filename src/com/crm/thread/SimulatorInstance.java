package com.crm.thread;

import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueSession;

import com.crm.kernel.message.Constants;
import com.crm.kernel.queue.QueueFactory;
import com.crm.provisioning.message.CommandMessage;
import com.crm.thread.DispatcherInstance;

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

public class SimulatorInstance extends DispatcherInstance
{
	// //////////////////////////////////////////////////////
	// Queue variables
	// //////////////////////////////////////////////////////
	public SimulatorInstance() throws Exception
	{
		super();
	}

	public SimulatorThread getDispatcher()
	{
		return (SimulatorThread) dispatcher;
	}

	public boolean isOverload()
	{
		if ((getDispatcher().maxLocalPending > 0)
				&& (QueueFactory.getTotalLocalPending() >= getDispatcher().maxLocalPending))
		{
			return true;
		}
		else if ((getDispatcher().maxServerPending > 0)
				&& (getDispatcher().getTotalServerPending() >= getDispatcher().maxServerPending))
		{
			return true;
		}

		return false;
	}

	// //////////////////////////////////////////////////////
	// process session
	// Author : ThangPV
	// Created Date : 16/09/2004
	// //////////////////////////////////////////////////////
	public void processOverload() throws Exception
	{
		// logMonitor("sleeping ..." + getDispatcher().getTotalServerPending());

		Thread.sleep(10);
	}

	@Override
	public Object detachMessage() throws Exception
	{
		CommandMessage order = null;

		long currentISDN = getDispatcher().getCurrentIsdn();
		if (currentISDN > 0)
		{
			order = new CommandMessage();

			order.setChannel(getDispatcher().channel);

			if (getDispatcher().channel.equals(Constants.CHANNEL_SMS))
			{
				order.setProvisioningType("SMSC");
			}

			order.setUserId(0);

			if (getDispatcher().deliveryUser.equals(""))
				order.setUserName("system");
			else
				order.setUserName(getDispatcher().deliveryUser);

			order.setServiceAddress(getDispatcher().serviceAddress);
			order.setIsdn(String.valueOf(currentISDN));
			order.setShipTo(getDispatcher().shipTo);
			order.setTimeout(getDispatcher().orderTimeout * 1000);

			order.setKeyword(getDispatcher().keyword);
		}

		return order;
	}

	// //////////////////////////////////////////////////////
	// process session
	// Author : ThangPV
	// Created Date : 16/09/2004
	// //////////////////////////////////////////////////////
	public void doProcessSession() throws Exception
	{
		Queue sendQueue = null;
		Queue responseQueue = null;

		QueueSession session = null;
		MessageProducer producer = null;
		MessageConsumer consumer = null;

		try
		{
			if (QueueFactory.queueServerEnable
					&& dispatcher.queueDispatcherEnable)
			{
				sendQueue = QueueFactory.getQueue(getDispatcher().queueName);

				if (!getDispatcher().queueCallback.equals(""))
				{
					responseQueue = QueueFactory
							.getQueue(getDispatcher().queueCallback);
				}

				session = dispatcher.getQueueSession();
				producer = QueueFactory.createProducer(session, sendQueue);
			}

			while (isAvailable())
			{
				if (isOverload())
				{
					processOverload();

					continue;
				}
				else
				{
					CommandMessage request = (CommandMessage) detachMessage();

					if (request == null)
					{
						continue;
					}

					long costTime = 0;
					long startTime = System.currentTimeMillis();

					if (dispatcher.queueDispatcherEnable)
					{
						Message message = QueueFactory.createObjectMessage(
								session, request);

						producer.send(message);

						costTime = System.currentTimeMillis() - startTime;

						if (costTime > 1500)
						{
							logMonitor("long time for sending request: "
									+ costTime);
						}

						synchronized (dispatcher.mutex)
						{
							getDispatcher().totalServerPending++;
						}
					}
					else
					{
						QueueFactory.attachLocal(dispatcher.queueLocalName,
								request);
					}

					debugMonitor(request.getIsdn() + " - "
							+ request.getServiceAddress() + " - "
							+ request.getKeyword());

					if (responseQueue != null)
					{
						startTime = System.currentTimeMillis();

						Object response = null;
						String correlationId = request.getCorrelationID();

						QueueFactory.callbackListerner.put(correlationId,
								request);

						if (getDispatcher().asynchronous)
						{
							try
							{
								synchronized (request)
								{
									request.wait(getDispatcher().orderTimeout);
								}
							}
							catch (Exception e)
							{
								logMonitor(e);
							}
							finally
							{
								response = QueueFactory.callbackOrder
										.remove(correlationId);

								QueueFactory.callbackListerner.remove(request
										.getCorrelationID());
							}
						}
						else
						{
							try
							{
								consumer = session.createConsumer(
										responseQueue, "JMSCorrelationID = '"
												+ correlationId + "'");

								response = consumer
										.receive(getDispatcher().orderTimeout);

								if (response == null)
								{

								}
							}
							finally
							{
								QueueFactory.closeQueue(consumer);
							}
						}

						costTime = (System.currentTimeMillis() - startTime);

						if (response == null)
						{
							logMonitor("order timeout:" + correlationId);
						}
						else
						{
							logMonitor("execute time: " + costTime
									+ ", correlationId = " + correlationId);
						}
					}
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
			QueueFactory.closeQueue(consumer);
			QueueFactory.closeQueue(producer);
			QueueFactory.closeQueue(session);
		}
	}

}
