package com.crm.provisioning.impl.ccws;

import com.crm.provisioning.cache.ProvisioningCommand;
import com.crm.provisioning.message.CommandMessage;
import com.crm.provisioning.thread.CommandInstance;

public class PaymentCommandImpl extends CCWSCommandImpl
{
	public CommandMessage voucherPostpaidTopup(CommandInstance instance, ProvisioningCommand provisioningCommand,
			CommandMessage request)
			throws Exception
	{
		return request;
	}
	
	public CommandMessage voucherPrepaidTopup(CommandInstance instance, ProvisioningCommand provisioningCommand,
			CommandMessage request)
			throws Exception
	{
		return request;
	}

	public CommandMessage nonVoucherRecharge(CommandInstance instance, ProvisioningCommand provisioningCommand,
			CommandMessage request)
			throws Exception
	{
		return request;
	}
}
