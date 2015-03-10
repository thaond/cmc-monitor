/**
 * 
 */
package com.crm.provisioning.thread;

import java.util.Vector;

import com.crm.kernel.queue.QueueFactory;
import com.crm.thread.util.ThreadUtil;

import com.fss.util.AppException;

/**
 * @author ThangPV
 * 
 */
public class OrderRoutingThread extends ProvisioningThread
{
	public int		queueMaxPendingSize	= 1000;
	public boolean	autoReplyIfOverload	= false;

	public OrderRoutingThread()
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
		vtReturn.addElement(ThreadUtil.createIntegerParameter("queueMaxPendingSize", "Max local queue size"));
		vtReturn.addElement(ThreadUtil.createBooleanParameter("autoReplyIfOverload", "auto reply when system overload"));

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

			queueMaxPendingSize = ThreadUtil.getInt(this, "queueMaxPendingSize", 1000);
			autoReplyIfOverload = ThreadUtil.getBoolean(this, "autoReplyIfOverload", false);
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

	public boolean isOverload()
	{
		if (QueueFactory.getLocalQueue(queueLocalName).isOverload())
		{
			return true;
		}
		else if (QueueFactory.getTotalLocalPending() > queueMaxPendingSize)
		{
			return true;
		}

		return false;
	}

}
