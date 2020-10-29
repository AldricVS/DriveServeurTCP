package process.connection;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
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
		String query = String.format("SELECT COUNT(*) AS count FROM %s WHERE nom_employe = '%s' AND mot_de_passe_Employe = '%s';",
				isAdmin ? "administrateur" : "employe",
				login,
				password);
		
		try {
			ResultSet result = databaseManager.executeSelectQuery(query);
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
			return ProtocolFactory.createErrorProtocol("Erreur lors de la communication avec la base de donn�es.");
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
	 * @return succes or echec protocol 
	 */
	public Protocol  queryAddNewProduct( Protocol recievedProtocol) {
	try {
		/*
		 * verify if the produc  exist 
		 */
		String queryExist = String.format("SELECT COUNT(*) AS count FROM produit Where nom_produit='%s';",
				recievedProtocol.getOptionsElement(0)
		);
		ResultSet exist=databaseManager.executeSelectQuery(queryExist)  ;
		exist.next();
		int count = exist.getInt("count");
		//if different from 1, we didn't found the id of produc 
		if(count != 1) {
			logger.error("wrong cause : invalid id product  ");	
			return ProtocolFactory.createErrorProtocol(" le produit existe d�ja  ");
		}else {
		/**
		 * veirify if the name is not more than 50 caracter  
		 */
		if(recievedProtocol.getOptionsElement(0).length()<51) {
			/*
			 * verification of price between 0.1 and 999.99 
			 */
				float  verprice =Float.parseFloat(recievedProtocol.getOptionsElement(1));
				if((0 < verprice) || (verprice<1000)) {
					int quantity = Integer.parseInt(recievedProtocol.getOptionsElement(2));
					if((quantity>= 0) ||(recievedProtocol.getOptionsElement(3).length()<5)) {
						BigDecimal price= new BigDecimal(recievedProtocol.getOptionsElement(1));
						/*
						 * prepare the SQL resquest fpr BD 
						 */
						String query = String.format("INSERT INTO produit (nom_produit,prix_produit,stock_total_produit) VALUES('%s',%s,%s);",
								recievedProtocol.getOptionsElement(0),
								price,
								quantity
						);
						databaseManager.executeDmlQuery(query);
						return  ProtocolFactory.createSuccessProtocol();
					}else {
						logger.error("wrong cause : invalid quantity ");	
						return ProtocolFactory.createErrorProtocol("le produit n'est pas ajouter cause : la quantit� n'est pas possible  ");
					}
				}else {
					logger.error("wrong cause : invalid price ");	
					return ProtocolFactory.createErrorProtocol("le produit n'est pas ajouter cause : le prix n'est pas valide  ");
				}
			}else {
				logger.error("wrong cause: invalid name ");	
				return ProtocolFactory.createErrorProtocol("le produit n'est pas ajouter cause : nom de produit trop long ");
			}			
		}
	}catch(SQLException ex) { // v�rifier l'execption 
		ex.printStackTrace();
		String errormessage=ex.getMessage() ;
		logger.error(errormessage);	
		return ProtocolFactory.createErrorProtocol("le produit n'est pas ajouter cause : impossible de se connecter a la base de donn�es");
	}
	}
	/**
	 * function for augment the quantity of one product 
	 * @param recievedProtocol
	 * @return succes or ehec protocol
	 */
	public Protocol queryAddProducQuantity(Protocol recievedProtocol) {
		try {
			/*
			 * verify if the produc id exist 
			 */
			String queryExist = String.format("SELECT COUNT(*) AS count FROM produit Where id_produit=%s;",
					recievedProtocol.getOptionsElement(0)
			);
			ResultSet exist=databaseManager.executeSelectQuery(queryExist)  ;
			exist.next();
			int count = exist.getInt("count");
			//if different from 1, we didn't found the id of produc 
			if(count != 1) {
				logger.error("wrong cause : invalid id product  ");	
				return ProtocolFactory.createErrorProtocol(" le produit n'a pas �t� trouver ");
			}else {
				/*
				 * verify the  quantity of stock is under 1000 and superior of 0
				 */
				int addquantity= Integer.parseInt(recievedProtocol.getOptionsElement(1));
				String queryQuantity = String.format("SELECT  stock_total_produit FROM produit Where id_produit=%s;",
						recievedProtocol.getOptionsElement(0)
				);
				ResultSet quantity=databaseManager.executeSelectQuery(queryQuantity)  ;
				quantity.next();
				/*
				 * addition the quantity in db and the addquantity
				 */
				int realquantity = quantity.getInt("stock_total_produit");
				int newquantity =addquantity+realquantity;
					if((newquantity >0) || (newquantity<1000)) {
						String queryNewQuantity = String.format("UPDATE produit SET stock_total_produit = %s WHERE id_produit = %s;",
								newquantity,
								recievedProtocol.getOptionsElement(0)
						);
						databaseManager.executeDmlQuery(queryNewQuantity);
						return  ProtocolFactory.createSuccessProtocol();
					}else {
						logger.error("wrong cause : invalid price  ");	
						return ProtocolFactory.createErrorProtocol(" le prix n'est pas possible  ");
					}

			}
				
			}catch(SQLException ex) { // v�rifier l'execption 
				ex.printStackTrace();
				String errormessage=ex.getMessage() ;
				logger.error(errormessage);	
				return ProtocolFactory.createErrorProtocol("le produit n'est pas ajouter cause : impossible de se connecter a la base de donn�es");
			}
		
		}
	/**
	 * function for remove some stock of one product 
	 * @param recievedProtocol
	 * @return succes or echec protocol 
	 */
	public Protocol queryRemoveProducQuantity(Protocol recievedProtocol) {
		try {
			/*
			 * verify if the produc id exist 
			 */
			String queryExist = String.format("SELECT COUNT(*) AS count FROM produit Where id_produit=%s;",
					recievedProtocol.getOptionsElement(0)
			);
			ResultSet exist=databaseManager.executeSelectQuery(queryExist)  ;
			exist.next();
			int count = exist.getInt("count");
			//if different from 1, we didn't found the id of produc 
			if(count != 1) {
				logger.error("wrong cause : invalid id product  ");	
				return ProtocolFactory.createErrorProtocol(" le produit n'a pas �t� trouver ");
			}else {
				/*
				 * verify the  quantity of stock is under 1000 and superior of 0
				 */
				int addquantity= Integer.parseInt(recievedProtocol.getOptionsElement(1));
				String queryQuantity = String.format("SELECT  stock_total_produit FROM produit Where id_produit=%s;",
						recievedProtocol.getOptionsElement(0)
				);
				ResultSet quantity=databaseManager.executeSelectQuery(queryQuantity)  ;
				quantity.next();
				/*
				 * addition the quantity in db and the addquantity
				 */
				int realquantity = quantity.getInt("stock_total_produit");
				int newquantity =addquantity-realquantity;
					if((newquantity >0) || (newquantity<1000)) {
						String queryNewQuantity = String.format("UPDATE produit SET stock_total_produit = %s WHERE id_produit = %s;",
								newquantity,
								recievedProtocol.getOptionsElement(0)
						);
						databaseManager.executeDmlQuery(queryNewQuantity);
						return  ProtocolFactory.createSuccessProtocol();
					}else {
						logger.error("wrong cause : invalid price  ");	
						return ProtocolFactory.createErrorProtocol(" le prix n'est pas possible  ");
					}

			}
				
			}catch(SQLException ex) { // v�rifier l'execption 
				ex.printStackTrace();
				String errormessage=ex.getMessage() ;
				logger.error(errormessage);	
				return ProtocolFactory.createErrorProtocol("le produit n'est pas ajouter cause : impossible de se connecter a la base de donn�es");
			}	
	}
	/**
	 *  function for delete a product on the table 
	 * @param recievedProtocol
	 * @return echec or success protocol
	 */
	public Protocol queryRemoveProduc(Protocol recievedProtocol) {
		try {
			/*
			 * verify if the produc id exist 
			 */
			String queryExist = String.format("SELECT COUNT(*) AS count FROM produit Where id_produit=%s;",
					recievedProtocol.getOptionsElement(0)
			);
			ResultSet exist=databaseManager.executeSelectQuery(queryExist)  ;
			exist.next();
			int count = exist.getInt("count");
			//if different from 1, we didn't found the id of produc 
			if(count != 1) {
				logger.error("wrong cause : invalid id product  ");	
				return ProtocolFactory.createErrorProtocol(" le produit n'a pas �t� trouver ");
			}else {
				/*
				 * delete de product
				 */
						String queryDeleteProduct = String.format("DELETE FROM produit WHERE id_produit='%s';",
								recievedProtocol.getOptionsElement(0)
						);
						databaseManager.executeDmlQuery(queryDeleteProduct);
						return  ProtocolFactory.createSuccessProtocol();
			}
				
			}catch(SQLException ex) { // v�rifier l'execption 
				ex.printStackTrace();
				String errormessage=ex.getMessage() ;
				logger.error(errormessage);	
				return ProtocolFactory.createErrorProtocol("le produit n'a  pas pus �tre supprimer  cause : impossible de se connecter a la base de donn�es");
			}
	}
	
}
