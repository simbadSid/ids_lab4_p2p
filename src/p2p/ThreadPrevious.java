package p2p;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import communication.CommunicationChanel;
import communication.Logger;





public class ThreadPrevious implements Runnable
{
// -----------------------------------
// Attributes
// -----------------------------------
	public static final int		initialPreviousPort			= 2222;

	private Object			lock = new Object();
	private boolean			initialized	= false;

	private Node			node;
	private CommunicationChanel chanel;
	private int				previousNodePort;
	private Logger			logger;
	private Constructor<?>	communicationChanelConstructor;

// -----------------------------------
// Builder
// -----------------------------------
	public ThreadPrevious(Node node, int nodeId, Logger logger, Constructor<?> communicationChanelConstructor)
	{
		this.node							= node;
		this.previousNodePort				= getPort(nodeId);
		this.logger							= logger;
		this.communicationChanelConstructor	= communicationChanelConstructor;

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
	public void closePreviousChanel()
	{
		if (this.chanel != null)
			this.chanel.close();
	}

	@Override
	public void run()
	{
		ServerSocket serverSocket = null;
		Socket socket;
		String request;

		while(true)
		{
			this.node.setPrevious(-1);
			try
			{
				serverSocket = new ServerSocket(previousNodePort);	// Initialize the previous connection
				logger.write("Previous thread: starts waiting\n");
				synchronized(this.lock)
				{
					this.initialized = true;
					this.lock.notifyAll();							// Notify the end of the initialization
				}
				socket = serverSocket.accept();						// Accept a connection from a previous node
				chanel = (CommunicationChanel) communicationChanelConstructor.newInstance(socket, false, true);
				serverSocket.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
				throw new RuntimeException();
			}
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

	public boolean join(CommunicationChanel chanel)
	{
		int		newNodeId	= chanel.readInt();
		String	newNodeIP	= chanel.readLine();

		this.node.join(newNodeId, newNodeIP);
		return true;
	}

	public boolean insert(CommunicationChanel chanel)
	{
		String key	= chanel.readLine();
		String value= chanel.readLine();
		return this.node.insert(key, value);
	}

// -----------------------------------
// Auxiliary
// -----------------------------------
	public static int getPort(int nodeId)
	{
		return initialPreviousPort + nodeId;
	}


}
