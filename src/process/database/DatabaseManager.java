package process.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import logger.LoggerUtility;

/**
 * Main class containing all methods to connect and query a PostgreSQL database.
 * @author Aldric Vitali Silvestre <aldric.vitali@outlook.fr>
 */
public class DatabaseManager {
	private static Logger logger = LoggerUtility.getLogger(DatabaseManager.class, LoggerUtility.LOG_PREFERENCE);
	private final int LOGIN_TIMEOUT = 30;
	private final int QUERY_TIMEOUT = 30;
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
		DriverManager.setLoginTimeout(LOGIN_TIMEOUT);
		connection = DriverManager.getConnection("jdbc:postgresql://" + url, user, password);
		//if we are here, we are connected
		logger.info("Database connected !");
	}
	
	public void closeConnection() {
		try {
			connection.close();
		} catch (SQLException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Permits to execute a single query and get result.
	 * @deprecated Security issues (vulnerable to SQL injection for example)
	 * @param query the query to execute 
	 * @return what database respond
	 * @throws SQLException if an error occurs while asking database
	 */
	public ResultSet executeSelectQuery(String query) throws SQLException{
		Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		return statement.executeQuery(query);
	}
	
	/**
	 * Excecute a select query specifiying parameters aside of query string (safer than putting parameters alone)
	 * @param query the SELECT query to do to the database. Instead of putting parameters directely in it (with {@code String#format()} for instance),
	 * they must be replace with '?'. For example :
	 * <pre>SELECT * FROM table WHERE id_table=?</pre>
	 * 
	 * @param params variable number of parameters which will be used in order to replace all '?' in initial query. 
	 * @return the ResultSet containing all data needed
	 * @throws IllegalArgumentException if the number of '?' in the query is not the same as the number of parameters provided 
	 * @throws SQLException if an error while communicating database occurs
	 */
	public ResultSet executeSelectQueryParams(String query, Object... params) throws SQLException, IllegalArgumentException{
		//we create the prepared statement from the connection and add query
		PreparedStatement preparedStatement = connection.prepareStatement(query);
		preparedStatement.setQueryTimeout(QUERY_TIMEOUT);
		//we have to check if we have the same numbers of params as wanted in the preparedStatement
		ParameterMetaData parameterMetaData = preparedStatement.getParameterMetaData();
		if(parameterMetaData.getParameterCount() != params.length) {
			throw new IllegalArgumentException(
					"Le nombre de paramètres nécéssaires de la requête et le nombre de paramètres fournis n'est pas le même.\n"
					+ "Nombre de paramètres nécéssaires : " + parameterMetaData.getParameterCount() + "\n"
					+ "Nombre de paramètres fournis : " + params.length);
		}
		
		//now, we can append all options one by one
		int counter = 1;
		for(Object object : params) {
			preparedStatement.setObject(counter, object);
			counter++;
		}
		try {
			return preparedStatement.executeQuery();
		}catch (SQLTimeoutException e) {
			logger.error("Query timeouts exceed");
			throw new SQLException(e);
		}
	}
	
	/**
	 * Permits to execute query that don't return result.
	 * @deprecated Security issues (vulnerable to SQL injection for example)
	 * @param query the query to execute 
	 * @return true if query has modified something, false else
	 * @throws SQLException if an error occurs while asking database
	 */
	public boolean executeDmlQuery(String query) throws SQLException{
		Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		
		return statement.executeUpdate(query) > 0;
		
	}
	
	/**
	 * Excecute a DML query specifiying parameters aside of query string (safer than putting parameters alone)
	 * @param query the DML query to do to the database. Instead of putting parameters directely in it (with {@code String#format()} for instance),
	 * they must be replace with '?'. For example :
	 * <pre>UPDATE FROM table SET name=? WHERE id_table=?</pre>
	 * 
	 * @param params variable number of parameters which will be used in order to replace all '?' in initial query. 
	 * @return true if query has modified something, false else
	 * @throws IllegalArgumentException if the number of '?' in the query is not the same as the number of parameters provided 
	 * @throws SQLException if an error while communicating database occurs
	 */
	public boolean executeDmlQueryParams(String query, Object... params) throws SQLException, IllegalArgumentException{
		//we create the prepared statement from the connection and add query
		PreparedStatement preparedStatement = connection.prepareStatement(query);
		preparedStatement.setQueryTimeout(QUERY_TIMEOUT);
		//we have to check if we have the same numbers of params as wanted in the preparedStatement
		ParameterMetaData parameterMetaData = preparedStatement.getParameterMetaData();
		if(parameterMetaData.getParameterCount() != params.length) {
			throw new IllegalArgumentException(
					"Le nombre de paramètres nécéssaires de la requête et le nombre de paramètres fournis n'est pas le même.\n"
					+ "Nombre de paramètres nécéssaires : " + parameterMetaData.getParameterCount() + "\n"
					+ "Nombre de paramètres fournis : " + params.length);
		}
		
		//now, we can append all options one by one
		int counter = 1;
		for(Object object : params) {
			preparedStatement.setObject(counter, object);
			counter++;
		}
		try {
			return preparedStatement.executeUpdate() > 0;
		}catch (SQLTimeoutException e) {
			logger.error("Query timeouts exceed");
			throw new SQLException(e);
		}
		
	}

}
