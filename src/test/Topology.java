package test;

import p2p.Node;





public class Topology
{
// ------------------------------------------
// Attributes
// ------------------------------------------
	private Node[]	topology;

// ------------------------------------------
// Builder
// ------------------------------------------
	public Topology(int nbrNode, String communicationChanelType)
	{
		if (nbrNode <= 0)
			throw new RuntimeException();

		this.topology = new Node[nbrNode];
		for (int i=0; i<nbrNode; i++)
		{
			this.topology[i] = new Node(i, communicationChanelType);
		}
	}

// ------------------------------------------
// Local methods
// ------------------------------------------
	public void setChordNetworkOverlay(Integer[] nextId)
	{
		if (nextId.length != this.topology.length)
			throw new RuntimeException();

		boolean test;
		for (int i=0; i<nextId.length; i++)
		{
			if (nextId[i] == null)
				continue;
			test = this.topology[i].setNext(nextId[i], "localhost");
			if (!test)
				throw new RuntimeException("Failed to link the node " + i + " to the node " + nextId[i]);
		}
	}

	public int nbrNode()
	{
		return this.topology.length;
	}

	public Node getNode(int index)
	{
		return this.topology[index];
	}
}