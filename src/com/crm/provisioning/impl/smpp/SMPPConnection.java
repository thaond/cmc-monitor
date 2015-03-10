/*
 * Copyright (c) 1996-2001
 * Logica Mobile Networks Limited
 * All rights reserved.
 *
 * This software is distributed under Logica Open Source License Version 1.0
 * ("Licence Agreement"). You shall use it and distribute only in accordance
 * with the terms of the License Agreement.
 *
 */
package com.crm.provisioning.impl.smpp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;

import com.crm.kernel.message.Constants;
import com.crm.provisioning.cache.ProvisioningConnection;
import com.crm.provisioning.impl.smpp.util.PushMessage;
import com.crm.provisioning.message.CommandMessage;
import com.crm.provisioning.thread.ProvisioningThread;
import com.crm.provisioning.thread.SMPPThread;
import com.crm.provisioning.util.ResponseUtil;
import com.crm.util.AppProperties;
import com.crm.util.GeneratorSeq;
import com.crm.util.StringUtil;
import com.logica.smpp.Data;
import com.logica.smpp.ServerPDUEvent;
import com.logica.smpp.ServerPDUEventListener;
import com.logica.smpp.Session;
import com.logica.smpp.TCPIPConnection;
import com.logica.smpp.pdu.Address;
import com.logica.smpp.pdu.AddressRange;
import com.logica.smpp.pdu.BindReceiver;
import com.logica.smpp.pdu.BindRequest;
import com.logica.smpp.pdu.BindResponse;
import com.logica.smpp.pdu.BindTransciever;
import com.logica.smpp.pdu.BindTransmitter;
import com.logica.smpp.pdu.DeliverSM;
import com.logica.smpp.pdu.EnquireLink;
import com.logica.smpp.pdu.EnquireLinkResp;
import com.logica.smpp.pdu.PDU;
import com.logica.smpp.pdu.Request;
import com.logica.smpp.pdu.Response;
import com.logica.smpp.pdu.SubmitSM;
import com.logica.smpp.pdu.SubmitSMResp;
import com.logica.smpp.pdu.Unbind;
import com.logica.smpp.pdu.UnbindResp;
import com.logica.smpp.pdu.WrongDateFormatException;
import com.logica.smpp.pdu.WrongLengthOfStringException;
import com.logica.smpp.util.ByteBuffer;

public class SMPPConnection extends ProvisioningConnection
{
	public static final int		SMS_TYPE_TEXT			= 0;
	public static final int		SMS_TYPE_WSP			= 1;
	public static String		ENC_GSM7BIT				= "X-Gsm7Bit";
	public static int			MAX_SMS_LENGTH			= 160;

	public boolean				hasError				= false;

	protected String			mstrNetCode				= "18001923";
	protected String			mstrServiceNumber		= "9242";

	/**
	 * This is the SMPP session used for communication with SMSC.
	 */
	protected Session			session					= null;

	/**
	 * If the application is bound to the SMSC.
	 */
	protected boolean			mblnBound				= false;

	/**
	 * How you want to bind to the SMSC: transmitter (t), receiver (r) or
	 * transciever (tr). Transciever can both send messages and receive
	 * messages. Note, that if you bind as receiver you can still receive
	 * responses to you requests (submissions).
	 */
	protected String			mstrBindOption			= "t";

	/**
	 * Indicates that the Session has to be asynchronous. Asynchronous Session
	 * means that when submitting a Request to the SMSC the Session does not
	 * wait for a response. Instead the Session is provided with an instance of
	 * implementation of ServerPDUListener from the smpp library which receives
	 * all PDUs received from the SMSC. It's application responsibility to match
	 * the received Response with sended Requests.
	 */
	protected boolean			mblnAsynchronous		= false;

	/**
	 * This is an instance of listener which obtains all PDUs received from the
	 * SMSC. Application doesn't have explicitly call Session's receive()
	 * function, all PDUs are passed to this application callback object. See
	 * documentation in Session, Receiver and ServerPDUEventListener classes
	 * form the SMPP library.
	 */
	protected PDUEventListener	pduListener				= null;

	/**
	 * The range of addresses the smpp session will serve.
	 */
	protected byte				addressTON				= 1;
	protected byte				addressNPI				= 1;
	protected String			mstrAddressRange		= "9242";
	protected AddressRange		addressRange			= new AddressRange();

