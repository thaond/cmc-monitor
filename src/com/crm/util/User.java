package com.crm.util;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class User
{
	private String		username	= "";
	private String		password	= "";
	public List<String>	Permissions	= new ArrayList<String>();

	public User()
	{

	}

	public User(String username, String password)
	{
		this.username = username;
		this.password = password;
	}

	/**
	 * I take a xml element and the tag name, look for the tag and get the text
	 * content i.e for <employee><name>John</name></employee> xml snippet if the
	 * Element points to employee node and tagName is 'name' I will return John
	 */
	private String getTextValue(Element ele, String tagName)
	{
		String textVal = null;
		NodeList nl = ele.getElementsByTagName(tagName);
		if (nl != null && nl.getLength() > 0)
		{
			Element el = (Element) nl.item(0);
			textVal = el.getFirstChild().getNodeValue();
		}

		return textVal;
	}

	/**
	 * Calls getTextValue and returns a int value
	 */
	@SuppressWarnings("unused")
	private int getIntValue(Element ele, String tagName)
	{
		// in production application you would catch the exception
		return Integer.parseInt(getTextValue(ele, tagName));
	}

	public void setData(Element elementData)
	{
		this.setUserName(this.getTextValue(elementData, "username"));
		this.setPassword(this.getTextValue(elementData, "password"));
		NodeList nl = elementData.getElementsByTagName("permissions");
		if (nl != null && nl.getLength() > 0)
		{
			Element el = (Element) nl.item(0);
			NodeList permisionList = el.getElementsByTagName("permission");
			if (permisionList != null && permisionList.getLength() > 0)
			{
				for (int i = 0; i < permisionList.getLength(); i++)
				{
					Node child = permisionList.item(i);
					Element childElement = (Element) child;
					this.add_permission(childElement.getFirstChild().getNodeValue());
				}
			}
		}
	}

	public void setUserName(String username)
	{
		this.username = username;
	}

	public String getUserName()
	{
		return this.username;
	}

	public void setPassword(String password)
	{
		this.password = password;
	}

	public String getPassword()
	{
		return this.password;
	}

	public void add_permission(String PermissionKey)
	{
		this.Permissions.add(PermissionKey);
	}

	public boolean hasPermission(String permissionKey)
	{

		for (int i = 0; i < this.Permissions.size(); i++)
		{
			String permission = (String) this.Permissions.get(i);

			if (permission != null)
			{
				if (permission.equalsIgnoreCase(permissionKey))
				{
					return true;
				}
			}
		}

		return false;
	}

}
