package com.crm.provisioning.thread;

import java.util.Calendar;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.crm.provisioning.cache.CommandEntry;
import com.crm.provisioning.cache.ProvisioningFactory;

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

public class CommandThread extends ProvisioningThread
{
	public boolean		isLogin		= false;
	
	public Object		syncLogin 	= "syncLogin";
	
	public ConcurrentHashMap<Long, CommandStatistic>	commandStatistic		= null;

	public void updateStatistic() throws Exception
	{
		try
		{
			Set<Long> keys = commandStatistic.keySet();

			Iterator<Long> iterator = keys.iterator();
			Calendar now = Calendar.getInstance();

			while (iterator.hasNext())
			{
				Long commandId = iterator.next();

				CommandStatistic cmdStatistic = commandStatistic.get(commandId);

				synchronized (cmdStatistic)
				{
					int total = cmdStatistic.getFailure() + cmdStatistic.getSuccess();

					if (total > 0)
					{
//						logMonitor(cmdStatistic.toString() + ", " + statisticInterval);

						try
						{
							CommandEntry command = ProvisioningFactory.getCache().getCommand(commandId);

							cmdStatistic.setAlias(command.getAlias());
							cmdStatistic.setCommandId(commandId);
							cmdStatistic.setStartTime(now);
							cmdStatistic.setSuccess(0);
							cmdStatistic.setFailure(0);

							commandStatistic.put(commandId, cmdStatistic);
						}
						catch (Exception e)
						{
							debugMonitor(e);
							debugMonitor("Fail to update database " + commandStatistic.toString());
						}
					}
				}
			}

			lastStatistic = System.currentTimeMillis();
		}
		catch (Exception e)
		{
			throw e;
		}
	}

	public void beforeProcessSession() throws Exception
	{
		try
		{
			super.beforeProcessSession();

			if (commandStatistic == null)
			{
				commandStatistic = new ConcurrentHashMap<Long, CommandStatistic>();
			}
		}
		catch (Exception e)
		{
			throw e;
		}
	}
}
