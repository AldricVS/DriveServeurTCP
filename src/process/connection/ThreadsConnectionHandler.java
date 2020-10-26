package process.connection;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import data.Protocol;
import data.User;
import logger.LoggerUtility;
import process.database.DatabaseManager;
import process.protocol.ProtocolFactory;

/**
 * Main class of the server : it will wait for new clients connecting and create threads for newcommers.
 * @author Aldric Vitali Silvestre <aldric.vitali@outlook.fr>
 */
public class ThreadsConnectionHandler extends Thread{
	private static Logger logger = LoggerUtility.getLogger(ThreadsConnectionHandler.class, LoggerUtility.LOG_PREFERENCE);
	
	private boolean isListening = true;
	
	/**
	 * We store user list in order to keep a trace of all users (useful for checking if any user want to connect twice for exemple)
	 */
	private List<User> users = new ArrayList<>(); 
	
	private ServerSocket serverSocket;
	
	private DatabaseManager databaseManager;
	
	/**
	 * Creating an instance of the class will start listenning for new clients
	 * @param port the port where to listen for clients
	 */
	public ThreadsConnectionHandler(int port, String databaseUrl, String databaseUser, String databasePassword) {
		try {
			//connect to database
			databaseManager = new DatabaseManager(databaseUrl, databaseUser, databasePassword);
			serverSocket = new ServerSocket(port);
			start();
			logger.info("Server waiting for clients on port " + port);
		}catch (IOException e) {
			//we can't do anything here, we have to stop the application  
			String errorMessage = "Cannot connect on port " + port + " : " + e.getMessage();
			System.err.println(errorMessage);
			logger.fatal(errorMessage);
			System.exit(-1);
		}
		catch (SQLException e) {
			//we can't do anything here, we have to stop the application  
			String errorMessage = "Cannot connect to database : " + e.getMessage();
			System.err.println(errorMessage);
			logger.fatal(errorMessage);
			System.exit(-1);
		}
		
		//here, the thread will start end witing for new clients
	}
	
	@Override
	public void run() {
		while(isListening) {
			try {
				//start a new thread for the client
				ClientThread clientThread = new ClientThread(serverSocket.accept(), this);
				clientThread.start();
				String message = "New client connected";
				logger.info(message);
				System.out.println(message);
			} catch (IOException e) {
				//error happened, stop communications
				String errorMessage = "Error while listening for new clients : " + e.getMessage();
				logger.error(errorMessage);
				System.err.println(errorMessage);
				isListening = false;
			}
		}
	}

	public Protocol queryConnectionDatabase(String login, String password, boolean isAdmin) {
		String query = String.format("SELECT COUNT(*) AS count FROM %s WHERE login = '%s' AND mot_de_passe = '%s';",
				isAdmin ? "administrateur" : "employe",
				login,
				password);
		
		try {
			ResultSet result = databaseManager.excecuteSingleQuery(query);
			//get the number returned
			result.next();
			int count = result.getInt("count");
			//if different from 1, we didn't found the user in the database
			if(count != 1) {
				return ProtocolFactory.createErrorProtocol("Le combo identifiant / mot de passe n'est pas valide");
			}else {
				return ProtocolFactory.createSuccessProtocol();
			}
			
		} catch (SQLException e) {
			String errorMessage = "Error while communicating with database : " + e.getMessage();
			logger.error(errorMessage);
			System.err.println(errorMessage);
			//we will send to client an error message
			return ProtocolFactory.createErrorProtocol("Erreur lors de la communication avec la base de données.");
		}
	}
	
	public void addUser(User user) {
		users.add(user);
	}
	
	public void removeUser(User user) {
		users.remove(user);
	}
	
	/**
	 * Check if user is already in list
	 * @param user the user to check
	 * @returns if user is already in list or not
	 */
	public boolean isUserInList(User user) {
		for(User u : users) {
			if(user.getName().equals(u.getName())) {
				return true;
			}
		}
		return false;
	}
	/**
	 * function for add a new product at the database 
	 * @param recievedProtocol
	 * @return succes or echec for this action 
	 */
	public String  queryAddNewProduct( Protocol recievedProtocol) {
	try {
		BigDecimal price= new BigDecimal(recievedProtocol.getOptionsElement(4));
		ResultSet result = databaseManager.excecuteSingleQuery("INSERT INTO produit ("+ recievedProtocol.getOptionsElement(1)+", "
		+recievedProtocol.getOptionsElement(2)+","+recievedProtocol.getOptionsElement(3)+","+price);
		return "reussite";
	}catch(SQLException ex) {
		String errormessage=ex.getMessage() ;
		logger.error(errormessage);	
	}
	return "echec";
	}
	
	public String queryAddProducQuantity(Protocol recievedProtocol) {
		try {
			BigDecimal price= new BigDecimal(recievedProtocol.getOptionsElement(2));
			ResultSet result=databaseManager.excecuteSingleQuery("UPDATE produit SET prix ="+price+" WHERE id_produit= "+recievedProtocol.getOptionsElement(1)+" ;");
			return "reussite";
		}catch(SQLException ex) {
			String errormessage=ex.getMessage() ;
			logger.error(errormessage);	
		}
		return "echec";
		}
	public String queryGetSpecificOrder(Protocol recievedProtocol) {
		try {
			ResultSet result=databaseManager.excecuteSingleQuery("select *from Produit_commande where 	id_commande ="+recievedProtocol.getOptionsElement(1)+";");
			return result.toString();
		}catch(SQLException ex){
			String errormessage=ex.getMessage() ;
			logger.error(errormessage);	
		}
		return "la commande existe pas ";
	}
	
}
