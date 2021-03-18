public class Main {

    public static void main(String[] args) {
        int port; // server acceptance port

        if ( args.length != 1 ) {//use default port if none is specified
            port = Server.DEFAULT_SERVER_PORT;
            System.out.println("listening to port "+port);
        } else {//if a port is specified use that
            port = Integer.parseInt ( args[ 0 ] );
            System.out.println("listening to port: " + port);
        }

        //initialize server on port
        Server server = new Server(port);
    }

}
