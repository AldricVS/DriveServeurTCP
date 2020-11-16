package test.manual;

import process.connection.ThreadsConnectionHandler;

public class ServerTest {
	
	private static final int PORT = 5000;
	private static final String DATABASE_URL_LOCAL = "localhost:5432/drivepiceriebd";
	private static final String DATABASE_USER_LOCAL = "drive_admin";
	private static final String DATABASE_PASSWORD = "AlRaMa311621";
	
	private static final String DATABASE_URL_ONLINE = "postgresql-drivepicerie.alwaysdata.net:5432/drivepicerie_bd";
	private static final String DATABASE_USER_ONLINE = "drivepicerie";
	
	/**
	 * Init connection to database and start server listening.
	 * @param args args to be passed in the command line.
	 * <ul>
	 * 	<li>If no args or the first arg is "-l", the server will start in local mode.</li>
	 * 	<li>If the first arg is "-o", then the server will try to connect on the alwaysdata database.</li>
	 * 	<li>If the first arg is "help", then the server will display this documentation.</li>
	 * 	<li>Any other possibility will result in an error</li>
	 * </ul> 
	 */
	public static void main(String[] args) {
		if(args.length == 0 || args[0].equals("-l")) {
			new ThreadsConnectionHandler(PORT, DATABASE_URL_LOCAL, DATABASE_USER_LOCAL, DATABASE_PASSWORD);
		}else if(args[0].equals("-o")) {
			new ThreadsConnectionHandler(PORT, DATABASE_URL_ONLINE, DATABASE_USER_ONLINE, DATABASE_PASSWORD);
		}else if(args[0].equals("help")){
			System.out.println("Type nothing or \"-l\" in order to start in local mode,\n or type \"-o\" to start in online mode");
		}else {
			System.err.println("Arguments are not valid, type \"help\" in order to see possible actions.");
		}
	}
}
