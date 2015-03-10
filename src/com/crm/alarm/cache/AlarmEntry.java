package com.crm.alarm.cache;

import com.crm.kernel.index.IndexNode;

public class AlarmEntry extends IndexNode
{
	private String	alias					= "";
	private long	alarmId					= 0;
	private String	title					= "";
	private int		waitDuration			= 0;
	private String	subject					= "";
	private String	senderPhone				= "";
	private String	senderEmail				= "";
	private String	sendToPhone				= "";
	private String	sendToEmail				= "";
	private String	template				= "";
	
	public AlarmEntry(long alarmId, String alias)
	{
		super(alias);

		setAlias(alias);
		setAlarmId(alarmId);
	}
	
	public String getAlias()
	{
		return alias;
	}
	
	public void setAlias(String alias)
	{
		this.alias = alias;
	}
	
	public long getAlarmId()
	{
		return alarmId;
	}

	public void setAlarmId(long alarmId)
	{
		this.alarmId = alarmId;
	}
	
	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}
	
	public int getWaitDuration()
	{
		return waitDuration;
	}
	
	public void setWaitDuration(int waitDuration)
	{
		this.waitDuration = waitDuration;
	}
	
	public String getSenderPhone()
	{
		return senderPhone;
	}

	public void setSenderPhone(String senderPhone)
	{
		this.senderPhone = senderPhone;
	}
	
	public String getSenderEmail()
	{
		return senderEmail;
	}

	public void setSenderEmail(String senderEmail)
	{
		this.senderEmail = senderEmail;
	}
	
	public String getSendToPhone()
	{
		return sendToPhone;
	}
	
	public void setSendToPhone(String sendToPhone)
	{
		this.sendToPhone = sendToPhone;
	}
	
	public String getSendToEmail()
	{
		return sendToEmail;
	}
	
	public void setSendToEmail(String sendToEmail)
	{
		this.sendToEmail = sendToEmail;
	}
	public String getSubject()
	{
		return subject;
	}
	
	public void setSubject(String subject)
	{
		this.subject = subject;
	}
	
	public String getTemplate()
	{
		return template;
	}
	
	public void setTemplate(String template)
	{
		this.template = template;
	}
}