	/**
	 * The range of addresses the smpp session will serve.
	 */
	protected byte				sourceTON				= 1;
	protected byte				sourceNPI				= 1;
	protected String			mstrSourceAddress		= "84983589789";
	protected Address			sourceAddress			= new Address();

	/**
	 * The range of addresses the smpp session will serve.
	 */
	protected byte				destTON					= 1;
	protected byte				destNPI					= 1;
	protected String			mstrDestAddress			= "9242";
	protected Address			destAddress				= new Address();

	/*
	 * for information about these variables have a look in SMPP 3.4
	 * specification
	 */
	protected String			mstrSystemType			= "SMPP";
	protected String			mstrServiceType			= "";
	protected boolean			useConcatenated			= false;

	/**
	 * If you attemt to receive message, how long will the application wait for
	 * data.
	 */
	// protected long timeout = Data.RECEIVE_BLOCKING;

	protected String			scheduleDeliveryTime	= "";
	protected String			validityPeriod			= "";
	protected String			strServiceNumber;
	protected String			messageId				= "";
	protected byte				esmClass				= 0;
	protected byte				protocolId				= 0;
	protected byte				priorityFlag			= 0;
	protected byte				registeredDelivery		= 0;
	protected byte				replaceIfPresentFlag	= 0;
	protected byte				dataCoding				= 0;
	protected byte				smDefaultMsgId			= 0;
	protected long				enquireInterval			= 1000;
	protected boolean			passiveEnquireLink		= false;
	protected Exception			lastError				= null;

	/**
	 * Initialises the application, lods default values for connection to SMSC
	 * and for various PDU fields.
	 * 
	 * @param mainThread
	 *            ManageableThread
	 * @param connection
	 *            Connection
	 */
	public SMPPConnection() throws Exception
	{
		super();
		setTimeout(Data.RECEIVE_BLOCKING);
	}

	@Override
	public void setParameters(AppProperties parameters) throws Exception
	{
		// TODO Auto-generated method stub
		super.setParameters(parameters);
		enquireInterval = parameters.getLong("enquireInterval", 3000);
		passiveEnquireLink = parameters.getBoolean("passiveEnquireLink", false);
	}

	@Override
	public boolean openConnection() throws Exception
	{
		try
		{
			String bindMode = parameters.getString("bindOption");

			useConcatenated = parameters.getBoolean("useConcatenated", false);
			if (bindMode.equalsIgnoreCase("transmitter"))
			{
				mstrBindOption = "t";
			}
			else if (bindMode.equalsIgnoreCase("receiver"))
			{
				mstrBindOption = "r";
			}
			else if (bindMode.equalsIgnoreCase("transciever"))
			{
				mstrBindOption = "tr";
			}
			else if (!mstrBindOption.equalsIgnoreCase("t") &&
					!mstrBindOption.equalsIgnoreCase("r") &&
					!mstrBindOption.equalsIgnoreCase("tr"))
			{
				logMonitor("The value of bind-mode parameter in " +
						"the configuration file " + "com/smpp.cfg" +
						" is wrong. " +
						"Setting the default");
				mstrBindOption = "t";
			}

			mblnAsynchronous = parameters.getBoolean("asynchronous", false);

			// address range
			mstrAddressRange = parameters.getString("addressRange", "");
			addressTON = parameters.getByte("addressTON", Byte.valueOf("1"));
			addressNPI = parameters.getByte("addressNPI", Byte.valueOf("1"));

			mstrSourceAddress = parameters.getString("sourceAddress", "");
			mstrServiceNumber = parameters.getString("serviceAddress", "");
			sourceTON = parameters.getByte("sourceTON", Byte.valueOf("1"));
			sourceNPI = parameters.getByte("sourceTON", Byte.valueOf("1"));

			mstrDestAddress = parameters.getString("destAddress", "");
			destTON = parameters.getByte("destTON", Byte.valueOf("1"));
			destNPI = parameters.getByte("destTON", Byte.valueOf("1"));

			registeredDelivery = parameters.getByte("registeredDelivery", Byte.valueOf("0"));

			bind();
		}
		catch (Exception e)
		{
			throw e;
		}

		return super.openConnection();
	}

