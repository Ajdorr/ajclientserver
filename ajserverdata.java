// server side data connection class
// data is passed to this class when a message is recieved and when a client disconnects
public abstract class ajserverdata {
	
	
	public ajserverdata() {
		
	}

	// return null to not send 
	public abstract ajpacket ajmessage(ajserver serv, int cid, ajpacket msg);
	
        // Function that is called when a client disconnects
	public abstract void ajdisconnect(ajserver serv, int cid);
}
