package com.crm.thread;

import java.io.IOException;
import java.util.Vector;

import com.crm.kernel.message.Constants;
import com.crm.kernel.queue.QueueFactory;
import com.crm.provisioning.cache.CommandEntry;
import com.crm.provisioning.cache.ProvisioningFactory;
import com.crm.provisioning.impl.ema.TelnetWrapper;
import com.crm.provisioning.message.CommandMessage;
import com.crm.provisioning.util.ResponseUtil;
import com.crm.thread.util.ThreadUtil;
import com.crm.util.StringPool;
import com.fss.thread.ParameterType;
import com.fss.util.AppException;

public class MonitorServiceThread extends MailThread
{
	protected TelnetWrapper	telnet			= null;

	protected Vector		vtService		= new Vector();
	
	protected boolean		alertViaSMS		= false;
	
	protected long			enquireInterval	= 900000;
	
	private long			lastCheck		= System.currentTimeMillis();
	
	private String			formatContent	= "";

	@Override
	public Vector getParameterDefinition()
	{
		Vector vtReturn = new Vector();

		Vector vtValue = new Vector();
		vtValue.addElement(createParameterDefinition("System", "",
				ParameterType.PARAM_TEXTBOX_MAX, "",
				"Service name", "0"));

		vtValue.addElement(createParameterDefinition("Host", "",
				ParameterType.PARAM_TEXTBOX_MAX, "",
				"Service address", "1"));

		vtValue.addElement(createParameterDefinition("Port", "",
				ParameterType.PARAM_TEXTBOX_MAX, "",
				"Service port", "2"));
		
		vtValue.addElement(createParameterDefinition("SenderIsdnList", "",
				ParameterType.PARAM_TEXTBOX_MAX, "",
				"Sms sender", "3"));
		
		vtValue.addElement(createParameterDefinition("SenderMailList", "",
				ParameterType.PARAM_TEXTBOX_MAX, "",
				"Mail sender", "4"));
		
		vtValue.addElement(createParameterDefinition("ReceiveIsdnList", "",
				ParameterType.PARAM_TEXTBOX_MAX, "",
				"Receiver sms list", "5"));
		
		vtValue.addElement(createParameterDefinition("ReceiveMailList", "",
				ParameterType.PARAM_TEXTBOX_MAX, "",
				"Receiver mail list", "6"));
		
		vtValue.addElement(createParameterDefinition("Subject", "",
				ParameterType.PARAM_TEXTBOX_MAX, "",
				"Mail Subject", "7"));
		
		vtReturn.addElement(createParameterDefinition("ServiceInfo", "",
				ParameterType.PARAM_TABLE, vtValue, "Service information"));
		
		vtReturn.addElement(ThreadUtil.createBooleanParameter("AlertViaSMS", "Alert via SMS channel"));
		vtReturn.addElement(ThreadUtil.createLongParameter("EnquireInterval", ""));
		vtReturn.addElement(ThreadUtil.createTextParameter("FormatContent", 4000, "Format content mail alert"));
		
		vtReturn.addAll(super.getParameterDefinition());

		return vtReturn;
	}

	@Override
	public void fillParameter() throws AppException
	{
		try
		{
			super.fillParameter();
			vtService = new Vector();
			Object obj = getParameter("ServiceInfo");
			if (obj != null && (obj instanceof Vector))
			{
				vtService = (Vector) ((Vector) obj).clone();
			}
			alertViaSMS = ThreadUtil.getBoolean(this, "AlertViaSMS", false);
			enquireInterval = ThreadUtil.getLong(this, "EnquireInterval", 600000);
			formatContent = ThreadUtil.getString(this, "FormatContent", true, "");
		}
		catch (AppException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void doProcessSession() throws Exception
	{
		try
		{
			long now = System.currentTimeMillis();
			if (now - lastCheck > enquireInterval)
			{
				for (int i = 0; i < vtService.size(); i++)
				{
					Vector vt = (Vector) vtService.elementAt(i);
					
					String serviceName = vt.elementAt(0).toString();
					String host = vt.elementAt(1).toString();
					int port = Integer.valueOf(vt.elementAt(2).toString());
					String smsSender = vt.elementAt(3).toString();
					String mailSender = vt.elementAt(4).toString();
					String smsReceiver = vt.elementAt(5).toString();
					String mailReceiver = vt.elementAt(6).toString();
					String subject = vt.elementAt(7).toString();
					
					boolean isOpen = false;
					try
					{
						telnet = new TelnetWrapper(host, port);
	
						isOpen = true;
						
						debugMonitor("Telnet to service " + serviceName + "@" + host + ":" + port + " success.");
					}
					catch (IOException e)
					{
						String content = formatContent;
						content = content.replaceAll("<%SERVICE%>", serviceName)
										.replaceAll("<%HOST%>", host)
										.replaceAll("<%PORT%>", String.valueOf(port))
										.replaceAll("<%EXCEPTION%>", e.getMessage());
						debugMonitor(content);
						
						sendAlarmMail(mailSender, mailReceiver, content, subject);
						
						if (alertViaSMS)
						{
							sendAlarmSMS(smsSender, smsReceiver, content);
						}
					}
					finally
					{
						try
						{
							if (isOpen)
							{
								telnet.disconnect();
			
								isOpen = false;
							}
						}
						catch (Exception e)
						{
							debugMonitor(e);
						}
			
						telnet = null;
					}
				}
				
				lastCheck = System.currentTimeMillis();
			}
		}
		catch (Exception e)
		{
			logMonitor(e.getMessage());
		}
	}
	
	protected void sendAlarmSMS(String sender, String receiver, String content) throws Exception
	{
		try
		{
			String[] isdns = receiver.split(StringPool.COMMA);
			String sentAlarmIsdn = "";
			
			CommandEntry command = ProvisioningFactory.getCache().getCommand(Constants.COMMAND_SEND_SMS);

			if (isdns.length > 0)
			{
				for (String isdn : isdns)
				{
					if (isdn.equals(""))
					{
						continue;
					}
					
					sentAlarmIsdn += isdn + ",";
					
					CommandMessage request = new CommandMessage();
					request.setChannel(Constants.CHANNEL_SMS);
					request.setUserId(0);
					request.setUserName("system");

					request.setServiceAddress(sender);
					request.setIsdn(isdn);
					
					request.setRequest(content);
					request.setKeyword("ALARM");
					request.setProvisioningType(Constants.PROVISIONING_SMSC);
					request.setCommandId(command.getCommandId());
					request.setRequestValue(ResponseUtil.SMS_CMD_CHECK, "false");
					
					QueueFactory.attachCommandRouting(request);
				}
				
				if (!sentAlarmIsdn.equals(""))
					debugMonitor("Sent alarm SMS to: " + sentAlarmIsdn);
			}
		}
		catch (Exception e)
		{
			debugMonitor(e.getMessage());
			e.printStackTrace();
		}
	}
	
	protected void sendAlarmMail(String sender, String receiver, String content, String subject) throws Exception
	{
		try
		{
			if (!sender.equals("") && !content.equals("") && !receiver.equals(""))
			{
				sendEmail(subject, sender,
							receiver, content, null, StringPool.COMMA);
			}
		}
		catch (Exception e)
		{
			debugMonitor(e.getMessage());
			e.printStackTrace();
		}
	}
}