	@Override
	public boolean closeConnection() throws Exception
	{
		unbind();

		return super.closeConnection();
	}

	@Override
	public boolean validate() throws Exception
	{
		// debugMonitor("Enquirelink interval " + enquireInterval);
		if (hasError)
			return false;
		// Date now = new Date();
		if (System.currentTimeMillis() - getLastCheck() < enquireInterval)
		{
			return true;
		}

		try
		{
			if (!passiveEnquireLink)
			{
				enquireLink();

				return true;
			}
			else
			{
				throw new Exception("Connection receive timeout. Too long wait for enquire link request.");
			}
		}
		catch (Exception e)
		{
			logMonitor(e);
		}
		finally
		{
			//setLastCheck();
		}

		return false;
	}

	// Sets attributes of <code>Address</code> to the provided values.
	public void setAddressParameter(String descr, Address address, byte ton, byte npi, String addr) throws Exception
	{
		address.setTon(ton);
		address.setNpi(npi);

		try
		{
			address.setAddress(addr);
		}
		catch (WrongLengthOfStringException e)
		{
			logMonitor("The length of " + descr + " parameter is wrong.");

			throw e;
		}
	}

	/**
	 * The first method called to start communication betwen an ESME and a SMSC.
	 * A new instance of <code>TCPIPConnection</code> is created and the IP
	 * address and port obtained from user are passed to this instance. New
	 * <code>Session</code> is created which uses the created
	 * <code>TCPIPConnection</code>. All the parameters required for a bind are
	 * set to the <code>BindRequest</code> and this request is passed to the
	 * <code>Session</code>'s <code>bind</code> method. If the call is
	 * successful, the application should be bound to the SMSC. See "SMPP
	 * Protocol Specification 3.4, 4.1 BIND Operation."
	 * 
	 * @see BindRequest
	 * @see BindResponse
	 * @see TCPIPConnection
	 * @see Session#bind(BindRequest)
	 * @see Session#bind(BindRequest,ServerPDUEventListener)
	 * @throws Exception
	 */
	public void bind() throws Exception
	{
		BindRequest request = null;
		BindResponse response = null;

		try
		{
			if (mblnBound)
			{
				this.unbind();
			}

			logMonitor("Connecting to SMSC ");
			initParameter();

			if (mstrBindOption.compareToIgnoreCase("t") == 0)
			{
				request = new BindTransmitter();
			}
			else if (mstrBindOption.compareToIgnoreCase("r") == 0)
			{
				request = new BindReceiver();
			}
			else if (mstrBindOption.compareToIgnoreCase("tr") == 0)
			{
				request = new BindTransciever();
			}
			else
			{
				throw new Exception(
						"Invalid bind mode, expected t, r or tr, got " +
								mstrBindOption + ". Operation canceled.");
			}

			TCPIPConnection connection = new TCPIPConnection(host, port);

			connection.setReceiveTimeout(timeout);
			session = new Session(connection);

			try
			{
				session.getDebug().deactivate();
			}
			catch (Exception e)
			{
				// dont care, just set debug object off
			}

			// set values
			request.setSystemId(userName);
			request.setPassword(password);

			request.setSystemType(mstrSystemType);
			// request.setInterfaceVersion((byte)0x34);
			request.setAddressRange(addressRange);

			// send the request
			logMonitor("Bind request " + request.debugString());
			if (mblnAsynchronous)
			{
				pduListener = new PDUEventListener(this, session);
				pduListener.setDispatcher(getDispatcher());
				setDispatcher(getDispatcher());
				response = session.bind(request, pduListener);
			}
			else
			{
				response = session.bind(request);
			}

			setLastCheck();

			// try
			// {
			//
			// session.getReceiver().setQueueWaitTimeout(timeout);
			// }
			// catch (Exception e)
			// {
			// // dont care, just set queue wait timeout.
			// }

			logMonitor("Bind response " + response.debugString());
			if (response.getCommandStatus() == Data.ESME_ROK)
			{
				mblnBound = true;
			}

			hasError = false;

			logMonitor("SMSC connection is established ");
		}
		catch (Exception e)
		{
			// event.write(e, "");
			// debug.write("Bind operation failed. " + e);

			logMonitor("Bind operation failed. " + e);
			throw e;
		}
		finally
		{
		}
	}

