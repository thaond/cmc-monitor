package com.crm.provisioning.thread;

import java.util.Calendar;

import org.apache.axis.AxisFault;

import com.comverse_in.prepaid.ccws.BalanceEntity;
import com.comverse_in.prepaid.ccws.SubscriberRetrieve;
import com.crm.kernel.message.Constants;
import com.crm.provisioning.impl.ccws.CCWSConnection;
import com.crm.provisioning.message.CommandMessage;
import com.crm.provisioning.util.CommandUtil;
import com.crm.util.StringUtil;

public class LowBalanceAlertInstance extends ProvisioningInstance
{
	public LowBalanceAlertThread getDispatcher()
	{
		return (LowBalanceAlertThread) super.getDispatcher();
	}

	public LowBalanceAlertInstance() throws Exception
	{
		super();
	}

	public int processMessage(Object message) throws Exception
	{
		if (message == null)
		{
			return Constants.BIND_ACTION_NONE;
		}

		if (message instanceof CommandMessage)
		{
			CommandMessage request = (CommandMessage) message;
			double dataLimitation = ((LowBalanceAlertThread) dispatcher).getDataLimitation(request.getProductId());
			String balanceName = ((LowBalanceAlertThread) dispatcher).getBalanceName(request.getProductId());
			String serviceAddress = ((LowBalanceAlertThread) dispatcher).getServiceAddress(request.getProductId());
			request.setServiceAddress(serviceAddress);

			CCWSConnection ccwsConnection = null;
			SubscriberRetrieve subscriberRetrieve = null;
			try
			{
				try
				{
					ccwsConnection = (CCWSConnection) getProvisioningConnection();
					subscriberRetrieve = ccwsConnection.getSubscriber(request.getIsdn(), 1);
				}
				catch (AxisFault error)
				{
					String errorCode = getErrorCode(error);
					if (!errorCode.equals(""))
					{
						logMonitor(request.getIsdn() + ": " + errorCode);
					}
					else
					{
						logMonitor(request.getIsdn() + ": " + error.getMessage());
					}
				}
				
				if (subscriberRetrieve != null)
				{
					BalanceEntity balance = CCWSConnection.getBalance(subscriberRetrieve.getSubscriberData(), balanceName);

					double dataAmount = balance.getAvailableBalance();
					if (balanceName.toUpperCase().equals("GPRS"))
					{
						dataAmount = dataAmount * 0.00000095367431640625;
					}

					String logCheckBalance = request.getIsdn() + ": Balance: " + balanceName + " Remain amount: " + dataAmount + " SubProductId: "
							+ request.getSubProductId();

					int status = request.getParameters().getInteger("SubscriberStatus", 1);
					if (dataAmount < dataLimitation)
					{
						status = Constants.SUBSCRIBER_ALERT_BALANCE_STATUS;
						// Send Sms alert
						CommandUtil.sendSMS(this, request, request.getServiceAddress(), request.getShipTo(),
								createContent(request.getIsdn(), StringUtil.format(dataAmount, "#,##0"), "", request.getProductId()));
					}

					int timePerData = ((LowBalanceAlertThread) dispatcher).getTimePerData(request.getProductId());
					Calendar scanTime = null;
					boolean byPassUpdate = false;
					if (timePerData > 0)
					{
						int nextTime = (int) (dataAmount / dataLimitation) * timePerData;
						scanTime = Calendar.getInstance();
						scanTime.add(Calendar.SECOND, nextTime);
						if (status == Constants.SUBSCRIBER_ALERT_BALANCE_STATUS)
						{
							getDispatcher().updateFlexi(request.getSubProductId(), scanTime, status);
						}
						else
						{
							getDispatcher().updateScheduleFlexi(request.getSubProductId(), scanTime);
						}
						byPassUpdate = true;
					}

					if (!byPassUpdate && status == Constants.SUBSCRIBER_ALERT_BALANCE_STATUS)
					{
						getDispatcher().updateFlexi(request.getSubProductId(), scanTime, status);
					}
					logMonitor(logCheckBalance);
				}
			}
			catch (Exception e)
			{
				logMonitor(e);

				getDispatcher().sendInstanceAlarm(e, Constants.ERROR);

				throw e;
			}
			finally
			{
				this.closeProvisioningConnection(ccwsConnection);
			}
		}

		return Constants.BIND_ACTION_SUCCESS;
	}

	public void doProcessSession() throws Exception
	{
		try
		{
			while (isAvailable())
			{
				Object request = detachMessage();

				if (request != null)
				{
					processMessage(request);
				}

				Thread.sleep(getDispatcher().restTime);
			}
		}
		catch (Exception e)
		{
			throw e;
		}
	}

	private String createContent(String isdn, String amount, String expirationDate, long productid)
	{
		String template = ((LowBalanceAlertThread) dispatcher).getSMSContent(productid);

		template = template.replaceAll("<isdn>", isdn);
		template = template.replaceAll("<amount>", String.valueOf(amount));

		return template;
	}
	
	public String getErrorCode(Exception error)
	{
		String errorCode = "";
		try
		{
			errorCode = error.getMessage();

			if (errorCode.startsWith("<ErrorCode>"))
			{
				errorCode = errorCode.substring("<ErrorCode>".length());
				errorCode = errorCode.substring(0,
						errorCode.indexOf("</ErrorCode>"));
			}
			else
			{
				errorCode = "";
			}
		}
		catch (Exception e)
		{
			errorCode = "CCWS unknown" + (error != null ? (": " + error.toString()) : "");
		}
		
		return errorCode;
	}
}
