/**
 * 
 */
package com.crm.provisioning.thread;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Vector;

import com.crm.kernel.index.BinaryIndex;
import com.crm.kernel.message.Constants;
import com.crm.kernel.queue.QueueFactory;
import com.crm.kernel.sql.Database;
import com.crm.provisioning.cache.ProvisioningRoute;
import com.crm.thread.DispatcherThread;
import com.crm.thread.util.ThreadUtil;
import com.crm.util.StringUtil;

import com.fss.util.AppException;

/**
 * @author ThangPV
 * 
 */
public class CommandRoutingThread extends DispatcherThread
{
	public BinaryIndex	routes				= new BinaryIndex();

	public boolean		loadBalanceEnable	= true;
	public int			maxRetryRouting		= 3;
	public String[]		externalQueues		= new String[0];

	public String		queuePrefix			= "";

	public CommandRoutingThread()
	{
		super();
	}

	// //////////////////////////////////////////////////////
	// Override
	// //////////////////////////////////////////////////////
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Vector getDispatcherDefinition()
	{
		Vector vtReturn = new Vector();

		vtReturn.addElement(ThreadUtil.createTextParameter("queuePrefix", 100, ""));
		vtReturn.addElement(ThreadUtil.createIntegerParameter("maxRetryRouting", "maxRetryRouting"));

		if (QueueFactory.queueServerEnable)
		{
			vtReturn.addElement(
					ThreadUtil.createBooleanParameter("loadBalanceEnable", "send to server if local is overloading"));
			vtReturn.addElement(ThreadUtil.createIntegerParameter("externalQueues", "externalQueues"));
		}
		vtReturn.addElement(ThreadUtil.createTextParameter("queueLocalName", 100, "jndi queue name"));
		vtReturn.addElement(ThreadUtil.createIntegerParameter("queueLocalSize", "Max local queue size"));

		vtReturn.addAll(ThreadUtil.createInstanceParameter(this));
		vtReturn.addAll(ThreadUtil.createLogParameter(this));

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

			queueDispatcherEnable = QueueFactory.queueServerEnable;
			queueMode = Constants.QUEUE_MODE_MANUAL;
			queuePrefix = ThreadUtil.getString(this, "queuePrefix", false, "queue");
			maxRetryRouting = ThreadUtil.getInt(this, "maxRetryRouting", 3);
			externalQueues = StringUtil.split(ThreadUtil.getString(this, "externalQueues", false, ""), "");

			loadBalanceEnable = queueDispatcherEnable && ThreadUtil.getBoolean(this, "loadBalanceEnable", true);

			instanceEnable = true;
			instanceClass = ThreadUtil.getString(this, "instanceClass", true, "");
			instanceSize = ThreadUtil.getInt(this, "instanceSize", 1);

			if (instanceSize <= 0)
			{
				throw new AppException("Instance size must greater than zero");
			}
			
			QueueFactory.getLocalQueue(QueueFactory.COMMAND_ROUTE_QUEUE).setCheckPending(true);
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
	}

	public void loadCache() throws Exception
	{
		Connection connection = null;
		PreparedStatement stmtConfig = null;
		ResultSet rsConfig = null;

		try
		{
			getLog().debug("Loading route table ...");

			routes.clear();

			connection = Database.getConnection();

			// //////////////////////////////////////////////////////
			// Connection parameters
			// //////////////////////////////////////////////////////
			String SQL = "Select * "
					+ " From ProvisioningRoute " 
					+ " Order by provisioningType desc, routeType desc "
					+ " , decode(substr(routeKey,length(routeKey)), '%', 0, 1) desc, routeKey desc";

			stmtConfig = connection.prepareStatement(SQL);
			rsConfig = stmtConfig.executeQuery();

			while (rsConfig.next())
			{
				ProvisioningRoute entry = new ProvisioningRoute();

				entry.setProvisioningType(Database.getString(rsConfig, "provisioningType"));
				entry.setRouteType(Database.getString(rsConfig, "routeType"));
				entry.setIndexKey(Database.getString(rsConfig, "routeKey"));

				entry.setProvisioningId(rsConfig.getLong("provisioningId"));

				routes.add(entry);
			}

			Database.closeObject(rsConfig);
			Database.closeObject(stmtConfig);

			getLog().debug("Routing table are loaded");
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			Database.closeObject(rsConfig);
			Database.closeObject(stmtConfig);

			Database.closeObject(connection);
		}
	}

	public ProvisioningRoute getRoute(String provisioningType, String routeType, String routeKey) throws Exception
	{
		ProvisioningRoute lookup = new ProvisioningRoute();

		lookup.setProvisioningType(provisioningType);
		lookup.setRouteType(routeType);
		lookup.setRouteKey(routeKey);

		lookup = (ProvisioningRoute) routes.get(lookup);

		if (lookup == null)
		{
			throw new AppException(Constants.ERROR_ROUTE_NOT_FOUND);
		}

		return lookup;
	}
}