	/**
	 * Ubinds (logs out) from the SMSC and closes the connection.
	 * 
	 * See "SMPP Protocol Specification 3.4, 4.2 UNBIND Operation."
	 * 
	 * @see Session#unbind()
	 * @see Unbind
	 * @see UnbindResp
	 */
	public void unbind() throws Exception
	{
		// debug.enter(this, "SMPPTest.unbind()");
		try
		{
			if (!mblnBound)
			{
				logMonitor("Not bound, cannot unbind.");
				return;
			}

			// send the request
			// logMonitor("Going to unbind.");
			if (session.getReceiver().isReceiver())
			{
				logMonitor("It can take a while to stop.");
			}
			UnbindResp response = session.unbind();
			logMonitor("Unbind response " + response.debugString());
			mblnBound = false;
		}
		catch (IOException e)
		{
			logMonitor("Unbind operation failed. Connection closed");
		}
		catch (Exception e)
		{
			// event.write(e, "");
			// debug.write("Unbind operation failed. " + e);
			// e.printStackTrace();
			logMonitor("Unbind operation failed. " + e);
		}
		finally
		{
			if (session.getReceiver() != null)
			{
				session.getReceiver().stop();
			}
			mblnBound = false;
			hasError = false;
			// debug.exit(this);
		}
	}

	/**
	 * Submits request <br>
	 * Author : ThangPV <br>
	 * Created Date : 16/09/2004 <br>
	 * Edited By: NamTA <br>
	 * Edited Date : 12/10/2012
	 * 
	 * @param request
	 * @throws Exception
	 */
	public synchronized void submit(CommandMessage request) throws Exception
	{
		try
		{
			int smsType = Integer.parseInt(request.getResponseValue(ResponseUtil.SMS_TYPE, SMS_TYPE_TEXT + ""));

			switch (smsType)
			{
			case SMS_TYPE_WSP:
				submitWSPMessage(request);
				break;
			default:
				submitTextMessage(request);
				break;
			}
		}
		catch (Exception e)
		{
			hasError = true;
			throw e;
		}

	}

	/**
	 * Submits WSP message<br >
	 * Created by NamTA<br>
	 * Created Date: 12/10/2012
	 * 
	 * @param request
	 * @throws Exception
	 */
	public void submitWSPMessage(CommandMessage request) throws Exception
	{
		String href = request.getResponseValue(ResponseUtil.SMS_HREF, "");
		String text = request.getResponseValue(ResponseUtil.SMS_TEXT, "");

		ByteBuffer headerBuffer = new ByteBuffer();
		headerBuffer.appendByte((byte) 0x06); // Header length

		headerBuffer.appendByte((byte) 0x05); // Information Element Identifier
		headerBuffer.appendByte((byte) 0x04); // Total length of 2 port
		headerBuffer.appendShort((short) 0x0B84); // Destination port 2948
		headerBuffer.appendShort((short) 0x23F0); // Source port

		ByteBuffer contentBuffer = new ByteBuffer();

		try
		{

			// PushMessage pushMessage = new PushMessage(href, text,
			// ENC_GSM7BIT);
			PushMessage pushMessage = new PushMessage(href, text);
			contentBuffer.appendBytes(pushMessage.getSMSBytes());
		}
		catch (UnsupportedEncodingException uee)
		{
			PushMessage pushMessage = new PushMessage(href, text);
			contentBuffer.appendBytes(pushMessage.getSMSBytes());
		}

		submit(request, headerBuffer, contentBuffer, (byte) 0xf5);

	}

	/**
	 * Submits text message<br >
	 * Created by NamTA<br>
	 * Created Date: 12/10/2012
	 * 
	 * @param request
	 * @throws Exception
	 */
	public void submitTextMessage(CommandMessage request) throws Exception
	{
		String smsContent = StringUtil.nvl(request.getRequest(), "");

		if (smsContent.length() == 0)
		{
			return;
		}

		ByteBuffer contentBuffer = new ByteBuffer();

		try
		{
			contentBuffer.appendBytes(smsContent.getBytes(ENC_GSM7BIT));

		}
		catch (UnsupportedEncodingException e)
		{
			debugMonitor("Not support GSM7BIT");
			try
			{
				contentBuffer.appendBytes(smsContent.getBytes(Data.ENC_ASCII));
			}
			catch (UnsupportedEncodingException uee)
			{
				throw uee;
			}
		}

		submit(request, null, contentBuffer, this.dataCoding);
	}

