package test.manual;

import process.connection.ThreadsConnectionHandler;

public class ServerTest {
	
	private static final int PORT = 5000;
	private static final String DATABASE_URL = "localhost:5432/drivebdd";
	private static final String DATABASE_USER = "etu";
	private static final String DATABASE_PASSWORD = "A123456*";
	
	public static void main(String[] args) {
		new ThreadsConnectionHandler(PORT, DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
	}
}
