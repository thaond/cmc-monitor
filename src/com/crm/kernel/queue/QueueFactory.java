package com.crm.kernel.queue;

import java.io.FileInputStream;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.log4j.Logger;

import com.crm.kernel.message.Constants;
import com.crm.provisioning.message.CommandMessage;
import com.crm.provisioning.message.OSACallbackMessage;
import com.crm.provisioning.thread.ProductStatistic;
import com.crm.util.AppProperties;
import com.fss.util.AppException;
import com.fss.util.StringUtil;
import com.logica.smpp.pdu.PDU;

public class QueueFactory
{
	public static String									QUEUE_FACTORY			= "jms/VAS";

	public static String									ORDER_REQUEST_QUEUE		= "vas/OrderRoute";
	public static String									ORDER_RESPONSE_QUEUE	= "vas/OrderResponse";
	public static String									TCP_RESPONSE_QUEUE		= "vas/TCPResponse";
	public static String									COMMAND_STATISTIC_QUEUE	= "vas/CommandStatistic";
	public static String									COMMAND_ROUTE_QUEUE		= "vas/CommandRoute";

	public static String									COMMAND_LOG_QUEUE		= "vas/CommandLog";
	public static String									COMMAND_CALLBACK		= "vas/CommandCallback";
	
	public static String									ALARM_QUEUE				= "vas/Alarm";

	public static boolean									queueServerEnable		= true;
	public static Context									context					= null;
	public static QueueConnectionFactory					connectionFactory		= null;
	public static QueueConnection							queueConnection			= null;
	public static int										queueConnectionPoolSize	= 10;
	public static int										queuePoolWaitTime		= 1000;
	public static int										connectionMode			= Constants.QUEUE_CONNECTION_DEDICATED;
	public static boolean									queuePersistent			= false;
	public static int										queueDeliverTimeout		= 0;
	public static int										queueTotalPending		= 0;
	public static ConcurrentHashMap<String, Integer>		queueSnapshot			= new ConcurrentHashMap<String, Integer>();
	public static HashMap<String, Queue>					appQueues				= new HashMap<String, Queue>();

	public static ConcurrentHashMap<String, LocalQueue>		localQueues				= new ConcurrentHashMap<String, LocalQueue>();

	public static ConcurrentHashMap<Long, ProductStatistic>	chpProductStatistic		= new ConcurrentHashMap<Long, ProductStatistic>();

	public static ConcurrentHashMap<String, Object>			callbackListerner		= new ConcurrentHashMap<String, Object>();
	public static ConcurrentHashMap<String, Object>			callbackOSA				= new ConcurrentHashMap<String, Object>();
	public static ConcurrentHashMap<String, Object>			callbackOrder			= new ConcurrentHashMap<String, Object>();

	protected static com.logica.smpp.util.Queue				incomeSMSQueue			= new com.logica.smpp.util.Queue();
	protected static ConcurrentLinkedQueue<CommandMessage>	commandRoutingQueue		= new ConcurrentLinkedQueue<CommandMessage>();
	protected static ConcurrentLinkedQueue<CommandMessage>	commandLogQueue			= new ConcurrentLinkedQueue<CommandMessage>();
	protected static ConcurrentLinkedQueue<CommandMessage>	commandStatiticQueue	= new ConcurrentLinkedQueue<CommandMessage>();

	private static Object									mutex					= "mutex";
	public static Logger									log						= Logger.getLogger(QueueFactory.class);

	public static MessageProducer createProducer(QueueSession session, Queue queue, long timeout) throws JMSException
	{
		MessageProducer producer = session.createProducer(queue);

		if (!queuePersistent)
		{
			producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		}
		if (timeout > 0)
		{
			producer.setTimeToLive(timeout);
		}

		return producer;
	}

	public static MessageProducer createProducer(QueueSession session, Queue queue) throws Exception
	{
		return createProducer(session, queue, 0);
	}