	/**
	 * Creates single SubmitSM<br >
	 * Created by NamTA<br>
	 * Created Date: 12/10/2012
	 * 
	 * @param request
	 * @param headerBuffer
	 * @param contentBuffer
	 * @return
	 * @throws Exception
	 */
	private SubmitSM createSubmitSM(CommandMessage request, ByteBuffer headerBuffer, ByteBuffer contentBuffer, byte dataCoding)
			throws Exception
	{
		boolean useHeader = false;

		if (headerBuffer != null)
		{
			if (headerBuffer.length() > 0)
				useHeader = true;
		}
		SubmitSM submitRequest = createSubmitFromRequest(request, useHeader, dataCoding);

		ByteBuffer messageBuffer = new ByteBuffer();
		messageBuffer.appendBuffer(headerBuffer);
		messageBuffer.appendBuffer(contentBuffer);

		if (null != messageBuffer && messageBuffer.length() >
				MAX_SMS_LENGTH)
		{
			submitRequest.setMessagePayload(messageBuffer);
		}
		else
		{
			submitRequest.setShortMessageData(messageBuffer);
		}

		return submitRequest;
	}

	/**
	 * Creates concatenated SubmitSMs<br >
	 * Created by NamTA<br>
	 * Created Date: 12/10/2012
	 * 
	 * @param request
	 * @param headerBuffer
	 * @param contentBuffer
	 * @return
	 * @throws Exception
	 */
	private SubmitSM[] createConcatenatedSubmitSMs(CommandMessage request, ByteBuffer headerBuffer, ByteBuffer contentBuffer,
			byte dataCoding)
			throws Exception
	{
		int total = 0;
		if (headerBuffer == null)
		{
			headerBuffer = new ByteBuffer();
			/**
			 * Add 1 byte to header, to set header length parameter.
			 */
			headerBuffer.appendByte((byte) 0);
		}
		/**
		 * max content length = max sms length - header length - 6 (concatenated
		 * bytes)
		 */
		int maxContentLength = MAX_SMS_LENGTH - headerBuffer.length() - 6;

		total = (int) (contentBuffer.length() / maxContentLength);
		if (contentBuffer.length() % maxContentLength != 0)
		{
			total = total + 1;
		}

		SubmitSM[] submitRequests = new SubmitSM[total];

		int refNumber = 0;
		try
		{
			refNumber = GeneratorSeq.getNextSeq();
			refNumber = refNumber % 256;
		}
		catch (Exception e)
		{
		}

		for (int index = 1; index <= total; index++)
		{
			submitRequests[index - 1] = createSubmitFromRequest(request, true, dataCoding);
			SubmitSM submitRequest = submitRequests[index - 1];

			// user data header
			ByteBuffer udhBuffer = new ByteBuffer();
			udhBuffer.appendBuffer(headerBuffer);
			// dataBuffer.appendByte((byte) 0x05);

			/**
			 * Can use 8-bit or 16-bit for reference number.<br>
			 * To use 8 bit: IEI = 0, IEDL = 3, set 1 byte for reference number<br>
			 * To use 16 bit: IEI = 8, IEDL = 4, set 2 byte (short) for
			 * reference number<br>
			 * 
			 */
			// Information Element Identifier (IEI; concatenated short message,
			// 8-bit
			udhBuffer.appendByte((byte) 0x00);

			// Information Element Data Length (IEDL)
			udhBuffer.appendByte((byte) 0x03);

			// Information Element Data (concatenated short message reference
			// number)
			udhBuffer.appendByte((byte) refNumber);

			// Information Element Data (total number of concatenated messages
			// (0-255))
			udhBuffer.appendByte((byte) total);

			// Information Element Data (sequence number of current short
			// message)
			udhBuffer.appendByte((byte) index);

			byte[] udhBytes = udhBuffer.getBuffer();

			// update header length byte
			udhBytes[0] = (byte) (udhBytes.length - 1);

			ByteBuffer messageBody = new ByteBuffer();
			messageBody.appendBytes(udhBytes);

			if (index == total)
			{
				messageBody.appendBuffer(contentBuffer);
			}
			else
			{
				messageBody.appendBuffer(contentBuffer.removeBytes(maxContentLength));
			}
			// submitRequest.setShortMessage(new String(dataBuffer.getBuffer(),
			// encoding), encoding);

			submitRequest.setShortMessageData(messageBody);
		}

		return submitRequests;
	}

