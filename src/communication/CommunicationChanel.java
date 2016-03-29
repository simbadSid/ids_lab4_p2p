package communication;







public interface CommunicationChanel
{
	public String	readLine	();
	public Integer	readInt		();
	public Boolean	readBoolean	();
	public boolean	writeLine	(String msg);
	public void		close		();
	public boolean	isClose		();

}
