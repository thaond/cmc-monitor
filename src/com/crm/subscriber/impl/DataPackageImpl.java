package com.crm.subscriber.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Date;

import com.crm.kernel.message.Constants;
import com.crm.kernel.sql.Database;

public class DataPackageImpl
{
	public static boolean confirmUnRegister(long subscriberId, int subscriberStatus)
			throws Exception
	{
		boolean success = false;

		Connection connection = null;
		PreparedStatement stmtProduct = null;

		try
		{
			String sql = "Update SubscriberProduct Set status = ?, modifiedDate = sysdate, lastRunDate = sysdate Where subProductId = ? ";

			connection = Database.getConnection();
			stmtProduct = connection.prepareStatement(sql);
			
			stmtProduct.setInt(1, subscriberStatus);
			stmtProduct.setLong(2, subscriberId);

			stmtProduct.setQueryTimeout(10);

			stmtProduct.execute();
			
			if (stmtProduct.getUpdateCount() > 0)
			{
				success = true;
			}
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			Database.closeObject(stmtProduct);
			Database.closeObject(connection);
		}

		return success;
	}
	
	public static boolean removeConfirm(long subscriberId, int subscriberStatus)
			throws Exception
	{
		boolean success = false;

		Connection connection = null;
		PreparedStatement stmtProduct = null;

		try
		{
			String strSQL = "Update SubscriberProduct Set status = ?, modifiedDate = sysdate, lastRunDate = sysdate Where subProductId = ? ";
			
			connection = Database.getConnection();
			stmtProduct = connection.prepareStatement(strSQL);

			stmtProduct.setInt(1, subscriberStatus);
			stmtProduct.setLong(2, subscriberId);
			
			stmtProduct.setQueryTimeout(10);

			stmtProduct.execute();
			
			if (stmtProduct.getUpdateCount() > 0)
			{
				success = true;
			}
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			Database.closeObject(stmtProduct);
			Database.closeObject(connection);
		}
		
		return success;
	}
	
	public static boolean isConfirm(String isdn, long productId, String orderType, String relateOrderType, int confirmScan)
			throws Exception
	{
		boolean confirmed = false;

		Connection connection = null;
		PreparedStatement stmtProduct = null;
		ResultSet rsProduct = null;

		try
		{
			StringBuilder strSQL = new StringBuilder();
			strSQL.append("select a.confirmdate, b.unregisterdate");
			strSQL.append("  from");
			strSQL.append("      (select max(orderdate) as confirmdate");
			strSQL.append("         from subscriberorder");
			strSQL.append("        where orderDate >= (sysdate - ? / 86400)");
			strSQL.append("          and orderDate <= sysdate");
			strSQL.append("          and isdn = ?");
			strSQL.append("          and productid = ?");
			strSQL.append("          and ordertype = ?");
			strSQL.append("          and status = ?) a,");
			strSQL.append("      (select max(orderdate) as unregisterdate");
			strSQL.append("         from subscriberorder");
			strSQL.append("        where orderDate >= (sysdate - ? / 86400)");
			strSQL.append("          and orderDate <= sysdate");
			strSQL.append("          and isdn = ?");
			strSQL.append("          and productid = ?");
			strSQL.append("          and ordertype = ?");
			strSQL.append("          and status not in (?)) b");
			
			connection = Database.getConnection();
			stmtProduct = connection.prepareStatement(strSQL.toString());
			
			stmtProduct.setInt(1, confirmScan);
			stmtProduct.setString(2, isdn);
			stmtProduct.setLong(3, productId);
			stmtProduct.setString(4, orderType);
			stmtProduct.setInt(5, Constants.ORDER_STATUS_APPROVED);
			stmtProduct.setInt(6, confirmScan);
			stmtProduct.setString(7, isdn);
			stmtProduct.setLong(8, productId);
			stmtProduct.setString(9, relateOrderType);
			stmtProduct.setInt(10, Constants.ORDER_STATUS_PENDING);

			rsProduct = stmtProduct.executeQuery();
			if (rsProduct.next())
			{
				Timestamp time = rsProduct.getTimestamp("confirmdate");
				if (time != null)
				{
					Date confirmDate = new Date(time.getTime());
					
					time = rsProduct.getTimestamp("unregisterdate");
					
					if (time != null)
					{
						Date unregisterDate = new Date(time.getTime());
						
						System.out.print(unregisterDate.getTime() < confirmDate.getTime());
						
						if (unregisterDate.getTime() < confirmDate.getTime())
						{
							confirmed = true;
						}
					}
					else
					{
						confirmed = true;
					}
				}
			}
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			Database.closeObject(rsProduct);
			Database.closeObject(stmtProduct);
			Database.closeObject(connection);
		}

		return confirmed;
	}
}
