package com.crm.provisioning.thread;

import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueSession;

import com.crm.kernel.domain.DomainFactory;
import com.crm.kernel.message.Constants;
import com.crm.kernel.queue.QueueFactory;
import com.crm.kernel.sql.Database;
import com.crm.product.cache.ProductFactory;
import com.crm.product.cache.ProductRoute;
import com.crm.provisioning.message.CommandMessage;
import com.crm.thread.DispatcherInstance;
import com.crm.thread.util.ThreadUtil;
import com.crm.util.DateUtil;

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

public class OrderResponseInstance extends DispatcherInstance
{
	private Connection			connection				= null;
	private PreparedStatement	stmtUpdateOrder			= null;
	private PreparedStatement	stmtUpdateSubscription	= null;

	private QueueSession		session					= null;
	private MessageProducer		producer				= null;

	public OrderResponseInstance() throws Exception
	{
		super();
	}

	public OrderResponseThread getDispatcher()
	{
		return (OrderResponseThread) dispatcher;
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
			producer = QueueFactory.createProducer(session, sendQueue,
					getDispatcher().timeout);
		}

		connection = Database.getConnection();
		connection.setAutoCommit(false);

		String SQL = "Update SubscriberOrder "
				+ "Set modifiedDate = sysDate, status = ?, cause = ?, "
				+ "description = ?, subproductid = ?, campaignId = ?, "
				+ "orderType = ?, subscriberId = ?, productId = ?, "
				+ "subscriberType = ?, price = ?, quantity = ?, "
				+ "discount = ?, amount = ?, score = ?, channel = ? "
				+ "Where orderDate >= trunc(?) and orderDate < (trunc(?) + 1) and orderId = ? ";

		stmtUpdateOrder = connection.prepareStatement(SQL);

		SQL = "Update SubscriberProduct Set status = ? Where subProductid = ?";

		stmtUpdateSubscription = connection.prepareStatement(SQL);
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

		Database.closeObject(stmtUpdateOrder);
		Database.closeObject(stmtUpdateSubscription);
		Database.closeObject(connection);

