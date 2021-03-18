import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthenticationModule {
    protected DataInputStream is;
    protected DataOutputStream os;
    protected Socket s;
    private HashMap<String, String> validUsersMap;
    private String username; //supplied username of the client
    private boolean usernameValid;//when a valid uname is supplied it is true
    private int failCount = 0;// count keeps track of num of Auth_Challenges sent to the client e.g failCount>2 terminate connection
    private String token;
    private String clientIP;
    private int clientPort;
    private int timeOutDuration = 10;//socket time outs after this amount of time in ms
    private ServerSocket dataSocket;
    private Socket ds;//reference to dataSocket associated with this thread's client


    public AuthenticationModule(DataInputStream is, DataOutputStream os, Socket s,ServerSocket dataSocket){
        this.is = is;
        this.os = os;
        this.s = s;
        this.dataSocket = dataSocket;
        validUsersMap = this.getValidUsers();//map storing valid uname-pwd combinations
        clientIP = s.getInetAddress().toString();
        clientPort = s.getPort();
        usernameValid = false;
    }

    //handles first incoming initialization message
    public void authRequestHandler(int type, int size, String payload) throws IOException {
        if(usernameValid){ //if uname is already validated then validate pwd
            pwdAuthHandler(type, size, payload);
        }else{//if supplied uname is not validated
            //check the type of the request
            switch (type){
                case 0:
                    //check if the uname is valid
                    if (validUsersMap.containsKey(payload)) { //if uname exists send Auth_Challenge message
                        //save the uname supplied
                        username = payload;
                        //ask client for pwd
                        pwdAuthHandler(type, size ,payload);
                    }else{//uname doesn't exist
                        //create Auth_Fail message
                        int replyPhase = 0;
                        int replyType = 2;
                        String replyPayload = "User does not exist";
                        int replySize = replyPayload.length();

                        TCPPayload replyMessage = new TCPPayload(replyPhase, replyType, replySize, replyPayload);
                        os.write(replyMessage.toStratonetProtocolByteArray()); // send the reply message to client

                        //terminate the connection with the client on wrong uname
                        terminateSocket();
                    }
                    break;
                default: // non valid authentication type
                    os.writeBytes("invalid authentication message type, terminating socket connection");
                    terminateSocket();
            }
        }
    }

    public HashMap<String, String> getValidUsers(){
        //read valid user info from the file
        String currentPath = new File("").getAbsolutePath();
        File file = new File(currentPath+"/src/main/resources/validUsers.txt");
        Scanner sc = null;
        try {
            sc = new Scanner(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        String text = "";//text of validUsers.txt
        // /read the validUsers.txt
        while (sc.hasNextLine()){
            text += sc.nextLine()+"\n";
        }

        //extract username and passwords using regex
        Pattern unamePattern = Pattern.compile("Username:(.*)\\b");
        Matcher unameMatcher = unamePattern.matcher(text);

        Pattern pwdPattern = Pattern.compile("Password:(.*)\\b");
        Matcher pwdMatcher = pwdPattern.matcher(text);

        //store valid username(key) pwd(value) combination in validUsersMap
        HashMap<String, String> validUsersMap = new HashMap<>();//
        while(unameMatcher.find() & pwdMatcher.find()){
            validUsersMap.put(unameMatcher.group(1),pwdMatcher.group(1)); // key=username, value=password
        }

        return validUsersMap;
    }

    //handles pwd message exchange w/ client
    public void pwdAuthHandler(int type, int size, String payload) throws IOException {
        if(usernameValid){//pwd has been asked before, now validate the pwd
            if(failCount <= 3){//client has remaining pwd tries
                //check for the validity of the pwd
                if(validUsersMap.get(username).equals(payload)){//supplied pwd is correct
                    //reply with a token
                    int replyPhase = 0;
                    int replyType = 3;
                    token = generateToken();
                    String replyPayload = token; //send token
                    int replySize = replyPayload.length();

                    TCPPayload replyMessage = new TCPPayload(replyPhase, replyType, replySize, replyPayload);
                    os.write(replyMessage.toStratonetProtocolByteArray()); // send the Auth_Success to client

                    //undo Auth_Challenge socket timeout mechanism
                    s.setSoTimeout(0);

                    //open DataSocket
                    boolean waitingForConnection = true;
                    while(waitingForConnection){//wait for client to connect to the DataSocket
                        ds = dataSocket.accept();//blocking method call
                        if(ds.getInetAddress().toString().equals(clientIP) & ds.getPort() == (clientPort + 1)){//check if the connected client is the one intended
                            waitingForConnection = false; //to terminate the while loop
                            System.out.println("Client at: " + clientIP + ":" + clientPort + " has connected to DataSocket.");
                        }else{//not the intended user terminate connection
                            continue;
                        }
                    }

                }else{//supplied pwd is incorrect
                    //ask for pwd again
                    failCount++;
                    if(failCount > 3){//pwd try limit is over terminate socket
                        int replyPhase = 0;
                        int replyType = 2;
                        String replyPayload = "Incorrect Password";
                        int replySize = replyPayload.length();

                        TCPPayload replyMessage = new TCPPayload(replyPhase, replyType, replySize, replyPayload);
                        os.write(replyMessage.toStratonetProtocolByteArray()); // send the Auth_Fail to client
                        //terminate the socket
                        terminateSocket();
                    }else{//ask for pwd again
                        int replyPhase = 0;
                        int replyType = 1;
                        String replyPayload = "Incorrect Password";
                        int replySize = replyPayload.length();

                        TCPPayload replyMessage = new TCPPayload(replyPhase, replyType, replySize, replyPayload);
                        os.write(replyMessage.toStratonetProtocolByteArray()); // send the Auth_Challenge to client


                        //wait 10 seconds if client doesn't reply terminate the command socket connection
                        s.setSoTimeout(timeOutDuration*1000);

                    }
                }

            }else{//3 times failed terminate the socket
                int replyPhase = 0;
                int replyType = 2;
                String replyPayload = "Incorrect Password"; //send empty payload for asking pwd for the first time
                int replySize = replyPayload.length();

                TCPPayload replyMessage = new TCPPayload(replyPhase, replyType, replySize, replyPayload);
                os.write(replyMessage.toStratonetProtocolByteArray()); // send the Auth_Challenge to client
                //terminate the socket
                terminateSocket();
            }
        }else{//pwd is gonna be asked for the first time
            usernameValid = true;//uname is already validated since we're gonna ask pwd now
            //first send the Auth_Challenge
            //create Auth_Challenge message
            int replyPhase = 0;
            int replyType = 1;
            String replyPayload = "pwd_req"; //send payload asking pwd for the first time
            int replySize = replyPayload.length();

            TCPPayload replyMessage = new TCPPayload(replyPhase, replyType, replySize, replyPayload);
            os.write(replyMessage.toStratonetProtocolByteArray()); // send the Auth_Challenge to client
            failCount++;//increment count since a challenge is just sent e.g failCount > 2 results in termination of connection

            //wait 10 seconds if client doesn't reply, terminate the command socket connection
            s.setSoTimeout(timeOutDuration*1000);
        }

    }

    //terminates the socket connection btw the server and the client
    public void terminateSocket(){
        try{//terminate socket
            is.close();
            os.close();
            s.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //generates token
    public String generateToken(){
        String stringToBeHashed = username + "68"; // hash username and last two digits of my kusisID(68965)
        String hashedString = Integer.toString(stringToBeHashed.hashCode());
        //take first 6 digits of hashedString as the token
        String token = hashedString.substring(0,7);
        return token;
    }

    public Socket getDataSocket(){
        return ds;
    }

    public String getToken(){
        return token;
    }

}
