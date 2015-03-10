package com.crm.thread;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import com.crm.alarm.cache.AlarmEntry;
import com.crm.alarm.cache.AlarmFactory;
import com.crm.kernel.message.AlarmMessage;
import com.crm.kernel.message.Constants;
import com.crm.kernel.queue.QueueFactory;
import com.crm.provisioning.cache.CommandEntry;
import com.crm.provisioning.cache.ProvisioningEntry;
import com.crm.provisioning.cache.ProvisioningFactory;
import com.crm.provisioning.message.CommandMessage;
import com.crm.provisioning.util.ResponseUtil;
import com.crm.thread.util.ThreadUtil;
import com.crm.util.StringPool;
import com.crm.util.StringUtil;
import com.fss.util.AppException;

public class AlarmThread extends MailThread
{
	protected ConcurrentHashMap<Long, ProvisioningAlarm>	chmProvisioningAlarm	= null;
	private long											startTime				= 0;
	private int												sendInterval			= 10;
	protected ConcurrentHashMap<Long, Date>					indexes					= new ConcurrentHashMap<Long, Date>();

	@Override
	@SuppressWarnings(value = { "unchecked", "rawtypes" })
	public Vector getDispatcherDefinition()
	{
		Vector vtReturn = new Vector();
		vtReturn.add(ThreadUtil.createIntegerParameter("sendInterval", "Send email after each interval in second."));
		vtReturn.addAll(super.getDispatcherDefinition());
		return vtReturn;
	}

	@Override
	public void fillParameter() throws AppException
	{
		super.fillParameter();
		setSendInterval(ThreadUtil.getInt(this, "sendInterval", 600));
	}

	@Override
	public void beforeProcessSession() throws Exception
	{
		super.beforeProcessSession();

		chmProvisioningAlarm = new ConcurrentHashMap<Long, AlarmThread.ProvisioningAlarm>();
		startTime = System.currentTimeMillis();
	}

	@Override
	public void afterProcessSession() throws Exception
	{
		try
		{
			sendProvisioningAlarm();
		}
		finally
		{
			chmProvisioningAlarm = null;
			startTime = 0;
		}
		super.afterProcessSession();
	}

	public AlarmMessage detachAlarm() throws Exception
	{
		long now = System.currentTimeMillis();
		
		if ((now - startTime) > (1000 * getSendInterval()))
		{
			sendProvisioningAlarm();
			
			chmProvisioningAlarm.clear();
			indexes.clear();
		}

		return (AlarmMessage) QueueFactory.detachLocal(QueueFactory.ALARM_QUEUE);
	}
	
