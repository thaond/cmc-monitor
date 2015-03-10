package com.crm.provisioning.thread;

import javax.jms.Message;

import com.crm.kernel.message.Constants;
import com.crm.kernel.queue.QueueFactory;
import com.crm.product.cache.ProductEntry;
import com.crm.product.cache.ProductFactory;
import com.crm.provisioning.message.CommandMessage;
import com.crm.thread.DispatcherInstance;

public class CommandStatisticInstance extends DispatcherInstance
{
	public CommandStatisticInstance() throws Exception
	{
		super();
	}

	public CommandStatisticThread getDispatcher()
	{
		return (CommandStatisticThread) dispatcher;
	}

	public int processMessage(Message message) throws Exception
	{
		try
		{
			CommandMessage request = (CommandMessage) QueueFactory.getContentMessage(message);

			return processMessage(request);
		}
		catch (Exception e)
		{
		}

		return Constants.BIND_ACTION_NONE;
	}

	public int processMessage(CommandMessage request) throws Exception
	{
		if (request == null)
		{
			return Constants.BIND_ACTION_NONE;
		}

		try
		{
			ProductStatistic productStatistic = null;

			productStatistic = QueueFactory.chpProductStatistic.get(request.getProductId());

			if (productStatistic == null)
			{
				ProductEntry product = ProductFactory.getCache().getProduct(request.getProductId());

				if (product == null)
				{
					return Constants.BIND_ACTION_NONE;
				}

				productStatistic = new ProductStatistic();

				productStatistic.setAlias(product.getAlias());
				productStatistic.setProductId(request.getProductId());

				QueueFactory.chpProductStatistic.put(request.getProductId(), productStatistic);
			}

			synchronized (productStatistic) 
			{
				if (request.getStatus() == Constants.ORDER_STATUS_APPROVED)
				{
					productStatistic.setSuccess(productStatistic.getSuccess() + 1);
				}
				else
				{
					productStatistic.setFailure(productStatistic.getFailure() + 1);
				}

				QueueFactory.chpProductStatistic.put(request.getProductId(), productStatistic);
			}
		}
		catch (Exception e)
		{
			throw e;
		}

		return Constants.BIND_ACTION_NONE;
	}
}
