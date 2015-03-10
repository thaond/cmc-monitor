package com.crm.alarm.cache;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;

import com.crm.kernel.index.BinaryIndex;
import com.crm.kernel.index.IndexNode;
import com.crm.kernel.message.Constants;
import com.crm.kernel.sql.Database;
import com.crm.util.StringPool;

public class AlarmCache
{
	// cache object
	private BinaryIndex	alarms		= new BinaryIndex();
	private BinaryIndex	templates	= new BinaryIndex();
	private BinaryIndex users		= new BinaryIndex();

	private Date		cacheDate	= null;

	public void clear()
	{
		alarms.clear();
	}

	public synchronized void loadCache() throws Exception
	{
		Connection connection = null;

		try
		{
			connection = Database.getConnection();

			loadAlarmUser(connection);
			loadTemplate(connection);
			loadAlarm(connection);
			
			setCacheDate(new Date());
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			Database.closeObject(connection);
		}
	}
	
	protected void loadAlarmUser(Connection connection) throws Exception
	{
		PreparedStatement stmtConfig = null;
		ResultSet rsConfig = null;

		try
		{
			String sql = "Select * From AlarmUser Where status = 1";

			stmtConfig = connection.prepareStatement(sql);
			rsConfig = stmtConfig.executeQuery();

			while (rsConfig.next())
			{
				AlarmUser user = new AlarmUser(rsConfig.getLong("alarmUserId"));

				user.setTitle(Database.getString(rsConfig, "Title"));
				user.setFullname(Database.getString(rsConfig, "Fullname"));
				user.setJobTitle(Database.getString(rsConfig, "Status"));
				user.setPhone(Database.getString(rsConfig, "Phone"));
				user.setEmail(Database.getString(rsConfig, "EmailAddress"));
				user.setStatus(rsConfig.getInt("Status"));
				user.setDeliveryType(rsConfig.getString("DeliveryType"));

				users.add(user.getAlarmUserId(), user.getIndexKey(), user);
			}
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			Database.closeObject(rsConfig);
			Database.closeObject(stmtConfig);
		}
	}
	
