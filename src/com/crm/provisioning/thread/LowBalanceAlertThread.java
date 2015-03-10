package com.crm.provisioning.thread;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import com.crm.kernel.message.Constants;
import com.crm.kernel.queue.QueueFactory;
import com.crm.kernel.sql.Database;
import com.crm.provisioning.message.CommandMessage;
import com.crm.thread.util.ThreadUtil;
import com.crm.util.DateUtil;
import com.crm.util.StringUtil;
import com.fss.thread.ParameterType;
import com.fss.util.AppException;

public class LowBalanceAlertThread extends ProvisioningThread
{
	@SuppressWarnings("rawtypes")
	protected Vector			vtAlert						= new Vector();

	protected Connection		connection					= null;
	protected PreparedStatement	stmtQueue					= null;
	protected PreparedStatement	stmtScheduleFlexi			= null;
	protected PreparedStatement	stmtFlexi					= null;

	protected ResultSet			rsQueue						= null;

	protected String			sqlCommand					= "";
	protected int				restTime					= 5;
	protected int				batchCounter				= 0;

	// //////////////////////////////////////////////////////
	// Override
	// //////////////////////////////////////////////////////
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Vector getParameterDefinition()
	{
		Vector vtReturn = new Vector();

		Vector vtValue = new Vector();
		vtValue.addElement(createParameterDefinition("ProductId", "",
				ParameterType.PARAM_TEXTBOX_MAX, "",
				"Context to mapping class", "0"));

		vtValue.addElement(createParameterDefinition("Limitation", "",
				ParameterType.PARAM_TEXTBOX_MAX, "",
				"Context to mapping class", "1"));

		vtValue.addElement(createParameterDefinition("Balance", "",
				ParameterType.PARAM_TEXTBOX_MAX, "",
				"Context to mapping class", "2"));

		vtValue.addElement(createParameterDefinition("ServiceAddress", "",
				ParameterType.PARAM_TEXTBOX_MAX, "",
				"Context to mapping class", "3"));

		vtValue.addElement(createParameterDefinition("TimePerData", "",
				ParameterType.PARAM_TEXTBOX_MAX, "",
				"Context to mapping class", "4"));

		vtValue.addElement(createParameterDefinition("SMSContent", "",
				ParameterType.PARAM_TEXTBOX_MAX, "",
				"Context to mapping class", "5"));

		vtReturn.addElement(createParameterDefinition("AlertConfig", "",
				ParameterType.PARAM_TABLE, vtValue, "Alert Config"));

		vtReturn.addElement(createParameterDefinition("SQLCommand", "",
				ParameterType.PARAM_TEXTBOX_MAX, "100"));
		vtReturn.addElement(createParameterDefinition("RestTime", "",
				ParameterType.PARAM_TEXTBOX_MAX, "100"));

		vtReturn.addAll(super.getParameterDefinition());

		return vtReturn;
	}

