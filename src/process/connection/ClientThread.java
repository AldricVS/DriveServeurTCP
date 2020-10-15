package process.connection;

import java.net.Socket;


/**
 * Thread dealing with a single client.
 * 
 * @author Aldric Vitali Silvestre <aldric.vitali@outtlok.fr>
 */
public class ClientThread extends Thread {
	Socket clientSocket;
	
	/**
	 * Make the connection with client, but user is not logged in yet
	 * @param clientSocket
	 */
	public ClientThread(Socket clientSocket) {
		this.clientSocket = clientSocket;
	}
}
