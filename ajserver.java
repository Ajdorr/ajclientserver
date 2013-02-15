import java.io.*;
import java.net.*;

public class ajserver implements Runnable {
	public ajserverdata sdata = null;

	private ServerSocket accptsoc = null;
	private int mclients;
	private volatile ajserverthread servlist[];
	private volatile boolean listen = true;	

	private void closeall() {
		int i;
		for (i = 0; i < mclients; i++) {
			if (servlist[i] != null) {
				this.closeid(i);
			}
		}
	}

	// create a server to listen on port prt with max clients max
	public ajserver(int prt, int max, ajserverdata s) throws IOException {
		// System.out.println("Created a ajserver");
		accptsoc = new ServerSocket(prt);

	        // System.err.println("ERROR: Could not listen on port!");
        	   
		mclients = max;
		servlist = new ajserverthread[mclients]; // create the server list
		sdata = s;
	}

	public void run() {
		// System.out.println("Running!");	
		while (listen) { // keep accepting connections
			int i;
			Socket tsoc;

			try {
				tsoc = accptsoc.accept();
			}catch (IOException e) {
				if (listen) {
					System.err.println("ajserver: Accepting on a socket failed");
					continue;
				}
				this.closeall();
				return;
			}

			// find an empty slot
			synchronized (servlist) {
				for (i = 0; i < mclients; i++) {
					if (servlist[i] == null) {
						// System.out.println("Found a slot");
						break;
					}
				}
				if (i >= mclients) {
					// double the list if its too big...
					ajserverthread newlist[] = new ajserverthread[2*mclients];
					for (i = 0; i < mclients; i++) {
						newlist[i] = servlist[i];					
					}
					servlist = newlist;
					mclients = 2*mclients;
				}
				try {
					servlist[i] = new ajserverthread(tsoc, i, this);
				} catch (IOException e) {
					servlist[i] = null;
					continue;
				}
			}
			(new Thread(servlist[i])).start();
		}	
		try  {
			accptsoc.close();
			this.closeall();
		} catch (IOException e) {
			System.err.println("ajserver: Could not close all sockets");
		}
	}

	// close a connection on id i
	public void closeid(int i) {
		synchronized (servlist) {
			servlist[i].close();
			servlist[i] = null;
		}
	}

	public int connect(String hostname, int port) {
		int i; 

		synchronized (servlist) {
			for (i = 0; i < mclients; i++) {
				if (servlist[i] == null)
					break;
			}
			if (i >= mclients) {
				// double the list if its too big...
				ajserverthread newlist[] = new ajserverthread[2*mclients];
				for (i = 0; i < mclients; i++) {
					newlist[i] = servlist[i];					
				}
				servlist = newlist;
				mclients = 2*mclients;
			}
			try {
				servlist[i] = new ajserverthread(hostname, port, i, this);
			} catch (IOException e) {
				servlist[i] = null;
				return -1;
			}
			(new Thread(servlist[i])).start();
			return i;
		}
	}

	public int sendmsg(int cid, ajpacket j) {
		synchronized (servlist) {
			if (servlist[cid] == null) {
				return -1;
			}
			return servlist[cid].sendmsg(j);
		}
	}

	// shutdown the server
	public void shutdown() {
		// System.out.println("trying to shut down");
		synchronized (accptsoc) {
			if (!listen) {
				return;			
			}
			listen = false;
			try {
				accptsoc.close();
				this.closeall();
			} catch (IOException e) {
				System.err.println("ajserver: could not shutdown properly");
			}
		}
	}
}

class ajserverthread implements Runnable {
	private ObjectInputStream fromClient = null;
	private ObjectOutputStream toClient = null;
	private Socket socket = null;
	private ajserver serv = null;
	private int cid; // client id
	boolean close = false;
	
	// create a server thread to connect to a client
	public ajserverthread(String hstnm, int prt, int i, ajserver j) throws IOException {
		serv = j;
		cid = i;
		socket = new Socket(hstnm, prt); // save the socket
		toClient = new ObjectOutputStream(socket.getOutputStream());
		fromClient = new ObjectInputStream(socket.getInputStream());
	}

	// create a server thread with socket tsoc, id i, from server j
	public ajserverthread(Socket tsoc, int i, ajserver j) throws IOException {
		serv = j;
		cid = i;
		socket = tsoc; // save the socket
		fromClient = new ObjectInputStream(socket.getInputStream());
		toClient = new ObjectOutputStream(socket.getOutputStream());
	}

	public void run() {
		try {
			ajpacket msg, ret;

			// Dialog
			while (!close) {
				// System.out.println("Waiting for...");
				msg = (ajpacket) fromClient.readObject();
				ret = serv.sdata.ajmessage(serv, cid, msg);
				if (ret != null) {
					toClient.writeObject(ret);
				}
			}

		} catch (IOException e) {
			if(!close) {
				this.close();
			}
		} catch (ClassNotFoundException e) {
			if(!close)
				e.printStackTrace();
		}
		serv.sdata.ajdisconnect(serv, cid);
		// System.out.println("Someone disconnected.");
	}

	public int sendmsg(ajpacket s) {
		try {
			toClient.writeObject(s);
		} catch (IOException e) {
			return -1;
		}
		return 0;
	}

	// close the connection
	public void close() {
		synchronized (socket) {
			if (close) {
				return;	
			}
			close = true;
			serv.closeid(cid);
			try {
				fromClient.close();
				toClient.close();
				socket.close();
			} catch(IOException e) {
				System.err.println("ajserver: an error occured while trying to close client " + cid);
			}		
		}
	}
}