	@Override
	public void doProcessSession() throws Exception
	{
		try
		{
			while (isAvailable())
			{
				AlarmMessage request = detachAlarm();
				while (isAvailable() && request != null)
				{
					try
					{
						processMessage(request);
					}
					catch (Exception e)
					{
						debugMonitor(e);
						throw e;
					}

					request = detachAlarm();
				}

				ThreadUtil.sleep(this);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw e;
		}
	}

	public void sendProvisioningAlarm() throws Exception
	{
		Set<Long> keys = chmProvisioningAlarm.keySet();

		Iterator<Long> iterator = keys.iterator();
		while (iterator.hasNext())
		{
			long alarmId = iterator.next();
			ProvisioningAlarm provisioningAlarm = chmProvisioningAlarm.get(alarmId);

			try
			{
				AlarmEntry alarmEntry = AlarmFactory.getCache().getAlarm(provisioningAlarm.getAlarmId());
				
				if (alarmEntry != null)
				{
					debugMonitor("Cause: " + provisioningAlarm.getAlarm().getCause());
					debugMonitor("waitDuration: " + alarmEntry.getWaitDuration());
					debugMonitor("Alarm total: " + provisioningAlarm.getCount());

					sendAlarmSMS(provisioningAlarm, alarmEntry);
					sendAlarmMail(provisioningAlarm, alarmEntry);
				}
			}
			catch (Exception e)
			{
				throw e;
			}
		}
		
		setStartTime(System.currentTimeMillis());
		
		storeConfig();
	}
	
	public void sendProvisioningAlarm(ProvisioningAlarm provisioningAlarm, AlarmEntry alarmEntry) throws Exception
	{
		try
		{
			if (alarmEntry != null)
			{
				debugMonitor("Cause: " + provisioningAlarm.getAlarm().getCause());
				debugMonitor("Alarm total: " + provisioningAlarm.getCount());

				sendAlarmSMS(provisioningAlarm, alarmEntry);
				sendAlarmMail(provisioningAlarm, alarmEntry);
			}
			
			chmProvisioningAlarm.remove(alarmEntry.getAlarmId());
			indexes.remove(alarmEntry.getAlarmId());
		}
		catch (Exception e)
		{
			throw e;
		}
	}

	public String formatContent(Object request)
	{
		if (request instanceof AlarmMessage)
		{
			AlarmMessage alarm = (AlarmMessage) request;
			String message = "Description:\r\n" + alarm.getDescription() + "\r\n"
					+ "Detail:\r\n" + alarm.getContent() + "\r\n";

			if (alarm.getProvisioningId() != 0)
			{
				try
				{
					ProvisioningEntry provisioningEntry = ProvisioningFactory.getCache().getProvisioning(
							alarm.getProvisioningId());
					if (provisioningEntry != null)
						message = "Provisioning: " + provisioningEntry.getAlias() + "-" + alarm.getProvisioningClass() + "\r\n"
								+ message;
				}
				catch (Exception e)
				{
				}
			}
			return message;
		}

		return request.toString();
	}

	public String formatSubject(Object request)
	{
		if (request instanceof AlarmMessage)
		{
			AlarmMessage alarm = (AlarmMessage) request;
			String subject = super.formatSubject(request) + " - " + alarm.getCause();

			if (alarm.getProvisioningId() != 0)
			{
				try
				{
					ProvisioningEntry provisioningEntry = ProvisioningFactory.getCache().getProvisioning(
							alarm.getProvisioningId());
					if (provisioningEntry != null)
						subject += " - " + provisioningEntry.getAlias() + "\r\n";
				}
				catch (Exception e)
				{
				}
			}

			return subject;
		}
		return super.formatSubject(request);
	}
	
	public void processMessage(Object request) throws Exception
	{
		if (request instanceof AlarmMessage)
		{
			AlarmMessage alarm = (AlarmMessage) request;

			AlarmEntry alarmEntry = AlarmFactory.getCache().getAlarm(alarm.getCause());

			if (alarmEntry != null)
			{
				alarm.setAlarmId(alarmEntry.getAlarmId());
				
				ProvisioningAlarm prAlarm = chmProvisioningAlarm.get(alarm.getAlarmId());
				
				if (prAlarm == null)
				{
					prAlarm = new ProvisioningAlarm();
					prAlarm.setStartTime(Calendar.getInstance());
					prAlarm.setAlarmId(alarmEntry.getAlarmId());
					prAlarm.setAlarm(alarm);
				}
				else
				{
					prAlarm.setCount(prAlarm.getCount() + 1);
				}

				if (alarm.isImmediately() || alarmEntry.getWaitDuration() == prAlarm.getCount())
				{
					long now = System.currentTimeMillis();
					Date lastALarmDate = indexes.get(alarmEntry.getAlarmId());
					if (lastALarmDate == null
							|| (lastALarmDate != null && (now - lastALarmDate.getTime()) > (1000 * getSendInterval())))
					{
						sendProvisioningAlarm(prAlarm, alarmEntry);
						indexes.put(alarmEntry.getAlarmId(), new Date());
					}
				}
				
				chmProvisioningAlarm.put(alarmEntry.getAlarmId(), prAlarm);
			}
		}
		else
		{
			super.processMessage(request);
		}
	}
	
	protected void sendAlarmMail(ProvisioningAlarm alarm, AlarmEntry alarmEntry) throws Exception
	{
		try
		{
			String sender = alarmEntry.getSenderEmail();
			String receiver = alarmEntry.getSendToEmail();
			
			String strContent = createContent(alarm, alarmEntry);
			
			if (!sender.equals("") && !strContent.equals("") && !receiver.equals(""))
			{
				debugMonitor("Sending alarm mail");
				
				try
				{
					initMailSession();
					
					sendEmail(alarmEntry.getSubject() + " - " + alarm.getAlarm().getCause(), alarmEntry.getSenderEmail(),
							alarmEntry.getSendToEmail(), strContent, null, StringPool.COMMA);
				}
				finally
				{
					detroyMailSessioin();
				}
				
				debugMonitor("Finish send alarm mail");
			}
		}
		catch (Exception e)
		{
			debugMonitor(e.getMessage());
			e.printStackTrace();
		}
	}

	protected void sendAlarmSMS(ProvisioningAlarm alarm, AlarmEntry alarmEntry) throws Exception
	{
		try
		{
			String sender = alarmEntry.getSenderPhone();
			if (!sender.equals(""))
			{
				String strContent = createContent(alarm, alarmEntry);
				
				String[] isdns = alarmEntry.getSendToPhone().split(StringPool.COMMA);
				String sentAlarmIsdn = "";
				
				CommandEntry command = ProvisioningFactory.getCache().getCommand(Constants.COMMAND_SEND_SMS);

				if (isdns.length > 0)
				{
					debugMonitor("Sending alarm SMS");
					
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
	
						request.setServiceAddress(alarmEntry.getSenderPhone());
						request.setShipTo(isdn);
						request.setIsdn(isdn);
						
						request.setRequest(strContent);
						request.setKeyword("ALARM");
						request.setProvisioningType(Constants.PROVISIONING_SMSC);
						request.setCommandId(command.getCommandId());
						request.setRequestValue(ResponseUtil.SMS_CMD_CHECK, "false");
						
						QueueFactory.attachCommandRouting(request);
					}
					
					if (!sentAlarmIsdn.equals(""))
						debugMonitor("Sent alarm SMS to: " + sentAlarmIsdn);
					
					debugMonitor("Finish send alarm SMS");
				}
			}
		}
		catch (Exception e)
		{
			debugMonitor(e.getMessage());
			e.printStackTrace();
		}
	}
	
