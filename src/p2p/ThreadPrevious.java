package p2p;

import java.lang.reflect.Method;
import communication.CommunicationChanel;
import communication.Logger;






public class ThreadPrevious implements Runnable
{
// -----------------------------------
// Attributes
// -----------------------------------
	public static final int		initialPreviousPort	= 2222;
	public static final String	chanelName_input	= "ThreadNextInput_";

	private Object			lock = new Object();
	private boolean			initialized	= false;

	private Node	node;
	private int		nodeId;
	private int		previousNodePort;
	private Logger	logger;
	private String	communicationChanelType;

// -----------------------------------
// Builder
// -----------------------------------
	public static void initThreadPrevious(Node node, int nodeId, Logger logger, String communicationChanelType)
	{
		new ThreadPrevious(node, nodeId, logger, communicationChanelType);
	}

	private ThreadPrevious(Node node, int nodeId, Logger logger, String communicationChanelType)
	{
		this.node						= node;
		this.nodeId						= nodeId;
		this.previousNodePort			= getPort(nodeId);
		this.logger						= logger;
		this.communicationChanelType	= communicationChanelType;

		Thread t = new Thread(this);
		t.start();
		try												// Wait for the thread to be initialized
		{												//		and its waiting port to be open
			synchronized(this.lock)
			{
				while(this.initialized == false)
				{
					this.lock.wait();
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException("");
		}
	}

// -----------------------------------
// Local methods
// -----------------------------------
	@Override
	public void run()
	{
		CommunicationChanel chanel = null;
		String request;

		try
		{
			logger.write("Previous thread: starts waiting\n");
			synchronized(this.lock)
			{
				this.initialized = true;
				this.lock.notifyAll();							// Notify the end of the initialization
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException();
		}
		while(true)
		{
			chanel = CommunicationChanel.instantiate(communicationChanelType, null, -1, previousNodePort, false, true, null, getChanelName(nodeId));
			if (chanel == null)
				continue;

			Integer previousId = chanel.readInt();					// Check the identity of the previous node
			if ((previousId == null) || (previousId < 0))
				continue;
			node.setPrevious(previousId);
			logger.write("Previous thread: connected to the previous node: " + previousId + "\n");

			while((request = chanel.readLine()) != null)			// Answer to the requests of the previous
			{
				Method m = null;
				try
				{
					m = ThreadPrevious.class.getDeclaredMethod(request, CommunicationChanel.class);
				}
				catch (Exception e)
				{
					logger.write("Previous thread: Received unknown request: " + request + "\n");
					continue;
				}

				try
				{
					this.logger.write("PreviousThread: Receive request: \"" + request + "\"\n");
					boolean test = (boolean) m.invoke(this, chanel);
					if (!test)
						this.logger.write("PreviousThread: Failed to process request: \"" + request + "\"\n");
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	public boolean receiveMsg(CommunicationChanel chanel)
	{
		String msg = chanel.readLine();
		this.logger.write("\t\"" + msg + "\"");
		return true;
	}

	public boolean insert(CommunicationChanel chanel)
	{
		String key	= chanel.readLine();
		String value= chanel.readLine();
		return this.node.insert(key, value);
	}

	public static String getChanelName(int nodeId)
	{
		return ThreadPrevious.chanelName_input + nodeId;
	}

// -----------------------------------
// Auxiliary
// -----------------------------------
	public static int getPort(int nodeId)
	{
		return initialPreviousPort + nodeId;
	}


}
