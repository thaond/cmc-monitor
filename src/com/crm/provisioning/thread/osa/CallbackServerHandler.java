/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.crm.provisioning.thread.osa;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.CharsetUtil;

import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.crm.kernel.message.Constants;
import com.crm.kernel.queue.QueueFactory;
import com.crm.provisioning.message.OSACallbackMessage;
import com.crm.thread.DispatcherThread;
import com.crm.thread.util.ThreadUtil;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpHeaders.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

public class CallbackServerHandler extends SimpleChannelInboundHandler<Object>
{
	private CallbackServer		callbackServer	= null;
	private DispatcherThread	dispatcher		= null;

	private HttpRequest			request			= null;
	private Date				requestDate		= null;

	/** Buffer that stores the response content */
	private final StringBuilder	buffer			= new StringBuilder();
	private final TimeZone		tz				= TimeZone.getTimeZone("GMT:00");
	private final DateFormat	dfGMT			= DateFormat.getTimeInstance(DateFormat.LONG);

	public CallbackServerHandler(CallbackServer callbackServer)
	{
		super();

		this.callbackServer = callbackServer;
		if (callbackServer != null)
		{
			dispatcher = callbackServer.getDispatcher();
		}
	}

	public void debugMonitor(Object message)
	{
		callbackServer.debugMonitor(message);
	}

	public String getValue(String content, String tag)
	{
		int startIndex = content.indexOf("<" + tag + ">");
		int endIndex = content.indexOf("</" + tag + ">");

		if ((startIndex >= 0) && (endIndex > 0))
		{
			return content.substring(startIndex + tag.length() + 2, endIndex);
		}
		else
		{
			return "";
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception
	{
		ctx.flush();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg)
	{
		if (msg instanceof HttpRequest)
		{
			HttpRequest request = this.request = (HttpRequest) msg;

			if (is100ContinueExpected(request))
			{
				send100Continue(ctx);
			}

			buffer.setLength(0);
			requestDate = new Date();
			appendDecoderResult(buffer, request);
		}

		if (msg instanceof HttpContent)
		{
			HttpContent httpContent = (HttpContent) msg;

			ByteBuf content = httpContent.content();
			if (content.isReadable())
			{
				buffer.append(content.toString(CharsetUtil.UTF_8));

				appendDecoderResult(buffer, request);
			}

			if (msg instanceof LastHttpContent)
			{
				String actionType = parserContent();

				writeResponse((LastHttpContent) msg, ctx, actionType);
			}
		}
	}

	private static void appendDecoderResult(StringBuilder buf, HttpObject o)
	{
		DecoderResult result = o.getDecoderResult();

		if (result.isSuccess())
		{
			return;
		}
	}

	private String parserContent()
	{
		String actionType = "";

		try
		{
			String message = buffer.toString();

			String sessionId = "";
			String cause = "";

			int indexStart = message.indexOf("<sessionID>");

			if (indexStart > 0)
			{
				sessionId = getValue(message, "sessionID");
			}

			if (message.indexOf("directDebitAmountErr") >= 0)
			{
				actionType = "directDebitAmountErr";
				cause = getValue(message, "error");
			}
			else if (message.indexOf("directDebitAmountRes") >= 0)
			{
				actionType = "directDebitAmountRes";
				cause = Constants.SUCCESS;
			}
			else if (message.indexOf("directCreditAmountErr") >= 0)
			{
				actionType = "directCreditAmountErr";
				cause = getValue(message, "error");
			}
			else if (message.indexOf("directCreditAmountRes") >= 0)
			{
				actionType = "directCreditAmountRes";
				cause = Constants.SUCCESS;
			}
			if (cause.equals(""))
			{
				cause = getValue(message, "report");
			}
			// process directDebitAmountErr - DuyMB Add 12/07/2011 sua loi OSA.

			String nextChargingSequence = "";

			indexStart = message.indexOf("<requestNumberNextRequest>");

			if (indexStart > 0)
			{
				nextChargingSequence = getValue(message, "requestNumberNextRequest");
			}
			// Add end 12/07/2011 sua loi OSA.

			OSACallbackMessage callbackContent = new OSACallbackMessage();

			callbackContent.setSessionId(sessionId);
			callbackContent.setNextChargingSequence(nextChargingSequence);
			callbackContent.setActionType(actionType);
			callbackContent.setCause(cause);
			/*
			 * callbackContent.getParameters().setString("callbackReceiveDate",
			 * ThreadUtil.logTimestamp(requestDate));
			 * callbackContent.getParameters().setString("callbackParseDate",
			 * ThreadUtil.logTimestamp(new Date()));
			 */
			long delayTime = (requestDate != null) ? (System.currentTimeMillis() - requestDate.getTime()) : 0;

			if ((delayTime > 500))
			{
				StringBuilder sbLog = new StringBuilder();

				sbLog.append("receive: ");

				if (requestDate != null)
				{
					sbLog.append(ThreadUtil.logTimestamp(requestDate));
					sbLog.append(", ");
				}
				sbLog.append(sessionId);
				sbLog.append(", ");
				sbLog.append(actionType);
				sbLog.append(", ");
				sbLog.append(cause);

				dispatcher.debugMonitor(sbLog.toString());
			}

			/**
			 * Attach callbackContent to local queue located in dispatcher.
			 */
			QueueFactory.attachLocal(dispatcher.queueLocalName, callbackContent);
		}
		catch (Exception e)
		{
			dispatcher.logMonitor(e);
		}

		return actionType;
	}

	private boolean writeResponse(HttpObject currentObj, ChannelHandlerContext ctx, String actionType)
	{
		StringBuilder content = new StringBuilder();

		content.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		content.append("<SOAP-ENV:Envelope");
		content.append(" xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"");
		content.append(" xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\"");
		content.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
		content.append(" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"");
		content.append(" xmlns:osaxsd=\"http://www.csapi.org/osa/schema\"");
		content.append(" xmlns:osa=\"http://www.csapi.org/osa/wsdl\"");
		content.append(" xmlns:csxsd=\"http://www.csapi.org/cs/schema\"");
		content.append(" xmlns:cs=\"http://www.csapi.org/cs/wsdl\">");
		content.append("<SOAP-ENV:Body");
		content.append(" SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">");
		content.append("<cs:");
		content.append(actionType);
		content.append("Response></cs:");
		content.append(actionType);
		content.append("Response>");
		content.append("</SOAP-ENV:Body>");
		content.append("</SOAP-ENV:Envelope>");
		content.append("\r\n");

		// Decide whether to close the connection or not.
		boolean keepAlive = isKeepAlive(request);

		// Build the response object.
		FullHttpResponse response = new DefaultFullHttpResponse(
				HTTP_1_0, currentObj.getDecoderResult().isSuccess() ? OK : BAD_REQUEST,
				Unpooled.copiedBuffer(content.toString(), CharsetUtil.UTF_8));

		dfGMT.setTimeZone(tz);

		response.headers().set(DATE, dfGMT.format(new Date()));
		response.headers().set(CONTENT_TYPE, "text/htlm");
		response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
		response.headers().set(CONNECTION, keepAlive ? HttpHeaders.Values.KEEP_ALIVE : HttpHeaders.Values.CLOSE);

		// Write the response.
		ctx.write(response);

		return keepAlive;
	}

	private static void send100Continue(ChannelHandlerContext ctx)
	{
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_0, CONTINUE);
		ctx.write(response);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
	{
		cause.printStackTrace();
		ctx.close();
	}
}
