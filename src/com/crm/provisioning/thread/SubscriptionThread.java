package com.crm.provisioning.thread;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import com.crm.kernel.message.Constants;
import com.crm.kernel.sql.Database;
import com.crm.product.cache.ProductEntry;
import com.crm.product.cache.ProductFactory;
import com.crm.thread.DispatcherThread;
import com.crm.thread.util.ThreadUtil;
import com.fss.util.AppException;
import com.fss.util.StringUtil;

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

public class SubscriptionThread extends DispatcherThread
{
	private PreparedStatement	_stmtSubscription		= null;
	private PreparedStatement	_stmtRequest			= null;
	private PreparedStatement	_stmtSubscriptionUpdate	= null;
	private Connection 			_conn 					= null;
	private String				_sqlSubscription		= "";
	private String				_sqlCheck				= "";
	private String				_channel				= "";
	private String				_lastRunDate			= "";

	// //////////////////////////////////////////////////////
	// Override
	// //////////////////////////////////////////////////////
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Vector getParameterDefinition()
	{
		Vector vtReturn = new Vector();

		vtReturn.addElement(ThreadUtil.createTextParameter("SQLSubscription", 400, "SQL query to get subscription."));
		vtReturn.addElement(ThreadUtil.createTextParameter("SQLCheck", 400, "SQL query to check recharge."));

		vtReturn.addElement(ThreadUtil.createTextParameter("LastRunDate", 400, "SQL query to get subscription."));

		vtReturn.addElement(ThreadUtil.createTextParameter("channel", 400, "Subscription channel \"web\" or \"SMS\"."));

		vtReturn.addAll(super.getParameterDefinition());

		return vtReturn;
	}

	// //////////////////////////////////////////////////////
	// Override
	// //////////////////////////////////////////////////////
	public void fillParameter() throws AppException
	{
		try
		{
			super.fillParameter();

			_sqlSubscription = loadMandatory("SQLSubscription");
			_sqlCheck = loadMandatory("SQLCheck");
			setLastRunDate(loadMandatory("LastRunDate"));

			_channel = ThreadUtil.getString(this, "channel", false, "web");
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

	// //////////////////////////////////////////////////////
	// after process session
	// Author : ThangPV
	// Created Date : 16/09/2004
	// //////////////////////////////////////////////////////
	public void beforeProcessSession() throws Exception
	{
		super.beforeProcessSession();

		try
		{
			neverExpire = false;
			_conn = Database.getConnection();
			String strSQL = _sqlSubscription;
			_stmtSubscription = _conn.prepareStatement(strSQL);

			strSQL = "Update SubscriberProduct Set lastRunDate = SysDate, subscriptionStatus = 1 Where subProductId = ?";
			_stmtSubscriptionUpdate = _conn.prepareStatement(strSQL);

			strSQL = "insert into CommandRequest "
					+ "		(requestId, userName, createDate, requestDate "
					+ "		, channel, serviceAddress, isdn, keyword) "
					+ " values "
					+ "		(command_seq.nextval, ?, SysDate, SysDate"
					+ "		, ?, ?, ?, ?) ";
			_stmtRequest = _conn.prepareStatement(strSQL);
		}
		catch (Exception e)
		{
			throw e;
		}
	}

	// //////////////////////////////////////////////////////
	// after process session
	// Author : ThangPV
	// Created Date : 16/09/2004
	// //////////////////////////////////////////////////////
	public void afterProcessSession() throws Exception
	{
		try
		{
			Database.closeObject(_stmtSubscription);
			Database.closeObject(_stmtSubscriptionUpdate);
			Database.closeObject(_conn);
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			super.afterProcessSession();
		}
	}

	// //////////////////////////////////////////////////////
	// process session
	// Author : ThangPV
	// Created Date : 16/09/2004
	// //////////////////////////////////////////////////////
	public void doProcessSession() throws Exception
	{
		long counter = 0;

		int batchSize = 5000;
		int batchCounter = 0;

		ResultSet rsQueue = null;

		try
		{
			rsQueue = _stmtSubscription.executeQuery();
			while (rsQueue.next() && isAvailable())
			{
				String sourceAddress = StringUtil.nvl(rsQueue.getString("isdn"), "");

				int subsriberType = rsQueue.getInt("subscriberType");

				if (subsriberType == Constants.PREPAID_SUB_TYPE)
				{
					ProductEntry product = ProductFactory.getCache().getProduct(rsQueue.getLong("productid"));
					
					int supplierStatus = rsQueue.getInt("supplierStatus");
					
					if (supplierStatus == Constants.SUPPLIER_BARRING_STATUS)
					{
						if (!checkRecharge(sourceAddress))
						{
							continue;
						}
					}

					if ((product != null) && product.isSubscription())
					{
						try
						{
							_stmtRequest.setString(1, "system");
							_stmtRequest.setString(2, _channel);
							_stmtRequest.setString(3, product.getAlias());
							_stmtRequest.setString(4, sourceAddress);
							_stmtRequest.setString(5, "SUBSCRIPTION_" + product.getAlias());

							_stmtRequest.addBatch();

							_stmtSubscriptionUpdate.setLong(1, rsQueue.getLong("subProductId"));
							_stmtSubscriptionUpdate.addBatch();

							batchCounter++;

							if (batchCounter >= batchSize)
							{
								_stmtRequest.executeBatch();
								_stmtSubscriptionUpdate.executeBatch();

								batchCounter = 0;
							}

							counter++;

							debugMonitor(sourceAddress + ": " + product.getAlias());
						}
						catch (Exception e)
						{
							logMonitor(sourceAddress + ": " + product.getAlias() + "(" + e.getMessage() + ")");
						}
					}
				}
			}

			if (batchCounter >= 0)
			{
				_stmtRequest.executeBatch();
				_stmtSubscriptionUpdate.executeBatch();
			}

			logMonitor("Total record is browsed: " + counter);

			setLastRunDate(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));

			mprtParam.setProperty("LastRunDate", getLastRunDate());
			storeConfig();
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			_conn.commit();
			Database.closeObject(rsQueue);
		}
	}

	public boolean checkRecharge(String isdn)
	{
		PreparedStatement stmtCheck = null;
		ResultSet rsRechargeCount = null;
		Connection connection = null;
		try
		{
			connection = Database.getConnection();
			stmtCheck = connection.prepareStatement(_sqlCheck);
			stmtCheck.setString(1, isdn);
			rsRechargeCount = stmtCheck.executeQuery();

			if (rsRechargeCount.next())
			{
				if (rsRechargeCount.getInt("total") > 0)
				{
					return true;
				}
				else
				{
					return false;
				}
			}
			else
			{
				return false;
			}
		}
		catch (Exception e)
		{
			debugMonitor(e);
			return false;
		}
		finally
		{
			Database.closeObject(rsRechargeCount);
			Database.closeObject(stmtCheck);
			Database.closeObject(connection);
		}
	}

	public void setLastRunDate(String _lastRunDate)
	{
		this._lastRunDate = _lastRunDate;
	}

	public String getLastRunDate()
	{
		return _lastRunDate;
	}
}
