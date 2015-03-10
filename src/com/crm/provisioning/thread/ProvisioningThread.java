/**
 * 
 */
package com.crm.provisioning.thread;

import java.util.Date;
import java.util.Vector;

import com.crm.alarm.cache.AlarmFactory;
import com.crm.kernel.domain.DomainFactory;
import com.crm.marketing.cache.CampaignFactory;
import com.crm.product.cache.ProductFactory;
import com.crm.provisioning.cache.ProvisioningConnection;
import com.crm.provisioning.cache.ProvisioningEntry;
import com.crm.provisioning.cache.ProvisioningFactory;
import com.crm.provisioning.cache.ProvisioningPool;
import com.crm.provisioning.util.CommandUtil;
import com.crm.thread.DispatcherThread;
import com.crm.thread.util.ThreadUtil;
import com.fss.util.AppException;

/**
 * @author ThangPV
 * 
 */
public class ProvisioningThread extends DispatcherThread
{
	// constant
	public String				alias							= "";
	public ProvisioningEntry	provisioning					= null;

	public String				host							= "";
	public int					port							= 0;
	public String				userName						= "";
	public String				password						= "";

	public String				provisioningClass				= "";
	public int					maxActive						= 30;
	public long					maxWait							= 30000;
	public int					maxIdle							= 30000;
	public int					minIdle							= -1;
	public boolean				testOnBorrow					= true;
	public boolean				testOnReturn					= true;
	public long					timeBetweenEvictionRunsMillis	= 3000;
	public int					numTestsPerEvictionRun			= 3;
	public long					minEvictableIdleTimeMillis		= -1;
	public boolean				testWhileIdle					= true;
	public long					softMinEvictableIdleTimeMillis	= -1;
	public boolean				lifo							= true;
	public String				testCommand						= "";
	public int					timeout							= 30000;
	public int					baseCostTime					= 0;
	public boolean				useSimulation					= false;
	public long					simulationTime					= 1000;
	public String				simulationCause					= "";

	public ProvisioningPool		provisioningPool				= null;

	public ProvisioningThread()
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

		vtReturn.addAll(ThreadUtil.createProvisioningParameter(this));

		vtReturn.add(ThreadUtil.createBooleanParameter("simulationMode", "Use simulation or not"));
		vtReturn.add(ThreadUtil.createLongParameter("simulationExecuteTime", "Simulation time in millisecond."));
		vtReturn.add(ThreadUtil.createTextParameter("simulationCause", 400, "Response cause after using simulation."));

		vtReturn.addAll(ThreadUtil.createQueueParameter(this));
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

			alias = ThreadUtil.getString(this, "provisioning", true, "");

			host = ThreadUtil.getString(this, "host", true, "");
			port = ThreadUtil.getInt(this, "port", 0);
			userName = ThreadUtil.getString(this, "userName", true, "");
			password = ThreadUtil.getString(this, "password", true, "");

			provisioningClass = ThreadUtil.getString(this, "provisioningClass", true, "");

			maxActive = ThreadUtil.getInt(this, "maxActive", 0);
			maxWait = ThreadUtil.getInt(this, "maxWait", 30000);
			maxIdle = ThreadUtil.getInt(this, "maxIdle", 30000);
			minIdle = ThreadUtil.getInt(this, "minIdle", -1);
			testOnBorrow = ThreadUtil.getBoolean(this, "testOnBorrow", true);
			testOnReturn = ThreadUtil.getBoolean(this, "testOnReturn", true);
			timeBetweenEvictionRunsMillis = ThreadUtil.getInt(this, "timeBetweenEvictionRunsMillis", 3000);
			numTestsPerEvictionRun = ThreadUtil.getInt(this, "numTestsPerEvictionRun", 3);
			minEvictableIdleTimeMillis = ThreadUtil.getInt(this, "minEvictableIdleTimeMillis", -1);
			testWhileIdle = ThreadUtil.getBoolean(this, "testWhileIdle", true);
			softMinEvictableIdleTimeMillis = ThreadUtil.getInt(this, "softMinEvictableIdleTimeMillis", -1);
			lifo = ThreadUtil.getBoolean(this, "lifo", true);

			testCommand = ThreadUtil.getString(this, "testCommand", false, "");
			timeout = ThreadUtil.getInt(this, "timeout", 30000);
			baseCostTime = ThreadUtil.getInt(this, "baseCostTime", 0);
			useSimulation = ThreadUtil.getBoolean(this, "simulationMode", false);
			simulationTime = ThreadUtil.getLong(this, "simulationExecuteTime", 1000);
			simulationCause = ThreadUtil.getString(this, "simulationCause", false, "");

			if (baseCostTime > 0)
			{

			}

			if (queueLocalSize == 0)
			{
				queueLocalSize = instanceSize;
			}
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

	// //////////////////////////////////////////////////////
	// after process session
	// Author : ThangPV
	// Created Date : 16/09/2004
	// //////////////////////////////////////////////////////
	public void initProvisioningParameters() throws Exception
	{
		try
		{
			provisioningPool.setHost(host);
			provisioningPool.setPort(port);
			provisioningPool.setUserName(userName);
			provisioningPool.setPassword(password);
			provisioningPool.setTimeout(timeout);
			provisioningPool.setTestCommand(testCommand);
		}
		catch (Exception e)
		{
			throw e;
		}
	}

	public void loadCache() throws Exception
	{
		try
		{
			Date now = new Date();

			DomainFactory.loadCache(now);
			ProductFactory.loadCache(now);
			ProvisioningFactory.loadCache(now);
			CampaignFactory.loadCache(now);
			AlarmFactory.loadCache(now);
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
	public void beforeProcessSession() throws Exception
	{
		ProvisioningConnection testConnection = null;

		super.beforeProcessSession();

		try
		{
			provisioning = ProvisioningFactory.getCache().getProvisioning(alias);

			if (maxActive > 0)
			{
				CommandUtil.closeConnection(provisioningPool);

				if (provisioningPool == null)
				{
					provisioningPool = new ProvisioningPool();
				}

				provisioningPool.setDispatcher(this);
				provisioningPool.setProvisioningId(provisioning.getProvisioningId());
				provisioningPool.setProvisioningClass(provisioningClass);

				initProvisioningParameters();

				provisioningPool.open(maxActive, maxWait, maxActive, minIdle, testOnBorrow, testOnReturn
						, timeBetweenEvictionRunsMillis, numTestsPerEvictionRun, minEvictableIdleTimeMillis
						, testWhileIdle, softMinEvictableIdleTimeMillis);

				logMonitor("Testing connection ...");

				testConnection = provisioningPool.getConnection();

				logMonitor("Testing connection are success");
			}
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			if ((provisioningPool != null) && (testConnection != null))
			{
				provisioningPool.closeConnection(testConnection);
			}
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
			if (provisioningPool != null)
			{
				provisioningPool.close();
			}
		}
		finally
		{
			super.afterProcessSession();
		}
	}

	public ProvisioningPool getProvisioningPool()
	{
		return provisioningPool;
	}

	public void setProvisioningPool(ProvisioningPool provisioningPool)
	{
		this.provisioningPool = provisioningPool;
	}
}
