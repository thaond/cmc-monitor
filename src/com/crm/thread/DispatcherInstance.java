package com.crm.thread;

import javax.jms.Message;
import javax.jms.MessageFormatException;

import org.apache.log4j.Logger;

import com.crm.kernel.message.Constants;
import com.crm.kernel.queue.QueueFactory;
import com.crm.provisioning.message.CommandMessage;
import com.fss.util.AppException;

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2011
 * </p>
 * <p>
 * Company:
 * </p>
 * 
 * @author Phan Viet Thang<br>
 *         Edited by NamTA Edited Date 21/08/2012 Edited Description: Change
 *         logMonitor to debugMonitor in run method
 * @version 1.0
 */

public class DispatcherInstance implements Runnable
{
	// //////////////////////////////////////////////////////
	// Member variables
	// //////////////////////////////////////////////////////
	protected Thread		thread			= null;
	protected boolean		stopRequested	= true;

	public DispatcherThread	dispatcher		= null;

	// //////////////////////////////////////////////////////
	// error variables
	// //////////////////////////////////////////////////////
	public Exception		error			= null;

	public DispatcherInstance() throws Exception
	{
	}

	public void setDispatcher(DispatcherThread dispatcher) throws Exception
	{
		if (dispatcher == null)
		{
			throw new AppException("dispatcher-can-not-be-null");
		}

		this.dispatcher = dispatcher;
	}

	public DispatcherThread getDispatcher()
	{
		return dispatcher;
	}

	public boolean isRunning()
	{
		return !stopRequested;
	}

	public void setRunning(boolean running)
	{
		this.stopRequested = !running;
	}

	public Logger getLog()
	{
		return (dispatcher == null) ? null : dispatcher.getLog();
	}

	// ////////////////////////////////////////////////////////////////////////
	// load directory parameters
	// Author: ThangPV
	// Modify DateTime: 19/02/2003
	// /////////////////////////////////////////////////////////////////////////
	public void logMonitor(Object message)
	{
		getDispatcher().logMonitor(message);
	}

	// ////////////////////////////////////////////////////////////////////////
	// load directory parameters
	// Author: ThangPV
	// Modify DateTime: 19/02/2003
	// /////////////////////////////////////////////////////////////////////////
	public void debugMonitor(Object message)
	{
		getDispatcher().debugMonitor(message);
	}

	// //////////////////////////////////////////////////////
	/**
	 * Start thread
	 * 
	 * @author Phan Viet Thang
	 */
	// //////////////////////////////////////////////////////
	public void start()
	{
		thread = new Thread(this);
		stopRequested = false;

		thread.start();
	}

	// //////////////////////////////////////////////////////
	/**
	 * Start thread
	 * 
	 * @author Phan Viet Thang
	 */
	// //////////////////////////////////////////////////////
	public void stop()
	{
		destroy();
	}

