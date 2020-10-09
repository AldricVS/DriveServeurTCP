package test;
import java.net.ServerSocket ;
import java.net.Socket ;
import java.io.IOException ;
import java.io.BufferedReader ;
import java.io.InputStreamReader ;
import java.io.PrintWriter ;

/**
* Tuyet Tram DANG NGOC (dntt) - 2001
* Serveur TCP
*/

public class FirstTest {
	public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null ;
        serverSocket = new ServerSocket (5000) ;

        Socket clientSocket = null ;
        try {
            clientSocket = serverSocket.accept () ;
        } 
        catch (IOException e) {
            System.err.println ("Accept echoue.") ;
            System.exit (1) ;
        }

        PrintWriter flux_sortie = new PrintWriter (clientSocket.getOutputStream (), true) ;
        BufferedReader flux_entree = new BufferedReader (
                                new InputStreamReader (
                                clientSocket.getInputStream ())) ;

        String chaine_entree, chaine_sortie ;
        while ( (chaine_entree = flux_entree.readLine ()) != null) {
             System.out.println ("J'ai recu " + chaine_entree) ;
             if (chaine_entree.equals ("Au revoir !")) {
                break ;
             }
             else {
                 flux_sortie.println ("Bien recu") ;
             }
        }
        flux_sortie.close () ;
        flux_entree.close () ;
        clientSocket.close () ;
        serverSocket.close () ;
    }
}
