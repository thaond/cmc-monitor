package com.crm.provisioning.thread;

import java.io.IOException;
import java.util.Vector;

import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.crm.kernel.queue.QueueFactory;
import com.crm.provisioning.impl.smpp.TransmitterMessage;
import com.crm.provisioning.message.CommandMessage;
import com.crm.thread.util.ThreadUtil;
import com.crm.util.AppProperties;
import com.fss.util.AppException;
import com.logica.smpp.util.Queue;

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2003
 * </p>
 * <p>
 * Company: FPT
 * </p>
 * 
 * @author DatPX
 * @version 1.0 Purpose : Compute file : R4
 */

public class SMPPThread extends CommandThread
{
	/**
	 * How you want to bind to the SMSC: transmitter (t), receiver (r) or
	 * transciever (tr). Transciever can both send messages and receive
	 * messages. Note, that if you bind as receiver you can still receive
	 * responses to you requests (submissions).
	 */
	public String	bindOption			= "";

	/**
	 * Indicates that the Session has to be asynchronous. Asynchronous Session
	 * means that when submitting a Request to the SMSC the Session does not
	 * wait for a response. Instead the Session is provided with an instance of
	 * implementation of ServerPDUListener from the smpp library which receives
	 * all PDUs received from the SMSC. It's application responsibility to match
	 * the received Response with sended Requests.
	 */
	public boolean	asynchronous		= false;

	/**
	 * The range of addresses the smpp session will serve.
	 */
	public byte		addressTON			= 1;
	public byte		addressNPI			= 1;
	public String	addressRange		= "9242";

	/**
	 * The range of addresses the smpp session will serve.
	 */
	public byte		sourceTON			= 1;
	public byte		sourceNPI			= 1;
	public String	sourceAddress		= "84983589789";

	/**
	 * The range of addresses the smpp session will serve.
	 */
	public byte		destTON				= 1;
	public byte		destNPI				= 1;
	public String	destAddress			= "9242";

	/*
	 * for information about these variables have a look in SMPP 3.4
	 * specification
	 */
	public String	systemType			= "SMPP";
	public String	serviceType			= "";

	public long		nextEnquireLink		= 0;
	public long		enquireInterval		= 10;
	public byte		registeredDelivery	= 0;

	public boolean	useConcatenated		= false;
	public int		orderTimeout		= 60000;
	public String	receivedQueueName	= "";
	
	public boolean	passiveEnquireLink	= false;
	public String logDatePattern = "'.'yyyyMMdd'.log'";
	
	protected Queue transmitterQueue = new Queue();

	// ////////////////////////////////////////////////////////////////////////
	// get directory parameters definition
	// Author: ThangPV
	// Modify DateTime: 19/02/2003
	// /////////////////////////////////////////////////////////////////////////
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Vector getDispatcherDefinition()
	{
		Vector vtReturn = new Vector();
		
		vtReturn.add(ThreadUtil.createBooleanParameter("passiveEnquireLink", "If false, send enquire link to remote host, if true, just send enquire link resp."));
		vtReturn.addAll(ThreadUtil.createSMPPParameter(this));
		vtReturn.add(ThreadUtil.createIntegerParameter("orderTimeout", "Time to live of order (s)."));
		vtReturn.addAll(ThreadUtil.createProvisioningParameter(this));
		vtReturn.addAll(ThreadUtil.createQueueParameter(this));
		vtReturn.add(ThreadUtil.createTextParameter("receivedQueueName", 100, "Incoming SMS queue."));
		vtReturn.addAll(ThreadUtil.createInstanceParameter(this));
		vtReturn.addAll(ThreadUtil.createLogParameter(this));
		vtReturn.add(ThreadUtil.createTextParameter("logDatePattern", 200, "Date pattern of rolling date to append in the end of file name for backing up, default ('.'yyyyMMdd'.log')."));

		return vtReturn;
	}

