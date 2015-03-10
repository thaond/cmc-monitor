package com.crm.provisioning.impl.sigCall;

import java.util.Date;

import com.crm.kernel.message.Constants;
import com.crm.marketing.cache.CampaignEntry;
import com.crm.marketing.cache.CampaignFactory;
import com.crm.product.cache.ProductEntry;
import com.crm.product.cache.ProductFactory;
import com.crm.provisioning.cache.ProvisioningCommand;
import com.crm.provisioning.impl.CommandImpl;
import com.crm.provisioning.message.CommandMessage;
import com.crm.provisioning.thread.CommandInstance;
import com.crm.util.StringUtil;

public class SignatureCallCommandImpl extends CommandImpl
{
	private int 	SUBSCRIBER_STATUS_ACTIVE		= 1;
	private int 	SUBSCRIBER_STATUS_SUSPENDED		= 2;
	private int 	CHANNEL_SMS						= 1;
	private int 	CHANNEL_WEB						= 5;
	private int 	CHANNEL_CORE					= 6;
	
	public CommandMessage registerService(CommandInstance instance, ProvisioningCommand provisioningCommand,
			CommandMessage request)
			throws Exception
	{
		if (instance.getDebugMode().equals("depend"))
		{
			simulation(instance, provisioningCommand, request);
		}
		else
		{
			int responseCode;
			
			try
			{
				int channel = CHANNEL_SMS;
				if (request.getChannel().equals(Constants.CHANNEL_CORE))
				{
					channel = CHANNEL_CORE;
				}
				else if (request.getChannel().equals(Constants.CHANNEL_WEB))
				{
					channel = CHANNEL_WEB;
				}
				
				int subscriberType = request.isPostpaid() ? 2 : 1;
				int chargingType = request.isFullOfCharge() ? 1 : 2;

				ProductEntry product = ProductFactory.getCache().getProduct(request.getProductId());
				int expirationPeriod = product.getSubscriptionPeriod();
				String expirationUnit = product.getSubscriptionUnit();
				int extensionDays = calculateExtensionDays(request.getCampaignId(), expirationPeriod, expirationUnit,
						request.getQuantity(), request.getParameters().getBoolean("includeCurrentDay"));
				
				SignatureCallConnection connection = null;
				try
				{
					connection = (SignatureCallConnection) instance.getProvisioningConnection();
					int sessionId = setRequestLog(instance, request, "REGISTER(" + request.getIsdn() + ", " + channel + ", " + subscriberType + ", " + chargingType + ", " + extensionDays + ")");
					responseCode = connection.register(request, channel, subscriberType, SUBSCRIBER_STATUS_ACTIVE, chargingType, extensionDays, sessionId);
					setResponse(instance, request, "CS." + responseCode, sessionId);
				}
				finally
				{
					instance.closeProvisioningConnection(connection);
				}
	
				boolean found = false;
				int[] arrExpecteds = StringUtil.toIntegerArray(provisioningCommand.getParameter("expectedResult", "0"), ";");

				for (int j = 0; !found && (j < arrExpecteds.length); j++)
				{
					if (responseCode == arrExpecteds[j])
					{
						found = true;
					}
				}
				
				if (!found)
				{
					request.setCause(Constants.ERROR);
				}
			}
			catch (Exception e)
			{
				processError(instance, provisioningCommand, request, e);
			}
		}
		
		return request;
	}

	public CommandMessage unregisterService(CommandInstance instance, ProvisioningCommand provisioningCommand,
			CommandMessage request)
			throws Exception
	{
		if (instance.getDebugMode().equals("depend"))
		{
			simulation(instance, provisioningCommand, request);
		}
		else
		{
			int responseCode;
			
			try
			{
				int channel = CHANNEL_SMS;
				if (request.getChannel().equals(Constants.CHANNEL_CORE))
				{
					channel = CHANNEL_CORE;
				}
				else if (request.getChannel().equals(Constants.CHANNEL_WEB))
				{
					channel = CHANNEL_WEB;
				}
				
				SignatureCallConnection connection = null;
				try
				{
					connection = (SignatureCallConnection) instance.getProvisioningConnection();
					int sessionId = setRequestLog(instance, request, "UNREGISTER(" + request.getIsdn() + ")");
					responseCode = connection.unregister(request, channel, sessionId);
	
					setResponse(instance, request, "CS." + responseCode, sessionId);
				}
				finally
				{
					instance.closeProvisioningConnection(connection);
				}
	
				boolean found = false;
				int[] arrExpecteds = StringUtil.toIntegerArray(provisioningCommand.getParameter("expectedResult", "0"), ";");

				for (int j = 0; !found && (j < arrExpecteds.length); j++)
				{
					if (responseCode == arrExpecteds[j])
					{
						found = true;
					}
				}
				
				if (!found)
				{
					request.setCause(Constants.ERROR);
				}
			}
			catch (Exception e)
			{
				processError(instance, provisioningCommand, request, e);
			}
		}

		return request;
	}

