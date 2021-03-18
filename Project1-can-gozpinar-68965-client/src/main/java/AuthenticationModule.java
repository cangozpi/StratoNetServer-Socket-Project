import javax.xml.crypto.Data;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthenticationModule {
    protected DataInputStream is;
    protected DataOutputStream os;
    protected Socket s;
    protected BufferedReader stdIn;
    private HashMap<String, String> validUsersMap;
    private String username; //supplied username of the client
    private boolean usernameValid;
    private int failCount = 0;// count keeps track of num of Auth_Challenges sent to the client e.g failCount>2 terminate connection
    private static String token;
    private String clientIP;
    private int clientPort;

    public AuthenticationModule(DataInputStream is, DataOutputStream os, Socket s, BufferedReader stdIn){
        this.is = is;
        this.os = os;
        this.s = s;
        this.stdIn = stdIn;
        usernameValid = false;
        clientIP = s.getRemoteSocketAddress().toString();
        clientPort = s.getPort();
    }

    //send username to the server
    public void authUname() throws IOException {
        //get the username from the user
        String userInput = "";
        try {
            userInput = stdIn.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //create the Auth_Request(username)
        TCPPayload sendPacket = new TCPPayload(0,0,userInput.length(), userInput);

        //set the username
        username = userInput;

        //send the packet
        os.write(sendPacket.toStratonetProtocolByteArray());
    }

    //handles first incoming initialization message
    public void authRequestHandler(int type, int size, String payload) throws IOException {
        switch(type){
            case 1://received Auth_Challenge
                    //reply with Auth_Request(pwd)

                if(failCount != 0){//if user has sent a wrong pwd before
                    System.out.println(payload);
                }
                failCount++;

                //prompt user to enter pwd
                System.out.println("Enter your password:");
                String pwd = "";
                pwd = stdIn.readLine();

                //create TCPPayload
                TCPPayload sendPacket = new TCPPayload(0,0, pwd.length(), pwd);
                //send packet
                os.write(sendPacket.toStratonetProtocolByteArray());

                break;
            case 2://Auth_fail
                System.out.println(payload);
                terminateSocket();
                break;
            case 3://Auth_Success(token)
                //retrieve the token and save it for queryModule to authenticate its queries later on
                token = payload;

                //connect to DataSocket at port 5555
                ClientSocket.startDataSocket();
                break;
        }
    }

    //terminates the socket connection btw the server and the client
    public void terminateSocket(){
        try{//terminate socket
            is.close();
            os.close();
            s.close();
            stdIn.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static String getToken(){
        return token;
    }

}
