/**
 * 
 */
package com.crm.provisioning.thread;

import javax.jms.QueueSession;

import com.crm.kernel.message.Constants;
import com.crm.kernel.queue.LocalQueue;
import com.crm.kernel.queue.QueueFactory;
import com.crm.product.cache.ProductEntry;
import com.crm.product.cache.ProductFactory;
import com.crm.provisioning.cache.CommandEntry;
import com.crm.provisioning.cache.ProvisioningEntry;
import com.crm.provisioning.cache.ProvisioningFactory;
import com.crm.provisioning.cache.ProvisioningRoute;
import com.crm.provisioning.message.CommandMessage;
import com.crm.thread.DispatcherInstance;

import com.fss.util.AppException;

/**
 * @author ThangPV
 * 
 */
public class CommandRoutingInstance extends DispatcherInstance
{
	private QueueSession	session	= null;

	public CommandRoutingInstance() throws Exception
	{
		super();
	}

	public CommandRoutingThread getDispatcher()
	{
		return (CommandRoutingThread) dispatcher;
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
			session = dispatcher.getQueueSession();
		}
	}

	// //////////////////////////////////////////////////////
	// process session
	// Author : ThangPV
	// Created Date : 16/09/2004
	// //////////////////////////////////////////////////////
	public void afterProcessSession() throws Exception
	{
		QueueFactory.closeQueue(session);

		super.afterProcessSession();
	}

	public int processMessage(CommandMessage request) throws Exception
	{
		ProvisioningEntry provisioning = null;
		CommandEntry command = null;

		try
		{
			ProvisioningRoute route =
					((CommandRoutingThread) dispatcher)
							.getRoute(request.getProvisioningType(), "ISDN", request.getIsdn());

			if (route == null)
			{
				throw new AppException(Constants.ERROR_ROUTE_NOT_FOUND);
			}

			// forward request to related provisioning queue
			request.setProvisioningId(route.getProvisioningId());

			provisioning = ProvisioningFactory.getCache().getProvisioning(route.getProvisioningId());
			command = ProvisioningFactory.getCache().getCommand(request.getCommandId());

			if (provisioning == null)
			{
				throw new AppException(Constants.ERROR_PROVISIONING_NOT_FOUND);
			}

			String queueName = provisioning.getQueueName();

			if (queueName.equals(""))
			{
				queueName = ((CommandRoutingThread) dispatcher).queuePrefix + "/" + provisioning.getIndexKey();
			}

			LocalQueue localQueue = QueueFactory.getLocalQueue(queueName);

			if (localQueue.isOverload() && getDispatcher().loadBalanceEnable)
			{
				try
				{
					QueueFactory.sendMessage(session, request, QueueFactory.getQueue(queueName));
				}
				catch (Exception e)
				{
					QueueFactory.attachCommandRouting(request);

					throw e;
				}
			}
			else
			{
				QueueFactory.attachLocal(queueName, request);
			}

			if (provisioning != null && command != null && getDispatcher().displayDebug)
			{
				String isdn = request.getIsdn();
				ProductEntry product = ProductFactory.getCache().getProduct(request.getProductId());
				if (product != null
						&& request.getKeyword().startsWith(Constants.INVITE_KEYWORD_SMS + product.getAlias()))
				{
					if (provisioning.getAlias().equals(Constants.PROVISIONING_OSA))
					{
						isdn = request.getParameters().getString("INVITER_ISDN");
						debugMonitor(isdn + ": " + provisioning.getAlias() + " - " + command.getAlias());
					}
					else if (provisioning.getAlias().equals(Constants.PROVISIONING_CCWS)
							|| provisioning.getAlias().equals(Constants.PROVISIONING_CRM))
					{
						isdn = request.getParameters().getString("INVITEE_ISDN");
						debugMonitor(isdn + ": " + provisioning.getAlias() + " - " + command.getAlias());
					}
					else
					{
						debugMonitor(isdn + ": " + provisioning.getAlias() + " - " + command.getAlias());
					}
				}
				else
				{
					debugMonitor(request.getIsdn() + ": " + provisioning.getAlias() + " - " + command.getAlias());
				}
			}
		}
		catch (AppException e)
		{
			request.setStatus(Constants.ORDER_STATUS_DENIED);
			request.setCause(e.getMessage());
			request.setDescription(e.getContext());

			logMonitor(request);
		}
		catch (Exception e)
		{
			throw e;
		}

		return Constants.BIND_ACTION_SUCCESS;
	}

}