	protected void loadAlarm(Connection connection) throws Exception
	{
		PreparedStatement stmtConfig = null;
		ResultSet rsConfig = null;

		try
		{
//			String sql = "Select * From AlarmEntry Order by alias_ desc";
			StringBuilder sql = new StringBuilder();
			
			sql.append("SELECT	a.alarmId, a.alias_, a.title, a.waitduration, a.templateid, a.subject, a.senderid, a.SendToPhone, b.SendToEmail ");
			sql.append("FROM	(");
			sql.append("		SELECT	a.alarmId, a.alias_, a.title, a.waitduration, a.templateid, a.subject, a.senderid, b.SendToPhone ");
			sql.append("		FROM (");
			sql.append("			SELECT	alarmId, alias_, title, waitduration, templateid, subject, senderid ");
			sql.append("			FROM	alarmentry ) a, ");
			sql.append("		( SELECT	alarmid, LISTAGG(alarmuserid, ',') WITHIN GROUP(ORDER BY alarmuserid) SendToPhone ");
			sql.append("			FROM	alarmactionuser ");
			sql.append("		   WHERE	deliveryType in ('SMS','BOTH') ");
			sql.append("		GROUP BY	alarmid ) b ");
			sql.append("		WHERE	a.alarmId = b.alarmId(+) ");
			sql.append("		) a,");
			sql.append("		( SELECT	alarmid, LISTAGG(alarmuserid, ',') WITHIN GROUP(ORDER BY alarmuserid) SendToEmail ");
			sql.append("			FROM	alarmactionuser ");
			sql.append("		   WHERE	deliveryType in ('EMAIL','BOTH') ");
			sql.append("		GROUP BY	alarmid ) b ");
			sql.append("WHERE	a.alarmId = b.alarmId(+) ");

			stmtConfig = connection.prepareStatement(sql.toString());
			rsConfig = stmtConfig.executeQuery();

			while (rsConfig.next())
			{
				AlarmEntry alarm = new AlarmEntry(rsConfig.getLong("alarmId"), Database.getString(rsConfig, "alias_"));

				alarm.setTitle(Database.getString(rsConfig, "title"));
				alarm.setWaitDuration(rsConfig.getInt("waitDuration"));
				
				AlarmTemplate template = (AlarmTemplate) templates.getById(rsConfig.getLong("templateId"));
				if (template !=null )
				{
					alarm.setTemplate(template.getContent());
				}
				alarm.setSubject(Database.getString(rsConfig, "Subject"));
				
				AlarmUser sender = (AlarmUser) users.getById(Long.valueOf(Database.getString(rsConfig, "SenderID", "0")));
				if (sender != null)
				{
					alarm.setSenderPhone(sender.getPhone());
					alarm.setSenderEmail(sender.getEmail());
				}
				
				String[] phoneList = Database.getString(rsConfig, "SendToPhone").split(StringPool.COMMA);
				String[] mailList = Database.getString(rsConfig, "SendToEmail").split(StringPool.COMMA);
				String sendToPhone = "";
				String sendToEmail = "";
				for(String user : phoneList)
				{
					if (!user.equals(""))
					{
						AlarmUser sendToUser = (AlarmUser) users.getById(Long.valueOf(user));
						if (sendToUser != null)
						{
							sendToPhone = sendToPhone + sendToUser.getPhone() + ",";
						}
					}
				}
				
				for(String user : mailList)
				{
					if (!user.equals(""))
					{
						AlarmUser sendToUser = (AlarmUser) users.getById(Long.valueOf(user));
						if (sendToUser != null)
						{
							sendToEmail = sendToEmail + sendToUser.getEmail() + ",";
						}
					}
				}
				alarm.setSendToPhone(sendToPhone);
				alarm.setSendToEmail(sendToEmail);

				alarms.add(alarm.getAlarmId(), alarm.getIndexKey(), alarm);
			}
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			Database.closeObject(rsConfig);
			Database.closeObject(stmtConfig);
		}
	}

//	protected void loadActions(Connection connection) throws Exception
//	{
//		PreparedStatement stmtConfig = null;
//		ResultSet rsConfig = null;
//
//		try
//		{
//			String SQL = "Select * From AlarmAction Order by alarmId desc, type_ desc";
//
//			stmtConfig = connection.prepareStatement(SQL);
//			rsConfig = stmtConfig.executeQuery();
//
//			while (rsConfig.next())
//			{
//				AlarmAction action = new AlarmAction();
//
//				action.setAlarmId(rsConfig.getLong("alarmId"));
//				action.setTemplateId(rsConfig.getLong("templateId"));
//				action.setDescription(Database.getString(rsConfig,"description"));
//				action.setType(rsConfig.getInt("type_"));
//				action.setSender(rsConfig.getString("sender"));
//				action.setSendto(rsConfig.getString("sendto"));
//
//				AlarmEntry alarm = getAlarm(rsConfig.getLong("alarmId"));
//
//				alarm.getActions().add(action);
//			}
//		}
//		catch (Exception e)
//		{
//			throw e;
//		}
//		finally
//		{
//			Database.closeObject(rsConfig);
//			Database.closeObject(stmtConfig);
//		}
//	}
	
	protected void loadTemplate(Connection connection) throws Exception
	{
		PreparedStatement stmtConfig = null;
		ResultSet rsConfig = null;

		try
		{
			String sql = "Select * From ALARMTEMPLATE Order by templateId desc";

			stmtConfig = connection.prepareStatement(sql);
			rsConfig = stmtConfig.executeQuery();

			while (rsConfig.next())
			{
				AlarmTemplate template = new AlarmTemplate();

				template.setTemplateId(rsConfig.getLong("templateId"));
				template.setContent(rsConfig.getString("content"));

				templates.add(template.getTemplateId(), template.getIndexKey(), template);
			}
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			Database.closeObject(rsConfig);
			Database.closeObject(stmtConfig);
		}
	}
	
	public void setCacheDate(Date cacheDate)
	{
		this.cacheDate = cacheDate;
	}

	public Date getCacheDate()
	{
		return cacheDate;
	}

	public BinaryIndex getCampaigns()
	{
		return alarms;
	}

	public void setCampaigns(BinaryIndex alarmRule)
	{
		this.alarms = alarmRule;
	}

	public AlarmEntry getAlarm(String alias) throws Exception
	{
		return (AlarmEntry) alarms.getByKey(alias.toUpperCase());
	}
	
	public AlarmEntry getAlarm(long alarmId) throws Exception
	{
		if (alarmId == Constants.DEFAULT_ID)
		{
			return null;
		}

		IndexNode node = alarms.getById(alarmId);
		
		return (AlarmEntry) node;
	}

	public AlarmTemplate getTemplate(long templateId) throws Exception
	{
		if (templateId == Constants.DEFAULT_ID)
		{
			return null;
		}
		
		IndexNode result = templates.getById(templateId);

		return (AlarmTemplate) result;
	}
}
