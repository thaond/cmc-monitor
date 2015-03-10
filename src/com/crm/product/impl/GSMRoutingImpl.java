package com.crm.product.impl;

import com.crm.kernel.message.Constants;
import com.crm.merchant.cache.MerchantAgent;
import com.crm.product.cache.ProductFactory;
import com.crm.product.cache.ProductRoute;
import com.crm.provisioning.message.CommandMessage;
import com.crm.provisioning.thread.OrderRoutingInstance;
import com.crm.subscriber.impl.GSMServiceImpl;
import com.crm.subscriber.impl.SubscriberEntryImpl;
import com.fss.util.AppException;

public class GSMRoutingImpl extends OrderRoutingImpl
{
	public CommandMessage report(OrderRoutingInstance instance,
			ProductRoute orderRoute, CommandMessage order) throws Exception
	{
		Exception error = null;
		try
		{
			smsParser(instance, orderRoute, order);
			
			// check product in available list
			ProductFactory.getCache().getProduct(order.getProductId());
			
			// Set subscriber type
			if (order.getSubscriberType() == Constants.UNKNOW_SUB_TYPE)
			{
				order.setSubscriberType(SubscriberEntryImpl.getSubscriberType(order.getIsdn()));
			}
			
			checkAgent(instance, orderRoute, order);
		}
		catch (Exception e)
		{
			error = e;
		}
		
		if (error != null)
		{
			order.setStatus(Constants.ORDER_STATUS_DENIED);

			if (error instanceof AppException)
			{
				order.setCause(error.getMessage());
			}
			else
			{
				order.setDescription(error.getMessage());
			}
		}
		
		return order;
	}

	public void checkAgent(OrderRoutingInstance instance,
			ProductRoute orderRoute, CommandMessage order) throws Exception
	{
		MerchantAgent agent = GSMServiceImpl.getAgent(order.getIsdn());
		
		if (agent == null)
		{
			throw new AppException(Constants.ERROR_AGENT_NOT_FOUND);
		}
	}
	
	@Override
	public void smsParser(OrderRoutingInstance instance,
			ProductRoute orderRoute, CommandMessage order) throws Exception
	{
		try
		{
			order.setKeyword(order.getKeyword().toUpperCase());

			// remove twice space
			String smsContent = "";

			if (!order.getRequest().equals(""))
			{
				smsContent = order.getRequest().toString();
			}
			else
			{
				smsContent = order.getKeyword();
			}
			
			smsContent = smsContent.trim();

			while (smsContent.indexOf("  ") >= 0)
			{
				smsContent = smsContent.replaceAll("  ", " ");
			}

			// SMS parser

			// Properties parameters = order.getParameters();
			if (smsContent.length() >= orderRoute.getKeyword().length())
			{
				smsContent = smsContent.substring(
						orderRoute.getKeyword().length()).trim();
			}
			
			String[] arrParams = smsContent.split("��");

			// use default number if value of the parameter is wrong.
			if ((orderRoute.getSmsMaxParams() >= 0)
					&& (arrParams.length > orderRoute.getSmsMaxParams()))
			{
				throw new AppException(Constants.ERROR_INVALID_SYNTAX);
			}
			
			if ((orderRoute.getSmsMinParams() > 0)
					&& (arrParams.length < orderRoute.getSmsMinParams()))
			{
				throw new AppException(Constants.ERROR_INVALID_SYNTAX);
			}
			
			// update SMS option parameter
			order.getParameters().setString("sms.params.count",
					String.valueOf(arrParams.length));

			for (int j = 0; j < arrParams.length; j++)
			{
				order.getParameters().setString("sms.params[" + j + "]",
						arrParams[j]);
			}
		}
		catch (Exception e)
		{
			throw new AppException(Constants.ERROR_INVALID_SYNTAX);
		}
	}
}
