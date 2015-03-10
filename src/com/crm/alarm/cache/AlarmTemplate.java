package com.crm.alarm.cache;

import com.crm.kernel.index.IndexNode;

public class AlarmTemplate extends IndexNode
{
	private long	templateId	= 0;
	private String	subject		= "";
	private String	content		= "";

	public AlarmTemplate()
	{
		super();
	}

	public long getTemplateId()
	{
		return templateId;
	}

	public void setTemplateId(long templateId)
	{
		this.templateId = templateId;
	}

	public String getSubject()
	{
		return subject;
	}

	public void setSubject(String subject)
	{
		this.subject = subject;
	}

	public String getContent()
	{
		return content;
	}

	public void setContent(String content)
	{
		this.content = content;
	}
}
