package com.crm.thread;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.Vector;

import com.crm.kernel.message.Constants;
import com.crm.kernel.queue.QueueFactory;
import com.crm.kernel.sql.Database;
import com.crm.provisioning.message.CommandMessage;
import com.crm.thread.util.ThreadUtil;
import com.fss.util.AppException;

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright:
 * </p>
 * <p>
 * Company:
 * </p>
 * 
 * @author: HungPQ
 * @version 1.0
 */

public class DBQueueThread extends DispatcherThread
{
	protected PreparedStatement	stmtQueue			= null;
	protected PreparedStatement	stmtSubscription	= null;
	protected ResultSet			rsQueue				= null;
	protected Connection		connection			= null;
	
	public String				selectSQL			= "";
	
	protected int				batchSize			= 100;
	private int					batchCounter		= 0;
	protected int				orderTimeOut		= 60000;
	
	public int					pendingMaxSize		= 0;
	public String				pendingQueueList	= "";
	
	protected boolean			pushFreeRequest		= false;

	public int					totalServerPending	= 0;
	private int					counter				= 0;
	
	protected long				lastRunTime			= System.currentTimeMillis();
	private Object				lock				= new Object();

	// //////////////////////////////////////////////////////
	// Override
	// //////////////////////////////////////////////////////
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Vector getDispatcherDefinition()
	{
		Vector vtReturn = new Vector();

		vtReturn.addElement(ThreadUtil.createTextParameter("SQLCommand", 4000, ""));
		
		vtReturn.addElement(ThreadUtil.createIntegerParameter("BatchSize", ""));
		vtReturn.addElement(ThreadUtil.createIntegerParameter("OrderTimeOut", ""));
		vtReturn.addElement(ThreadUtil.createBooleanParameter("PushFreeRequest", ""));
		
		vtReturn.addElement(ThreadUtil.createIntegerParameter("pendingMaxSize", "max request is waitting for process"));
		vtReturn.addElement(ThreadUtil.createIntegerParameter("overloadWaitTime", "wait in seconds when system is overloading"));

		if (QueueFactory.queueServerEnable)
		{
			vtReturn.addElement(
					ThreadUtil.createBooleanParameter("queueDispatcherEnable", "init queue connection when start dispatcher"));
			vtReturn.addElement(
					ThreadUtil.createTextParameter("pendingQueueList", 4000, "list of queue are used to check pending size"));
		}
		vtReturn.addElement(ThreadUtil.createTextParameter("queueName", 100, "jndi queue name"));
		vtReturn.addElement(ThreadUtil.createTextParameter("queueLocalName", 100, "temporary queue name"));
		vtReturn.addElement(ThreadUtil.createIntegerParameter("queueLocalSize", "Max local queue size"));

		vtReturn.addAll(ThreadUtil.createInstanceParameter(this));

		vtReturn.addAll(ThreadUtil.createLogParameter(this));

		return vtReturn;
	}

	// //////////////////////////////////////////////////////
	// Override
	// //////////////////////////////////////////////////////
	public void fillDispatcherParameter() throws AppException
	{
		try
		{
			super.fillDispatcherParameter();

			selectSQL = ThreadUtil.getString(this, "SQLCommand", true, "");

			pendingMaxSize = ThreadUtil.getInt(this, "pendingMaxSize", 1000);
			
			batchSize = ThreadUtil.getInt(this, "BatchSize", 500);
			if (batchSize > queueLocalSize)
			{
				batchSize = queueLocalSize;
			}
			
			orderTimeOut = ThreadUtil.getInt(this, "OrderTimeOut", 60000);
			pushFreeRequest = ThreadUtil.getBoolean(this, "PushFreeRequest", false);
		}
		catch (AppException e)
		{
			logMonitor(e);

			throw e;
		}
		catch (Exception e)
		{
			logMonitor(e);

			throw new AppException(e.getMessage());
		}
		finally
		{
		}
	}
	
	// /////////////////////////////////////////////////////////////////////////
	// Prepare data target
	// Author: ThangPV
	// Modify DateTime: 19/02/2003
	// /////////////////////////////////////////////////////////////////////////
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
		else if (pendingMaxSize > 0)
		{
			int totalPending = QueueFactory.getTotalLocalPending() + totalServerPending;

			return (totalPending > pendingMaxSize);
		}

