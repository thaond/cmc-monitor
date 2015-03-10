/**
 * 
 */
package com.crm.provisioning.impl.osa;

import org.apache.axis.AxisFault;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.crm.kernel.message.Constants;
import com.crm.product.cache.ProductEntry;
import com.crm.product.cache.ProductFactory;
import com.crm.provisioning.cache.CommandEntry;
import com.crm.provisioning.cache.ProvisioningCommand;
import com.crm.provisioning.cache.ProvisioningFactory;
import com.crm.provisioning.impl.CommandImpl;
import com.crm.provisioning.message.CommandMessage;
import com.crm.provisioning.message.VNMMessage;
import com.crm.provisioning.thread.CommandInstance;
import com.crm.provisioning.util.CommandUtil;
import com.fss.util.AppException;

/**
 * @author ThangPV
 * 
 */
public class OSACommandImpl extends CommandImpl
{
	@Override
	public String getErrorCode(CommandInstance instance, CommandMessage request, Exception error)
	{
		String errorCode = Constants.ERROR;
		String errorDescription = "";
		if (!(error instanceof AxisFault) && !(error instanceof AppException))
		{
			instance.debugMonitor(error);
			return errorCode;
		}

		AxisFault axisError = (AxisFault) error;

		if (axisError.getFaultDetails() != null)
		{
			if (axisError.getFaultDetails().length > 0)
			{
				Element e = axisError.getFaultDetails()[0];
				String name = e.getLocalName();
				errorDescription = name;

				try
				{
					NodeList nodeList = e.getElementsByTagName("ExtraInformation");
					if (nodeList != null)
					{
						if (nodeList.getLength() > 0)
						{
							Element extraInfo = (Element) nodeList.item(0);
							if (extraInfo.getChildNodes().getLength() > 0)
							{
								errorDescription += ": " + extraInfo.getChildNodes().item(0).getNodeValue();
							}
						}
					}
				}
				catch (Exception ex)
				{
				}

				if (name == null)
				{
					errorCode = "osa-server-error";
					errorDescription = "OSA Server Error";
				}
				else if (name.equals("P_INVALID_USER"))
				{
					errorCode = "invalid-user";
				}
				else if (name.equals("P_INVALID_ACCOUNT"))
				{
					errorCode = "invalid-account";
				}
				else if (name.equals("P_INVALID_AMOUNT"))
				{
					errorCode = "invalid-amount";
				}
				else if (name.equals("P_INVALID_CURRENCY"))
				{
					errorCode = "invalid-currency";
				}
				else if (name.equals("P_INVALID_SESSION_ID"))
				{
					errorCode = "invalid-session";
				}
				else if (name.equals("P_INVALID_REQUEST_NUMBER"))
				{
					errorCode = "invalid-request-number";
				}
			}
		}

		request.setDescription(errorDescription);

		return errorCode;
	}

	public void creditAmount(
			CommandInstance instance, ProvisioningCommand provisioningCommand, CommandMessage request, String description)
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
				CommandEntry command = ProvisioningFactory.getCache().getCommand(request.getCommandId());
	
				ProductEntry product = ProductFactory.getCache().getProduct(request.getProductId());
	
				command.setMaxRetry(1);
	
				int amount = CommandUtil.getAmount(request.getAmount());
	