	/**
	 * Creates new SubmitSM instance from CommandMessage instance<br >
	 * Created by NamTA<br>
	 * Created Date: 12/10/2012
	 * 
	 * @param request
	 * @param useUserDataHeader
	 * @return
	 * @throws WrongLengthOfStringException
	 * @throws WrongDateFormatException
	 */
	private SubmitSM createSubmitFromRequest(CommandMessage request, boolean useUserDataHeader, byte dataCoding)
			throws WrongLengthOfStringException,
			WrongDateFormatException
	{
		SubmitSM submitRequest = new SubmitSM();

		submitRequest.setServiceType(mstrServiceType);

		if (request.getIsdn().equals(""))
		{
			submitRequest.setSourceAddr(mstrServiceNumber);
			request.setIsdn(mstrServiceNumber);
		}
		else
			submitRequest.setSourceAddr(request.getServiceAddress());

		submitRequest.setDestAddr(request.getIsdn());

		submitRequest.setReplaceIfPresentFlag(replaceIfPresentFlag);

		submitRequest.setScheduleDeliveryTime(scheduleDeliveryTime);
		submitRequest.setValidityPeriod(validityPeriod);
		submitRequest.setProtocolId(protocolId);
		submitRequest.setPriorityFlag(priorityFlag);
		submitRequest.setRegisteredDelivery((byte) registeredDelivery);
		submitRequest.setSmDefaultMsgId(smDefaultMsgId);
		submitRequest.setDataCoding(dataCoding);
		submitRequest.assignSequenceNumber(true);
		if (useUserDataHeader)
			submitRequest.setEsmClass((byte) Data.SM_UDH_GSM);
		else
			submitRequest.setEsmClass(esmClass);

		return submitRequest;
	}

	/**
	 * Submit message<br >
	 * Created by NamTA<br>
	 * Created Date: 12/10/2012
	 * 
	 * @param request
	 * @param headerBuffer
	 * @param contentBuffer
	 * @throws Exception
	 */
	private void submit(CommandMessage request, ByteBuffer headerBuffer, ByteBuffer contentBuffer, byte dataCoding)
			throws Exception
	{
		int msgLength = contentBuffer.length();

		if (headerBuffer != null)
			msgLength += headerBuffer.length();

		if (msgLength > MAX_SMS_LENGTH &
				useConcatenated)
		{
			SubmitSM[] submitRequests = createConcatenatedSubmitSMs(request, headerBuffer, contentBuffer, dataCoding);
			for (SubmitSM submitRequest : submitRequests)
			{
				getDispatcher().debugMonitor("Submit concatenated request " +
						submitRequest.debugString());
				submit(submitRequest);
				
				logTransmitter(request, submitRequest, 6);
			}
		}
		else
		{
			// prepare SMPP request
			SubmitSM submitRequest = createSubmitSM(request, headerBuffer, contentBuffer, dataCoding);
			getDispatcher().debugMonitor("Submit request " +
					submitRequest.debugString());
			request.setRequestTime(new Date());
			submit(submitRequest);
			
			logTransmitter(request, submitRequest, 0);
		}
	}

	/**
	 * Submit SubmitSM <br>
	 * Author: NamTA <br>
	 * Created Date: 23/04/2012
	 * 
	 * @param submitRequest
	 * @throws Exception
	 */
	private void submit(SubmitSM submitRequest) throws Exception
	{
		try
		{
			SubmitSMResp submitResponse = null;

			if (mblnAsynchronous)
			{
				session.submit(submitRequest);
			}
			else
			{
				synchronized (session)
				{
					submitResponse = session.submit(submitRequest);
				}
				logMonitor("Submit response " + submitResponse.debugString());
				messageId = submitResponse.getMessageId();
			}

		}
		catch (Exception e)
		{
			hasError = true;
			logMonitor("Submit operation failed. " + e);
			throw e;
		}
		finally
		{
		}
	}