		return false;
	}

	// //////////////////////////////////////////////////////
	/**
	 * Event raised when session prepare to run
	 * 
	 * @throws java.lang.Exception
	 * @author Phan Viet Thang
	 */
	// //////////////////////////////////////////////////////
	public void beforeProcessSession() throws Exception
	{
		super.beforeProcessSession();

		try
		{
			counter = 0;
			
			QueueFactory.getLocalQueue(queueLocalName).empty();

			QueueFactory.getLocalQueue(queueLocalName).setCheckPending(false);
		}
		catch (Exception e)
		{
			throw e;
		}
	}

	// //////////////////////////////////////////////////////
	/**
	 * Event raised when session prepare to run
	 * 
	 * @throws java.lang.Exception
	 * @author Phan Viet Thang
	 */
	// //////////////////////////////////////////////////////
	public void afterProcessSession() throws Exception
	{
		try
		{
			if (connection != null && !connection.isClosed())
			{
				updateToDB();
				Database.closeObject(stmtQueue);
				Database.closeObject(stmtSubscription);
				Database.closeObject(connection);
			}
			
		}
		finally
		{
			QueueFactory.getLocalQueue(queueLocalName).empty();
			super.afterProcessSession();
		}
	}

	public void updateSnapshot() throws Exception
	{
		totalServerPending = pendingMaxSize;
		totalServerPending = QueueFactory.getSnapshotSize(pendingQueueList);
	}
	
	public void loadDataBase() throws Exception
	{
		try
		{
			String strSQL = selectSQL;
			
			if (batchSize > 0)
			{
				strSQL = strSQL + " and rownum <= "	+ batchSize;
			}
			
			if (!pushFreeRequest)
			{
				strSQL = strSQL + " and keyword not like 'FREE_%'";
			}
			
			connection = Database.getConnection();
			stmtQueue = connection.prepareStatement(strSQL);
			
			strSQL = "Delete CommandRequest Where RequestID = ?";
			stmtSubscription = connection.prepareStatement(strSQL);
			
			rsQueue = stmtQueue.executeQuery();
			
			while (rsQueue.next())
			{
				CommandMessage request = new CommandMessage();
	
				request.setRequestTime(new Date());
				request.setRequestId(rsQueue.getLong("requestId"));
				request.setChannel(rsQueue.getString("channel"));
				request.setServiceAddress(rsQueue.getString("serviceAddress"));
				request.setIsdn(rsQueue.getString("isdn"));
				request.setKeyword(rsQueue.getString("keyword"));
				request.setSubProductId(rsQueue.getLong("subProductId"));
				request.setTimeout(orderTimeOut);
				
				addCounter(queueLocalName, request);
			}
		}
		catch (Exception e)
		{
			logMonitor(e);
			
			sendInstanceAlarm(e, Constants.ERROR);
			
			throw e;
		}
		finally
		{
			Database.closeObject(rsQueue);
			Database.closeObject(stmtQueue);
		}
	}
	
	public synchronized void removeRequest(long requestId)
	{
		try
		{
			stmtSubscription.setLong(1, requestId);
			stmtSubscription.addBatch();
			batchCounter++;
			if (batchCounter >= 50)
			{
				batchCounter = 0;
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
		if (stmtSubscription != null)
		{
			stmtSubscription.executeBatch();
			connection.commit();
		}
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
				Database.closeObject(stmtSubscription);
				Database.closeObject(connection);
			}
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			stmtSubscription = null;
			connection = null;
			QueueFactory.getLocalQueue(queueLocalName).empty();
		}
	}
	
	public void addCounter(String queueLocalName, CommandMessage message) throws Exception
	{
		QueueFactory.attachLocal(queueLocalName, message);
		synchronized (lock)
		{
			counter++;
		}
	}

	public void removeCounter()
	{
		synchronized (lock)
		{
			counter--;
		}
	}

	public synchronized int getCounter()
	{
		return counter;
	}

	// //////////////////////////////////////////////////////
	// process session
	// Author : ThangPV
	// Created Date : 16/09/2004
	// //////////////////////////////////////////////////////
	public void doProcessSession() throws Exception
	{
		try
		{
			boolean EOF = false;
			
			if ((System.currentTimeMillis() - lastSnapshot) > snapshotInterval)
			{
				updateSnapshot();
				lastSnapshot = System.currentTimeMillis();
			}
			
			if (isOverload())
			{
				debugMonitor("Too many order in queue. Total server pending: " + totalServerPending);
			}
			else
			{
				loadDataBase();
				
				while (isAvailable() && !EOF)
				{
					checkInstance();

					if ((System.currentTimeMillis() - lastSnapshot) > snapshotInterval)
					{
						updateSnapshot();
						lastSnapshot = System.currentTimeMillis();
					}
					
					if (getCounter() > 0)
					{
						Thread.sleep(10);
					}
					else
					{
						EOF = true;
					}
				}
				
				closeDatabase();
			}
		}
		catch (Exception e)
		{
			throw e;
		}
	}
}
