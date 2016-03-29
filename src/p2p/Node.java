package p2p;

import java.lang.reflect.Constructor;
import java.net.Socket;
import java.util.LinkedList;
import communication.CommunicationChanel;
import communication.ExceptionUnknownCommunicationChanelType;
import communication.Logger;







public class Node
{
// ---------------------------------
// Attributes
// ---------------------------------
	public static final int		maxNbrNodes						= 160;// TODO (int) Math.pow(2, 160);
	public static final String	COMMUNICATION_CHANEL_SOCKET		= "communication.CommunicationChanel_Socket";
	public static final String	COMMUNICATION_CHANEL_RABBITMQ	= "communication.CommunicationChanel_RabbitMQ";

	public static final String	THREAD_PREVIOUS_RECEIVE_MSG		= "receiveMsg";
	public static final String	THREAD_PREVIOUS_JOIN			= "join";
	public static final String	THREAD_PREVIOUS_INSERT			= "insert";

	public static final String	THREAD_EXTERNAL_SET_NEXT		= "setNext";
	public static final String	THREAD_EXTERNAL_SEND_MSG		= "sendMsg";
	public static final String	THREAD_EXTERNAL_GET_PREVIOUS	= "getPrevious";
	public static final String	THREAD_EXTERNAL_GET_NEXT		= "getNext";
	public static final String	THREAD_EXTERNAL_JOIN			= "join";
	public static final String	THREAD_EXTERNAL_INSERT			= "insert";
	public static final String	THREAD_EXTERNAL_IS_RESPONSIBLE_FOR_KEY = "isResponsibleForKey";
	public static final String	THREAD_EXTERNAL_GET_VALUE		= "getValue";

	private int					nodeId				= -1;
	private NodeInternalHashTable internalHashTable	= null;
	private int					previousId			= -1;
	private ThreadPrevious		threadPrevious		= null;
	private int					nextId				= -1;
	private CommunicationChanel	nextChanel			= null;
	private Constructor<?>		communicationChanelConstructor;
	private Logger				logger;

// ---------------------------------
// Builder
// ---------------------------------
	public Node(int nodeId, String communicationChanelType) throws ExceptionUnknownCommunicationChanelType
	{
		this.nodeId				= nodeId; //TODO NodeInternalHashTable.hash(""+nodeId);
		this.logger				= new Logger(""+nodeId);

		this.initCommunicationChanelType(communicationChanelType);

		this.threadPrevious = new ThreadPrevious(this, nodeId, logger, communicationChanelConstructor);
		ThreadExternal.initThreadExternal(this, nodeId, logger, communicationChanelConstructor);
	}

// ---------------------------------
// Local method
// ---------------------------------
	public void setPrevious(int previousId)
	{
		this.previousId			= previousId;
//TODO change this by un update of the existing internal hash table
		if (previousId >= 0)
			this.internalHashTable	= new NodeInternalHashTable(previousId, nodeId);
		else
			this.internalHashTable	= null;
	}

	public boolean setNext(int nextId, String nextIP)
	{
		if (nextId == this.nextId)
			return true;
		if (nextId < 0)
			return false;

		Socket socket = null;
		CommunicationChanel chanel = null;
		boolean test;
		try
		{
			socket	= new Socket(nextIP, ThreadPrevious.getPort(nextId));
			chanel	= (CommunicationChanel) communicationChanelConstructor.newInstance(socket, true, true);
			test	= chanel.writeLine(""+this.nodeId);
			if (!test) throw new Exception();
			this.nextChanel	= chanel;
			this.nextId		= nextId;
			logger.write("\tnext = " + this.nextId);
			return true;
		}
		catch(Exception e)
		{
e.printStackTrace();
			logger.write("\t**** Fail ****\n");
			return false;
		}

	}

	public boolean sendMessage(String msg)
	{
		if (this.nextChanel == null)
			return false;

		boolean res = true;
		res &= this.nextChanel.writeLine(Node.THREAD_PREVIOUS_RECEIVE_MSG);
		res &= this.nextChanel.writeLine(msg);
		return res;
	}

	public int getNext()
	{
		return this.nextId;
	}

	public int getPrevious()
	{
		return this.previousId;
	}

	public void join(int newNodeId, String newNodeIP)
	{
System.out.println("My id is " + this.nodeId);
		if (newNodeId == this.nodeId)		throw new RuntimeException();
		if (newNodeId >=Node.maxNbrNodes)	throw new RuntimeException();
		if (this.isResponsibleForKey(""+newNodeId))
		{
			Socket socket = null;
			CommunicationChanel chanel = null;
			boolean test;
			Boolean res;
			try
			{
				this.threadPrevious.closePreviousChanel();

				// Set the next of my previous
				socket = new Socket(newNodeIP, ThreadExternal.getPort(previousId));
				chanel = (CommunicationChanel) communicationChanelConstructor.newInstance(socket, true, true);
				test = true;
				test &= chanel.writeLine(THREAD_EXTERNAL_SET_NEXT);
				test &= chanel.writeLine(""+newNodeId);
				test &= chanel.writeLine(newNodeIP);
				if (!test) throw new Exception();
				res = chanel.readBoolean();
				if ((res == null) || (res == false))
					throw new Exception();

				// Set the next of the new node
				socket = new Socket(newNodeIP, ThreadExternal.getPort(newNodeId));
				chanel = (CommunicationChanel) communicationChanelConstructor.newInstance(socket, true, true);
				test = true;
				test &= chanel.writeLine(THREAD_EXTERNAL_SET_NEXT);
				test &= chanel.writeLine(""+this.nodeId);
//TODO replace by my current ip
				test &= chanel.writeLine("localhost");
				if (!test) throw new Exception();
				res = chanel.readBoolean();
				if ((res == null) || (res == false))
					throw new Exception();

// TODO transfert data
			}
			catch(Exception e)
			{
e.printStackTrace();
				logger.write("\t **** Can't establish connection with node " + nodeId + " at the IP " + newNodeIP + "****");
			}

		}
		else
		{
System.out.println("next");
			this.nextChanel.writeLine(THREAD_PREVIOUS_JOIN);
			this.nextChanel.writeLine(""+newNodeId);
			this.nextChanel.writeLine(newNodeIP);
		}
	}

	public boolean insert(String key, String value)
	{
		boolean test;
		
		if (this.internalHashTable != null)
		{
			test = this.internalHashTable.insert(key, value);
			if (test)
				return true;
		}

		if (this.nextChanel == null)
			return false;

		test = true;
		test &= this.nextChanel.writeLine(Node.THREAD_PREVIOUS_INSERT);
		test &= this.nextChanel.writeLine(key);
		test &= this.nextChanel.writeLine(value);
		return test;
	}

	public Boolean isResponsibleForKey(String key)
	{
		if (this.internalHashTable == null)
			return null;

		return this.internalHashTable.isResponsibleForKey(key);
	}

	public String getValue(String key)
	{
		if (this.internalHashTable != null)
		{
			return this.internalHashTable.getValue(key);
		}
		return null;
	}

	public LinkedList<String> getKeySet()
	{
		if (this.internalHashTable == null)
			return null;

		return this.internalHashTable.getKeySet();
	}

// ---------------------------------
// Private method
// ---------------------------------
	private void initCommunicationChanelType(String type) throws ExceptionUnknownCommunicationChanelType
	{
		try
		{
			Class<?> c = Class.forName(type);
			this.communicationChanelConstructor = c.getConstructor(Socket.class, boolean.class, boolean.class);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new ExceptionUnknownCommunicationChanelType();
		}
	}
}