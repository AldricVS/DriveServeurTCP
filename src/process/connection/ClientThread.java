package process.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.apache.log4j.Logger;

import logger.LoggerUtility;


/**
 * Thread dealing with a single client.
 * 
 * @author Aldric Vitali Silvestre <aldric.vitali@outtlok.fr>
 */
public class ClientThread extends Thread {
	private static Logger logger = LoggerUtility.getLogger(ClientThread.class, LoggerUtility.LOG_PREFERENCE);
	
	private Socket clientSocket;
	
	/**
	 * We keep trace of the handler of all clients thread in order to call his methods
	 */
	private ThreadsConnectionHandler handler;
	
	/**
	 * To communicate with client
	 */
	private PrintWriter outputFlow;
	private BufferedReader inputFlow;
	
	/**
	 * Make the connection with client, but user is not logged in yet
	 * @param clientSocket
	 * @param ThreadsConnectionHandler
	 */
	public ClientThread(Socket clientSocket, ThreadsConnectionHandler threadsConnectionHandler){
		this.clientSocket = clientSocket;
		this.handler = threadsConnectionHandler;
	}
	
	@Override
	public void run() {
		try {
			inputFlow = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			outputFlow = new PrintWriter(clientSocket.getOutputStream(), true);
			
			
			
		}catch (IOException e) {
			String errorMessage = "Error while communicating with client : " + e.getMessage();
			ClientThread.logger.error(errorMessage);
			System.err.println(errorMessage);
		}
	}
}
