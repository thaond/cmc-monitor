package com.crm.provisioning.thread;

import java.util.Date;

import com.crm.kernel.index.IndexNode;
import com.crm.kernel.message.Constants;
import com.crm.kernel.queue.QueueFactory;
import com.crm.product.cache.ProductRoute;
import com.crm.provisioning.cache.CommandAction;
import com.crm.provisioning.cache.CommandEntry;
import com.crm.provisioning.cache.ProvisioningConnection;
import com.crm.provisioning.cache.ProvisioningFactory;
import com.crm.provisioning.message.CommandMessage;
import com.crm.provisioning.util.ResponseUtil;
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

public class ProvisioningInstance extends DispatcherInstance
{
	public ProvisioningInstance() throws Exception
	{
		super();
	}

	public ProvisioningThread getDispatcher()
	{
		return (ProvisioningThread) dispatcher;
	}

	public boolean isTimeout(CommandMessage request)
	{
		if (request.getOrderDate().getTime() + request.getTimeout() < (new Date())
				.getTime())
		{
			return true;
		}

		return false;
	}

	public String getDebugMode()
	{
		if (getDispatcher().useSimulation)
		{
			return "depend";
		}
		else
		{
			return "false";
		}
	}

	public boolean isDebug()
	{
		String debug = getDebugMode();

		return debug.equals("true") || debug.equals("depend");
	}

	public ProvisioningConnection getProvisioningConnection() throws Exception
	{
		try
		{
			return getDispatcher().getProvisioningPool().getConnection();
		}
		catch (Exception e)
		{
			if (((ProvisioningThread) dispatcher).getProvisioningPool()
					.getProvisioningPool().getNumActive() == 0)
			{
				long provisioningId = 0;
				if (((ProvisioningThread) dispatcher).provisioning != null)
					provisioningId = ((ProvisioningThread) dispatcher).provisioning
							.getProvisioningId();

				String detail = "ProvisioningId: " + provisioningId
						+ " - ProvisioningClass: "
						+ ((ProvisioningThread) dispatcher).provisioningClass;
				getDispatcher().sendInstanceAlarm(
						e,
						((ProvisioningThread) dispatcher).alias + "."
								+ Constants.ERROR_CONNECTION, detail);
			}

			throw e;
		}
	}

	public void closeProvisioningConnection(ProvisioningConnection connection)
			throws Exception
	{
		getDispatcher().getProvisioningPool().closeConnection(connection);
	}

	public void sendOrderResponse(ProductRoute orderRoute,
			CommandMessage request) throws Exception
	{
		try
		{
			if (request.getActionType().equals(Constants.ACTION_INVITE))
			{
				request.setIsdn(request.getParameters().getString(
						"INVITER_ISDN"));
			}
			
			try
			{
				if (orderRoute != null)
				{
					if (orderRoute.isNotifyOwner())
					{
						orderRoute.getExecuteImpl().notifyOwner(this,
								orderRoute, request);
					}

					if (orderRoute.isNotifyDeliver()
							&& (request.getStatus() != Constants.ORDER_STATUS_DENIED)
							&& !request.getShipTo().equals("")
							&& !request.getIsdn().equals(request.getShipTo()))
					{
						orderRoute.getExecuteImpl().notifyDeliver(this,
								orderRoute, request);
					}

					if (orderRoute.isSendAdvertising())
					{
						orderRoute.getExecuteImpl().sendAdvertising(this,
								orderRoute, request);
					}
				}
				else if (request.getChannel().equals(Constants.CHANNEL_SMS))
				{
					ResponseUtil.notifyOwner(this, orderRoute, request);
				}
			}
			catch (Exception e)
			{
				logMonitor(e);
			}

			QueueFactory
					.attachLocal(QueueFactory.ORDER_RESPONSE_QUEUE, request);

			String localQueueName = request.getParameters().getString(
					"responseQueue", QueueFactory.ORDER_RESPONSE_QUEUE);

			if (!localQueueName.equals(QueueFactory.ORDER_RESPONSE_QUEUE))
			{
				QueueFactory.attachLocal(localQueueName, request);
			}

			sendCommandStatistic(request);
		}
		catch (Exception e)
		{
			throw e;
		}
	}

	public CommandMessage getNextCommandMessage(CommandAction commandAction,
			CommandMessage request) throws Exception
	{
		CommandMessage nextRequest = null;

		if (commandAction.getExecuteMethod() != null)
		{
			nextRequest = (CommandMessage) commandAction.getExecuteMethod()
					.invoke(commandAction.getExecuteImpl(), commandAction,
							request);
		}
		else
		{
			nextRequest = request.clone();

			nextRequest.setActionType(commandAction.getNextAction());
			nextRequest.setCommandId(commandAction.getNextCommandId());
		}

		return nextRequest;
	}

	public int sendNextCommand(ProductRoute orderRoute, CommandMessage request,
			CommandEntry command, String actionType, String actionCause)
			throws Exception
	{
		// get next command if available
		int nextCounter = 0;

		try
		{
			if (command == null)
			{
				return nextCounter;
			}

			if (actionType.equals(""))
			{
				actionType = request.getActionType();
			}

			if (actionCause.equals(""))
			{
				actionCause = request.getCause();
			}

			for (IndexNode node : command.getActions().getNodes())
			{
				CommandAction commandAction = (CommandAction) node;

				if (commandAction.equals(request.getProductId(), actionType,
						request.getSubscriberType(), actionCause))
				{
					CommandMessage nextRequest = getNextCommandMessage(
							commandAction, request);

					if (!nextRequest.getActionType().equals(
							Constants.ACTION_ROLLBACK))
					{
						nextCounter++;
					}

					CommandEntry nextCommand = ProvisioningFactory.getCache()
							.getCommand(nextRequest.getCommandId());

					nextRequest.setProvisioningType(nextCommand
							.getProvisioningType());

					sendCommandRouting(nextRequest);
				}
			}
		}
		catch (Exception e)
		{
			throw e;
		}

		return nextCounter;
	}

	public void sendCommandRouting(CommandMessage request) throws Exception
	{
		try
		{
			request.setRetryCounter(0);

			QueueFactory.attachCommandRouting(request);
		}
		catch (Exception e)
		{
			throw e;
		}
	}

	public void sendCommandLog(CommandMessage request) throws Exception
	{
		try
		{
			QueueFactory.attachLocal(QueueFactory.COMMAND_LOG_QUEUE, request);
		}
		catch (Exception e)
		{
			logMonitor(e);
		}
	}

	public void sendCommandStatistic(CommandMessage request) throws Exception
	{
		try
		{
			QueueFactory.attachLocal(QueueFactory.COMMAND_STATISTIC_QUEUE,
					request);
		}
		catch (Exception e)
		{
			logMonitor(e);
		}
	}
}
