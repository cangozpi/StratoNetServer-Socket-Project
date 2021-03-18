import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class ClientSocket {

    static String host;
    static int port;//command socket port
    static int dataSocketPort = 5555;
    static int localCommandSocketPort;
    static int localDataSocketPort;
    static InetAddress localCommandSocketIp;
    private static Socket s;
    private static DataOutputStream os;
    private static DataInputStream is;
    private static BufferedReader stdIn;

    static Socket dataSocket;//data socket

    public ClientSocket(String host, int port){
        this.host = host;
        this.port = port;
    }

    //opens a command socket and initiates StratoNetProtocol
    public  void startSocket() throws IOException {

        try{

                s = new Socket ( host, port );
                os = new DataOutputStream (s.getOutputStream());
                is = new DataInputStream(s.getInputStream() );
                stdIn = new BufferedReader (new InputStreamReader ( System.in ) );

                localCommandSocketPort = s.getLocalPort();
                localDataSocketPort = localCommandSocketPort + 1;
                localCommandSocketIp = s.getLocalAddress();
                //hand in socket properties to requestHandler and it will take care of the protocol
                RequestHandler requestHandler = new RequestHandler(is,os,s, stdIn);
                requestHandler.handleRequest();//act according to receivedPackage
            }catch (Exception e){
            e.printStackTrace();
        }

    }

    //opens a DataSocket and initiates StratoNetProtocol
    public static void startDataSocket() throws IOException {

        try{
            dataSocket = new Socket ( host, dataSocketPort, localCommandSocketIp,  localDataSocketPort);
            System.out.println("Connected to Data Socket at: " + host + " " + dataSocketPort + ".");

            //prompt user to query the server
            Socket ds = ClientSocket.getDataSocket();
            //attain the validation token
            String token = AuthenticationModule.getToken();
            QueryModule queryModule = QueryModule.getInstance(is,os,s, ds, token, stdIn);//initializes the QueryModule for the first time
            //prompt user to make the first query to the server
            queryModule.initiateQueryModule();

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public static Socket getDataSocket(){
        return dataSocket;
    }

    
}
