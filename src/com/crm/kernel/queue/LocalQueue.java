package com.crm.kernel.queue;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.crm.provisioning.message.CommandMessage;
import com.crm.thread.DispatcherThread;

public class LocalQueue
{
	protected int							maxSize			= 0;
	protected String						queueName		= "jms/CCS";
	protected String						remoteQueueName	= "jms/CCS";
	protected boolean						checkPending	= true;

	protected ConcurrentLinkedQueue<Object>	localQueue		= new ConcurrentLinkedQueue<Object>();
	protected DispatcherThread				dispathcher		= null;

	public LocalQueue(String name, int size)
	{
		queueName = name;
		maxSize = size;
	}

	public LocalQueue(String name)
	{
		queueName = name;
	}

	public int getMaxSize()
	{
		return maxSize;
	}

	public void setMaxSize(int maxSize)
	{
		this.maxSize = maxSize;
	}

	public boolean isCheckPending()
	{
		return checkPending;
	}

	public void setCheckPending(boolean checkPending)
	{
		this.checkPending = checkPending;
	}

	public ConcurrentLinkedQueue<Object> getQueue()
	{
		return localQueue;
	}

	public void setQueue(ConcurrentLinkedQueue<Object> queue)
	{
		localQueue = queue;
	}

	public DispatcherThread getDispathcher()
	{
		return dispathcher;
	}

	public void setDispathcher(DispatcherThread dispathcher)
	{
		this.dispathcher = dispathcher;
	}

	public int getSize()
	{
		return localQueue.size();
	}

	public boolean isOverload()
	{
		return ((maxSize > 0) && (getSize() >= maxSize));
	}

	public void attach(Object request)
	{
		if (request == null)
		{
			return;
		}

		localQueue.offer(request);

		if ((dispathcher != null) && dispathcher.instanceEnable 
				&& (dispathcher.instances != null) && (dispathcher.instances.getActiveTaskCount() == 0))
		{
			try
			{
				dispathcher.addInstance();
			}
			catch (Exception e)
			{

			}
		}
	}

	public Object detach()
	{
		return localQueue.poll();
	}

	public CommandMessage detachCommand() throws Exception
	{
		return (CommandMessage) localQueue.poll();
	}

	public void empty()
	{
		localQueue.clear();
	}
}
