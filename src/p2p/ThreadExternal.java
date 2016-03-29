package p2p;

import java.lang.reflect.Constructor;
import java.net.ServerSocket;
import java.net.Socket;
import communication.CommunicationChanel;
import communication.Logger;






public class ThreadExternal implements Runnable
{
// ---------------------------------
// Attributes
// ---------------------------------
	public static final int		initialExternalPort			= 3333;


	private int				externalNodePort;
	private Node			node;
	private Logger			logger;
	private Constructor<?>	communicationChanelConstructor;

// -----------------------------------
// Builder
// -----------------------------------
	public static void initThreadExternal(Node node, int nodeId, Logger logger, Constructor<?> communicationChanelConstructor)
	{
		new ThreadExternal(node, nodeId, logger, communicationChanelConstructor);
	}

	private ThreadExternal(Node node, int nodeId, Logger logger, Constructor<?> communicationChanelConstructor)
	{
		this.node							= node;
		this.externalNodePort				= getPort(nodeId);
		this.logger							= logger;
		this.communicationChanelConstructor	= communicationChanelConstructor;

		Thread t = new Thread(this);
		t.start();
	}

// -----------------------------------
// Local methods
// -----------------------------------
	@Override
	public void run()
	{
		ServerSocket serverSocket = null;
		CommunicationChanel chanel= null;
	
		while(true)
		{
			try 
			{
				serverSocket	= new ServerSocket(externalNodePort);
				Socket socket	= serverSocket.accept();				// Accept a connection from an external agent
				chanel			= (CommunicationChanel) communicationChanelConstructor.newInstance(socket, true, true);
				serverSocket.close();

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
					else if (request.equals(Node.THREAD_EXTERNAL_JOIN))
					{
						int newNodeId = chanel.readInt();
						String newNodeIP = chanel.readLine();
						chanel.writeLine(""+true);
						this.node.join(newNodeId, newNodeIP);
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

// -----------------------------------
// Request handler
// -----------------------------------

// -----------------------------------
// Auxiliary
// -----------------------------------
	public static int getPort(int nodeId)
	{
		return initialExternalPort + nodeId;
	}
}