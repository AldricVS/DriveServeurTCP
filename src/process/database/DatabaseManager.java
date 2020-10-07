package process.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import logger.LoggerUtility;

/**
 * Main class containing all methods to connect and query a PostgreSQL database.
 * @author Aldric Vitali Silvestre
 */
public class DatabaseManager {
	private static Logger logger = LoggerUtility.getLogger(DatabaseManager.class, LoggerUtility.LOG_PREFERENCE);
	
	private Connection connection;
	
	/**
	 * Create the database manager specifying data needed to connect.<p>
	 * This will try to create a connection between the application and the database.
	 * @param url the database url without following protocol ("jdbc:postgresql://" will be added in front of the url provided).
	 * @param user the user that owns the database (or at least can use it)
	 * @param password the user's password.
	 * @throws SQLException if a database access error occurs. This can be happening if database server is down or one of the arguments is not valid.
	 */
	public DatabaseManager(String url, String user, String password) throws SQLException{
		logger.info("Start connection to " + url);
		connection = DriverManager.getConnection("jdbc:postgresql://" + url, user, password);
		//if we are here, we are connected
		logger.info("Database connected !");
	}
	
	/**
	 * Permits to execute a single query and get result.
	 * @param query the query to execute 
	 * @return what database respond
	 * @throws SQLException if an error occurs while asking database
	 */
	public ResultSet excecuteSingleQuery(String query) throws SQLException{
		Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		return statement.executeQuery(query);
	}

}
