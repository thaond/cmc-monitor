package com.crm.provisioning.impl;

import java.util.Date;

import com.crm.kernel.message.Constants;
import com.crm.provisioning.cache.ProvisioningCommand;
import com.crm.provisioning.message.CommandMessage;
import com.crm.provisioning.thread.CommandInstance;
import com.crm.subscriber.impl.GSMServiceImpl;

public class GSMCommandImpl extends CommandImpl
{
	public CommandMessage report(CommandInstance instance,
			ProvisioningCommand provisioningCommand, CommandMessage request)
			throws Exception
	{
		CommandMessage result = request;

		try
		{
			Date reportDate = new Date(result.getParameters().getLong("sms.params[0]", 0));
			String mcc = result.getParameters().getString("sms.params[1]", "");
			String mnc = result.getParameters().getString("sms.params[2]", "");
			String type = result.getParameters().getString("sms.params[3]", "");
			long lac = result.getParameters().getLong("sms.params[4]", 0);
			long cellId = result.getParameters().getLong("sms.params[5]", 0);
			long rnc = result.getParameters().getLong("sms.params[6]", 0);
			long psc = result.getParameters().getLong("sms.params[7]", 0);
			String signal = result.getParameters().getString("sms.params[8]", "");
			String latitude = result.getParameters().getString("sms.params[9]", "");
			String longitude = result.getParameters().getString("sms.params[10]", "");
			int voiceStatus = result.getParameters().getInt("sms.params[11]");
			int smsStatus = result.getParameters().getInt("sms.params[12]");
			int speedStatus = result.getParameters().getInt("sms.params[13]");
			int networkStatus = result.getParameters().getInt("sms.params[14]");
			
			int status = Constants.GSM_APPROVE_STATUS;
			if (voiceStatus != 5 || smsStatus != 4 || (networkStatus != 2 && networkStatus != 4))
			{
				status = Constants.GSM_NO_NETWORK_STATUS;
			}
			long sessionId = setRequest(instance, request,
					getLogRequest("com.crm.provisioning.impl.GSMCommandImpl.report", request.getIsdn()));

			GSMServiceImpl.report(result.getUserId(), result.getUserName(), result.getIsdn(), reportDate,
					mcc, mnc, type, lac, cellId, rnc, psc, signal,
					latitude, longitude, voiceStatus, smsStatus,
					speedStatus, networkStatus, "", status);
					
			
			setResponse(instance, request, "success", sessionId);
		}
		catch (Exception error)
		{
			processError(instance, provisioningCommand, request, error);
		}

		return result;
	}
}
