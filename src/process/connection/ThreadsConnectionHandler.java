package process.connection;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import data.Protocol;
import data.User;
import data.enums.ActionCodes;
import logger.LoggerUtility;
import process.database.DatabaseManager;
import process.protocol.ProtocolFactory;

/**
 * Main class of the server : it will wait for new clients connecting and create
 * threads for newcommers.
 * 
 * @author Aldric Vitali Silvestre <aldric.vitali@outlook.fr>
 * @author D'Urso Raphaël <rdurso@outlook.fr>
 */
public class ThreadsConnectionHandler extends Thread {
	private static Logger logger = LoggerUtility.getLogger(ThreadsConnectionHandler.class,
			LoggerUtility.LOG_PREFERENCE);

	private boolean isListening = true;

	/**
	 * We store user list in order to keep a trace of all users (useful for checking
	 * if any user want to connect twice for exemple)
	 */
	private List<User> users = new ArrayList<>();

	private ServerSocket serverSocket;

	private DatabaseManager databaseManager;

	/**
	 * Creating an instance of the class will start listenning for new clients
	 * 
	 * @param port the port where to listen for clients
	 */
	public ThreadsConnectionHandler(int port, String databaseUrl, String databaseUser, String databasePassword) {
		try {
			// connect to database
			databaseManager = new DatabaseManager(databaseUrl, databaseUser, databasePassword);
			serverSocket = new ServerSocket(port);
			start();
			logger.info("Server waiting for clients on port " + port);
		} catch (IOException e) {
			// we can't do anything here, we have to stop the application
			String errorMessage = "Cannot connect on port " + port + " : " + e.getMessage();
			System.err.println(errorMessage);
			logger.fatal(errorMessage);
			System.exit(-1);
		} catch (SQLException e) {
			// we can't do anything here, we have to stop the application
			String errorMessage = "Cannot connect to database : " + e.getMessage();
			System.err.println(errorMessage);
			logger.fatal(errorMessage);
			System.exit(-1);
		}

		// here, the thread will start end witing for new clients
	}

	@Override
	public void run() {
		while (isListening) {
			try {
				// start a new thread for the client
				ClientThread clientThread = new ClientThread(serverSocket.accept(), this);
				clientThread.start();
				String message = "New client connected";
				logger.info(message);
			} catch (IOException e) {
				// error happened, stop communications
				String errorMessage = "Error while listening for new clients : " + e.getMessage();
				logger.error(errorMessage);
				isListening = false;
			}
		}
	}

	/*
	 * 
	 * @todo faire une actualisation de la connection
	 */
	public Protocol queryConnectionDatabase(String login, String password, boolean isAdmin) {
		try {
			ResultSet result;
			// the select will not be done in the same table if client is admin or not
			if (isAdmin) {
				result = databaseManager.executeSelectQueryParams(
						"SELECT COUNT(*) AS count FROM administrateur WHERE nom_employe=? AND mot_de_passe_employe=?",
						login, password);
			} else {
				result = databaseManager.executeSelectQueryParams(
						"SELECT COUNT(*) AS count FROM employe WHERE nom_employe=? AND mot_de_passe_employe=?", login,
						password);
			}

			// get the number returned
			result.next();
			int count = result.getInt("count");
			// if different from 1, we didn't found the user in the database
			if (count != 1) {
				return ProtocolFactory.createErrorProtocol("Le combo identifiant / mot de passe n'est pas valide");
			} else {
				return ProtocolFactory.createSuccessProtocol();
			}

		} catch (IllegalArgumentException e) {
			logger.error("Error while preparing statement : " + e.getMessage());
			// should never happen in normal circumstances
			return ProtocolFactory.createErrorProtocol(
					"La requête envoyée a un problème de coception. Nous ne pouvons pas accéder à votre requête pour l'instant");

		} catch (SQLException e) {
			String errorMessage = "Error while communicating with database : " + e.getMessage();
			logger.error(errorMessage);
			// we will send to client an error message
			return ProtocolFactory.createErrorProtocol("Erreur lors de la communication avec la base de données.");
		}
	}

	public void addUser(User user) {
		users.add(user);
	}