	public static Message sendMessage(
			QueueSession session, Object object, Queue queue, String correlationId
			, String[] propsName, Object[] propsValue, long timeout) throws Exception
	{
		Message message = null;
		MessageProducer producer = null;

		try
		{
			if (object instanceof Message)
			{
				message = (Message) object;
			}
			else if (object instanceof Serializable)
			{
				message = createObjectMessage(session, (Serializable) object);
			}
			else
			{
				throw new Exception("invalid-message-object");
			}

			if (!correlationId.equals(""))
			{
				message.setJMSCorrelationID(correlationId);
			}

			if ((propsName != null) && (propsValue != null))
			{
				if (propsName.length > 0)
				{
					for (int i = 0; i < propsName.length; i++)
					{
						if (propsValue[i] instanceof String)
						{
							message.setStringProperty(propsName[i], (String) propsValue[i]);
						}
						else if (propsValue[i] instanceof Byte)
						{
							message.setByteProperty(propsName[i], ((Byte) propsValue[i]).byteValue());
						}
						else if (propsValue[i] instanceof Integer)
						{
							message.setIntProperty(propsName[i], ((Integer) propsValue[i]).intValue());
						}
						else if (propsValue[i] instanceof Boolean)
						{
							message.setBooleanProperty(propsName[i], ((Boolean) propsValue[i]).booleanValue());
						}
						else if (propsValue[i] instanceof Short)
						{
							message.setShortProperty(propsName[i], ((Short) propsValue[i]).shortValue());
						}
						else if (propsValue[i] instanceof Long)
						{
							message.setLongProperty(propsName[i], ((Long) propsValue[i]).longValue());
						}
						else if (propsValue[i] instanceof Double)
						{
							message.setDoubleProperty(propsName[i], ((Double) propsValue[i]).doubleValue());
						}
						else if (propsValue[i] instanceof Float)
						{
							message.setFloatProperty(propsName[i], ((Float) propsValue[i]).floatValue());
						}
						else
						{
							message.setObjectProperty(propsName[i], propsValue[i]);
						}
					}
				}
			}

			producer = createProducer(session, queue, timeout);
			producer.send(message);

			if (session.getTransacted())
			{
				session.commit();
			}
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			QueueFactory.closeQueue(producer);
		}

		return message;
	}

	public static Message sendMessage(QueueSession session, Object object, Queue queue) throws Exception
	{
		return sendMessage(session, object, queue, 0);
	}

	public static Message sendMessage(QueueSession session, Object object, Queue queue, long timeout) throws Exception
	{
		return sendMessage(session, object, queue, "", null, null, timeout);
	}

	public static com.logica.smpp.util.Queue getIncomeSMSQueue()
	{
		return incomeSMSQueue;
	}

	public static int getIncomeSMSQueueSize()
	{
		return incomeSMSQueue.size();
	}

	public static void attachIncomeSMSQueue(PDU pdu)
	{
		incomeSMSQueue.enqueue(pdu);
	}

	public static PDU detachIncomeSMSQueue()
	{
		return (PDU) incomeSMSQueue.dequeue();
	}

	public static LocalQueue getLocalQueue(String queueName)
	{
		LocalQueue queue = localQueues.get(queueName);

		if (queue == null)
		{
			queue = new LocalQueue(queueName);

			boolean checkPending = true;

			if (queueName.equals(COMMAND_LOG_QUEUE)
				|| queueName.equals(COMMAND_STATISTIC_QUEUE)
				|| queueName.equals(ORDER_RESPONSE_QUEUE))
			{
				checkPending = false;
			}

			queue.setCheckPending(checkPending);

			localQueues.put(queueName, queue);
		}

		return queue;
	}

	public static int getLocalQueueSize(String queueName)
	{
		LocalQueue queue = localQueues.get(queueName);

		if (queue == null)
		{
			return 0;
		}

		return queue.getSize();
	}

	public static int getTotalLocalPending()
	{
		int totalPending = 0;

		for (String key : localQueues.keySet())
		{
			LocalQueue localQueue = getLocalQueue(key);

			if (localQueue.isCheckPending())
			{
				totalPending += localQueue.getSize();
			}
		}
		return totalPending;
	}

	public static int getTotalRemotePending(String queueNames)
	{
		int totalPending = 0;

		if (!queueServerEnable)
		{
			return totalPending;
		}

		String[] checklist = StringUtil.toStringArray(queueNames, ";");

		for (String key : queueSnapshot.keySet())
		{
			for (int j = 0; j < checklist.length; j++)
			{
				if (checklist[j].equals(key))
				{
					totalPending += queueSnapshot.get(key);
					break;
				}
			}
		}

		return totalPending;
	}

	public static boolean isOverload(String queueName)
	{
		if (!queueName.equals(""))
		{
			return getLocalQueue(queueName).isOverload();
		}

		return false;
	}

	public static void attachLocal(String queueName, Object request) throws Exception
	{
		if (request == null)
		{
			return;
		}

		LocalQueue localQueue = getLocalQueue(queueName);

		if (request instanceof CommandMessage)
		{
			localQueue.attach(((CommandMessage) request).clone());
		}
		else
		{
			localQueue.attach(request);
		}

		if (localQueue.isCheckPending())
		{
		}
	}