	private long	lastChecking	= System.currentTimeMillis();

	public synchronized long getLastCheck()
	{
		return lastChecking;
	}
	
	public synchronized void setLastCheck()
	{
		lastChecking = System.currentTimeMillis();
	}
	/**
	 * Creates a new instance of <code>EnquireSM</code> class. This PDU is used
	 * to check that application level of the other party is alive. It can be
	 * sent both by SMSC and ESME.
	 * 
	 * See "SMPP Protocol Specification 3.4, 4.11 ENQUIRE_LINK Operation."
	 * 
	 * @see Session#enquireLink(EnquireLink)
	 * @see EnquireLink
	 * @see EnquireLinkResp
	 */
	public boolean enquireLink() throws Exception
	{
		try
		{

			EnquireLink request = new EnquireLink();
			EnquireLinkResp response = null;

			// getDispatcher().debugMonitor("Enquire Link request " +
			// request.debugString());

			logMonitor("Enquirelink request: " + request.debugString());
			if (mblnAsynchronous)
			{
				synchronized (session)
				{
					session.enquireLink(request);

				}

				/**
				 * Need to get enquirelink response?
				 */
				// response = (EnquireLinkResp)
				// pduListener.getEnquireLinkRespPDU(
				// (EnquireLinkResp) request.getResponse(), timeout);
			}
			else
			{
				synchronized (session)
				{
					response = session.enquireLink(request);
					debugMonitor("Reponse: async response received " + response.debugString());
				}
			}

			/**
			 * Need to get enquirelink response?
			 */
			// if (response == null)
			// {
			// throw new
			// Exception("Time out on receiving enquirelink response");
			// }
			//
			// logMonitor("Enquirelink response: " + response.debugString());
		}
		catch (Exception e)
		{
			hasError = true;
			session.close();
			logMonitor("Enquire Link operation failed. " + e);

			throw e;
		}
		finally
		{
		}

		return true;
	}

	/**
	 * Receives one PDU of any type from SMSC and prints it on the screen.
	 * Return null immediatly if this is transmitter mode.
	 * 
	 * @see Session#receive()
	 * @see Response
	 * @see ServerPDUEvent
	 */
	public CommandMessage receive() throws Exception
	{
		// debug.enter(this, "SMPPTest.receive()");
		try
		{
			if (mstrBindOption.equals("t"))
				return null;

			PDU pdu = null;

			if (mblnAsynchronous)
			{
				// pdu = pduListener.getPDUNoWait();
			}
			else
			{
				pdu = session.receive(timeout);
				if (pdu.canResponse())
				{
					Response response = ((Request) pdu).getResponse();
					// respond with default response
					// logMonitor("Going to send default response to request "
					logMonitor("Delivered response: " + response.debugString());
					try
					{
						session.respond(response);
					}
					catch (Exception ex)
					{
						hasError = true;
						logMonitor(ex);
					}
				}
			}

			if (pdu == null)
			{
				// logMonitor("No PDU received this time.");
			}
			else
			{
				try
				{
					if (pdu instanceof DeliverSM)
					{
						DeliverSM deliverSM = (DeliverSM) pdu;

						debugMonitor("Enqueue: " + deliverSM.debugString());
						byte dataCoding = deliverSM.getDataCoding();
						String message = "";
						
						try
						{
							switch(dataCoding)
							{
							case 8:
								message = deliverSM.getShortMessage(Data.ENC_UTF16);
								break;
							case 3:
								message = deliverSM.getShortMessage(Data.ENC_ISO8859_1);
								break;
							case 2:
								message = deliverSM.getShortMessage(Data.ENC_UTF8);
								break;
							case 0:
								message = deliverSM.getShortMessage(ENC_GSM7BIT);
								break;
							default:
								message = deliverSM.getShortMessage();
								break;
							}
						}
						catch (UnsupportedEncodingException e)
						{
							message = deliverSM.getShortMessage();
						}

						CommandMessage sms = new CommandMessage();

						sms.setResponseTime(new Date());
						
						sms.setChannel("SMS");
						sms.setProvisioningType(Constants.PROVISIONING_SMSC);
						sms.setIsdn(deliverSM.getSourceAddr().getAddress());
						sms.setServiceAddress(deliverSM.getDestAddr().getAddress());
						// sms.setShipTo(deliverSM.getSourceAddr().getAddress());

						sms.setRequest(message.toUpperCase().trim());
						sms.setResponse(deliverSM.getResponse().debugString());
						sms.setKeyword(message.toUpperCase().trim());

						sms.setRequestId(deliverSM.getSequenceNumber());

						return sms;
					}
				}
				catch (Exception e)
				{
					logMonitor(e);
					throw e;
				}
				finally
				{
				}
			}

			return null;
		}
		catch (Exception e)
		{
			// event.write(e, "");
			// debug.write("Receiving failed. " + e);
			logMonitor(e);
			throw e;
		}
	}

