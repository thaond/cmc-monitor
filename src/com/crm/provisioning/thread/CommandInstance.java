package com.crm.provisioning.thread;

import java.util.Date;

import javax.jms.JMSException;

import com.crm.product.cache.ProductFactory;
import com.crm.product.cache.ProductRoute;
import com.crm.provisioning.cache.CommandEntry;
import com.crm.provisioning.cache.ProvisioningCommand;
import com.crm.provisioning.cache.ProvisioningEntry;
import com.crm.provisioning.cache.ProvisioningFactory;
import com.crm.provisioning.message.CommandMessage;
import com.crm.provisioning.util.CommandUtil;
import com.crm.thread.util.ThreadUtil;
import com.crm.kernel.message.Constants;

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

public class CommandInstance extends ProvisioningInstance
{
	public CommandInstance() throws Exception
	{
		super();
	}

	public CommandThread getDispatcher()
	{
		return (CommandThread) dispatcher;
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
			sbLog.append(", status = ");
			sbLog.append(request.getStatus());
			sbLog.append(", cause = ");
			sbLog.append(request.getCause());
			sbLog.append(", description = ");
			sbLog.append(request.getDescription());
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

	public CommandMessage executeProvisioning(ProvisioningCommand action, CommandMessage request) throws Exception
	{
		return (CommandMessage) action.getExecuteMethod().invoke(action.getExecuteImpl(), this, action, request);
	}

	public int processMessage(CommandMessage request) throws Exception
	{
		CommandMessage result = request;

		CommandEntry command = null;
		ProductRoute productRoute = null;
		ProvisioningCommand action = null;

		Exception error = null;

		long startTime = System.currentTimeMillis();

		try
		{
			result.setRequest("");
			result.setResponse("");
			
			result.setRequestTime(new Date());
			result.setResponseTime(null);

			productRoute = ProductFactory.getCache().getProductRoute(result.getRouteId());

			command = ProvisioningFactory.getCache().getCommand(request.getCommandId());

			if (!result.getActionType().equals(Constants.ACTION_ROLLBACK)
					&& productRoute != null
					&& CommandUtil.isTimeout(result, productRoute.getTimeout()))
			{
				throw new AppException(Constants.ERROR_TIMEOUT);
			}

			ProvisioningEntry provisioning = ProvisioningFactory.getCache().getProvisioning(request.getProvisioningId());

			if (provisioning.getStatus() == Constants.SERVICE_STATUS_DENIED)
			{
				throw new AppException(Constants.UPGRADING);
			}

			action = provisioning.getAction(command.getCommandId());

			if (action.getStatus() == Constants.SERVICE_STATUS_DENIED)
			{
				throw new AppException(Constants.UPGRADING);
			}
			else
			{
				result = executeProvisioning(action, request); 

				if (result == null)
				{
					result = request;
				}

				if (result.getResponseTime() == null)
					result.setResponseTime(new Date());
			}
		}
		catch (AppException e)
		{
			result.setCause(e.getMessage());
			result.setDescription(e.getContext());

			error = e;
		}
		catch (Exception e)
		{
			result.setCause(Constants.ERROR);
			result.setDescription(e.getMessage());

			error = e;
		}
		finally
		{
			if ((action != null && action.isLogEnable())
					&& (!result.getParameters().getProperty("ByPassCommandLog", "false").equals("true")))
			{
				sendCommandLog(result);
			}
			result.getParameters().setProperty("ByPassCommandLog", "false");
		}

		// get next command if available
		try
		{
			boolean rollbacked = false;

			String actionCause = result.getCause();

			if (error != null)
			{
				result.setStatus(Constants.ORDER_STATUS_DENIED);
			}

			if (result.getStatus() == Constants.ORDER_STATUS_DENIED)
			{
				if (actionCause.equals(""))
					actionCause = Constants.ERROR;
			}
			else if (actionCause.equals(""))
			{
				actionCause = Constants.SUCCESS;
			}

			if (!result.getActionType().equals(Constants.ACTION_ROLLBACK)
					&& actionCause.equals(Constants.SUCCESS)
					&& command != null)
				result.getCompletedCommands().add(command.getAlias());

			int nextCounter = 0;

			if (!request.getActionType().equals(Constants.ACTION_ROLLBACK))
			{
				if (isTimeout(request))
				{
					actionCause = Constants.ERROR_TIMEOUT;
				}
			}

			result.setCause(actionCause);

			if (!result.getActionType().equals(Constants.ACTION_ROLLBACK)
//					&& !result.getActionType().equals(Constants.ACTION_CANCEL)
					&& !actionCause.contains(Constants.SUCCESS)
					&& command != null)
			{
				result.setStatus(Constants.ORDER_STATUS_DENIED);
				rollbacked = rollback(result);
			}

//			if ((result.getActionType().equals(Constants.ACTION_CANCEL)
//					|| result.getActionType().equals(Constants.ACTION_SUPPLIER_DEACTIVE)
//					|| result.getActionType().equals(Constants.ACTION_UNREGISTER))
//					&& !result.getCause().equals(Constants.SUCCESS)
//					&& !result.getCause().equals(Constants.ERROR_RESOURCE_BUSY)
//					&& !result.getCause().equals(Constants.ERROR_UNREGISTERED)
//					&& !result.getCause().equals(Constants.ERROR_SUBSCRIPTION_NOT_FOUND))
//			{
//				result.setDescription(result.getCause());
//				result.setCause(Constants.SUCCESS);
//				result.setStatus(Constants.ORDER_STATUS_PENDING);
//				actionCause = Constants.SUCCESS;
//			}

			try
			{
				nextCounter = sendNextCommand(productRoute, result, command, result.getActionType(), actionCause);
			}
			catch (JMSException jme)
			{
				result.setCause(Constants.ERROR_RESOURCE_BUSY);

				result.setStatus(Constants.ORDER_STATUS_DENIED);
				if (!rollbacked)
				{
					rollbacked = rollback(result);
				}
			}

			// reply to sender
			if (!result.getActionType().equals(Constants.ACTION_ROLLBACK) && ((nextCounter == 0) || (nextCounter > 1)))
			{
				if (result.getStatus() == Constants.ORDER_STATUS_PENDING)
				{
					result.setStatus(Constants.ORDER_STATUS_APPROVED);
				}

				try
				{
					sendOrderResponse(productRoute, result);
				}
				catch (Exception e)
				{
					logMonitor(e);

					result.setCause(Constants.ERROR_RESOURCE_BUSY);
					result.setStatus(Constants.ORDER_STATUS_DENIED);

					if (!rollbacked)
					{
						rollback(result);
					}
				}
			}

			if ((error != null) && !(error instanceof AppException))
			{
				throw error;
			}
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			if (result != null)
			{
				long commandId = result.getCommandId();

				CommandStatistic cmdStatistic = getDispatcher().commandStatistic.get(commandId);

				if (cmdStatistic == null)
				{
					cmdStatistic = new CommandStatistic();

					cmdStatistic.setCommandId(commandId);

					getDispatcher().commandStatistic.put(commandId, cmdStatistic);
				}

				synchronized (cmdStatistic)
				{
					if (result.getStatus() == Constants.ORDER_STATUS_DENIED)
					{
						cmdStatistic.setFailure(cmdStatistic.getFailure() + 1);
					}
					else
					{
						cmdStatistic.setSuccess(cmdStatistic.getSuccess() + 1);
					}

					getDispatcher().commandStatistic.put(commandId, cmdStatistic);
				}
			}
			
//			displayCommand(result, startTime, dispatcher.displayDebug);
			
			if (result.getStatus() == Constants.ORDER_STATUS_DENIED)
			{
				String detail = "ISDN: " + result.getIsdn() + " - Keyword: " + result.getKeyword() + " - OrderDate: " + result.getOrderDate();
				getDispatcher().sendInstanceAlarm(error, result, detail);
			}
		}

		return Constants.BIND_ACTION_SUCCESS;
	}

	public boolean rollback(CommandMessage request)
	{
		CommandMessage rollback = null;
		for (int i = 0; i < request.getCompletedCommands().size(); i++)
		{
			String commandAlias = request.getCompletedCommands().get(i);

			try
			{
				CommandEntry completedCommand = null;
				try
				{
					completedCommand = ProvisioningFactory.getCache().getCommand(commandAlias);
				}
				catch (Exception e)
				{
					debugMonitor("Error: rollback command [" + completedCommand + "]not found.");
				}

				String rollbackCommandAlias = completedCommand.getParameter("rollback", "");

				if (!"".equals(rollbackCommandAlias))
				{
					CommandEntry commandEntry = null;
					try
					{
						commandEntry = ProvisioningFactory.getCache().getCommand(rollbackCommandAlias);
					}
					catch (Exception e)
					{

					}
					if (commandEntry != null)
					{
						rollback = request.clone();
						rollback.setProvisioningType(commandEntry.getProvisioningType());
						rollback.setCommandId(commandEntry.getCommandId());

						rollback.setActionType(Constants.ACTION_ROLLBACK);
						// rollback.setParameter("ignoreDBUpdate", "true");
						rollback.getParameters().setProperty("ignoreError", "true");
						rollback.setStatus(Constants.SERVICE_STATUS_DENIED);

						rollback.setRequest("");
						rollback.setResponse("");

						// Send to command routing queue.
						try
						{
							sendCommandRouting(rollback);
						}
						catch (Exception e)
						{
							debugMonitor("Error: rollback error [" + rollbackCommandAlias + "].");
						}
					}
					else
					{
						// Rollback command not found.
						debugMonitor("Error: rollback command [" + rollbackCommandAlias + "]not found.");
					}
				}
			}
			catch (Exception e)
			{
				debugMonitor(e);
			}
		}

		return true;
	}
}