	public static Object detachLocal(String queueName)
	{
		LocalQueue localQueue = getLocalQueue(queueName);

		Object message = null;

		message = localQueue.detach();

		if ((message != null) && localQueue.isCheckPending())
		{
		}

		return message;
	}

	public static void attachCommandRouting(Object request) throws Exception
	{
		attachLocal(COMMAND_ROUTE_QUEUE, request);
	}

	public static Queue getQueue(String queueName) throws Exception
	{
		Queue queue = null;

		try
		{
			if (queueName.equals(""))
			{
				return null;
			}
			
			queue = appQueues.get(queueName);

			if (queue == null)
			{
				if (context == null)
				{
					initContext();
				}
				
				queue = (Queue) context.lookup(queueName);

				appQueues.put(queueName, queue);
			}
		}
		catch (javax.naming.NamingException e)
		{
			throw new AppException("queue-not-found", queueName);
		}
		catch (Exception e)
		{
			throw e;
		}

		return queue;
	}

	public static void closeQueue(Object object)
	{
		if (object == null)
		{
			return;
		}

		try
		{
			if (object instanceof QueueBrowser)
			{
				((QueueBrowser) object).close();
			}
			else if (object instanceof MessageConsumer)
			{
				((MessageConsumer) object).close();
			}
			else if (object instanceof MessageProducer)
			{
				((MessageProducer) object).close();
			}
			else if (object instanceof QueueReceiver)
			{
				((QueueReceiver) object).close();
			}
			else if (object instanceof QueueSession)
			{
				((QueueSession) object).close();
			}
			else if (object instanceof QueueConnection)
			{
				QueueConnection connection = (QueueConnection) object;

				connection.stop();
				connection.close();
			}
		}
		catch (Exception e)
		{
			log.info("safeClose:", e);
		}
	}

	public static Message createObjectMessage(QueueSession queueSession, Object object) throws Exception
	{
		if (object instanceof Message)
		{
			return (Message) object;
		}
		else if (object instanceof Serializable)
		{
			try
			{
				ObjectMessage message = queueSession.createObjectMessage();

				message.setObject((Serializable) object);

				if (object instanceof CommandMessage)
				{
					message.setJMSCorrelationID(((CommandMessage) object).getCorrelationID());
				}

				if (object instanceof OSACallbackMessage)
				{
					message.setJMSCorrelationID(((OSACallbackMessage) object).getSessionId());
				}

				return message;
			}
			catch (Exception e)
			{
				throw e;
			}
		}

		throw new Exception("invalid-message-body");
	}

	public static Object getContentMessage(Message message) throws Exception
	{
		if (message == null)
		{
			return null;
		}
		else if (message instanceof ObjectMessage)
		{
			return ((ObjectMessage) message).getObject();
		}
		else
		{
			return message;
		}
	}

	public static void emptyQueue(QueueSession queueSession, String queueName) throws Exception
	{
		QueueReceiver receiver = null;

		try
		{
			receiver = queueSession.createReceiver(getQueue(queueName));

			while (receiver.receiveNoWait() != null)
			{

			}
		}
		finally
		{
			closeQueue(receiver);
		}
	}

	public static int getSnapshotSize(String queueNames) throws Exception
	{
		int size = 0;

		String[] queues = StringUtil.toStringArray(queueNames, ";");

		for (int j = 0; j < queues.length; j++)
		{
			if (queues[j].equals(""))
			{
				continue;
			}

			Integer queueSize = queueSnapshot.get(queues[j]);

			if (queueSize != null)
			{
				size += queueSize;
			}
			else
			{
				size += 1000;
			}
		}

		return size;
	}

	public static int getQueueSize(QueueSession queueSession, String queueNames) throws Exception
	{
		int size = 0;

		String[] queues = StringUtil.toStringArray(queueNames, ";");

		for (int j = 0; j < queues.length; j++)
		{
			if (queues[j].equals(""))
			{
				continue;
			}

			size += getQueueSize(queueSession, getQueue(queues[j]));
		}

		return size;
	}

	public static int getQueueSize(QueueSession queueSession, Queue queue) throws Exception
	{
		int count = 0;

		QueueBrowser browser = null;

		try
		{
			browser = queueSession.createBrowser(queue);

			Enumeration<?> messagesOnQ = browser.getEnumeration();

			while (messagesOnQ.hasMoreElements())
			{
				messagesOnQ.nextElement();

				count++;
			}

			log.debug("number of messages on input Q= " + count);
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			closeQueue(browser);
		}

		return count;
	}

