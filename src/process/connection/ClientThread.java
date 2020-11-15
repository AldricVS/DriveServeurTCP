package process.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;

import data.Protocol;
import data.User;
import data.enums.ActionCodes;
import exceptions.InvalidProtocolException;
import logger.LoggerUtility;
import process.database.DatabaseManager;
import process.protocol.ProtocolExtractor;
import process.protocol.ProtocolFactory;

/**
 * Thread dealing with a single client.
 * 
 * @author Aldric Vitali Silvestre <aldric.vitali@outlook.fr>
 * @author D'Urso Raphaël <rdurso@outlook.fr> 
 */
public class ClientThread extends Thread {
	private static Logger logger = LoggerUtility.getLogger(ClientThread.class, LoggerUtility.LOG_PREFERENCE);
	
	private User user;

	private Socket clientSocket;
	private boolean isListening = true;

	/**
	 * We keep trace of the handler of all clients thread in order to call his
	 * methods
	 */
	private ThreadsConnectionHandler handler;

	/**
	 * To communicate with client
	 */
	private PrintWriter outputFlow;
	private BufferedReader inputFlow;

	/**
	 * Make the connection with client, but user is not logged in yet
	 * 
	 * @param clientSocket
	 * @param ThreadsConnectionHandler
	 */
	public ClientThread(Socket clientSocket, ThreadsConnectionHandler threadsConnectionHandler) {
		this.clientSocket = clientSocket;
		this.handler = threadsConnectionHandler;
	}

	@Override
	public void run() {
		try {
			inputFlow = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			outputFlow = new PrintWriter(clientSocket.getOutputStream(), true);

			String inputMessage;
			Protocol protocolToSend;
			ProtocolExtractor extractor;
			
			// first, we must wait for a first message from client :
			inputMessage = inputFlow.readLine();
			try {
				protocolToSend = sendConnectionQuery(inputMessage);
			} catch (InvalidProtocolException e) {
				protocolToSend = ProtocolFactory.createErrorProtocol("Le message envoyé n'est pas valide pour le serveur. Il est attendu un message de connexion.");
				String errorMessage = "Connection message is not valid : " + e.getMessage();
				ClientThread.logger.warn(errorMessage);
				isListening = false;
			}
			//send protocol message to client
			outputFlow.println(protocolToSend.toString());
			
			//check if action code of protocol is SUCESS, in this case, we can continue the communication
			if(protocolToSend.getActionCode() != ActionCodes.SUCESS) {
				isListening = false;
				logger.info("Client not connected. reason : " + protocolToSend.getOptionsElement(0));
			}else {
				//here, user is created, so we can add it to the list
				handler.addUser(user);
				ClientThread.logger.info(user.getName() + " is now connected.");
			}
			
			/**
			 * Main loop where thread will be when connected
			 */
			while(isListening) {
				/* Get the message from user, extract the protocol from it and check what client want with the action code.
				 * If the user send protocol with code DISCONNECT, we get out the loop (we also get out of it if client suddently disconnects).
				 */
				inputMessage = inputFlow.readLine();
				
				try {
					extractor = new ProtocolExtractor(inputMessage);
				}catch(InvalidProtocolException e) {
					//if protocol is invalid, send to client an error message and return at the beggining of the loop
					protocolToSend = ProtocolFactory.createErrorProtocol("Le message envoyé n'est pas valide pour le serveur.");
					outputFlow.println(protocolToSend);
					String errorMessage = user.getName() + "'s message is not valid : " + e.getMessage();
					ClientThread.logger.warn(errorMessage);
					continue;
				}
				
				//check if protocol has code DISCONNECT
				Protocol protocolRecieved = extractor.getProtocol();
				if(protocolRecieved.getActionCode() == ActionCodes.DISCONNECT) {
					ClientThread.logger.info(user.getName() + " has disconnected");
					break;
				}else {
					protocolToSend = askToServer(protocolRecieved);
				}
				outputFlow.println(protocolToSend.toString());
				
				
			}
			
		} catch (SocketException e) {
			// if we are here, it probably means that client has not disconnected properly
			String errorMessage = "Communication loss with client : " + e.getMessage();
			ClientThread.logger.error(errorMessage);
		} catch (IOException e) {
			String errorMessage = "Error while communicating with client : " + e.getMessage();
			ClientThread.logger.error(errorMessage);
		} finally {
			//we have to remove this user from the list before exiting
			if(user != null) {
				handler.removeUser(user);
			}
			closeConnection();
		}
	}
	
	private void closeConnection() {
		logger.info("Client disconnected.");
		try {
			inputFlow.close();
			outputFlow.close();
			clientSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}	
		
	}