	/**
	 * Loads configuration parameters from the file with the given name. Sets
	 * private variable to the loaded values.
	 * 
	 * @throws Exception
	 */
	private void initParameter() throws Exception
	{
		addressRange.setTon(addressTON);
		addressRange.setNpi(addressNPI);

		try
		{
			addressRange.setAddressRange(mstrAddressRange);
		}
		catch (WrongLengthOfStringException e)
		{
			logMonitor("The length of address-range parameter is wrong.");
			throw e;
		}

		setAddressParameter("source-address", sourceAddress, sourceTON, sourceNPI, mstrSourceAddress);

		setAddressParameter("destination-address", destAddress, destTON, destNPI, mstrDestAddress);
	}

	public void setDispatcher(ProvisioningThread dispatcher)
	{
		super.setDispatcher(dispatcher);

		if (pduListener != null)
		{
			pduListener.setDispatcher(dispatcher);
		}
	}

	public void markError()
	{
		hasError = true;
	}

	public static CommandMessage getMessageFromPDU(PDU pdu)
	{
		if (pdu instanceof DeliverSM)
		{
			DeliverSM deliverSM = (DeliverSM) pdu;

			byte dataCoding = deliverSM.getDataCoding();
			String message = "";
			
			try
			{
				switch(dataCoding)
				{
				case 8:
					message = deliverSM.getShortMessage(Data.ENC_UTF16);
					break;
				case 3:
					message = deliverSM.getShortMessage(Data.ENC_ISO8859_1);
					break;
				case 2:
					message = deliverSM.getShortMessage(Data.ENC_UTF8);
					break;
				case 0:
					message = deliverSM.getShortMessage(ENC_GSM7BIT);
					break;
				default:
					message = deliverSM.getShortMessage();
					break;
				}
			}
			catch (UnsupportedEncodingException e)
			{
				message = deliverSM.getShortMessage();
			}

			CommandMessage sms = new CommandMessage();

			sms.setResponseTime(new Date());
			
			sms.setChannel("SMS");
			sms.setProvisioningType(Constants.PROVISIONING_SMSC);
			sms.setIsdn(deliverSM.getSourceAddr().getAddress());
			sms.setServiceAddress(deliverSM.getDestAddr().getAddress());
			// sms.setShipTo(deliverSM.getSourceAddr().getAddress());

			sms.setRequest(message.toUpperCase().trim());
			sms.setResponse(deliverSM.getResponse().debugString());
			sms.setKeyword(message.toUpperCase().trim());

			sms.setRequestId(deliverSM.getSequenceNumber());

			return sms;
		}
		else
			return null;
	}
	
	public void logTransmitter(CommandMessage request, SubmitSM submitRequest, int byteIndex)
	{
		try
		{
			TransmitterMessage transMessage = new TransmitterMessage();
			transMessage.setSequenceNumber(submitRequest.getSequenceNumber());
			CommandMessage transRequest = new CommandMessage();
			transRequest = request.clone();
			transRequest.setRequest(submitRequest.getSourceAddr().getAddress() + " - " + submitRequest.getShortMessage().substring(byteIndex));
			transMessage.setMessage(transRequest);
			((SMPPThread) getDispatcher()).attachTransmitter(transMessage);
		}
		catch (Exception e)
		{
		}
	}
}
