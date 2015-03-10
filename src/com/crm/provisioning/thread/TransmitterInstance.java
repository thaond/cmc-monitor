package com.crm.provisioning.thread;

import java.net.SocketException;
import com.crm.kernel.message.Constants;
import com.crm.provisioning.impl.smpp.SMPPConnection;
import com.crm.provisioning.message.CommandMessage;
import com.crm.provisioning.util.ResponseUtil;

public class TransmitterInstance extends CommandInstance
{
	public TransmitterInstance() throws Exception
	{
		super(); 
	}

	public int processMessage(CommandMessage message) throws Exception
	{
		boolean checkCommand = false;

		//CommandMessage sms = (CommandMessage) QueueFactory.getContentMessage(message);
		
		String strCmdCheck = message.getRequestValue(ResponseUtil.SMS_CMD_CHECK, "true");
		
		if (strCmdCheck.toUpperCase().equals("TRUE"))
		{
			checkCommand = true;
		}

		if (checkCommand)
		{
			return super.processMessage(message);
		}
		else
		{
			SMPPConnection smppConnection = null;
			try
			{
				// debugMonitor(message.toString());

				smppConnection = (SMPPConnection) getProvisioningConnection();

				try
				{
					message.setProvisioningType("SMSC");

					smppConnection.submit(message);
					// debugMonitor(sms);
				}
				catch (Exception e)
				{
					if (e instanceof SocketException)
					{
						getDispatcher().sendInstanceAlarm(e, "SMSC-Exception", "");
					}
					message.setStatus(Constants.ORDER_STATUS_DENIED);

					throw e;
				}
				finally
				{
					closeProvisioningConnection(smppConnection);
				}
			}
			catch (Exception e)
			{
				throw e;
			}

			return Constants.BIND_ACTION_SUCCESS;
		}
	}
}
