package p2p;

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

	public static final String	THREAD_PREVIOUS_RECEIVE_MSG		= "receiveMsg";
	public static final String	THREAD_PREVIOUS_INSERT			= "insert";

	public static final String	THREAD_EXTERNAL_SET_NEXT		= "setNext";
	public static final String	THREAD_EXTERNAL_SEND_MSG		= "sendMsg";
	public static final String	THREAD_EXTERNAL_GET_PREVIOUS	= "getPrevious";
	public static final String	THREAD_EXTERNAL_GET_NEXT		= "getNext";
	public static final String	THREAD_EXTERNAL_INSERT			= "insert";
	public static final String	THREAD_EXTERNAL_IS_RESPONSIBLE_FOR_KEY = "isResponsibleForKey";
	public static final String	THREAD_EXTERNAL_GET_VALUE		= "getValue";
	public static final String	THREAD_EXTERNAL_JOIN			= "join";

	private int					nodeId				= -1;
	private NodeInternalHashTable internalHashTable	= null;
	private int					previousId			= -1;
	private int					nextId				= -1;
	private CommunicationChanel	nextChanel			= null;
	private Logger				logger;
	private String				communicationChanelType;

// ---------------------------------
// Builder
// ---------------------------------
	public Node(int nodeId, String communicationChanelType) throws ExceptionUnknownCommunicationChanelType
	{
		this.nodeId						= nodeId; //TODO NodeInternalHashTable.hash(""+nodeId);
		this.logger						= new Logger(""+nodeId);
		this.communicationChanelType	= new String(communicationChanelType);

		ThreadPrevious.initThreadPrevious(this, nodeId, logger, communicationChanelType);
		ThreadExternal.initThreadExternal(this, nodeId, logger, communicationChanelType);
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

		CommunicationChanel chanel = null;
		boolean test;
		try
		{
			chanel	= CommunicationChanel.instantiate(this.communicationChanelType, nextIP, ThreadPrevious.getPort(nextId), -1, true, false, ThreadPrevious.getChanelName(nextId), null);
			test	= chanel.writeLine(""+this.nodeId);
			if (!test) throw new Exception();
			this.nextChanel	= chanel;
			this.nextId		= nextId;
			logger.write("\tnext = " + this.nextId + "\n");
			return true;
		}
		catch(Exception e)
		{
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
}