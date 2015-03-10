/**
 * 
 */
package com.crm.thread;

import java.util.Vector;

import com.crm.kernel.queue.QueueFactory;
import com.crm.thread.util.ThreadUtil;

import com.fss.util.AppException;

/**
 * @author ThangPV
 * 
 */
public class SimulatorThread extends DispatcherThread
{
	public String	queueCallback		= "";

	public String	deliveryUser		= "";
	public String	channel				= "";
	public String	serviceAddress		= "";
	public String	isdn				= "";
	public String	endIsdn				= "";
	public String	shipTo				= "";
	public String	keyword				= "";
	public String	content				= "";
	public int		orderTimeout		= 30000;

	public boolean	asynchronous		= false;
	public long		minIsdn				= 0;
	public long		maxIsdn				= 0;
	public int		maxServerPending	= 0;
	public int		maxLocalPending		= 0;

	public long		currentIsdn			= 0;
	public int		totalServerPending	= 0;

	public SimulatorThread()
	{
		super();
	}

	// //////////////////////////////////////////////////////
	// Override
	// //////////////////////////////////////////////////////
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Vector getSimulatorDefinition()
	{
		Vector vtReturn = new Vector();

		vtReturn.addElement(ThreadUtil.createTextParameter("deliveryUser", 30, ""));
		vtReturn.addElement(ThreadUtil.createComboParameter("channel", "SMS,web,core", ""));
		vtReturn.addElement(ThreadUtil.createTextParameter("serviceAddress", 30, ""));
		vtReturn.addElement(ThreadUtil.createTextParameter("keyword", 30, ""));

		vtReturn.addElement(ThreadUtil.createTextParameter("isdn", 30, ""));
		vtReturn.addElement(ThreadUtil.createTextParameter("endIsdn", 30,
				"If parameter is set, the simulator send order of subscribers range from isdn to endIsdn."));
		vtReturn.addElement(ThreadUtil.createTextParameter("shipTo", 30, ""));
		vtReturn.addElement(ThreadUtil.createIntegerParameter("orderTimeout", ""));

		return vtReturn;
	}

	// //////////////////////////////////////////////////////
	// Override
	// //////////////////////////////////////////////////////
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Vector getDispatcherDefinition()
	{
		Vector vtReturn = new Vector();

		vtReturn.addAll(getSimulatorDefinition());

		vtReturn.addAll(ThreadUtil.createQueueParameter(this));

		if (QueueFactory.queueServerEnable)
		{
			vtReturn.addElement(
					ThreadUtil.createIntegerParameter("maxServerPending", "pending when server queue over this value"));
			vtReturn.addElement(ThreadUtil.createTextParameter("queueList", 4000, ""));
			vtReturn.addElement(ThreadUtil.createTextParameter("queueCallback", 4000, ""));
		}
		vtReturn.addElement(
				ThreadUtil.createIntegerParameter("maxLocalPending", "pending when local queue over this value"));
		vtReturn.addElement(
				ThreadUtil.createBooleanParameter("asynchronous", "send request to queue server"));

		vtReturn.addAll(ThreadUtil.createInstanceParameter(this));
		vtReturn.addAll(ThreadUtil.createLogParameter(this));

		return vtReturn;
	}

	// //////////////////////////////////////////////////////
	// Override
	// //////////////////////////////////////////////////////
	public void fillSimulatorParameter() throws AppException
	{
		try
		{
			// //////////////////////////////////////////////////////
			// Fill extent parameter
			// //////////////////////////////////////////////////////
			queueCallback = ThreadUtil.getString(this, "queueCallback", false, "");

			deliveryUser = ThreadUtil.getString(this, "deliveryUser", false, "");
			channel = ThreadUtil.getString(this, "channel", false, "");
			serviceAddress = ThreadUtil.getString(this, "serviceAddress", false, "");
			keyword = ThreadUtil.getString(this, "keyword", false, "");
			shipTo = ThreadUtil.getString(this, "shipTo", false, "");
			orderTimeout = ThreadUtil.getInt(this, "orderTimeout", 30000);

			asynchronous = ThreadUtil.getBoolean(this, "asynchronous", false);
			isdn = ThreadUtil.getString(this, "isdn", false, "");
			endIsdn = ThreadUtil.getString(this, "endIsdn", false, isdn);
			maxServerPending = ThreadUtil.getInt(this, "maxServerPending", 0);
			maxLocalPending = ThreadUtil.getInt(this, "maxLocalPending", 0);
			totalServerPending = queueDispatcherEnable ? maxServerPending : 0;

			currentIsdn = 0;

			try
			{
				maxIsdn = Long.parseLong(endIsdn);
			}
			catch (Exception e)
			{
				maxIsdn = Long.parseLong(isdn);
			}

			instanceEnable = true;
			instanceClass = ThreadUtil.getString(this, "instanceClass", true, "");
			instanceSize = ThreadUtil.getInt(this, "instanceSize", 1);

			if (instanceSize <= 0)
			{
				throw new AppException("Instance size must greater than zero");
			}
		}
		catch (AppException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new AppException(e.getMessage());
		}
	}

	// //////////////////////////////////////////////////////
	// Override
	// //////////////////////////////////////////////////////
	public void fillParameter() throws AppException
	{
		try
		{
			super.fillParameter();

			fillSimulatorParameter();
		}
		catch (AppException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new AppException(e.getMessage());
		}
	}

	public int getTotalServerPending()
	{
		synchronized (mutex)
		{
			return totalServerPending;
		}
	}

	// //////////////////////////////////////////////////////
	// process session
	// Author : ThangPV
	// Created Date : 16/09/2004
	// //////////////////////////////////////////////////////
	public synchronized long getCurrentIsdn() throws Exception
	{
		if (currentIsdn == 0)
		{
			currentIsdn = Long.parseLong(isdn);
		}
		else
		{
			currentIsdn++;
		}

		if (currentIsdn > maxIsdn)
		{
			return 0;
		}

		return currentIsdn;
	}

	// //////////////////////////////////////////////////////
	// process session
	// Author : ThangPV
	// Created Date : 16/09/2004
	// //////////////////////////////////////////////////////
	public void doProcessSession() throws Exception
	{
		String queueNames = ThreadUtil.getString(this, "queueList", false, "");

		if (queueDispatcherEnable && QueueFactory.queueServerEnable && !queueNames.equals(""))
		{
			totalServerPending = maxServerPending;

			while (isAvailable())
			{
				checkInstance();

				synchronized (mutex)
				{
					totalServerPending = maxServerPending;

					totalServerPending = QueueFactory.getSnapshotSize(queueNames);
				}

				if (instanceEnable && (instances.getActiveCount() < instanceSize))
				{
					for (int j = 0; (instances.getActiveCount() < instanceSize) && (j < instanceSize); j++)
					{
						addInstance();
					}
				}

				ThreadUtil.sleep(this);
			}
		}
	}
}
