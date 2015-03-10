package com.crm.provisioning.thread;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Time;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.crm.kernel.message.Constants;
import com.crm.kernel.queue.QueueFactory;
import com.crm.kernel.sql.Database;
import com.crm.provisioning.message.CommandMessage;
import com.crm.thread.DispatcherInstance;
import com.crm.util.DateUtil;
import com.crm.util.StringUtil;

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

public class CommandLogInstance extends DispatcherInstance
{
	protected PreparedStatement			stmtLog		= null;
	protected Vector<Vector<Object>>	batchData	= null;
	protected long						startTime	= System.currentTimeMillis();

	public CommandLogInstance() throws Exception
	{
		super();
	}

	public CommandLogThread getDispatcher()
	{
		return (CommandLogThread) dispatcher;
	}

	@Override
	public Object detachMessage() throws Exception
	{
		long now = System.currentTimeMillis();

		if (((now - startTime) > (1000 * getDispatcher().updateDatabaseInterval))
				|| (batchData.size() >= getDispatcher().maxBatchSize))
		{
			updateLog();
		}
		return QueueFactory.detachLocal(QueueFactory.COMMAND_LOG_QUEUE);
	}

	@Override
	public int processMessage(CommandMessage request) throws Exception
	{
		if (request == null)
			return Constants.BIND_ACTION_NONE;

		if (!checkByPass(String.valueOf(request.getCommandId()), ((CommandLogThread) getDispatcher()).byPassCommandIds))
		{
			try
			{
				Vector<Object> vtRow = new Vector<Object>();
				vtRow.add(DateUtil.getTimeSQL(request.getRequestTime()));
				vtRow.add(DateUtil.getTimeSQL(request.getResponseTime()));
				vtRow.add(request.getUserId());
				vtRow.add(request.getUserName());

				vtRow.add(request.getIsdn());
				vtRow.add(request.getProvisioningType());
				vtRow.add(request.getProvisioningId());
				vtRow.add(request.getProductId());
				vtRow.add(request.getCommandId());

//				vtRow.add(StringUtil.nvl(request.getParameters(), ""));
				vtRow.add("");
				vtRow.add(StringUtil.nvl(request.getRequest(), ""));
				vtRow.add(StringUtil.nvl(request.getResponse(), ""));

//				vtRow.add(request.getCause());
				vtRow.add("");
				vtRow.add(request.getOrderId());
				
				batchData.add(vtRow);
			}
			catch (Exception e)
			{
				throw e;
			}
		}
		else
		{
			// debugMonitor("By pass commandId=" + request.getCommandId());
		}
		
		return Constants.BIND_ACTION_NONE;
	}

	@Override
	public void beforeProcessSession() throws Exception
	{
		batchData = new Vector<Vector<Object>>();
		startTime = System.currentTimeMillis();
		super.beforeProcessSession();
	}

	@Override
	public void afterProcessSession() throws Exception
	{
		try
		{
			if (batchData.size() > 0)
			{
				updateLog();
			}
		}
		catch (Exception e)
		{
			debugMonitor(e);
		}
		finally
		{
			super.afterProcessSession();
		}
	}

	private void updateLog() throws Exception
	{
		Connection connection = null;
		PreparedStatement stmtLog = null;
		try
		{
			connection = Database.getConnection();
			stmtLog = connection.prepareStatement("Insert into CommandLog " +
					"	(commandLogId, requestDate, responseDate, userId, username, isdn  " +
					"	, provisioningType, provisioningId, productId, commandId " +
					"	, parameters, request, response, cause, orderId) " +
					"Values " +
					"	(log_seq.nextval, ?, ?, ?, ?, ?  " +
					"	, ?, ?, ?, ?, ?, ?, ?, ?, ?) ");

			for (int index = 0; index < batchData.size(); index++)
			{
				Vector<Object> row = batchData.get(index);

				stmtLog.setTime(1, (Time) row.get(0));
				stmtLog.setTime(2, (Time) row.get(1));
				stmtLog.setLong(3, (Long) row.get(2));
				stmtLog.setString(4, (String) row.get(3));

				stmtLog.setString(5, (String) row.get(4));
				stmtLog.setString(6, (String) row.get(5));
				stmtLog.setLong(7, (Long) row.get(6));
				stmtLog.setLong(8, (Long) row.get(7));
				stmtLog.setLong(9, (Long) row.get(8));

				stmtLog.setString(10, (String) row.get(9));
				if (((CommandLogThread) dispatcher).useShortLog)
				{
					stmtLog.setString(11, "");
					stmtLog.setString(12, "");
				}
				else
				{
					stmtLog.setString(11, (String) row.get(10));
					stmtLog.setString(12, (String) row.get(11));
				}

				stmtLog.setString(13, (String) row.get(12));
				stmtLog.setLong(14, (Long) row.get(13));

				stmtLog.addBatch();
			}

			stmtLog.executeBatch();

			debugMonitor(batchData.size() + " records were writen to database.");

			startTime = System.currentTimeMillis();
			batchData.clear();
		}
		catch (Exception e)
		{
			debugMonitor(e);
		}
		finally
		{
			Database.closeObject(stmtLog);
			Database.closeObject(connection);
		}
	}

	private boolean checkByPass(String commandId, String byPassList)
	{
		Pattern findPattern = Pattern.compile("(([^0-9]+)|^)(" + commandId + ")(([^0-9]+)|$)");
		Matcher findMatcher = findPattern.matcher(byPassList);
		if (findMatcher.find())
			return true;
		return false;
	}
}
