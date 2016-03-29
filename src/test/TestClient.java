package test;

import java.io.File;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.Scanner;
import p2p.Node;
import p2p.ThreadExternal;
import communication.CommunicationChanel;






public class TestClient
{
// -------------------------------------
// Attributes
// -------------------------------------
	public static String	neighborMatrixFile		= "resource/input/neighborhoodMatrix.txt";
	public static String	communicationChanelType	= CommunicationChanel.COMMUNICATION_CHANEL_RABBITMQ;
	public static Topology	topology;

// -------------------------------------
// Main method
// -------------------------------------
	public static void main(String[] args)
	{
		Scanner sc = new Scanner(System.in);
		TestClient test = new TestClient();
		String nodeIP = null;
		int nodeId = -1;
		topology = parseTopology(neighborMatrixFile);

/*
		System.out.println("Please enter:");
		System.out.println("\t- \"localhost\" if all the nodes are on the local machine");
		System.out.println("\t- any thing else otherwise:");
		try
		{
			nodeIP = sc.next();
		}
		catch(Exception e)
		{
			sc.close();
			return;
		}
		if (!nodeIP.equals("localhost"))
			nodeIP = null;
*/
nodeIP = "localhost";

		boolean parsed = false;
		while(!parsed)
		{
			System.out.println("\n\nPlease enter:");
			System.out.println("\t- The index of the node to call");
			System.out.println("\t- (-1) to set the node index dynamically");
			try
			{
				nodeId = sc.nextInt();
				if ((nodeId < -1) || (nodeId >= topology.nbrNode()))
					throw new Exception();
				parsed = true;
			}
			catch(Exception e)
			{
				System.out.println("\t**** wrong node id ****");
				continue;
			}
		}

		CommunicationChanel chanel = null;
		while(true)
		{
			System.out.println("\n\nPlease enter");
			System.out.println("\t- \"" + Node.THREAD_EXTERNAL_SET_NEXT + "\"");
			System.out.println("\t- \"" + Node.THREAD_EXTERNAL_SEND_MSG + "\"");
			System.out.println("\t- \"" + Node.THREAD_EXTERNAL_GET_NEXT + "\"");
			System.out.println("\t- \"" + Node.THREAD_EXTERNAL_GET_PREVIOUS + "\"");
			System.out.println("\t- \"" + Node.THREAD_EXTERNAL_JOIN + "\"");
			System.out.println("\t- \"" + Node.THREAD_EXTERNAL_INSERT + "\"");
			System.out.println("\t- \"" + Node.THREAD_EXTERNAL_IS_RESPONSIBLE_FOR_KEY + "\"");
			System.out.println("\t- \"" + Node.THREAD_EXTERNAL_GET_VALUE + "\"");
/*			System.out.println("\t- \"" + Node.THREAD_EXTERNAL_HALT + "\"");
*/
			System.out.println("\t- \"printOverlay\"");
			System.out.println("\t- \"setLocalNode\"");

			String method = sc.next();

			if ((chanel == null) || (chanel.isClose()))
				chanel = connectToNode(sc, nodeIP, nodeId);

			if (method.equals("printOverlay"))
			{
				printOverlay(topology);
				continue;
			}
			if (method.equals("setLocalNode"))
			{
				System.out.println("\tPlease enter the index of the node to call ( 0 <= index <= " + topology.nbrNode() + "):");
				while(true)
				{
					try
					{
						nodeId = sc.nextInt();
						if ((nodeId < 0) || (nodeId >= topology.nbrNode()))
							throw new Exception();
						chanel.close();
						break;
					}
					catch(Exception e)
					{
						System.out.println("\t**** The index must respect ( 0 <= index <= " + topology.nbrNode() + ") ****");
					}
				}
				continue;
			}
			Method m = null;
			try
			{
				m = TestClient.class.getDeclaredMethod(method, CommunicationChanel.class, Scanner.class, String.class, int.class, String.class);
			}
			catch (Exception e)
			{
				System.out.println("**** Unhandeled choice ****");
				continue;
			}

			try
			{
				String res = (String) m.invoke(test, chanel, sc, nodeIP, nodeId, null);
				if (res == null)
				{
					System.out.println("\t**** Request refused by the node ****");
				}
				else
				{
					System.out.println("\tNode response = \"" + res + "\"");
				}
				System.out.println("---------------------------------------");

			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

	}


	private static void printOverlay(Topology topology)
	{
		for (int i=0; i<topology.nbrNode(); i++)
		{
			Node node = topology.getNode(i);
			System.out.println("\t- Node         : " + i);
			System.out.println("\t- Previous node: " + node.getPrevious());
			System.out.println("\t- Next node    : " + node.getNext());
			System.out.println("\t- Data         : ");
			LinkedList<String> keySet = node.getKeySet();
			if (keySet != null)
			{
				for (String key: keySet)
				{
					System.out.println("\t\t " + key + ":\t" + node.getValue(key));
				}
			}
			System.out.println("\t-------------------------------");
		}
	}

// -------------------------------------
// Local methods
// -------------------------------------
	public String setNext(CommunicationChanel chanel, Scanner sc, String nodeIP, int nodeId, String keyTrash)
	{
		int		nextNodeId = parseNodeId(sc, "reach next");
		String	nextNodeIP = parseNodeIP(sc, "reach next", nodeIP);

		boolean res = true;
		res &= chanel.writeLine(Node.THREAD_EXTERNAL_SET_NEXT);
		res &= chanel.writeLine(""+nextNodeId);
		res &= chanel.writeLine(nextNodeIP);
		if (!res)
			return null;
		return chanel.readLine();
	}

	public String sendMsg(CommunicationChanel chanel, Scanner sc, String nodeIP, int nodeId, String keyTrash)
	{
		String msg = "";

		System.out.print("\t\tPlease write the msg = ");
		while(msg.length() == 0)
		{
			msg = sc.nextLine();
		}
		boolean res = true;
		res &= chanel.writeLine(Node.THREAD_EXTERNAL_SEND_MSG);
		res &= chanel.writeLine(msg);
		if (!res)
			return null;
		return chanel.readLine();
	}

	public String getPrevious(CommunicationChanel chanel, Scanner sc, String nodeIP, int nodeId, String keyTrash)
	{
		boolean res = chanel.writeLine(Node.THREAD_EXTERNAL_GET_PREVIOUS);
		if (!res)
			return null;
		return chanel.readLine();
	}

	public String getNext(CommunicationChanel chanel, Scanner sc, String nodeIP, int nodeId, String keyTrash)
	{
		boolean res = chanel.writeLine(Node.THREAD_EXTERNAL_GET_NEXT);
		if (!res)
			return null;
		return chanel.readLine();
	}

	public String join(CommunicationChanel chanel, Scanner sc, String nodeIP, int nodeId, String keyTrash)
	{
// TODO
		return null;
/*
		int		nextNodeId = parseNodeId(sc, "reach next");
		String	nextNodeIP = parseNodeIP(sc, "reach next", null);
		CommunicationChanel newChanel = connectToNode(sc, nextNodeIP, nextNodeId);

		do
		{
			String res = this.isResponsibleForKey(newChanel, sc, nodeIP, nextNodeId, ""+nodeId);
			if ((res != null) && (Boolean.parseBoolean(res)))
			{
				// set previous of .... to -1
				topology.getNode(nextNodeId).setPrevious(-1)
				// get previous previous;
				// Set next of previous to me
				
			}
			String str = this.getNext(newChanel, sc, nodeIP, newNodeId, null);
			if (str == null) break;
			newNodeId = Integer.parseInt(str);
			newChanel = connectToNode(sc, nodeIP, newNodeId);
		}while(newNodeId != nodeId);
		return null;
*/
	}

	public String insert(CommunicationChanel chanel, Scanner sc, String nodeIP, int nodeId, String keyTrash)
	{
		String key	= "";
		String value= "";

		System.out.print("\t\tPlease write the key: ");
		while(key.length() == 0)
		{
			key = sc.nextLine();
		}
		System.out.print("\t\tPlease write the value: ");
		while(value.length() == 0)
		{
			value = sc.nextLine();
		}

		boolean res = true;
		res &= chanel.writeLine(Node.THREAD_EXTERNAL_INSERT);
		res &= chanel.writeLine(key);
		res &= chanel.writeLine(value);
		if (!res)
			return null;
		return chanel.readLine();
	}

	public String isResponsibleForKey(CommunicationChanel chanel, Scanner sc, String nodeIP, int nodeId, String keyTrash)
	{
		String key;

		if (keyTrash != null)
			key = keyTrash;

		else
		{
			key = "";

			System.out.print("\t\tPlease write the key: ");
			while(key.length() == 0)
			{
				key = sc.nextLine();
			}
		}
		boolean res = true;
		res &= chanel.writeLine(Node.THREAD_EXTERNAL_IS_RESPONSIBLE_FOR_KEY);
		res &= chanel.writeLine(key);
		if (!res)
			return null;
		return chanel.readLine();
	}

	public String getValue(CommunicationChanel chanel, Scanner sc, String nodeIP, int nodeId, String keyTrash)
	{
		String key;

		if (keyTrash != null)
			key = keyTrash;

		else
		{
			key = "";

			System.out.print("\t\tPlease write the key: ");
			while(key.length() == 0)
			{
				key = sc.nextLine();
			}
		}

		CommunicationChanel newChanel = chanel;
		int newNodeId = nodeId;
		do
		{
			String res = this.isResponsibleForKey(newChanel, sc, nodeIP, newNodeId, key);
			if ((res != null) && (Boolean.parseBoolean(res)))
			{
				boolean test = true;
				test &= newChanel.writeLine(Node.THREAD_EXTERNAL_GET_VALUE);
				test &= newChanel.writeLine(key);
				if (!test)
					break;
				return newChanel.readLine();
			}
			String str = this.getNext(newChanel, sc, nodeIP, newNodeId, null);
			if (str == null) break;
			newNodeId = Integer.parseInt(str);
			if (newNodeId <= 0)
				return "(null)";
			newChanel = connectToNode(sc, nodeIP, newNodeId);
		}while(newNodeId != nodeId);
		return null;
	}

	public boolean halt(CommunicationChanel chanel, Scanner sc, String nodeIP, int nodeId, String keyTrash)
	{
		sc.close();
		System.exit(0);
		return true;
	}

// -------------------------------------
// Private methods
// -------------------------------------
	@SuppressWarnings("resource")
	private static Topology parseTopology(String fileName)
	{
		Scanner sc = null;
		Integer[] next;
		try
		{
			sc = new Scanner(new File(fileName));
			int nbrNode = sc.nextInt();
			next = new Integer[nbrNode];
			for (int y=0; y<nbrNode; y++)
			{
				next[y] = null;
				for (int x=0; x<nbrNode; x++)
				{
					int n = -1;
					try
					{
						n = sc.nextInt();
						if(n == 0)
							continue;
					}
					catch(Exception e)
					{
						throw new Exception("Unknown char in the neighbor matrix file at index (x, y): (" + x + ", " + y + ")");
					}
					if (next[y] != null) throw new Exception("The node " + y + " has multiple successors");
					next[y] = x;
				}
			}
			sc.close();
			Topology res = new Topology(nbrNode, communicationChanelType);
			res.setChordNetworkOverlay(next);
			return res;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			if (sc != null)
				sc.close();
			System.exit(0);
			return null;
		}
	}

	private static CommunicationChanel connectToNode(Scanner sc, String nodeIP, int nodeId)
	{
		if (nodeIP == null)
		{
			System.out.println("\tPlease enter the IP address of the node to call");
			nodeIP = sc.next();
		}
		if (nodeId < 0)
		{
			nodeId = parseNodeId(sc, "call");
		}
		try
		{
			return CommunicationChanel.instantiate(communicationChanelType, nodeIP, ThreadExternal.getPort(nodeId), -1, true, true, ThreadExternal.getInputChanelName(nodeId), ThreadExternal.getOutputChanelName(nodeId));
		}
		catch(Exception e)
		{
			System.out.println("\t **** Can't establish connection with node " + nodeId + " at the IP " + nodeIP + "****");
			System.exit(0);
			return null;
		}
	}

	private static int parseNodeId (Scanner sc, String nodeType)
	{
		int nodeId;

		while(true)
		{
			System.out.println("\tPlease enter the ID of the node to " + nodeType + " ( >= 0)");
			try
			{
				nodeId = sc.nextInt();
				if (nodeId < 0) throw new Exception();
				return nodeId;
			}
			catch(Exception e)
			{
				System.out.println("\t**** The node ID must be >= 0****");
			}
		}

	}

	private static String parseNodeIP (Scanner sc, String nodeType, String nodeIP)
	{
		String resNodeIP;

		if (nodeIP != null)
			return new String(nodeIP);
		while(true)
		{
			System.out.println("\tPlease enter the IP of the node to " + nodeType);
			try
			{
				resNodeIP = sc.next();
				return resNodeIP;
			}
			catch(Exception e)
			{
				System.exit(0);
			}
		}

	}
}
