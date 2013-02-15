
// create your own overridden class, recv is called whenever a message is recieved
public abstract class ajclientdata {

	public ajclientdata() {
		// default constructor
	}

	// do something whenever a message is received
	public abstract void recv(ajclient client, ajpacket msg);
}
