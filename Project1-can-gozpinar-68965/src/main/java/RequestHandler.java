import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class RequestHandler {

    protected DataInputStream is;
    protected DataOutputStream os;
    protected Socket s;
    protected ServerSocket dataSocket;
    private byte[] sBackUpFile;//backup file for resending due to corruption(command line packet)
    private byte[] dsBackUpFile;//backup file for resending due to corruption(File socket packet)


    public RequestHandler(DataInputStream is, DataOutputStream os, Socket s, ServerSocket dataSocket){
        this.is = is;
        this.os = os;
        this.s = s;
        this.dataSocket = dataSocket;
    }

    //listens for requests and handles them according to the StratoNet Protocol
    public void handleRequest(){
        //listen to command socket
        AuthenticationModule authenticationModule = new AuthenticationModule(is,os,s, dataSocket);
        try{
            byte[] b = new byte[20000];//byte array to incoming socket input message
            while ( (is.read(b)) != 0 ) {
                //convert inputStream to byte[] and parse packet info
                int phase = Arrays.copyOfRange(b, 0,1)[0];
                int type = Arrays.copyOfRange(b, 1,2)[0];
                int size  = ByteBuffer.wrap(Arrays.copyOfRange(b, 2,6)).getInt();
                String payload = new String(Arrays.copyOfRange(b, 6,6 + size));

                //act accordingly
                switch(phase){
                    case 0: //for Authentication
                        authenticationModule.authRequestHandler(type, size, payload);
                        break;
                    case 1: //for Querying
                            //retain Data(File Transfer) Socket from AuthenticationModule
                            Socket ds = authenticationModule.getDataSocket();
                            String token = authenticationModule.getToken();
                            QueryModule queryModule = new QueryModule(is,os,s, ds, token);
                            //handle the request handling to the query module to respond accordingly
                            queryModule.queryRequestHandler(type, size, payload, this);
                            //TODO: implement query module
                            break;

                }
                //clear the array
                b = new byte[20000];

            }
        }catch (SocketTimeoutException e){//if timed out
            //send Auth_Fail(msg) to client
            String timeOutPayload = "Connection Timeout";
            TCPPayload timeOutPacket = new TCPPayload(0,2, timeOutPayload.length(),timeOutPayload);
            System.out.println(timeOutPayload);
            try {
                os.write(timeOutPacket.toStratonetProtocolByteArray());
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            //terminate the socket connection
            authenticationModule.terminateSocket();
        }catch (Exception e){
            System.err.println(e);
        }

    }

    //getters  setters below


    public byte[] getsBackUpFile() {
        return sBackUpFile;
    }

    public void setsBackUpFile(byte[] sBackUpFile) {
        this.sBackUpFile = sBackUpFile;
    }

    public byte[] getDsBackUpFile() {
        return dsBackUpFile;
    }

    public void setDsBackUpFile(byte[] dsBackUpFile) {
        this.dsBackUpFile = dsBackUpFile;
    }
}
