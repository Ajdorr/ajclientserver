import java.io.*;
import java.net.*;

public class ajclient implements Runnable{
	private String hostname = null;
	private int prt = 0;
	private Socket soc = null;
	private ObjectOutputStream out = null;
	private ObjectInputStream in = null;
	private volatile boolean run = true;
	private volatile boolean trans;

	public ajclientdata cdata = null;
	
	// create a client on hostname s, port p, with client data j
	// creates a recieve thread to recieve all messages
	public ajclient(String hn, int port, ajclientdata data, boolean usetrans) 
						throws IOException, ClassNotFoundException{
		hostname = new String(hn);
		prt = port;
		cdata = data;

		soc = new Socket(hostname, prt);
		out = new ObjectOutputStream(soc.getOutputStream());
		in = new ObjectInputStream(soc.getInputStream());

		if (!usetrans) {
			(new Thread(this)).start();
		}
		trans = usetrans;
	}

	public void run() {
		ajpacket msg = null;
		try {
			while (run) {
				msg = (ajpacket) in.readObject(); // recieve messages
				if (cdata != null) {
					cdata.recv(this, msg);
				}
			}
		} catch (IOException e) {
			if (run) {
				System.err.println("ajclient: connection error");
				this.close();
			}
		} catch (ClassNotFoundException e) {
			if (run) {
				System.err.println("ajclient: message save error");
			}
		}
		this.close();
	}

	// send a message, blocks until it recieves the returned message
	// returns null if the server shuts down
	public int send(ajpacket s) {
		ajpacket msg;
		// System.out.println(s.msg);
		try {
			out.writeObject(s);
			if (trans) {
				msg = (ajpacket) in.readObject();
				if (cdata != null) {
					cdata.recv(this, msg);
				}
			}
		} catch (IOException e) {
			return -1;
		} catch (ClassNotFoundException e) {
			return -1;
		}
		return 0;
	}

	public void close() {
		synchronized (soc) {
			if (!run)
				return;
			run = false;
			try {
				out.close();
				in.close();
				soc.close();
			} catch(IOException e) {
				System.err.println("ajclient.close: failed to close the connection");
			}
		}
	}
}
