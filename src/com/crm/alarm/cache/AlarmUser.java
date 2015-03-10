package com.crm.alarm.cache;

import com.crm.kernel.index.IndexNode;

public class AlarmUser extends IndexNode
{
	private long 	alarmUserId		= 0;
	private String	phone			= "";
	private String	email			= "";
	private String	jobTitle		= "";
	private String	fullname		= "";
	private int		status			= 0;
	private String	deliveryType	= "";
	private String	title			= "";
	
	public AlarmUser(long alarmUserId)
	{
		super();
		
		this.alarmUserId = alarmUserId;
	}
	
	public long getAlarmUserId()
	{
		return alarmUserId;
	}
	
	public void setAlarmUserId(long alarmUserId)
	{
		this.alarmUserId = alarmUserId;
	}
	
	public String getPhone()
	{
		return phone;
	}
	
	public void setPhone(String phone)
	{
		this.phone = phone;
	}
	
	public String getEmail()
	{
		return email;
	}
	
	public void setEmail(String email)
	{
		this.email = email;
	}
	
	public String getJobTitle()
	{
		return jobTitle;
	}
	
	public void setJobTitle(String jobTitle)
	{
		this.jobTitle = jobTitle;
	}
	
	public String getTitle()
	{
		return title;
	}
	
	public void setTitle(String title)
	{
		this.title = title;
	}
	
	public String getFullname()
	{
		return fullname;
	}
	
	public void setFullname(String fullname)
	{
		this.fullname = fullname;
	}
	
	public int getStatus()
	{
		return status;
	}
	
	public void setStatus(int status)
	{
		this.status = status;
	}
	
	public String getDeliveryType()
	{
		return deliveryType;
	}
	
	public void setDeliveryType(String deliveryType)
	{
		this.deliveryType = deliveryType;
	}
}