	public CommandMessage reactiveService(CommandInstance instance, ProvisioningCommand provisioningCommand,
			CommandMessage request)
			throws Exception
	{
		if (instance.getDebugMode().equals("depend"))
		{
			simulation(instance, provisioningCommand, request);
		}
		else
		{
			int responseCode;
			try
			{
				int channel = CHANNEL_SMS;
				if (request.getChannel().equals(Constants.CHANNEL_CORE))
				{
					channel = CHANNEL_CORE;
				}
				else if (request.getChannel().equals(Constants.CHANNEL_WEB))
				{
					channel = CHANNEL_WEB;
				}
				
				int chargingType = request.isFullOfCharge() ? 1 : 2;
				
				ProductEntry product = ProductFactory.getCache().getProduct(request.getProductId());
				int expirationPeriod = request.isFullOfCharge() ? product.getSubscriptionPeriod() : request.getQuantity();
				String expirationUnit = request.isFullOfCharge() ? product.getSubscriptionUnit() : "daily";
				int extensionDays = calculateExtensionDays(request.getCampaignId(), expirationPeriod, expirationUnit,
						1, request.getParameters().getBoolean("includeCurrentDay"));
				
				SignatureCallConnection connection = null;
				try
				{
					connection = (SignatureCallConnection) instance.getProvisioningConnection();
					int sessionId = setRequestLog(instance, request, "RENEWAL(" + request.getIsdn() + ", " + channel + ", " + chargingType + ", " + extensionDays + ")");
					responseCode = connection.renewal(request, channel, SUBSCRIBER_STATUS_ACTIVE, chargingType, extensionDays, sessionId);
					setResponse(instance, request, "CS." + responseCode, sessionId);
				}
				finally
				{
					instance.closeProvisioningConnection(connection);
				}
	
				boolean found = false;
				int[] arrExpecteds = StringUtil.toIntegerArray(provisioningCommand.getParameter("expectedResult", "0"), ";");

				for (int j = 0; !found && (j < arrExpecteds.length); j++)
				{
					if (responseCode == arrExpecteds[j])
					{
						found = true;
					}
				}
				
				if (!found)
				{
					request.setCause(Constants.ERROR);
				}
			}
			catch (Exception e)
			{
				processError(instance, provisioningCommand, request, e);
			}
		}

		return request;
	}

	public CommandMessage deactiveService(CommandInstance instance, ProvisioningCommand provisioningCommand,
			CommandMessage request)
			throws Exception
	{
		if (instance.getDebugMode().equals("depend"))
		{
			simulation(instance, provisioningCommand, request);
		}
		else
		{
			int responseCode;
			try
			{
				int channel = CHANNEL_SMS;
				if (request.getChannel().equals(Constants.CHANNEL_CORE))
				{
					channel = CHANNEL_CORE;
				}
				else if (request.getChannel().equals(Constants.CHANNEL_WEB))
				{
					channel = CHANNEL_WEB;
				}
				
				int chargingType = request.isFullOfCharge() ? 1 : 2;
				
				SignatureCallConnection connection = null;
				try
				{
					connection = (SignatureCallConnection) instance.getProvisioningConnection();
					int sessionId = setRequestLog(instance, request, "DEACTIVE(" + request.getIsdn() + ")");
					responseCode = connection.renewal(request, channel, SUBSCRIBER_STATUS_SUSPENDED, chargingType, 0, sessionId);
					setResponse(instance, request, "CS." + responseCode, sessionId);
					
					boolean found = false;
					int[] arrExpecteds = StringUtil.toIntegerArray(provisioningCommand.getParameter("expectedResult", "0"), ";");

					for (int j = 0; !found && (j < arrExpecteds.length); j++)
					{
						if (responseCode == arrExpecteds[j])
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
	
	public int setRequestLog(CommandInstance instance, CommandMessage request, String requestString) throws Exception
	{
		request.setRequestTime(new Date());
		long sessionId = setRequest(instance, request, requestString);
		if (sessionId > (long)Integer.MAX_VALUE)
			return (int)(sessionId % (long)Integer.MAX_VALUE);
		else
			return (int) sessionId;
	}
	
	public int calculateExtensionDays(long campaignId, int expirationPeriod, String expirationUnit, int quantity, boolean includeCurrentDay) throws Exception
	{
		if (campaignId != Constants.DEFAULT_ID)
		{
			CampaignEntry campaign = CampaignFactory.getCache()
					.getCampaign(campaignId);

			if ((campaign != null))
			{
				expirationPeriod = campaign.getSchedulePeriod();
				expirationUnit = campaign.getScheduleUnit();
			}
		}

		if (expirationUnit.equalsIgnoreCase("monthly")
				|| expirationUnit.equalsIgnoreCase("month"))
		{
			expirationPeriod = 30 * expirationPeriod * quantity;
		}
		else if (expirationUnit.equalsIgnoreCase("weekly")
				|| expirationUnit.equalsIgnoreCase("week"))
		{
			expirationPeriod = 7 * expirationPeriod * quantity;
		}
		else if (expirationUnit.equalsIgnoreCase("daily")
				|| expirationUnit.equalsIgnoreCase("day"))
		{
			expirationPeriod = 1 * expirationPeriod * quantity;
		}

		if (includeCurrentDay)
		{
			expirationPeriod = expirationPeriod - 1;
		}
		
		return expirationPeriod;
	}
}
