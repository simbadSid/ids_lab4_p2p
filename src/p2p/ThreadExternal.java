package p2p;

import communication.CommunicationChanel;
import communication.Logger;







public class ThreadExternal implements Runnable
{
// ---------------------------------
// Attributes
// ---------------------------------
	public static final int		initialExternalPort	= 3333;
	public static final String	chanelName_input	= "ThreadExternalInput_";
	public static final String	chanelName_output	= "ThreadExternalOutput_";

	private int		externalNodePort;
	private Node	node;
	private int		nodeId;
	private Logger	logger;
	private String	communicationChanelType;

// -----------------------------------
// Builder
// -----------------------------------
	public static void initThreadExternal(Node node, int nodeId, Logger logger, String communicationChanelType)
	{
		new ThreadExternal(node, nodeId, logger, communicationChanelType);
	}

	private ThreadExternal(Node node, int nodeId, Logger logger, String communicationChanelType)
	{
		this.node						= node;
		this.nodeId						= nodeId;
		this.externalNodePort			= getPort(nodeId);
		this.logger						= logger;
		this.communicationChanelType	= communicationChanelType;

		Thread t = new Thread(this);
		t.start();
	}

// -----------------------------------
// Local methods
// -----------------------------------
	@Override
	public void run()
	{
		CommunicationChanel chanel= null;
	
		while(true)
		{
			try
			{
				chanel = CommunicationChanel.instantiate(this.communicationChanelType, null, -1, externalNodePort, true, true, getOutputChanelName(nodeId), getInputChanelName(nodeId));

				while(true)
				{
					String request	= chanel.readLine();
					if (request == null)
					{
						chanel.close();
						chanel = null;
						break;
					}

					this.logger.write("ExternalThread: Receive request: \"" + request + "\"\n");
					String res = "false";
					if (request.equals(Node.THREAD_EXTERNAL_SET_NEXT))
					{
						int		nextId	= chanel.readInt();
						String	nextIP	= chanel.readLine();
						res				= ""+this.node.setNext(nextId, nextIP);
					}
					else if (request.equals(Node.THREAD_EXTERNAL_SEND_MSG))
					{
						String msg	= chanel.readLine();
						res			= ""+this.node.sendMessage(msg);
					}
					else if (request.equals(Node.THREAD_EXTERNAL_GET_NEXT))
					{
						res = "" + this.node.getNext();
					}
					else if (request.equals(Node.THREAD_EXTERNAL_GET_PREVIOUS))
					{
						res = "" + this.node.getPrevious();
					}
					else if (request.equals(Node.THREAD_EXTERNAL_INSERT))
					{
						String key	= chanel.readLine();
						String value= chanel.readLine();
						res			= ""+this.node.insert(key, value);
					}
					else if (request.equals(Node.THREAD_EXTERNAL_IS_RESPONSIBLE_FOR_KEY))
					{
						String key	= chanel.readLine();
						res			= ""+this.node.isResponsibleForKey(key);
					}
					else if (request.equals(Node.THREAD_EXTERNAL_GET_VALUE))
					{
						String key	= chanel.readLine();
						res			= this.node.getValue(key);
					}
					else
					{
						this.logger.write("**** ExternalThread: Unhandeled request \"" + request + "\"****\n");
					}
	
					chanel.writeLine(res);
				}
				if(chanel != null)
					chanel.close();
				chanel = null;
			}
			catch (Exception e)
			{
				e.printStackTrace();
				if (chanel != null)
					chanel.close();
				chanel = null;
			}
		}
	}

	public static String getOutputChanelName(int nodeId)
	{
		return ThreadExternal.chanelName_output + nodeId;
	}

	public static String getInputChanelName(int nodeId)
	{
		return ThreadExternal.chanelName_input + nodeId;
	}

// -----------------------------------
// Auxiliary
// -----------------------------------
	public static int getPort(int nodeId)
	{
		return initialExternalPort + nodeId;
	}
}