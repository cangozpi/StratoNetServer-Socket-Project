import java.io.*;

public class Main {
    public static void main ( String... args ) throws IOException {
        String host;
        int port;
        if ( args.length != 2 ) {
            System.out.println("binding to port localhost:9999");
            host = "localhost";
            port = 9999;
        } else {
            host = args[ 0 ];
            port = Integer.parseInt ( args[ 1 ] );
        }

        //start client connection
        ClientSocket clientSocket = new ClientSocket(host, port);
        clientSocket.startSocket();
    }

}