	/**
	 * Send the first protocol of the connection. If all succeded, the user attribute is initialized
	 * @param inputMessage the message recevied by the client
	 * @return the protocol to send back to server
	 * @throws InvalidProtocolException
	 */
	private Protocol sendConnectionQuery(String inputMessage) throws InvalidProtocolException{
		ProtocolExtractor extractor = new ProtocolExtractor(inputMessage);
		
		// now we can check if message content is valid
		extractor.assertActionCodeValid(ActionCodes.CONNECTION_ADMIN, ActionCodes.CONNECTION_NORMAL);
		extractor.assertOptionsNumberValid(2);
		
		Protocol protocol = extractor.getProtocol();

		//get data from protocol
		boolean isAdmin = protocol.getActionCode() == ActionCodes.CONNECTION_ADMIN;
		String login = protocol.getOptionsElement(0);
		String password = protocol.getOptionsElement(1);
		
		//we can finally send the query to the database
		Protocol answerProtocol = handler.queryConnectionDatabase(login, password, isAdmin);
		 
		if(answerProtocol.getActionCode() == ActionCodes.SUCESS) {
			//before sending it, we need to know if user is already connected to server
			user = new User(login, isAdmin);
			if(handler.isUserInList(user)) {
				answerProtocol = ProtocolFactory.createErrorProtocol("Un client est déja connecté avec cet identifiant.");
			}
		}
		return answerProtocol;
	}
	
	/**
	 * Main method handling all queries from client to server.
	 * @param recievedProtocol the protocol recieved from client
	 * @return the answer to send to client
	 */
	private Protocol askToServer(Protocol recievedProtocol) {
		
		switch(recievedProtocol.getActionCode()) {

			case ADD_NEW_PRODUCT  :
				/**
				 * we verify if the number of atributs is have the right number 
				 */
					if(verifyAttributNumber(3,recievedProtocol )) {
						return handler.queryAddNewProduct(recievedProtocol);
						  
								// on return le protocol correspondant 
					} else{
						logger.error("error for add new product ");
						
					}
			break;	
			case ADD_PRODUCT_QUANTITY :
				if(verifyAttributNumber(2,recievedProtocol )) {
					return handler.queryAddProductQuantity(recievedProtocol);
				}else {
					logger.error("error for update product price ");
				}
			break;
			case REMOVE_PRODUCT_QUANTITY :
				if(verifyAttributNumber(2,recievedProtocol )) {
					return handler.queryRemoveProductQuantity(recievedProtocol);
				}else {
					logger.error("error for update product price ");
				}
			break;
			case REMOVE_PRODUCT_DEFINITELY :
				if(verifyAttributNumber(1,recievedProtocol )) {
					return handler.queryRemoveProduct(recievedProtocol);
				}else {
					logger.error("error for delete product ");
				}
			break;	
			case VALIDATE_ORDER:
				if(verifyAttributNumber(1,recievedProtocol )) {
					return handler.queryValidOrder(recievedProtocol);
				}else {
					logger.error("error for valid order  ");
				}
				break;
			case ADD_EMPLOYE :
				if(verifyAttributNumber(2,recievedProtocol )) {
					return handler.queryAddEmploye(recievedProtocol, user);
				}else {
					logger.error("error for add Employe  ");
				}
			break;
			case REMOVE_EMPLOYE :
				if(verifyAttributNumber(2,recievedProtocol )) {
					return handler.queryDeleteEmploye(recievedProtocol, user);
				}else {
					logger.error("error for delete Employe  ");
				}
			break;
			case GET_PRODUCT_LIST :
				
				if(verifyAttributNumber(0,recievedProtocol )) {
					return handler.queryListProduct(recievedProtocol);
				}else {
					logger.error("error for send list of product  ");
				}
			break;
			case 	GET_ORDER_LIST :
				if(verifyAttributNumber(0,recievedProtocol )) {
					return handler.queryListOrder(recievedProtocol);
				}else {
					logger.error("error for send list of Order ");
				}
				
			break;
			case GET_EMPLOYEE_LIST :
				if(verifyAttributNumber(0,recievedProtocol )) {
					return handler.queryListEmploye(recievedProtocol,user);
				}else {
					logger.error("error for send list of employe ");
				}
			break;
			case APPLY_PROMOTION :
				if(verifyAttributNumber(2,recievedProtocol )) {
					return handler.queryApplyPromotion(recievedProtocol);
				}else {
					logger.error("error for apply promotion ");
				}
			break;
			case REMOVE_PROMOTION :
				if(verifyAttributNumber(1,recievedProtocol )) {
					return handler.queryRemovePromotion(recievedProtocol);
				}else {
					logger.error("error for removed  promotion ");
				}
			break;
			case DELETE_ORDER :
				if(verifyAttributNumber(1,recievedProtocol )) {
					return handler.queryRemoveOrder(recievedProtocol);
				}else {
					logger.error("error for removed order ");
				}
			break;
			
			default:
				return ProtocolFactory.createErrorProtocol("L'action demandée n'est pas reconnue par le serveur");
		}
		
		return null;
	}
	
	/**
	 * method use for verify if the number of attribute is right 
	 * @param nbAttribut
	 * @param recievdeProtocol
	 * @return if the number of attribute corresponding at the action code 
	 */
	
	private boolean verifyAttributNumber(int nbAttribut,Protocol recievdeProtocol) {
			if(nbAttribut== recievdeProtocol.getOptionsListSize() ) {
				return true;
			}
		
	return false;
	}
}
