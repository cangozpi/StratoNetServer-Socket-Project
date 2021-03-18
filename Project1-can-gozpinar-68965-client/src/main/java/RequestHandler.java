import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class RequestHandler {

    protected DataInputStream is;
    protected DataOutputStream os;
    protected Socket s;
    protected BufferedReader stdIn;

    public RequestHandler(DataInputStream is, DataOutputStream os, Socket s, BufferedReader stdIn){
        this.is = is;
        this.os = os;
        this.s = s;
        this.stdIn = stdIn;
    }

    //listens for requests and handles them according to the StratoNet Protocol
    public void handleRequest(){
        //listen to command socket
        try{
            AuthenticationModule authenticationModule = new AuthenticationModule(is,os,s, stdIn);

            System.out.println("Please enter your StratoNet username.");
            authenticationModule.authUname();//authenticates username for the first time

            //parse replies from the server
            byte[] b = new byte[2000];//byte array to incoming socket input message
            while ( (is.read(b)) != 0 ) {

                //convert inputStream to byte[] and parse packet info
                int phase = Arrays.copyOfRange(b, 0,1)[0];
                int type = Arrays.copyOfRange(b, 1,2)[0];
                int size;
                String payload;

                //act accordingly
                switch(phase){
                    case 0: //for Authentication
                        size  = ByteBuffer.wrap(Arrays.copyOfRange(b, 2,6)).getInt();
                        payload = new String(Arrays.copyOfRange(b, 6,6 + size));

                        authenticationModule.authRequestHandler(type, size, payload);
                        break;
                    case 1: //for Querying
                        size  = ByteBuffer.wrap(Arrays.copyOfRange(b, 2,6)).getInt();
                        int dsSize  = ByteBuffer.wrap(Arrays.copyOfRange(b, 6,10)).getInt();//size of the payload sent over the dataSocket
                        payload = new String(Arrays.copyOfRange(b, 10,10 + size));

                        //attain the data socket
                        Socket ds = ClientSocket.getDataSocket();
                        //attain the validation token
                        String token = authenticationModule.getToken();
                        QueryModule queryModule = QueryModule.getInstance(is, os, s, ds, token, stdIn);
                        //handles request coming from the server and responds accordingly
                        queryModule.queryRequestHandler(type, size, payload, dsSize);
                        //TODO: implement query module
                        break;

                }

            }
        }catch (Exception e){

        }

    }

}