	public void removeUser(User user) {
		users.remove(user);
	}

	public void updateLastConnexionUser(User user) {
		try {
			databaseManager.executeDmlQueryParams(
					"UPDATE Employe SET date_derniere_connexion_employe = NOW() WHERE nom_employe = '" + user.getName()
							+ "'");
		} catch (IllegalArgumentException | SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Check if user is already in list
	 * 
	 * @param user the user to check
	 * @returns if user is already in list or not
	 */
	public boolean isUserInList(User user) {
		for (User u : users) {
			if (user.getName().equals(u.getName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * function for add a new product at the database
	 * 
	 * @param recievedProtocol
	 * @return succes or echec protocol
	 * @TODO number format execption
	 */
	public Protocol queryAddNewProduct(Protocol recievedProtocol) {
		try {
			/*
			 * verify if the produc exist
			 */

			ResultSet exist = databaseManager.executeSelectQueryParams(
					"SELECT COUNT(*) AS count FROM produit Where nom_produit=?", recievedProtocol.getOptionsElement(0));
			exist.next();
			int count = exist.getInt("count");
			// if different from 1, we didn't found the id of produc
			if (count != 0) {
				logger.error("wrong cause : invalid id product  ");
				return ProtocolFactory.createErrorProtocol(" le produit existe déja  ");
			} else {
				/**
				 * veirify if the name is not more than 50 caracter
				 */
				if (recievedProtocol.getOptionsElement(0).length() < 51) {
					/*
					 * verification of price between 0.1 and 999.99
					 */
					float verprice = Float.parseFloat(recievedProtocol.getOptionsElement(1));
					if ((0 < verprice) || (verprice < 1000)) {
						int quantity = Integer.parseInt(recievedProtocol.getOptionsElement(2));
						if ((quantity >= 0) || (recievedProtocol.getOptionsElement(3).length() < 5)) {
							BigDecimal price = new BigDecimal(recievedProtocol.getOptionsElement(1));
							/*
							 * prepare the SQL resquest fpr BD
							 */
							boolean query;
							query = databaseManager.executeDmlQueryParams(
									"INSERT INTO produit (nom_produit,prix_produit,stock_total_produit) VALUES(?,?,?)",
									recievedProtocol.getOptionsElement(0), price, quantity);
							if (query) {
								return ProtocolFactory.createSuccessProtocol();
							} else {
								return ProtocolFactory.createErrorProtocol("n'a pas pus ajouter le produit");
							}

						} else {
							logger.error("wrong cause : invalid quantity ");
							return ProtocolFactory.createErrorProtocol(
									"le produit n'est pas ajouter cause : la quantité n'est pas possible  ");
						}
					} else {
						logger.error("wrong cause : invalid price ");
						return ProtocolFactory
								.createErrorProtocol("le produit n'est pas ajouter cause : le prix n'est pas valide");
					}
				} else {
					logger.error("wrong cause: invalid name ");
					return ProtocolFactory
							.createErrorProtocol("le produit n'est pas ajouter cause : nom de produit trop long");
				}
			}
		} catch (SQLException ex) { // vérifier l'execption
			ex.printStackTrace();
			String errormessage = ex.getMessage();
			logger.error(errormessage);
			return ProtocolFactory.createErrorProtocol(
					"le produit n'est pas ajouté. Cause : impossible de se connecter a la base de données");
		} catch (NumberFormatException ex) {
			String errormessage = ex.getMessage();
			logger.error(errormessage);
			return ProtocolFactory
					.createErrorProtocol("le produit n'est pas ajouté. Cause : les données sont invalides ");
		}
	}

	/**
	 * function for augment the quantity of one product
	 * 
	 * @param recievedProtocol
	 * @return succes or ehec protocol
	 */
	public Protocol queryAddProductQuantity(Protocol recievedProtocol) {
		try {
			/*
			 * verify if the produc id exist
			 */

			ResultSet exist = databaseManager.executeSelectQueryParams(
					"SELECT COUNT(*) AS count FROM produit Where id_produit=?",
					Integer.parseInt(recievedProtocol.getOptionsElement(0)));
			exist.next();
			int count = exist.getInt("count");
			// if different from 1, we didn't found the id of produc
			if (count != 1) {
				logger.error("wrong cause : invalid id product  ");
				return ProtocolFactory.createErrorProtocol(" le produit n'a pas été trouver ");
			} else {
				/*
				 * verify the quantity of stock is under 1000 and superior of 0
				 */
				int addquantity = Integer.parseInt(recievedProtocol.getOptionsElement(1));
				ResultSet quantity = databaseManager.executeSelectQueryParams(
						"SELECT  stock_total_produit FROM produit Where id_produit=?",
						Integer.parseInt(recievedProtocol.getOptionsElement(0)));
				quantity.next();
				/*
				 * addition the quantity in db and the addquantity
				 */
				int realquantity = quantity.getInt("stock_total_produit");
				int newquantity = addquantity + realquantity;
				if ((newquantity > 0) || (newquantity < 1000)) {

					Boolean querynewquantity;
					querynewquantity = databaseManager.executeDmlQueryParams(
							"UPDATE produit SET stock_total_produit = ? WHERE id_produit =?", newquantity,
							Integer.parseInt(recievedProtocol.getOptionsElement(0)));
					if (querynewquantity) {
						return ProtocolFactory.createSuccessProtocol();
					} else {
						return ProtocolFactory.createErrorProtocol("on n'a pas pus changer la quantité");
					}

				} else {
					logger.error("wrong cause : invalid price  ");
					return ProtocolFactory.createErrorProtocol(" le prix n'est pas possible  ");
				}

			}

		} catch (SQLException ex) { // vérifier l'execption
			ex.printStackTrace();
			String errormessage = ex.getMessage();
			logger.error(errormessage);
			return ProtocolFactory.createErrorProtocol(
					"le produit n'est pas ajouter cause : impossible de se connecter a la base de données");
		} catch (NumberFormatException ex) {
			String errormessage = ex.getMessage();
			logger.error(errormessage);
			return ProtocolFactory
					.createErrorProtocol("le produit n'est pas modifié . Cause : les données sont invalides ");
		}

	}

	/**
	 * function for remove some stock of one product
	 * 
	 * @param recievedProtocol
	 * @return succes or echec protocol
	 */
	public Protocol queryRemoveProductQuantity(Protocol recievedProtocol) {
		try {
			/*
			 * verify if the produc id exist
			 */

			ResultSet exist = databaseManager.executeSelectQueryParams(
					"SELECT COUNT(*) AS count FROM produit Where id_produit=?",
					Integer.parseInt(recievedProtocol.getOptionsElement(0)));
			exist.next();
			int count = exist.getInt("count");
			// if different from 1, we didn't found the id of produc
			if (count != 1) {
				logger.error("wrong cause : invalid id product  ");
				return ProtocolFactory.createErrorProtocol(" le produit n'a pas été trouver ");
			} else {
				/*
				 * verify the quantity of stock is under 1000 and superior of 0
				 */
				int addquantity = Integer.parseInt(recievedProtocol.getOptionsElement(1));
				ResultSet quantity = databaseManager.executeSelectQueryParams(
						"SELECT  stock_total_produit FROM produit Where id_produit=?",
						Integer.parseInt(recievedProtocol.getOptionsElement(0)));
				quantity.next();
				/*
				 * addition the quantity in db and the addquantity
				 */
				int realquantity = quantity.getInt("stock_total_produit");
				int newquantity = realquantity - addquantity;
				if ((newquantity > 0) && (newquantity < 1000)) {

					Boolean removeQuantity;
					removeQuantity = databaseManager.executeDmlQueryParams(
							"UPDATE produit SET stock_total_produit = ? WHERE id_produit =?", newquantity,
							Integer.parseInt(recievedProtocol.getOptionsElement(0)));
					if (removeQuantity) {
						return ProtocolFactory.createSuccessProtocol();
					} else {
						return ProtocolFactory.createErrorProtocol("on n'a pas pus retirer la quantite demander");
					}

				} else {
					logger.error("Wrong cause : invalid quantity");
					return ProtocolFactory
							.createErrorProtocol("La nouvelle quantité n'est pas valide : " + newquantity);
				}

			}

		} catch (SQLException ex) { // vérifier l'execption
			ex.printStackTrace();
			String errormessage = ex.getMessage();
			logger.error(errormessage);
			return ProtocolFactory.createErrorProtocol(
					"le produit n'est pas modifier cause : impossible de se connecter a la base de données");
		} catch (NumberFormatException ex) {
			String errormessage = ex.getMessage();
			logger.error(errormessage);
			return ProtocolFactory
					.createErrorProtocol("le produit n'est pas ajouté. Cause : les données sont invalides ");
		}
	}

	/**
	 * function for delete a product on the table
	 * 
	 * @param recievedProtocol
	 * @return echec or success protocol
	 * 
	 */
	public Protocol queryRemoveProduct(Protocol recievedProtocol) {
		try {
			/*
			 * verify if the produc id exist
			 */
			ResultSet exist = databaseManager.executeSelectQueryParams(
					"SELECT COUNT(*) AS count FROM produit Where id_produit=? ",
					Integer.parseInt(recievedProtocol.getOptionsElement(0)));
			exist.next();
			int count = exist.getInt("count");
			// if different from 1, we didn't found the id of produc
			if (count != 1) {
				logger.error("Wrong cause : invalid id product");
				return ProtocolFactory.createErrorProtocol("Le produit n'a pas été trouvé ");
			} else {
				/*
				 * delete de product delte produit ins favorit table before produit table
				 */
				Boolean deleteProduct;

				deleteProduct = databaseManager.executeDmlQueryParams("DELETE FROM Favori WHERE id_produit=?",
						Integer.parseInt(recievedProtocol.getOptionsElement(0)));
				deleteProduct = databaseManager.executeDmlQueryParams("DELETE FROM promotion WHERE id_produit=?",
						Integer.parseInt(recievedProtocol.getOptionsElement(0)));

				deleteProduct = databaseManager.executeDmlQueryParams("DELETE FROM produit WHERE id_produit=?",
						Integer.parseInt(recievedProtocol.getOptionsElement(0)));
				if (deleteProduct) {
					return ProtocolFactory.createSuccessProtocol();
				} else {
					return ProtocolFactory.createErrorProtocol("on n'a pas pus supprimer le produit");
				}

			}

		} catch (SQLException ex) { // vérifier l'execption
			ex.printStackTrace();
			String errormessage = ex.getMessage();
			logger.error(errormessage);
			return ProtocolFactory.createErrorProtocol(
					"le produit n'a  pas pus être supprimé  cause : impossible de se connecter a la base de données");
		} catch (NumberFormatException ex) {
			String errormessage = ex.getMessage();
			logger.error(errormessage);
			return ProtocolFactory.createErrorProtocol("le produit n'est supprimé  Cause : l'id est invalide");
		}
	}

	/**
	 * function use went the order is finish
	 * 
	 * @param recievedProtocol
	 * @return echec or success protocol
	 */
	public Protocol queryValidOrder(Protocol recievedProtocol) {
		try {
			/*
			 * verify if the order id exist
			 */

			ResultSet exist = databaseManager.executeSelectQueryParams(
					"SELECT COUNT(*) AS count FROM commande Where id_commande=?",
					Integer.parseInt(recievedProtocol.getOptionsElement(0)));
			exist.next();
			int count = exist.getInt("count");
			// if different from 1, we didn't found the id of produc
			if (count != 1) {
				logger.error("wrong cause : invalid id command  ");
				return ProtocolFactory.createErrorProtocol(" la commande n'a pas été trouver  n'a pas été trouver ");
			} else {
				Boolean queryDelete;
				queryDelete = databaseManager.executeDmlQueryParams("DELETE FROM Produit_commande WHERE id_commande=?",
						Integer.parseInt(recievedProtocol.getOptionsElement(0)));
				if (queryDelete) {
					queryDelete = databaseManager.executeDmlQueryParams("DELETE FROM commande WHERE id_commande=?",
							Integer.parseInt(recievedProtocol.getOptionsElement(0)));
					if (queryDelete) {
						return ProtocolFactory.createSuccessProtocol();
					} else {
						return ProtocolFactory.createErrorProtocol("on n'a pas pus facilité  la commande");
					}
				} else {
					return ProtocolFactory.createErrorProtocol("on n'a pas supprimer les produit commander ");
				}
			}
		} catch (SQLException ex) { // vérifier l'execption
			ex.printStackTrace();
			String errormessage = ex.getMessage();
			logger.error(errormessage);
			return ProtocolFactory.createErrorProtocol(
					"la commande n'a pas pus être validée. Cause : impossible de se connecter a la base de données");
		} catch (NumberFormatException ex) {
			String errormessage = ex.getMessage();
			logger.error(errormessage);
			return ProtocolFactory.createErrorProtocol("la commande n'a pas été validée. Cause : l'id est invalide");
		}
	}

	/**
	 * function for add new employe using only for admin
	 * 
	 * @param recievedProtocol
	 * @param userAsking       the user who want to do this. It must be an
	 *                         administrator in order to do this operation
	 * @return a protocol permiting to the client to now if the operation is a
	 *         success or not.
	 * 
	 */
	public Protocol queryAddEmploye(Protocol recievedProtocol, User userAsking) {
		// first, we want to check if user is admin or not
		if (!userAsking.isAdmin()) {
			return ProtocolFactory.createErrorProtocol(
					"Vous n'êtes pas un administrateur, vous n'êtes donc pas autorisés à faire ceci.");
		}
		try {
			ResultSet exist = databaseManager.executeSelectQueryParams(
					"SELECT COUNT(*) AS count FROM Employe Where nom_employe=?", recievedProtocol.getOptionsElement(0));
			exist.next();
			int count = exist.getInt("count");
			// if different from 1, we didn't found the id of produc
			if (count != 0) {
				logger.error("wrong cause : have a employe with this name  ");
				return ProtocolFactory.createErrorProtocol(" il y a déja un employer ayant ce nom ");
			} else {

				Boolean newEmploye;
				String password = recievedProtocol.getOptionsElement(1);
				if (password.length() < 4) {
					logger.error("invalid password");
					return ProtocolFactory.createErrorProtocol(
							"Mot de passe invalide : un mot de passe doit être composé d'au moins 4 caractères");

				} else {
					newEmploye = databaseManager.executeDmlQueryParams(
							"INSERT INTO Employe (nom_employe,mot_de_passe_Employe) VALUES(?,?)",
							recievedProtocol.getOptionsElement(0), password);
					if (newEmploye) {
						return ProtocolFactory.createSuccessProtocol();
					} else {
						return ProtocolFactory.createErrorProtocol("on n'a pas pus ajouter le nouvelle employer");
					}
				}

			}
		} catch (SQLException ex) { // vérifier l'execption
			ex.printStackTrace();
			String errormessage = ex.getMessage();
			logger.error(errormessage);
			return ProtocolFactory.createErrorProtocol(
					"l'employer n'a pas pu être ajouter: impossible de se connecter à la base de donnée");
		}
	}

	/**
	 * 
	 * @param recievedProtocol
	 * @param userAsking
	 * @return echec or sucess protocol
	 */
	public Protocol queryDeleteEmploye(Protocol recievedProtocol, User userAsking) {
		// first, we want to check if user is admin or not
		if (!userAsking.isAdmin()) {
			return ProtocolFactory.createErrorProtocol(
					"Vous n'êtes pas un administrateur, vous n'êtes donc pas autorisés à faire ceci.");
		}
		try {
			/*
			 * verify if the order id exist
			 */

			ResultSet exist = databaseManager.executeSelectQueryParams(
					"SELECT COUNT(*) AS count FROM Employe Where nom_employe=?", recievedProtocol.getOptionsElement(0));
			exist.next();
			int count = exist.getInt("count");
			// if different from 1, we didn't found the id of produc
			if (count != 1) {
				logger.error("wrong cause : doesn't have a employe with this name  ");
				return ProtocolFactory.createErrorProtocol(" il n'y a pas d'employer de ce nom ");
			} else {
				Boolean deleteEmploye;

				deleteEmploye = databaseManager.executeDmlQueryParams("DELETE FROM Employe WHERE nom_employe=?",
						recievedProtocol.getOptionsElement(0));
				if (deleteEmploye) {
					return ProtocolFactory.createSuccessProtocol();
				} else {
					return ProtocolFactory.createSuccessProtocol();
				}

			}
		} catch (SQLException ex) { // vérifier l'execption
			ex.printStackTrace();
			String errormessage = ex.getMessage();
			logger.error(errormessage);
			return ProtocolFactory.createErrorProtocol(
					"l'employer n'a  pas pus être supprimer  cause : impossible de se connecter a la base de données");
		}
	}

	/**
	 * function use for seen all product
	 * 
	 * @param recievedProtocol
	 * @return the list of product on protocol
	 */
	Protocol queryListProduct(Protocol recievedProtocol) {
		try {
			ResultSet list;
			list = databaseManager.executeSelectQueryParams(
					"select produit.id_produit,nom_produit,prix_produit,stock_total_produit,prix_promotion  from produit LEFT OUTER JOIN promotion on produit.id_produit = promotion.id_produit");
			// create a list for insert product
			List<String> listProduct = new ArrayList<String>();
			while (list.next()) {
				listProduct.add(list.getString(1) + ";" + list.getString(2) + ";" + list.getString(3) + ";"
						+ list.getString(4) + ";" + list.getString(5));

			}
			return ProtocolFactory.listProtocol(listProduct);
		} catch (SQLException ex) {
			ex.printStackTrace();
			String errormessage = ex.getMessage();
			logger.error(errormessage);
			return ProtocolFactory.createErrorProtocol("on n'a pas pus afficher la liste des produit");

		}

	}

	/**
	 * @param recievedProtocol
	 * @return the list of order on protocol
	 */
	Protocol queryListOrder(Protocol recievedProtocol) {
		try {
			ResultSet list;
			list = databaseManager.executeSelectQueryParams("select * from commande");
			// create a list for insert product
			List<String> listOrder = new ArrayList<String>();
			while (list.next()) {
				// get total price
				ResultSet totalPrice = databaseManager.executeSelectQueryParams(
						"SELECT SUM(prix_total_commande) FROM produit_commande WHERE id_commande =?",
						Integer.parseInt(list.getString(1)));

				if (!totalPrice.next()) {
					throw new SQLException("Impossible de calculer le prix de la commande");
				}
				listOrder.add(list.getString(1) + ";" + list.getString(2) + ";" + list.getString(3) + ";"
						+ list.getString(4) + ";" + list.getString(5) + ";" + totalPrice.getString(1));
				logger.info(listOrder.toString());

			}
			return ProtocolFactory.listProtocol(listOrder);
		} catch (SQLException ex) {
			ex.printStackTrace();
			String errormessage = ex.getMessage();
			logger.error(errormessage);
			return ProtocolFactory.createErrorProtocol("on n'a pas pus afficher la liste des commandes");

		} catch (NumberFormatException ex) {
			String errormessage = ex.getMessage();
			logger.error(errormessage);
			return ProtocolFactory
					.createErrorProtocol("La commande n'a  pas été trouver . Cause : l'id est  invalides");
		}

	}

	/**
	 * 
	 * @param recievedProtocol
	 * @param userAsking
	 * @return
	 * @TODO changer les requête SQL
	 */
	Protocol queryListEmploye(Protocol recievedProtocol, User userAsking) {
		// first, we want to check if user is admin or not
		if (!userAsking.isAdmin()) {
			return ProtocolFactory.createErrorProtocol(
					"Vous n'êtes pas un administrateur, vous n'êtes donc pas autorisés à faire ceci.");
		}
		try {
			ResultSet list;
			list = databaseManager
					.executeSelectQueryParams("select nom_employe,date_derniere_connexion_employe from Employe");
			// create a list for insert product
			List<String> listEmploye = new ArrayList<String>();
			while (list.next()) {
				listEmploye.add(list.getString(1) + ";" + list.getString(2));
			}
			return ProtocolFactory.listProtocol(listEmploye);
		} catch (SQLException ex) { // vérifier l'execption
			ex.printStackTrace();
			String errormessage = ex.getMessage();
			logger.error(errormessage);
			return ProtocolFactory.createErrorProtocol("on n'a pas pu afficher les employés");
		}
	}

	/**
	 * 
	 * @param recievedProtocol
	 * @return protocol for sucess or echec to add promotion
	 */
	Protocol queryApplyPromotion(Protocol recievedProtocol) {
		try {
			ResultSet exist;
			/*
			 * verify if the produc exist
			 */
			exist = databaseManager.executeSelectQueryParams(
					"SELECT COUNT(*) AS count FROM produit Where id_produit=?;",
					Integer.parseInt(recievedProtocol.getOptionsElement(0)));
			exist.next();
			int count = exist.getInt("count");
			// if different from 1, we didn't found the id of produc
			if (count != 1) {
				logger.error("wrong cause : invalid id product  ");
				return ProtocolFactory.createErrorProtocol(" le produit existe pas   ");
			} else {
				/*
				 * verification of price between 0.1 and 999.99
				 */ResultSet realprice;
				realprice = databaseManager.executeSelectQueryParams(
						"SELECT prix_produit	 FROM produit Where id_produit=?;",
						Integer.parseInt(recievedProtocol.getOptionsElement(0)));
				BigDecimal promotionPrice = new BigDecimal(recievedProtocol.getOptionsElement(1));

				realprice.next();
				BigDecimal initialPrice = realprice.getBigDecimal(1);
				if ((promotionPrice.compareTo(new BigDecimal("0.01"))) >= 0
						&& promotionPrice.compareTo(initialPrice) == -1) {
					/*
					 * prepare the SQL resquest fpr BD
					 */
					ResultSet promotionexist;
					/*
					 * verify if the produc exist
					 */
					promotionexist = databaseManager.executeSelectQueryParams(
							"SELECT COUNT(*) AS count FROM promotion Where id_produit=?;",
							Integer.parseInt(recievedProtocol.getOptionsElement(0)));
					promotionexist.next();
					int existP = promotionexist.getInt("count");
					// if different from 1, we didn't found the id of produc
					if (existP == 1) {
						Boolean addPromotion;
						addPromotion = databaseManager.executeDmlQueryParams(
								"UPDATE promotion SET prix_promotion = ? WHERE id_produit =?", promotionPrice,
								Integer.parseInt(recievedProtocol.getOptionsElement(0)));
						if (addPromotion) {
							return ProtocolFactory.createSuccessProtocol();
						} else {
							logger.error("Error while modifying promotion query");
							return ProtocolFactory.createErrorProtocol("n'a pas pus mdofier  cette promotion");
						}
					} else {
						Boolean addPromotion;
						addPromotion = databaseManager.executeDmlQueryParams(
								"INSERT INTO promotion (id_produit,prix_promotion)  VALUES (?,?)",
								Integer.parseInt(recievedProtocol.getOptionsElement(0)), promotionPrice);
						if (addPromotion) {
							return ProtocolFactory.createSuccessProtocol();
						} else {
							logger.error("Error while inserting promotion query");
							return ProtocolFactory.createErrorProtocol("n'a pas pus ajouter cette promotion");
						}
					}
				} else {
					logger.error("wrong cause : invalid price ");
					return ProtocolFactory.createErrorProtocol(
							"La promotion n'a pas pu être ajoutée. Verifiez que la promotion est comprise entre 0.01 € et le prix initial. Prix initial : "
									+ initialPrice + " prix promotion : " + promotionPrice);
				}

			}
		} catch (SQLException ex) { // vérifier l'execption
			ex.printStackTrace();
			String errormessage = ex.getMessage();
			logger.error(errormessage);
			return ProtocolFactory.createErrorProtocol(
					"la promotion n'a pas pus être ajouter : impossible de se connecter a la base de données");
		} catch (NumberFormatException ex) {
			String errormessage = ex.getMessage();
			logger.error(errormessage);
			return ProtocolFactory
					.createErrorProtocol("le produit n'est pas ajouté. Cause : les données sont invalides ");
		}
	}

	/**
	 * 
	 * @param recievedProtocol
	 * @return echec or sucess protocol
	 */
	Protocol queryRemovePromotion(Protocol recievedProtocol) {
		try {
			ResultSet exist;
			/*
			 * verify if the produc exist
			 */
			exist = databaseManager.executeSelectQueryParams(
					"SELECT COUNT(*) AS count FROM promotion Where id_produit=?;",
					Integer.parseInt(recievedProtocol.getOptionsElement(0)));
			exist.next();
			int count = exist.getInt("count");
			// if different from 1, we didn't found the id of produc
			if (count != 1) {
				logger.error("wrong cause : invalid id product  ");
				return ProtocolFactory.createErrorProtocol(" le produit n'a pas de pormotion   ");
			} else {
				/*
				 * prepare the SQL resquest fpr BD
				 */
				Boolean removePromotion;
				removePromotion = databaseManager.executeDmlQueryParams("delete from promotion where id_produit=?",
						Integer.parseInt(recievedProtocol.getOptionsElement(0)));
				if (removePromotion) {
					return ProtocolFactory.createSuccessProtocol();
				} else {
					return ProtocolFactory.createErrorProtocol("n'a pas pus supprimer  cette promotion");
				}
			}
		} catch (SQLException ex) { // vérifier l'execption
			ex.printStackTrace();
			String errormessage = ex.getMessage();
			logger.error(errormessage);
			return ProtocolFactory.createErrorProtocol(
					"la promotion na pas pus être supprimer  cause : impossible de se connecter a la base de données");
		}
	}

	/**
	 * 
	 * @param recievedProtocol
	 * @return succes or echec protocol
	 */
	Protocol queryRemoveOrder(Protocol recievedProtocol) {
		try {
			ResultSet exist;
			/*
			 * verify if the produc exist
			 */
			exist = databaseManager.executeSelectQueryParams(
					"SELECT COUNT(*) AS count FROM commande Where id_commande=?;",
					Integer.parseInt(recievedProtocol.getOptionsElement(0)));
			exist.next();
			int count = exist.getInt("count");
			// if different from 1, we didn't found the id of produc
			if (count != 1) {
				logger.error("wrong cause : invalid id commande  ");
				return ProtocolFactory.createErrorProtocol(" la commande existe pas    ");
			} else {
				ResultSet orderProduct;
				boolean deleteProductOrder;
				orderProduct = databaseManager.executeSelectQueryParams(
						"SELECT id_produit,quantite_commande FROM produit_commande WHERE id_commande=?",
						Integer.parseInt(recievedProtocol.getOptionsElement(0)));
				boolean removeProductOrder;
				// we add the stock taked by the commande
				while (orderProduct.next()) {
					removeProductOrder = databaseManager.executeDmlQueryParams(
							"UPDATE produit SET stock_total_produit = stock_total_produit + ? WHERE id_produit = ?",
							orderProduct.getInt(2), orderProduct.getInt(1));
					if (!removeProductOrder) {

						return ProtocolFactory.createErrorProtocol("on n'a pas pus remettre un produit dans le stock");
					}
				}
				// delete de commande from table produit_commander and commande
				deleteProductOrder = databaseManager.executeDmlQueryParams(
						"Delete from produit_commande where id_commande=? ",
						Integer.parseInt(recievedProtocol.getOptionsElement(0)));
				if (deleteProductOrder) {
					deleteProductOrder = databaseManager.executeDmlQueryParams(
							"Delete from commande where id_commande=? ",
							Integer.parseInt(recievedProtocol.getOptionsElement(0)));
					if (deleteProductOrder) {
						return ProtocolFactory.createSuccessProtocol();
					} else {
						return ProtocolFactory.createErrorProtocol("on n'a pas pus supprimer la commander");
					}

				} else {

					return ProtocolFactory.createErrorProtocol("on n'a pas pus supprimer les produit commander");
				}

			}
		} catch (SQLException ex) { // vérifier l'execpstion
			ex.printStackTrace();
			String errormessage = ex.getMessage();
			logger.error(errormessage);
			return ProtocolFactory.createErrorProtocol(
					"la commande  na pas pus être supprimer  cause : impossible de se connecter a la base de données");
		}
	}
}