		super.afterProcessSession();
	}

	// //////////////////////////////////////////////////////
	// process session
	// Author : ThangPV
	// Created Date : 16/09/2004
	// //////////////////////////////////////////////////////
	public int processMessage(CommandMessage message) throws Exception
	{
		boolean updated = false;
		boolean missing = false;

		long startTime = System.currentTimeMillis();
		long sqlCostTime = 0;
		long startTimeSendToQueue = 0;
		long endTimeSendToQueue = 0;

		ProductRoute orderRoute = null;

		try
		{
			orderRoute = ProductFactory.getCache().getProductRoute(
					message.getRouteId());

			if (message.getSubProductId() != Constants.DEFAULT_ID
					&& message.getChannel().equals(Constants.CHANNEL_CORE)
					&& (message.getActionType().equals(
							Constants.ACTION_SUBSCRIPTION)
							|| message.getActionType().equals(
									Constants.ACTION_SUPPLIER_DEACTIVE)
							|| message.getActionType().equals(
									Constants.ACTION_UNREGISTER)
							|| message.getActionType().equals(
									Constants.ACTION_TOPUP) || message
							.getActionType().equals(
									Constants.ACTION_SUPPLIER_REACTIVE)))
			{
				int subProductStatus = Constants.SUBSCRIBER_REGISTER_STATUS;
				if (message.getStatus() != Constants.ORDER_STATUS_APPROVED)
				{
					if (message.getParameters().getProperty("IsVBService", "")
							.equals("true")
							&& message.getCause().equals(
									Constants.ERROR_NOT_ENOUGH_MONEY))
					{
						subProductStatus = Constants.SUBSCRIBER_REGISTER_STATUS;
					}
					else if (message.getParameters().getInteger(
							"LastSubProductStatus") == Constants.SUBSCRIBER_TERMINATE_FREE_STATUS)
					{
						subProductStatus = Constants.SUBSCRIBER_ALERT_FREE_NOT_REACTIVE_STATUS;
					}
					else
					{
						subProductStatus = Constants.SUBSCRIBER_ALERT_EXPIRE_STATUS;
					}
				}
				else if (message.getParameters().getProperty("IsVBService", "")
						.equals("true")
						&& !message.getActionType().equals(
								Constants.ACTION_SUPPLIER_DEACTIVE))
				{
					subProductStatus = Constants.SUBSCRIBER_PENDING_STATUS;
				}

				stmtUpdateSubscription.setInt(1, subProductStatus);
				stmtUpdateSubscription.setLong(2, message.getSubProductId());
				stmtUpdateSubscription.execute();

				updated = true;
			}

			if ((orderRoute != null) && orderRoute.isCreateOrder()
					&& message.getParameters().getBoolean("CreatedOrder"))
			{
				int subscribertype = message.getSubscriberType();
				if (message.getActionType().equals(Constants.ACTION_INVITE))
				{
					subscribertype = message.getParameters().getInteger(
							"INVITER_SUBSCRIBERTYPE");
				}

				String respCode = "";
				String errorCode = "";

				if (message.getCause() != null
						&& message.getDescription() != null
						&& message.getCause().equals(Constants.ERROR)
						&& !message.getDescription().equals(""))
				{
					respCode = "RESP." + message.getDescription();
					errorCode = DomainFactory.getCache().getDomain(
							"RESPONSE_CODE", respCode);
				}

				if (errorCode.equals(""))
				{
					respCode = "RESP." + message.getCause();
					errorCode = DomainFactory.getCache().getDomain(
							"RESPONSE_CODE", respCode);
				}

				if (errorCode.equals(""))
				{
					errorCode = message.getCause();
				}

				stmtUpdateOrder.setInt(1, message.getStatus());
				stmtUpdateOrder.setString(2, errorCode);
				stmtUpdateOrder.setString(3, message.getDescription());
				stmtUpdateOrder.setLong(4, message.getSubProductId());
				stmtUpdateOrder.setLong(5, message.getCampaignId());
				stmtUpdateOrder.setString(6, message.getActionType());
				stmtUpdateOrder.setLong(7, message.getSubscriberId());
				stmtUpdateOrder.setLong(8, message.getProductId());
				stmtUpdateOrder.setInt(9, subscribertype);
				stmtUpdateOrder.setDouble(10, message.getPrice());
				stmtUpdateOrder.setDouble(11, message.getQuantity());
				stmtUpdateOrder.setDouble(12, message.getDiscount());
				stmtUpdateOrder.setDouble(13, message.getAmount());
				stmtUpdateOrder.setDouble(14, message.getScore());
				stmtUpdateOrder.setString(15, message.getChannel());
				stmtUpdateOrder.setTimestamp(16,
						DateUtil.getTimestampSQL(message.getOrderDate()));
				stmtUpdateOrder.setTimestamp(17,
						DateUtil.getTimestampSQL(message.getOrderDate()));
				stmtUpdateOrder.setLong(18, message.getOrderId());

				stmtUpdateOrder.execute();

				updated = true;
			}

			if (updated)
			{
				connection.commit();
			}

			String correlationId = message.getCorrelationID();

			if (message.getChannel().equals(Constants.CHANNEL_SMS))
			{
				return Constants.BIND_ACTION_SUCCESS;
			}
			else if ((orderRoute == null) || !orderRoute.isSynchronous())
			{
				return Constants.BIND_ACTION_SUCCESS;
			}
			else if (correlationId.equals(""))
			{
				return Constants.BIND_ACTION_NONE;
			}

			Object waitingRequest = QueueFactory.callbackListerner
					.get(correlationId);

			if (waitingRequest != null)
			{
				synchronized (waitingRequest)
				{
					QueueFactory.callbackOrder.put(correlationId, message);

					try
					{
						waitingRequest.notifyAll();
					}
					catch (Exception e)
					{
						logMonitor(e);
					}
				}
			}
			else if (dispatcher.queueDispatcherEnable)
			{
				try
				{
					Message response = QueueFactory.createObjectMessage(
							session, message);
					if (!isTimeOut(message.getOrderDate().getTime(),
							System.currentTimeMillis(), message.getTimeout()))
					{
						startTimeSendToQueue = System.currentTimeMillis();
						producer.send(response);
						endTimeSendToQueue = System.currentTimeMillis();
					}
				}
				catch (Exception e)
				{
					QueueFactory.attachLocal(QueueFactory.ORDER_RESPONSE_QUEUE,
							message);

					logMonitor(e);
				}
			}
			else
			{
				missing = true;
			}

		}
		catch (Exception e)
		{
			Database.rollback(connection);

			throw e;
		}
		finally
		{
			long costTime = System.currentTimeMillis() - startTime;
			boolean highCost = ((sqlCostTime > 100) || ((costTime - sqlCostTime) > 100));

			if (highCost || dispatcher.displayDebug)
			{
				StringBuilder sbLog = new StringBuilder();

				sbLog.append("orderId = ");
				sbLog.append(message.getOrderId());
				sbLog.append(", orderDate = ");
				sbLog.append(ThreadUtil.logTimestamp(message.getOrderDate()));
				sbLog.append(", isdn = ");
				sbLog.append(message.getIsdn());
				sbLog.append(", status = ");
				sbLog.append(message.getStatus());
				sbLog.append(", cause = ");
				sbLog.append(message.getCause());
				sbLog.append(", description = ");
				sbLog.append(message.getDescription());
				sbLog.append(", response date: ");
				sbLog.append(ThreadUtil.logTimestamp(message.getResponseTime()));
				sbLog.append(", order cost: ");
				sbLog.append(System.currentTimeMillis()
						- message.getOrderDate().getTime());
				sbLog.append(", update cost: ");
				sbLog.append(costTime);
				sbLog.append(", sendToQueue cost: ");
				sbLog.append(endTimeSendToQueue - startTimeSendToQueue);

				if (missing)
				{
					sbLog.append(", warning: missing destination");
				}

				if (highCost)
				{
					logMonitor(sbLog.toString());
				}
				else
				{
					debugMonitor(sbLog.toString());
				}
			}
		}

		return Constants.BIND_ACTION_SUCCESS;
	}

	public boolean isTimeOut(long startTime, long endTime, long timeout)
	{
		return (endTime - startTime) > timeout;
	}
}