	public static QueueConnection createQueueConnection() throws Exception
	{
		initContext();

		QueueConnection connection = null;

		try
		{
			if (connectionMode == Constants.QUEUE_CONNECTION_DEDICATED)
			{
				connection = connectionFactory.createQueueConnection();

				connection.start();
			}
			else
			{
				connection = queueConnection;
			}
		}
		catch (Exception e)
		{
			closeQueue(connection);

			throw e;
		}

		return connection;
	}

	public static synchronized void initContext() throws Exception
	{
		if ((context != null) && (connectionFactory != null))
		{
			return;
		}

		synchronized (mutex)
		{
			getLocalQueue(COMMAND_LOG_QUEUE).setCheckPending(false);
			getLocalQueue(COMMAND_STATISTIC_QUEUE).setCheckPending(false);

			appQueues.clear();

			try
			{
				if (context == null)
				{
					// get server host
					AppProperties configProvider = new AppProperties();

					configProvider.loadFromFile("ServerConfig.txt");

					queueConnectionPoolSize = configProvider.getInteger("connectionFactory", 100);

					QUEUE_FACTORY = configProvider.getString("queue.factory", "jms/VAS");

					ORDER_REQUEST_QUEUE = configProvider.getString("queue.orderRoute", "vas/OrderRoute");
					ORDER_RESPONSE_QUEUE = configProvider.getString("queue.orderResponse", "vas/OrderResponse");
					COMMAND_STATISTIC_QUEUE = configProvider.getString("queue.commandStatistic", "vas/CommandStatistic");

					COMMAND_ROUTE_QUEUE = configProvider.getString("queue.commandRoute", "vas/CommandRoute");
					COMMAND_LOG_QUEUE = configProvider.getString("queue.commandLog", "vas/CommandLog");
					COMMAND_CALLBACK = configProvider.getString("queue.commandCallback", "vas/CommandCallback");
					
					ALARM_QUEUE = configProvider.getString("queue.alarm", "vas/Alarm");

					// connection mode
					String mode = configProvider.getString("queue.connection", "dedicated");

					if (mode.equalsIgnoreCase("dedicated"))
					{
						connectionMode = Constants.QUEUE_CONNECTION_DEDICATED;
					}
					else
					{
						connectionMode = Constants.QUEUE_CONNECTION_SHARING;
					}

					// get context properties
					Properties properties = new Properties();

					properties.load(new FileInputStream("jndi.properties"));

					System.setProperty("org.omg.CORBA.ORBInitialPort", properties.getProperty("org.omg.CORBA.ORBInitialPort"));
					System.setProperty("org.omg.CORBA.ORBInitialHost", properties.getProperty("org.omg.CORBA.ORBInitialHost"));

					context = new InitialContext(properties);
				}

				// lookup the queue connection factory
				connectionFactory = (QueueConnectionFactory) context.lookup(QUEUE_FACTORY);

				if (connectionMode == Constants.QUEUE_CONNECTION_SHARING)
				{
					queueConnection = connectionFactory.createQueueConnection();

					queueConnection.start();
				}
			}
			catch (Exception e)
			{
				context = null;
				connectionFactory = null;

				throw e;
			}
		}
	}

	public static void resetContext() throws Exception
	{
		try
		{
			if (queueConnection != null)
			{
				queueConnection.stop();

				queueConnection.close();
			}
		}
		finally
		{
			context = null;

			connectionFactory = null;
		}
	}

	public static void initLocalQueue() throws Exception
	{
		getLocalQueue(COMMAND_LOG_QUEUE).setCheckPending(false);
		getLocalQueue(COMMAND_STATISTIC_QUEUE).setCheckPending(false);

		appQueues.clear();

		try
		{
			// get server host
			AppProperties configProvider = new AppProperties();

			configProvider.loadFromFile("ServerConfig.txt");

			ORDER_REQUEST_QUEUE = configProvider.getString("queue.orderRoute", "vas/OrderRoute");
			ORDER_RESPONSE_QUEUE = configProvider.getString("queue.orderResponse", "vas/OrderResponse");
			COMMAND_STATISTIC_QUEUE = configProvider.getString("queue.commandStatistic", "vas/CommandStatistic");

			COMMAND_ROUTE_QUEUE = configProvider.getString("queue.commandRoute", "vas/CommandRoute");
			COMMAND_LOG_QUEUE = configProvider.getString("queue.commandLog", "vas/CommandLog");
			COMMAND_CALLBACK = configProvider.getString("queue.commandCallback", "vas/CommandCallback");
			
			ALARM_QUEUE = configProvider.getString("queue.alarm", "vas/Alarm");
		}
		catch (Exception e)
		{
			throw e;
		}
	}

}