	// //////////////////////////////////////////////////////
	// Override
	// //////////////////////////////////////////////////////
	@SuppressWarnings("rawtypes")
	public void fillParameter() throws AppException
	{
		try
		{
			super.fillParameter();
			vtAlert = new Vector();
			Object obj = getParameter("AlertConfig");
			if (obj != null && (obj instanceof Vector))
			{
				vtAlert = (Vector) ((Vector) obj).clone();
			}

			sqlCommand = ThreadUtil.getString(this, "SQLCommand", true, "");
			restTime = ThreadUtil.getInt(this, "RestTime", 5);
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

	public void beforeProcessSession() throws Exception
	{
		super.beforeProcessSession();

		try
		{
			QueueFactory.getLocalQueue(queueLocalName).empty();

			QueueFactory.getLocalQueue(queueLocalName).setCheckPending(false);
		}
		catch (Exception e)
		{
			throw e;
		}
	}

	public void afterProcessSession() throws Exception
	{
		try
		{
			if (connection != null && !connection.isClosed())
			{
				updateToDB();
				Database.closeObject(stmtQueue);
				Database.closeObject(stmtFlexi);
				Database.closeObject(stmtScheduleFlexi);
				Database.closeObject(connection);
			}
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			QueueFactory.getLocalQueue(queueLocalName).empty();
			super.afterProcessSession();
		}
	}

	public void loadDataBase() throws Exception
	{
		try
		{
			connection = Database.getConnection();

			String strSQL = sqlCommand;
			stmtQueue = connection.prepareStatement(strSQL);

			strSQL = "UPDATE SubscriberProduct SET  scheduletime = ? WHERE subproductid = ? ";
			stmtScheduleFlexi = connection.prepareStatement(strSQL);

			strSQL = "UPDATE SubscriberProduct SET  scheduletime = ?, status = ? WHERE subproductid = ? ";
			stmtFlexi = connection.prepareStatement(strSQL);
		}
		catch (Exception e)
		{
			throw e;
		}
	}

	public synchronized void updateFlexi(long id, Calendar scanTime, int status)
	{
		try
		{
			stmtFlexi.setTimestamp(
					1,
					(scanTime != null ? DateUtil.getTimestampSQL(scanTime
							.getTime()) : null));
			stmtFlexi.setInt(2, status);
			stmtFlexi.setLong(3, id);
			stmtFlexi.addBatch();
			
			addBatchCount();
			if (getBatchCount() >= 50)
			{
				setBatchCount(0);
				updateToDB();
			}
		}
		catch (Exception e)
		{
			debugMonitor(e);
		}
	}

	public synchronized void updateScheduleFlexi(long id, Calendar scanTime)
	{
		try
		{
			stmtScheduleFlexi.setTimestamp(
					1,
					(scanTime != null ? DateUtil.getTimestampSQL(scanTime
							.getTime()) : null));
			stmtScheduleFlexi.setLong(2, id);
			stmtScheduleFlexi.addBatch();
			
			addBatchCount();
			if (getBatchCount() >= 50)
			{
				setBatchCount(0);
				updateToDB();
			}
		}
		catch (Exception e)
		{
			debugMonitor(e);
		}
	}

	public void updateToDB() throws Exception
	{
		if (stmtScheduleFlexi != null)
		{
			stmtScheduleFlexi.executeBatch();
		}

		if (stmtFlexi != null)
		{
			stmtFlexi.executeBatch();
		}
		
		connection.commit();
	}

	public void closeDatabase() throws Exception
	{
		try
		{
			try
			{
				updateToDB();
			}
			finally
			{
				Database.closeObject(rsQueue);
				Database.closeObject(stmtQueue);
				Database.closeObject(stmtFlexi);
				Database.closeObject(stmtScheduleFlexi);
				Database.closeObject(connection);
			}
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			stmtQueue = null;
			stmtFlexi = null;
			stmtScheduleFlexi = null;
			connection = null;
			QueueFactory.getLocalQueue(queueLocalName).empty();
		}
	}
	
	public synchronized void addBatchCount()
	{
		batchCount++;
	}
	
	public synchronized void setBatchCount(int count)
	{
		batchCount = count;
	}

	public synchronized int getBatchCount()
	{
		return batchCount;
	}
	
	public boolean isOverload()
	{
		if (QueueFactory.getLocalQueue(queueName).isOverload())
		{
			return true;
		}
		else if (QueueFactory.getLocalQueue(queueLocalName).isOverload())
		{
			return true;
		}

		return false;
	}

	public void doProcessSession() throws Exception
	{
		try
		{
			boolean EOF = false;
			
			loadDataBase();
			
			rsQueue = stmtQueue.executeQuery();

			while (isAvailable() && !EOF)
			{
				checkInstance();
				
				if (!isOverload())
				{
					if (rsQueue.next())
					{
						CommandMessage request = new CommandMessage();
		
						request.setRequestTime(new Date());
						request.setUserName("system");
						request.setChannel("SMS");
						request.setSubProductId(rsQueue.getLong("subproductid"));
						request.setProductId(rsQueue.getLong("productid"));
						request.setServiceAddress("LBA");
						request.setIsdn(rsQueue.getString("isdn"));
						request.setKeyword("LowBalanceAlert");
						request.getParameters().setInteger("SubscriberStatus", rsQueue.getInt("status"));
		
						QueueFactory.attachLocal(queueLocalName, request);
					}
					else
					{
						EOF = true;
						Thread.sleep(3000);
					}
				}
			}
			
			closeDatabase();
		}
		catch (Exception e)
		{
			logMonitor(e);

			sendInstanceAlarm(e, Constants.ERROR);
			
			throw e;
		}
	}

	@SuppressWarnings("rawtypes")
	public String getSMSContent(long productid)
	{
		String content = "";
		for (int i = 0; i < vtAlert.size(); i++)
		{
			Vector vt = (Vector) vtAlert.elementAt(i);

			if (Long.parseLong(vt.elementAt(0).toString()) == productid)
			{
				content = vt.elementAt(5).toString();
				break;
			}
		}
		return content;
	}

	@SuppressWarnings("rawtypes")
	public int getDataLimitation(long productid)
	{
		int dataLimitation = 0;
		for (int i = 0; i < vtAlert.size(); i++)
		{
			try
			{
				Vector vt = (Vector) vtAlert.elementAt(i);

				if (Long.parseLong(vt.elementAt(0).toString()) == productid)
				{
					dataLimitation = Integer.parseInt(vt.elementAt(1)
							.toString());
					break;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		return dataLimitation;
	}

	@SuppressWarnings("rawtypes")
	public int getTimePerData(long productid)
	{
		int TimePerData = 0;
		for (int i = 0; i < vtAlert.size(); i++)
		{
			Vector vt = (Vector) vtAlert.elementAt(i);

			if (Long.parseLong(vt.elementAt(0).toString()) == productid)
			{
				TimePerData = Integer.parseInt(vt.elementAt(4).toString());
				break;
			}
		}
		return TimePerData;
	}

	@SuppressWarnings("rawtypes")
	public String getBalanceName(long productid)
	{
		String balanceName = "";
		for (int i = 0; i < vtAlert.size(); i++)
		{
			Vector vt = (Vector) vtAlert.elementAt(i);

			if (Long.parseLong(vt.elementAt(0).toString()) == productid)
			{
				balanceName = vt.elementAt(2).toString();
				break;
			}
		}
		return balanceName;
	}

	@SuppressWarnings("rawtypes")
	public String getServiceAddress(long productid)
	{
		String serviceAddress = "";
		for (int i = 0; i < vtAlert.size(); i++)
		{
			Vector vt = (Vector) vtAlert.elementAt(i);

			if (Long.parseLong(vt.elementAt(0).toString()) == productid)
			{
				serviceAddress = vt.elementAt(3).toString();
				break;
			}
		}
		return serviceAddress;
	}
}
