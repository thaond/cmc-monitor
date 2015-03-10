package com.crm.provisioning.impl.vim;

import java.util.Date;

import com.crm.kernel.message.Constants;
import com.crm.product.cache.ProductEntry;
import com.crm.product.cache.ProductFactory;
import com.crm.provisioning.cache.ProvisioningCommand;
import com.crm.provisioning.impl.CommandImpl;
import com.crm.provisioning.message.CommandMessage;
import com.crm.provisioning.thread.CommandInstance;
import com.crm.util.GeneratorSeq;
import com.crm.util.StringUtil;
import com.unified.provisioning.ws.Response;

public class VIMCommandImpl extends CommandImpl
{
	private static final String	ERROR_SUB_NOT_EXISTS	= "ERR_SUB_NOT_EXISTS";

	public CommandMessage registerService(CommandInstance instance, ProvisioningCommand provisioningCommand, CommandMessage request) throws Exception
	{
		if (instance.getDebugMode().equals("depend"))
		{
			simulation(instance, provisioningCommand, request);
		}
		else
		{
			try
			{
				int subscriberType;
				if (request.isPostpaid())
				{
					subscriberType = 501;
				}
				else
				{
					subscriberType = 500;
				}

				ProductEntry product = ProductFactory.getCache().getProduct(request.getProductId());
				int packageType = Integer.parseInt(product.getParameter("PackageTypeCharge", "101"));
				if (request.getCampaignId() != Constants.DEFAULT_ID)
				{
					packageType = Integer.parseInt(product.getParameter("PackageTypeFree", "110"));
					;
				}

				VIMConnection connection = null;
				try
				{
					connection = (VIMConnection) instance.getProvisioningConnection();
					int sessionId = setRequestLog(instance, request, "REGISTER(" + request.getIsdn() + ", " + packageType + ")");
					Response response = connection.register(request, subscriberType, packageType, sessionId);
					setResponse(instance, request, "VIM." + response.getResponseCode(), sessionId);
					
					boolean found = false;
					int[] arrExpecteds = StringUtil.toIntegerArray(provisioningCommand.getParameter("expectedResult", "0"), ";");

					for (int j = 0; !found && (j < arrExpecteds.length); j++)
					{
						if (response.getResponseCode() == arrExpecteds[j])
						{
							found = true;
						}
					}
					
					if (!found)
					{
						request.setCause(Constants.ERROR);
					}
				}
				finally
				{
					instance.closeProvisioningConnection(connection);
				}
			}
			catch (Exception e)
			{
				processError(instance, provisioningCommand, request, e);
			}
		}

		return request;
	}

	public CommandMessage unregisterService(CommandInstance instance, ProvisioningCommand provisioningCommand, CommandMessage request)
			throws Exception
	{
		if (instance.getDebugMode().equals("depend"))
		{
			simulation(instance, provisioningCommand, request);
		}
		else
		{
			try
			{
				VIMConnection connection = null;
				try
				{
					connection = (VIMConnection) instance.getProvisioningConnection();
					int sessionId = setRequestLog(instance, request, "UNREGISTER(" + request.getIsdn() + ")");
					Response response = connection.unregister(request, sessionId);

					setResponse(instance, request, "VIM." + response.getResponseCode(), sessionId);
					
					boolean found = false;
					int[] arrExpecteds = StringUtil.toIntegerArray(provisioningCommand.getParameter("expectedResult", "0"), ";");

					for (int j = 0; !found && (j < arrExpecteds.length); j++)
					{
						if (response.getResponseCode() == arrExpecteds[j])
						{
							found = true;
						}
					}
					
					if (!found)
					{
						if (response.getResponseName().equalsIgnoreCase(ERROR_SUB_NOT_EXISTS))
						{
							request.setCause(ERROR_SUB_NOT_EXISTS);
						}
						else
						{
							request.setCause(Constants.ERROR);
						}
					}
				}
				finally
				{
					instance.closeProvisioningConnection(connection);
				}
			}
			catch (Exception e)
			{
				processError(instance, provisioningCommand, request, e);
			}
		}

		return request;
	}

	public CommandMessage reactiveService(CommandInstance instance, ProvisioningCommand provisioningCommand, CommandMessage request) throws Exception
	{
		if (instance.getDebugMode().equals("depend"))
		{
			simulation(instance, provisioningCommand, request);
		}
		else
		{
			try
			{
				VIMConnection connection = null;
				try
				{
					connection = (VIMConnection) instance.getProvisioningConnection();
					int sessionId = setRequestLog(instance, request, "REACTIVE(" + request.getIsdn() + ")");
					Response response = connection.reactive(request, sessionId);

					if (response.getResponseName().equalsIgnoreCase(ERROR_SUB_NOT_EXISTS))
					{
						int subscriberType;
						if (request.isPostpaid())
						{
							subscriberType = 501;
						}
						else
						{
							subscriberType = 500;
						}

						response = connection.register(request, subscriberType, 101, GeneratorSeq.getNextSeq());
					}

					setResponse(instance, request, "VIM." + response.getResponseCode(), sessionId);
					
					boolean found = false;
					int[] arrExpecteds = StringUtil.toIntegerArray(provisioningCommand.getParameter("expectedResult", "0"), ";");

					for (int j = 0; !found && (j < arrExpecteds.length); j++)
					{
						if (response.getResponseCode() == arrExpecteds[j])
						{
							found = true;
						}
					}
					
					if (!found)
					{
						request.setCause(Constants.ERROR);
					}
				}
				finally
				{
					instance.closeProvisioningConnection(connection);
				}
			}
			catch (Exception e)
			{
				processError(instance, provisioningCommand, request, e);
			}
		}

		return request;
	}

	public CommandMessage deactiveService(CommandInstance instance, ProvisioningCommand provisioningCommand, CommandMessage request) throws Exception
	{
		if (instance.getDebugMode().equals("depend"))
		{
			simulation(instance, provisioningCommand, request);
		}
		else
		{
			try
			{
				VIMConnection connection = null;
				try
				{
					connection = (VIMConnection) instance.getProvisioningConnection();
					int sessionId = setRequestLog(instance, request, "DEACTIVE(" + request.getIsdn() + ")");
					Response response = connection.renewal(request, 600, sessionId);
					setResponse(instance, request, "VIM." + response.getResponseCode(), sessionId);
					
					boolean found = false;
					int[] arrExpecteds = StringUtil.toIntegerArray(provisioningCommand.getParameter("expectedResult", "0"), ";");

					for (int j = 0; !found && (j < arrExpecteds.length); j++)
					{
						if (response.getResponseCode() == arrExpecteds[j])
						{
							found = true;
						}
					}
					
					if (!found)
					{
						if (response.getResponseName().equalsIgnoreCase(ERROR_SUB_NOT_EXISTS))
						{
							request.setCause(ERROR_SUB_NOT_EXISTS);
						}
						else
						{
							request.setCause(Constants.ERROR);
						}
					}
				}
				finally
				{
					instance.closeProvisioningConnection(connection);
				}
			}
			catch (Exception e)
			{
				processError(instance, provisioningCommand, request, e);
			}
		}

		return request;
	}

	public int setRequestLog(CommandInstance instance, CommandMessage request, String requestString) throws Exception
	{
		request.setRequestTime(new Date());
		long sessionId = setRequest(instance, request, requestString);
		if (sessionId > (long) Integer.MAX_VALUE)
			return (int) (sessionId % (long) Integer.MAX_VALUE);
		else
			return (int) sessionId;
	}

}
