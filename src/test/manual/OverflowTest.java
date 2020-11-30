package test.manual;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class OverflowTest {
	
	private static final int LENGTH_TO_CHECK = 9000;

	public static void main(String args[]) throws IOException{
		BufferedReader inputFlow;
		PrintWriter outputFlow;
		
		Socket socket = new Socket("127.0.0.1", 5000);
		System.out.println("Connection");
		
		inputFlow = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		outputFlow = new PrintWriter(socket.getOutputStream(), true);
		
		//connect to the server
		outputFlow.println("<0001><Alfred><1234>");
		String str = inputFlow.readLine();
		
		if(str.equals("<9993>")) {
			String longMessage = createLongString();
			System.out.println("Send very long message ("+ longMessage.length() +" characters)...");
			outputFlow.println(longMessage);
			System.out.println(inputFlow.readLine());
		}

		outputFlow.close();
		inputFlow.close();
		socket.close();
	}
	
	public static String createLongString() {
		//the string is well formed of course
		StringBuilder stringBuilder = new StringBuilder("<0301>");
		while(stringBuilder.length() < LENGTH_TO_CHECK) {
			stringBuilder.append("<12345678913456789123456789123456789>");
		}
		return stringBuilder.toString();
	}
	
}
