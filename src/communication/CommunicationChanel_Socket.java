package communication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;







public class CommunicationChanel_Socket implements CommunicationChanel
{
// ---------------------------------
// Attributes
// ---------------------------------
	private Socket			socket;
	private PrintWriter		out;
	private BufferedReader	in;
	private boolean			printError	= true;
	private boolean			isClose		= true;

// ---------------------------------
// Builder
// ---------------------------------
	public CommunicationChanel_Socket(Socket socket) throws IOException
	{
		this(socket, true, true);
	}

	public CommunicationChanel_Socket(Socket socket, boolean write, boolean read) throws IOException
	{
		this.socket	= socket;
		if (write)
			this.out	= new PrintWriter(socket.getOutputStream(), true);
		if (read)
			this.in		= new BufferedReader(new InputStreamReader(socket.getInputStream()));
		this.isClose = false;
	}

	public CommunicationChanel_Socket(Socket socket, boolean printError) throws IOException
	{
		this(socket);
		this.printError = printError;
	}

// ---------------------------------
// Local method
// ---------------------------------
	@Override
	public String readLine()
	{
		try
		{
			return this.in.readLine();
		}
		catch (Exception e)
		{
			if (printError) e.printStackTrace();
			return null;
		}
	}

	@Override
	public Integer readInt()
	{
		try
		{
			String str = this.in.readLine();
			return Integer.parseInt(str);
		}
		catch (Exception e)
		{
			if (printError) e.printStackTrace();
			return null;
		}
	}

	@Override
	public Boolean readBoolean()
	{
		try
		{
			String str = this.in.readLine();
			return Boolean.parseBoolean(str);
		}
		catch (Exception e)
		{
			if (printError) e.printStackTrace();
			return null;
		}
	}

	@Override
	public boolean writeLine(String msg)
	{
		try
		{
			this.out.write(msg + "\n");
			this.out.flush();
			return true;
		}
		catch (Exception e)
		{
			if (printError) e.printStackTrace();
			return false;
		}
	}

	@Override
	public void close()
	{
		try
		{
			if (this.in != null)
				this.in.close();
			if (this.out != null)
				this.out.close();
			this.socket.close();
		}
		catch (IOException e)
		{
			if (printError) e.printStackTrace();
		}
		this.isClose = true;
	}

	@Override
	public boolean isClose()
	{
		return this.isClose;
	}
}