				if (amount < 0)
				{
					throw new AppException("invalid-amount");
				}
				else if (amount == 0)
				{
					instance.debugMonitor("Amount = 0, Do not credit amount.");
				}
				else
				{
					if (description.equals(""))
					{
						String descKey = "osa.credit.description." + request.getActionType();
		
						description = product.getParameter(descKey, "");
						if (description.equals(""))
						{
							description = product.getParameter("osa.credit.description", "");
						}
						if (description.equals(""))
						{
							description = "Cancel Reserve: <ALIAS>";
						}
					}
					
					description = description.replace("<ALIAS>", product.getIndexKey());
	
					if (amount > 0)
					{
						request = OSAConnection.charging(instance, request, description, false);
					}
	
					request.getParameters().setBoolean("isPaid", (request.getStatus() == Constants.ORDER_STATUS_DENIED));
				}
			}
			catch (Exception e)
			{
				processError(instance, provisioningCommand, request, e);
			}
		}
	}

	public VNMMessage cancelDebit(CommandInstance instance, ProvisioningCommand provisioningCommand, CommandMessage request)
			throws Exception
	{
		VNMMessage result = CommandUtil.createVNMMessage(request);

		try
		{
			result.setRequest("cancelDebit");

			if (result.isPaid())
			{
				creditAmount(instance, provisioningCommand, result, "");
			}
		}
		catch (Exception e)
		{
			processError(instance, provisioningCommand, request, e);
		}

		return result;
	}

	public VNMMessage directDebit(CommandInstance instance, ProvisioningCommand provisioningCommand, CommandMessage request)
			throws Exception
	{
		VNMMessage result = CommandUtil.createVNMMessage(request);

		if (instance.getDebugMode().equals("depend"))
		{
			simulation(instance, provisioningCommand, result);
		}
		else
		{
			try
			{
				CommandEntry command = ProvisioningFactory.getCache().getCommand(result.getCommandId());

				ProductEntry product = ProductFactory.getCache().getProduct(result.getProductId());

				command.setMaxRetry(1);

				int amount = CommandUtil.getAmount(result.getAmount());

				if (amount < 0)
				{
					throw new AppException("invalid-amount");
				}
				else if (amount == 0)
				{
					instance.debugMonitor("Amount = 0, Do not debit amount.");
					result.getParameters().setProperty("ByPassCommandLog", "true");
				}
				else
				{
					String descKey = "osa.debit.description." + result.getChannel() + "." + result.getActionType();

					String description = product.getParameter(descKey, "");
					if (description.equals(""))
					{
						description = product.getParameter("osa.debit.description", "");
					}
					if (description.equals(""))
					{
						if (result.getActionType().equals(Constants.ACTION_SUBSCRIPTION)
								|| (result.getActionType().equals(Constants.ACTION_TOPUP)
										&& result.getChannel().equals(Constants.CHANNEL_CORE)))
						{

							description = "Vas-MonthlyBilling: <ALIAS> fee";
						}
						else
						{
							description = "Reserve <ALIAS> fee";
						}
					}

					description = description.replace("<ALIAS>", product.getIndexKey());
					
					if (amount > 0)
					{
						result = (VNMMessage) OSAConnection.charging(instance, result, description, true);
					}

					result.setPaid(result.getStatus() != Constants.ORDER_STATUS_DENIED);
				}
			}
			catch (Exception e)
			{
				processError(instance, provisioningCommand, result, e);
			}
		}
		return result;
	}

	public VNMMessage subscription(CommandInstance instance, ProvisioningCommand provisioningCommand, CommandMessage request)
			throws Exception
	{
		VNMMessage result = CommandUtil.createVNMMessage(request);

		try
		{
			CommandEntry command = ProvisioningFactory.getCache().getCommand(result.getCommandId());

			ProductEntry product = ProductFactory.getCache().getProduct(result.getProductId());

			command.setMaxRetry(1);

			int amount = CommandUtil.getAmount(result.getAmount());

			if (amount < 0)
			{
				throw new AppException("invalid-amount");
			}
			else if (amount == 0)
			{

			}
			else
			{
				String descKey = "osa.debit.description." + result.getActionType();

				String description = product.getParameter(descKey, "");
				if (description.equals(""))
				{
					description = product.getParameter("osa.debit.description", "");
				}
				if (description.equals(""))
				{
					if (result.getActionType().equals(Constants.ACTION_SUBSCRIPTION))
					{
						description = "Vas-MonthlyBilling: <ALIAS> fee";
					}
					else
					{
						description = "Reserve <ALIAS> fee";
					}
				}

				description = description.replace("<ALIAS>", product.getIndexKey());

				if (instance.getDebugMode().equals("depend"))
				{
					simulation(instance, provisioningCommand, result);
				}
				else if (amount > 0)
				{
					result = (VNMMessage) OSAConnection.charging(instance, result, description, true);
				}

				result.setPaid(result.getStatus() != Constants.ORDER_STATUS_DENIED);
			}
		}
		catch (Exception e)
		{
			processError(instance, provisioningCommand, result, e);
		}

		return result;
	}
	
	public VNMMessage cancelDebitWhenInvite(CommandInstance instance, ProvisioningCommand provisioningCommand, CommandMessage request)
			throws Exception
	{
		VNMMessage result = CommandUtil.createVNMMessage(request);
		
		result.setIsdn(CommandUtil.addCountryCode(request.getParameters().getString("INVITER_ISDN")));

		try
		{
			result.setRequest("cancelDebit");
			
			ProductEntry product = ProductFactory.getCache().getProduct(request.getProductId());
			
			String descKey = "osa.credit.description.invite";

			String description = product.getParameter(descKey, "Cancel Gift: <ALIAS>");

			if (result.isPaid())
			{
				creditAmount(instance, provisioningCommand, result, description);
			}
		}
		catch (Exception e)
		{
			processError(instance, provisioningCommand, request, e);
		}

		return result;
	}

	public VNMMessage directDebitWhenInvite(CommandInstance instance, ProvisioningCommand provisioningCommand, CommandMessage request)
			throws Exception
	{
		VNMMessage result = CommandUtil.createVNMMessage(request);

		if (instance.getDebugMode().equals("depend"))
		{
			simulation(instance, provisioningCommand, result);
		}
		else
		{
			String isdn = CommandUtil.addCountryCode(request.getParameters().getString("INVITER_ISDN"));
			result.setIsdn(isdn);
			
			try
			{
				CommandEntry command = ProvisioningFactory.getCache().getCommand(result.getCommandId());

				ProductEntry product = ProductFactory.getCache().getProduct(result.getProductId());

				command.setMaxRetry(1);

				int amount = CommandUtil.getAmount(result.getAmount());

				if (amount < 0)
				{
					throw new AppException("invalid-amount");
				}
				else if (amount == 0)
				{
					instance.debugMonitor("Amount = 0, Do not debit amount.");
					result.getParameters().setProperty("ByPassCommandLog", "true");
				}
				else
				{

					String descKey = "osa.debit.description." + result.getChannel() + "." + result.getActionType();

					String description = product.getParameter(descKey, "");
					if (description.equals(""))
					{
						description = product.getParameter("osa.debit.description", "");
					}
					if (description.equals(""))
					{
						if (result.getActionType().equals(Constants.ACTION_SUBSCRIPTION)
								|| (result.getActionType().equals(Constants.ACTION_TOPUP)
										&& result.getChannel().equals(Constants.CHANNEL_CORE)))
						{

							description = "Vas-MonthlyBilling: <ALIAS> fee";
						}
						else
						{
							description = "Reserve <ALIAS> fee";
						}
					}

					description = description.replace("<ALIAS>", product.getIndexKey());

					if (amount > 0)
					{
						result = (VNMMessage) OSAConnection.charging(instance, result, description, true);
					}

					result.setPaid(result.getStatus() != Constants.ORDER_STATUS_DENIED);
				}
			}
			catch (Exception e)
			{
				processError(instance, provisioningCommand, result, e);
			}
		}
		return result;
	}
}
