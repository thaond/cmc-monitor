/**
 * 
 */
package com.crm.provisioning.thread;

import java.sql.SQLException;

import javax.jms.QueueSession;

import com.crm.kernel.message.Constants;
import com.crm.kernel.queue.QueueFactory;
import com.crm.product.cache.ProductAction;
import com.crm.product.cache.ProductEntry;
import com.crm.product.cache.ProductFactory;
import com.crm.product.cache.ProductRoute;
import com.crm.provisioning.cache.CommandEntry;
import com.crm.provisioning.cache.ProvisioningEntry;
import com.crm.provisioning.cache.ProvisioningFactory;
import com.crm.provisioning.message.CommandMessage;
import com.crm.provisioning.util.CommandUtil;
import com.crm.subscriber.bean.SubscriberOrder;
import com.crm.subscriber.impl.SubscriberEntryImpl;
import com.crm.subscriber.impl.SubscriberOrderImpl;
import com.fss.util.AppException;

/**
 * @author ThangPV
 * 
 */
public class OrderRoutingInstance extends ProvisioningInstance
{
	private QueueSession		session					= null;
	
	public OrderRoutingInstance() throws Exception
	{
		super();
	}

	public OrderRoutingThread getDispatcher()
	{
		return (OrderRoutingThread) dispatcher;
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

	public boolean isOverload()
	{
		return false;
	}

	public CommandMessage validateOrder(ProductRoute orderRoute, CommandMessage order) throws AppException, Exception
	{
		try
		{
			if ((orderRoute == null) || orderRoute.getExecuteMethod() == null)
			{
				return order;
			}

			Object result = orderRoute.getExecuteMethod().invoke(orderRoute.getExecuteImpl(), this, orderRoute, order);

			if (result instanceof CommandMessage)
			{
				return (CommandMessage) result;
			}
			else
			{
				throw new AppException("order-invalid");
			}
		}
		catch (Exception e)
		{
			throw e;
		}
	}

	public int processMessage(CommandMessage order) throws Exception
	{
		ProductRoute orderRoute = null;
		ProductEntry product = null;
		ProductAction action = null;
		CommandEntry command = null;

		Exception error = null;
		
		boolean createOrder = false;

		long costTotal = 0;
		long costCreateOrder = 0;
		long costUpdateOrder = 0;
		long firstTime = System.currentTimeMillis();

		try
		{
			if (order.getQuantity() == 0)
			{
				order.setQuantity(1);
			}

			// source address
			String isdn = CommandUtil.addCountryCode(order.getIsdn());
			order.setIsdn(isdn);

			// destination address
			String shippingTo = CommandUtil.addCountryCode(order.getShipTo());
			order.setShipTo(shippingTo);

			// get command
			if (order.getKeyword().equals(""))
			{
				throw new AppException("unknow-keyword");
			}

			if (isOverload() && getDispatcher().autoReplyIfOverload)
			{
				throw new AppException(Constants.ERROR_RESOURCE_BUSY);
			}

			// get order routing
			if (order.getRouteId() != Constants.DEFAULT_ID)
			{
				orderRoute = ProductFactory.getCache().getProductRoute(order.getRouteId());
			}
			else
			{
				orderRoute =
						ProductFactory.getCache().getProductRoute(
								order.getChannel(), order.getServiceAddress(), order.getKeyword(), order.getOrderDate());
			}

			if (orderRoute == null)
			{
				throw new AppException(Constants.ERROR_INVALID_SYNTAX);
			}
			else
			{
				order.setProductId(orderRoute.getProductId());
				order.setActionType(orderRoute.getActionType());

				order.setRouteId(orderRoute.getRouteId());

				// check timeout
				if (orderRoute.getStatus() == Constants.SERVICE_STATUS_DENIED)
				{
					throw new AppException(Constants.UPGRADING);
				}
				else if (CommandUtil.isTimeout(order, orderRoute.getTimeout()))
				{
					throw new AppException(Constants.ERROR_TIMEOUT);
				}
			}

			/**
			 * Throws AppException(duplicate request) if the same request is in
			 * processing
			 */
			// ((OrderRoutingThread) dispatcher).checkProcessing(order);
			// checkProcessingPass = true;

			// product
			product = ProductFactory.getCache().getProduct(order.getProductId());

			if (product == null)
			{
				throw new AppException(Constants.ERROR_PRODUCT_NOT_FOUND);
			}
			else if (product.getStatus() == Constants.SERVICE_STATUS_DENIED)
			{
				throw new AppException(Constants.UPGRADING);
			}

			/**
			 * Create order
			 */
			SubscriberOrder subscriberOrder = null;
			if ((orderRoute != null) && orderRoute.isCreateOrder())
			{
				try
				{
					long startTime = System.currentTimeMillis();

					subscriberOrder = SubscriberOrderImpl.createOrder(
							order.getUserId(), order.getUserName(), order.getOrderDate(), order.getActionType()
							, order.getSubscriberId(), order.getIsdn(), order.getSubscriberType()
							, order.getSubProductId(), order.getProductId()
							, order.getPrice(), order.getQuantity(), order.getDiscount(), order.getAmount(), order.getScore()
							, order.getCause(), order.getStatus(), order.getChannel());

					order.setOrderDate(subscriberOrder.getOrderDate());
					order.setOrderId(subscriberOrder.getOrderId());
					createOrder = true;
					
					costCreateOrder = (System.currentTimeMillis() - startTime);
				}
				catch (SQLException e)
				{
					order.setStatus(Constants.ORDER_STATUS_DENIED);

					if (e.getMessage().startsWith("ORA-00001"))
					{
						order.setCause(Constants.ERROR_DUPLICATED);
						order.setDescription(e.getMessage());
					}
					else
					{
						order.setCause(Constants.ERROR_CREATE_ORDER_FAIL);
						order.setDescription(e.getMessage());
					}
				}
				catch (AppException e)
				{
					order.setStatus(Constants.ORDER_STATUS_DENIED);
					order.setDescription(e.getMessage());
				}
				catch (Exception e)
				{
					order.setStatus(Constants.ORDER_STATUS_DENIED);
					order.setCause(Constants.ERROR_CREATE_ORDER_FAIL);
					order.setDescription(e.getMessage());
				}
			}

			if (order.getStatus() != Constants.ORDER_STATUS_DENIED)
			{
				if (order.getChannel().equals(Constants.CHANNEL_SMS) && createOrder)
				{
					CommandMessage logReceiver = new CommandMessage();
					logReceiver = order.clone();
					logReceiver.setRequest(order.getServiceAddress() + " - " + order.getKeyword());
					logReceiver.setResponse(order.getResponse());
					sendCommandLog(logReceiver);
					
					order.setRequest("");
					order.setResponse("");
				}
				
				if (orderRoute.getExecuteMethod() != null)
				{
					order = validateOrder(orderRoute, order);
				}
				else
				{
					// get subscriber type
					order.setSubscriberType(SubscriberEntryImpl.getSubscriberType(order.getIsdn()));
				}

				// get price
				if (!orderRoute.isCheckBalance() && (order.getStatus() != Constants.ORDER_STATUS_DENIED))
				{
					// set default price
					order.setOfferPrice(product.getPrice());

//					ProductPrice productPrice =
//							product.getProductPrice(
//									order.getChannel(), order.getActionType(), order.getSegmentId()
//									, order.getAssociateProductId(), order.getQuantity(), order.getOrderDate());
//
//					if (productPrice != null)
//					{
//						order.setPrice(productPrice.getFullOfCharge());
//					}
//					else
//					{
						order.setPrice(product.getPrice());
//					}

					order.setAmount(order.getPrice() * order.getQuantity());
				}

				if (order.getParameters().getProperty("IsQueryRTBS", "false").equals("true"))
				{
					order.setProvisioningType("ROUTE");
					sendCommandLog(order);
				}

				/**
				 * Get command
				 */
				if (order.getStatus() != Constants.ORDER_STATUS_DENIED)
				{
					// get destination queue
					String queueName =
							orderRoute.getParameter(
									order.getActionType(), "destination.queue", "", order.isPrepaid(), orderRoute.getQueueName());

					// get first provisioning command
					boolean chargeMulti = Boolean.parseBoolean(product.getParameter("ChargeMulti." + order.getActionType(), "false"));
					if (chargeMulti && order.getBalanceType() != null && !order.getBalanceType().equals(""))
					{
						action = product.getProductAction(order.getActionType(), order.getSubscriberType(), order.getBalanceType());
					}
					else
					{
						action = product.getProductAction(order.getActionType(), order.getSubscriberType());
					}

					if (action != null)
					{
						// first execute command for this order
						order.setCommandId(action.getCommandId());

						command = ProvisioningFactory.getCache().getCommand(order.getCommandId());

						if (command == null)
						{
							throw new AppException(Constants.ERROR_COMMAND_NOT_FOUND);
						}
						else
						{
							order.setProvisioningType(command.getProvisioningType());
						}
					}
					else
					{
						throw new AppException(Constants.ERROR_COMMAND_NOT_FOUND);
					}

					// send provisioning request to command routing queue
					if (queueName.equals(""))
					{
						sendCommandRouting(order);
					}
					else
					{
						String provisioningName =
								orderRoute.getParameter(
										order.getActionType(), "destination.provisioning", "", order.isPrepaid(), "");

						if (!provisioningName.equals(""))
						{
							ProvisioningEntry provisioning = ProvisioningFactory.getCache().getProvisioning(provisioningName);

							order.setProvisioningId(provisioning.getProvisioningId());
						}

						QueueFactory.sendMessage(session, order, QueueFactory.getQueue(queueName));
					}
				}
			}
		}
		catch (AppException e)
		{
			order.setCause(e.getMessage());
			order.setDescription(e.getContext());

			error = e;
		}
		catch (Exception e)
		{
			order.setCause(Constants.ERROR);
			order.setDescription(e.getMessage());

			error = e;
		}

		try
		{
			if (error != null)
			{
				order.setStatus(Constants.ORDER_STATUS_DENIED);
			}
			else
			{
				/**
				 * Add log ISDN: PRODUCT_ALIAS - COMMAND_ALIAS<br>
				 * NamTA<br>
				 * 21/08/2012
				 */
//				if (product != null & command != null)
//					debugMonitor(order.getIsdn() + ": " + product.getAlias() + " - " + command.getAlias());
				debugMonitor(order.toOrderString());
			}

			if ((orderRoute == null) || !orderRoute.isSynchronous()
					|| (order.getStatus() == Constants.ORDER_STATUS_DENIED) || (command == null))
			{
				sendOrderResponse(orderRoute, order);
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
			order.getParameters().setBoolean("CreatedOrder", createOrder);
			
			costTotal = System.currentTimeMillis() - firstTime;

			StringBuilder sbLog = new StringBuilder();

			sbLog.append("orderId = ");
			sbLog.append(order.getOrderId());
			sbLog.append(" , orderDate = ");
			sbLog.append(order.getOrderDate());
			sbLog.append(" , isdn = ");
			sbLog.append(order.getIsdn());
			sbLog.append(" , total execute time = ");
			sbLog.append(costTotal);
			sbLog.append(", validation cost = ");
			sbLog.append(costTotal - costCreateOrder - costUpdateOrder);
			sbLog.append(" , create order cost = ");
			sbLog.append(costCreateOrder);
			sbLog.append(" , update order cost = ");
			sbLog.append(costUpdateOrder);

			if ((getDispatcher().baseCostTime > 0) && (costTotal > getDispatcher().baseCostTime))
			{
				logMonitor(sbLog.toString());
			}
			else
			{
				debugMonitor(sbLog.toString());
			}
			
			if (order.getStatus() == Constants.ORDER_STATUS_DENIED)
			{
				getDispatcher().sendInstanceAlarm(error, order, order.getDescription());
			}
		}

		return Constants.BIND_ACTION_SUCCESS;
	}
}