	// //////////////////////////////////////////////////////
	/**
	 * Destroy thread
	 * 
	 * @author Phan Viet Thang
	 */
	// //////////////////////////////////////////////////////
	public void destroy()
	{
		try
		{
			stopRequested = true;

			try
			{
				thread.join(1000);
			}
			catch (InterruptedException e)
			{
				thread.interrupt();
			}
			// dispatcher.destroyQueuePool();

			if ((thread != null) && !thread.isInterrupted())
			{
				Thread tmpThread = thread;
				thread = null;

				if (tmpThread != null)
				{
					tmpThread.interrupt();
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public boolean isAvailable()
	{
		// Thread thisThread = Thread.currentThread();

		return (!stopRequested && dispatcher.isAvailable() && (dispatcher.instanceEnable || !dispatcher.instances.isShutdown()));
	}

	public boolean isNeverExpire()
	{
		return dispatcher.isAutoLoop();
	}

	public void initQueue() throws Exception
	{

	}

	// //////////////////////////////////////////////////////
	// process session
	// Author : ThangPV
	// Created Date : 16/09/2004
	// //////////////////////////////////////////////////////
	public void beforeProcessSession() throws Exception
	{
		initQueue();
	}

	// //////////////////////////////////////////////////////
	// process session
	// Author : ThangPV
	// Created Date : 16/09/2004
	// //////////////////////////////////////////////////////
	public void afterProcessSession() throws Exception
	{
	}

	// /////////////////////////////////////////////////////////////////////////
	// Bind data into batch statement
	// Author: ThangPV
	// Modify DateTime: 07-Jan-2012
	// /////////////////////////////////////////////////////////////////////////
	public int exportData(Object message) throws Exception
	{
		return Constants.BIND_ACTION_SUCCESS;
	}

	// /////////////////////////////////////////////////////////////////////////
	// Bind data into batch statement
	// Author: ThangPV
	// Modify DateTime: 07-Jan-2012
	// /////////////////////////////////////////////////////////////////////////
	public int exportError(Object message, String error) throws Exception
	{
		return Constants.BIND_ACTION_SUCCESS;
	}

	// /////////////////////////////////////////////////////////////////////////
	// Bind data into batch statement
	// Author: ThangPV
	// Modify DateTime: 07-Jan-2012
	// /////////////////////////////////////////////////////////////////////////
	public int exportError(Object message, Exception error) throws Exception
	{
		return Constants.BIND_ACTION_SUCCESS;
	}

	protected int processMessage(Message message) throws Exception
	{
		if (message instanceof Message)
		{
			Object request = QueueFactory.getContentMessage(message);

			if (request instanceof CommandMessage)
			{
				return processMessage((CommandMessage) request);
			}
		}

		return Constants.BIND_ACTION_NONE;
	}

	protected int processMessage(CommandMessage message) throws Exception
	{
		return Constants.BIND_ACTION_NONE;
	}

	public int processMessage(Object message) throws Exception
	{
		int action = Constants.BIND_ACTION_NONE;

		if (message instanceof Message)
		{
			action = processMessage((Message) message);
		}
		else if (message instanceof CommandMessage)
		{
			action = processMessage((CommandMessage) message);
		}
		else 
		{
			logMonitor("ignore message with class " + message.getClass().getName());
		}

		return action;
	}

	// //////////////////////////////////////////////////////
	// process session
	// Author : ThangPV
	// Created Date : 16/09/2004
	// //////////////////////////////////////////////////////
	public Object detachMessage() throws Exception
	{
		try
		{
			if (dispatcher.queueLocalName.equals(""))
			{
				return null;
			}

			return QueueFactory.detachLocal(dispatcher.queueLocalName);
		}
		catch (Exception e)
		{
			throw e;
		}
	}

	public boolean isOverload()
	{
		return false;
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
			Object message = detachMessage();
			while (isAvailable() && (message != null))
			{
				int action = Constants.BIND_ACTION_SUCCESS;

				try
				{
					action = processMessage(message);
				}
				catch (MessageFormatException e)
				{
					logMonitor(e);

					action = Constants.BIND_ACTION_ERROR;
				}
				catch (Exception e)
				{
					throw e;
				}

				if (action == Constants.BIND_ACTION_SUCCESS)
				{
					dispatcher.successCount++;
				}
				else if (action == Constants.BIND_ACTION_EXPORT)
				{
					exportData(message);

					dispatcher.exportCount++;
				}
				else if (action == Constants.BIND_ACTION_ERROR)
				{
					exportError(message, error);

					dispatcher.errorCount++;
				}
				else if (action == Constants.BIND_ACTION_BYPASS)
				{
					dispatcher.bypassCount++;
				}

				Thread.sleep(1);

				for (int j = 0; j < 50; j++)
				{
					message = detachMessage();
					if (message == null)
					{
						Thread.sleep(10);
					}
					else
					{						
						break;
					}
				}
				if (message == null)
				{
					break;
				}
			}
		}
		catch (Exception e)
		{
			throw e;
		}
	}

	@Override
	public void run()
	{
		setRunning(true);

		try
		{
			beforeProcessSession();

			doProcessSession();
		}
		catch (Exception e)
		{
			logMonitor(e);
		}
		finally
		{
			try
			{
				afterProcessSession();
			}
			catch (Exception e)
			{
				logMonitor(e);
			}

			setRunning(false);
		}
	}
}
