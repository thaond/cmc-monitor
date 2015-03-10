package com.crm.provisioning.impl.smpp.util;

import java.io.IOException;

import com.crm.kernel.queue.QueueFactory;
import com.crm.provisioning.impl.smpp.SMPPConnection;
import com.crm.provisioning.message.CommandMessage;
import com.crm.provisioning.thread.SMPPThread;
import com.crm.thread.DispatcherThread;

import com.logica.smpp.*;
import com.logica.smpp.pdu.*;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2004</p>
 *
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */

/**
 * Implements simple PDU listener which handles PDUs received from SMSC. It puts
 * the received requests into a queue and discards all received responses.
 * Requests then can be fetched (should be) from the queue by calling to the
 * method <code>getRequestEvent</code>.
 * 
 * @see Queue
 * @see ServerPDUEvent
 * @see ServerPDUEventListener
 * @see SmppObject
 */
public class PDUEventListener extends SmppObject implements ServerPDUEventListener
{
	private Session				session		= null;
	private SMPPConnection		connection	= null;

	private DispatcherThread	dispatcher	= null;

	public PDUEventListener(SMPPConnection connection, Session session)
	{
		this.session = session;
		this.connection = connection;
	}

	// //////////////////////////////////////////////////////
	// process file
	// Author : ThangPV
	// Created Date : 16/09/2004
	// //////////////////////////////////////////////////////
	public void logMonitor(Object message)
	{
		if (dispatcher != null)
		{
			dispatcher.logMonitor(message);
		}
	}

	// //////////////////////////////////////////////////////
	// process file
	// Author : ThangPV
	// Created Date : 16/09/2004
	// //////////////////////////////////////////////////////
	public void debugMonitor(String message)
	{
		if (dispatcher != null)
			dispatcher.debugMonitor(message);
	}

	public void handleEvent(ServerPDUEvent event)
	{
		PDU pdu = event.getPDU();

		String sourceAddress = "";

		if (pdu == null)
		{
			return;
		}

		connection.setLastCheck();

		if (pdu.isRequest())
		{

			debugMonitor("Has incoming message: " + pdu.debugString());

			try
			{
				if (pdu.canResponse())
				{
					Response response = ((Request) pdu).getResponse();
					// respond with default response
					// logMonitor("Going to send default response to request "

					DispatcherThread dispatcher = getDispatcher();

					boolean displayDebug = ((dispatcher != null) && dispatcher.displayDebug);

					try
					{

						final java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("dd/MM HH:mm:ss:SSS");

						String strLog = fmt.format(new java.util.Date());

						String smsLog = response.debugString();

						// if (displayDebug)
						// {
						// dispatcher.logToFile(strLog +
						// " Begin send response: " + sourceAddress + " - " +
						// smsLog + "\r\n");
						// }
						// debugMonitor("Begin send response: " +
						// response.debugString());

						try
						{
							// session.respond(response);
							synchronized (session)
							{
								session.getTransmitter().send(response);
							}
						}
						catch (IOException ioe)
						{
							connection.markError();
							throw ioe;
						}

						strLog = strLog + " - " + fmt.format(new java.util.Date());
						strLog = strLog + " Sent response:  " + sourceAddress + " - " + smsLog + "\r\n";

						if (displayDebug)
						{
							dispatcher.logToFile(strLog);
							// dispatcher.logToFile(strLog +
							// " End send response: " + sourceAddress + " - " +
							// smsLog + "\r\n");
						}
						// debugMonitor("End send response: " +
						// response.debugString());
					}
					catch (Exception ex)
					{
						dispatcher.logToFile("Failed to send response: " + sourceAddress + " - " + response.debugString()
									+ "\r\n");
						throw ex;
					}
				}

				if (pdu instanceof DeliverSM)
				{
					if (((DeliverSM) pdu).getShortMessage() == null)
					{
						try
						{
							((DeliverSM) pdu).setShortMessage("");
						}
						catch (Exception e)
						{
						}
					}

					sourceAddress = ((DeliverSM) pdu).getSourceAddr().getAddress();

					// receivedQueue.enqueue(pdu);
					// receivedQueue.notify();

					try
					{
						CommandMessage message = SMPPConnection.getMessageFromPDU(pdu);

						QueueFactory.attachLocal(getDispatcher().queueLocalName, message);
					}
					catch (Exception ex)
					{
						logMonitor(ex);
					}
				}

			}
			catch (Exception e)
			{
				logMonitor(e);
			}
		}
		else if (pdu.isResponse())
		{

			if (pdu instanceof EnquireLinkResp)
			{
				logMonitor("Enquirelink response: " + pdu.debugString());

				/**
				 * Need to get enquirelink response?
				 */
				// synchronized (enquiredLinkQueue)
				// {
				// //debugMonitor("Has incoming response");
				// enquiredLinkQueue.enqueue(pdu);
				// enquiredLinkQueue.notifyAll();
				// }
			}
			else
				debugMonitor("Reponse: async response received " + pdu.debugString());
		}
		else
		{
			debugMonitor("pdu of unknown class (not request nor response) received, discarding " + pdu.debugString());
		}
	}

	public void setDispatcher(DispatcherThread dispatcher)
	{
		this.dispatcher = dispatcher;
	}

	public SMPPThread getDispatcher()
	{
		return (SMPPThread) dispatcher;
	}
}
