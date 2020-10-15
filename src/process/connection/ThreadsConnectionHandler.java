package process.connection;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import data.User;
import logger.LoggerUtility;

/**
 * Main class of the server : it will wait for new clients connecting and create threads for newcommers.
 * @author Aldric Vitali Silvestre <aldric.vitali@outlook.fr>
 */
public class ThreadsConnectionHandler {
	private static Logger logger = LoggerUtility.getLogger(ThreadsConnectionHandler.class, LoggerUtility.LOG_PREFERENCE);
	
	private List<User> users = new ArrayList<>(); 
	
	/**
	 * Creating an instance of the class will start listenning for new clients
	 */
	public ThreadsConnectionHandler() {
		
	}
	
	public void handleNewClient() {
		
	}
}