	// //////////////////////////////////////////////////////
	// Override
	// //////////////////////////////////////////////////////
	public void fillSMPPParameter() throws AppException
	{
		passiveEnquireLink = ThreadUtil.getBoolean(this, "passiveEnquireLink", false);
		asynchronous = ThreadUtil.getBoolean(this, "asynchronous", false);
		useConcatenated = ThreadUtil.getBoolean(this, "use-concatenated", false);
		bindOption = ThreadUtil.getString(this, "bindMode", true, "");

		addressRange = ThreadUtil.getString(this, "address-range", true, "");
		addressTON = Byte.parseByte(ThreadUtil.getString(this, "addr-ton", true, "1"));
		addressNPI = Byte.parseByte(ThreadUtil.getString(this, "addr-npi", true, "1"));

		sourceAddress = ThreadUtil.getString(this, "source-address", true, "");
		sourceTON = Byte.parseByte(ThreadUtil.getString(this, "source-ton", true, "1"));
		sourceNPI = Byte.parseByte(ThreadUtil.getString(this, "source-npi", true, "1"));

		destAddress = ThreadUtil.getString(this, "destination-address", true, "");
		destTON = Byte.parseByte(ThreadUtil.getString(this, "destination-ton", true, "1"));
		destNPI = Byte.parseByte(ThreadUtil.getString(this, "destination-npi", true, "1"));

		enquireInterval = ThreadUtil.getLong(this, "enquireInterval", 3000);

		registeredDelivery = Byte.parseByte(ThreadUtil.getString(this, "registeredDelivery", true, "0"));

		orderTimeout = ThreadUtil.getInt(this, "orderTimeout", 60000);
		logDatePattern = ThreadUtil.getString(this, "logDatePattern", false, "'.'yyyyMMdd'.log'");
		receivedQueueName = ThreadUtil.getString(this, "receivedQueueName", false, "queue/OrderRoute");
	}

	// //////////////////////////////////////////////////////
	// Override
	// //////////////////////////////////////////////////////
	public void fillDispatcherParameter() throws Exception
	{
		super.fillDispatcherParameter();

		fillSMPPParameter();
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
			super.initProvisioningParameters();

			AppProperties parameters = new AppProperties();

			parameters.setBoolean("asynchronous", asynchronous);
			parameters.setBoolean("useConcatenated", useConcatenated);
			parameters.setProperty("bindOption", bindOption);

			parameters.setProperty("addressRange", addressRange);
			parameters.setByte("addressTON", addressTON);
			parameters.setByte("addressNPI", addressNPI);

			parameters.setProperty("sourceAddress", sourceAddress);
			parameters.setByte("sourceTON", sourceTON);
			parameters.setByte("sourceNPI", sourceNPI);

			parameters.setProperty("destAddress", destAddress);
			parameters.setByte("destTON", destTON);
			parameters.setByte("destNPI", destNPI);

			parameters.setLong("enquireInterval", enquireInterval);
			parameters.setByte("registeredDelivery", registeredDelivery);
			parameters.setBoolean("passiveEnquireLink", passiveEnquireLink);
			provisioningPool.setParameters(parameters);
		}
		catch (Exception e)
		{
			throw e;
		}
	}

	private Logger	logger	= null;

	public SMPPThread()
	{
		super();
	}

	public Logger getLogger() throws IOException
	{
		if (logger == null)
		{
			//logger = Logger.getLogger(instanceClass);
			String fileName = "";
			Object param = getParameter("LogDir");
			if (param == null)
				fileName = "logs/" + bindOption;
			else
				fileName = param.toString();
				
			String logFile = ".log";
			
			logFile = bindOption + logFile;
			
			if (fileName.endsWith("/") | fileName.endsWith("\\"))
				fileName = fileName + logFile;
			else
				fileName = fileName + "/" + logFile;
			
			logger = Logger.getLogger(getThreadName());
			PatternLayout layout = new PatternLayout("%m\r\n");
			DailyRollingFileAppender dailyRollingAppender = new DailyRollingFileAppender(layout, fileName, logDatePattern);
			logger.removeAllAppenders();
			logger.addAppender(dailyRollingAppender);

			logger.setLevel(Level.DEBUG);
		}

		return logger;
	}

	@Override
	public void logToFile(String arg0)
	{
		if (bindOption == null || "".equals(bindOption))
		{
			super.logToFile(arg0);
		}
		else
		{
			try
			{
				getLogger();
				logger.debug(arg0);
			}
			catch (Exception e)
			{
				logToUser(e.getMessage());
			}
		}
	}
	
	public void attachTransmitter(TransmitterMessage transmitterMessage)
	{
		transmitterQueue.enqueue(transmitterMessage);
	}

	public TransmitterMessage detachTransmitter(TransmitterMessage transmitter)
	{
		return (TransmitterMessage)transmitterQueue.dequeue(transmitter);
	}

	public void sendTransmitterLog(CommandMessage transmitter)
	{
		if (transmitter != null)
		{
			try
			{
				QueueFactory.attachLocal(QueueFactory.COMMAND_LOG_QUEUE, transmitter);
//				QueueFactory.sendMessage(getQueueSession(), transmitter, QueueFactory.getQueue(QueueFactory.COMMAND_LOG_QUEUE));
			}
			catch (Exception e)
			{
				logMonitor(e);
			}
		}
	}
}
