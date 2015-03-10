package com.crm.thread;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class InstanceFactory extends ThreadPoolExecutor
{
	// Lock object used for synchronization
	private final Object	lockObject		= new Object();

	// Contains the active task count
	private int				activeTaskCount	= 0;

	public InstanceFactory(int instanceSize)
	{
		super(0, instanceSize, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(instanceSize));
	}

	public int getActiveTaskCount()
	{
		synchronized (lockObject)
		{
			return activeTaskCount;
		}
	}

	protected void beforeExecute(Thread thread, Runnable instance)
	{
		super.beforeExecute(thread, instance);

		synchronized (lockObject)
		{
			activeTaskCount++;
		}
	}

	protected void afterExecute(Runnable instance, Throwable t)
	{
		super.afterExecute(instance, t);

		if (instance != null)
		{
			((DispatcherInstance) instance).setRunning(false);
		}

		synchronized (lockObject)
		{
			activeTaskCount--;
		}
	}
}