	private String createContent(ProvisioningAlarm alarm,AlarmEntry alarmEntry)
	{
		Calendar now = Calendar.getInstance();
		
		String content = alarmEntry.getTemplate();
		content = content.replaceAll("~START_DATE~", StringUtil.format(alarm.getStartTime().getTime(), "dd/MM/yyyy HH:mm:ss"))
						.replaceAll("~END_DATE~", StringUtil.format(now.getTime(), "dd/MM/yyyy HH:mm:ss"))
						.replaceAll("~COUNT~", "" + alarm.count)
						.replaceAll("~CAUSE~", StringUtil.nvl(alarm.getAlarm().getCause(), ""))
						.replaceAll("~DESCRIPTION~", StringUtil.nvl(alarm.getAlarm().getDescription(), ""))
						.replaceAll("~DETAIL~", StringUtil.nvl(alarm.getAlarm().getContent(), ""));
		
		return content;
	}
	
//	private AlarmUser getAlarmUsers(long actionId, long senderId, String channel) throws Exception
//	{
//		PreparedStatement stmtSendTo = null;
//		ResultSet rsSendTo = null;
//		Connection connection = null;
//		
//		AlarmUser actionUser = null;
//		try
//		{
//			StringBuilder sql = new StringBuilder();
//			sql.append("SELECT A.ACTIONID, A.SENDERPHONE, A.SENDEREMAIL, B.SENDTOPHONE, B.SENDTOEMAIL");
//			sql.append("  FROM (SELECT ? ACTIONID, PHONE SENDERPHONE, EMAILADDRESS SENDEREMAIL");
//			sql.append("          FROM ALARM_USER WHERE ALARMUSERID = ?) A,");
//			sql.append("       (SELECT AU.ACTIONID, LISTAGG(U.PHONE, ',') WITHIN GROUP(ORDER BY AU.MODIFIEDDATE) SENDTOPHONE,");
//			sql.append("               LISTAGG(U.EMAILADDRESS, ',') WITHIN GROUP(ORDER BY AU.MODIFIEDDATE) SENDTOEMAIL");
//			sql.append("          FROM ALARM_ACTION_USER AU");
//			sql.append("    INNER JOIN ALARM_USER U ON  AU.ALARMUSERID = U.ALARMUSERID");
//			sql.append("         WHERE AU.ACTIONID = ? AND AU.TYPE IN (?, 'both') AND U.STATUS = 1");
//			sql.append("      GROUP BY AU.ACTIONID) B");
//			sql.append("         WHERE A.ACTIONID = B.ACTIONID");
//
//			connection = Database.getConnection();
//			stmtSendTo = connection.prepareStatement(sql.toString());
//			stmtSendTo.setLong(1, actionId);
//			stmtSendTo.setLong(2, senderId);
//			stmtSendTo.setLong(3, actionId);
//			stmtSendTo.setString(4, channel);
//
//			rsSendTo = stmtSendTo.executeQuery();
//
//			if (rsSendTo.next())
//			{
//				actionUser = new AlarmUser();
//				
//				actionUser.setActionId(rsSendTo.getLong("ACTIONID"));
//				actionUser.setSenderPhone(rsSendTo.getString("SENDERPHONE"));
//				actionUser.setSenderEmail(rsSendTo.getString("SENDEREMAIL"));
//				actionUser.setSendToPhone(rsSendTo.getString("SENDTOPHONE"));
//				actionUser.setSendToEmail(rsSendTo.getString("SENDTOEMAIL"));
//			}
//		}
//		catch (Exception e)
//		{
//			throw e;
//		}
//		finally
//		{
//			Database.closeObject(rsSendTo);
//			Database.closeObject(stmtSendTo);
//			Database.closeObject(connection);
//		}
//
//		return actionUser;
//	}

	protected class ProvisioningAlarm
	{
		private long			alarmId			= 0;
		private AlarmMessage	alarm			= null;
		private Calendar		startTime		= Calendar.getInstance();
		private int				count			= 1;

		public long getAlarmId()
		{
			return alarmId;
		}

		public void setAlarmId(long alarmId)
		{
			this.alarmId = alarmId;
		}
		
		public AlarmMessage getAlarm()
		{
			return alarm;
		}

		public void setAlarm(AlarmMessage alarm)
		{
			this.alarm = alarm;
		}

		public Calendar getStartTime()
		{
			return startTime;
		}

		public void setStartTime(Calendar startTime)
		{
			this.startTime = startTime;
		}

		public void setCount(int count)
		{
			this.count = count;
		}

		public int getCount()
		{
			return count;
		}
	}
	
	public void setSendInterval(int sendInterval)
	{
		this.sendInterval = sendInterval;
	}

	public int getSendInterval()
	{
		return sendInterval;
	}
	
	public long getStartTime()
	{
		return startTime;
	}

	public void setStartTime(long startTime)
	{
		this.startTime = startTime;
	}
}
