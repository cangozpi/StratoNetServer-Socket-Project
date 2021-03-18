import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class QueryModule {

    //singleton instance
    private static QueryModule queryModuleInstance;

    protected DataInputStream is;
    protected DataOutputStream os;
    protected Socket s;//command socket
    protected Socket ds;//data socket
    private DataOutputStream dos;//data socket output stream
    private DataInputStream dis;//data socket input stream
    private String token;//validation token
    private BufferedReader stdIn;

    private QueryModule(DataInputStream is, DataOutputStream os, Socket s, Socket ds, String token, BufferedReader stdIn){
        this.is = is;
        this.os = os;
        this.s = s;
        this.ds = ds;
        this.token = token;
        this.stdIn = stdIn;
        try {
            dos = new DataOutputStream (ds.getOutputStream());
            dis = new DataInputStream(ds.getInputStream() );
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static QueryModule getInstance(DataInputStream is, DataOutputStream os, Socket s, Socket ds, String token, BufferedReader stdIn){
        if(queryModuleInstance == null){
            queryModuleInstance = new QueryModule(is, os, s, ds, token, stdIn);
        }
        return queryModuleInstance;
    }

    //prompt user and start querying the StratoNetServer
    public void initiateQueryModule() throws IOException {
        //prompt the user about querying syntax
        System.out.println("For querying APOD type \"APOD <YYYY-MM-DD>\"");
        System.out.println("For querying Insight API type \"insight \"");

        boolean validQuery = false;//to check if a valid query request is made

        while (!validQuery){
            //read and parse the user input
            String queryString = "";
            queryString = stdIn.readLine();


            Pattern apodPattern = Pattern.compile("APOD (\\d{4})-(\\d{2})-(\\d{2})$");
            Pattern insigthApiPattern = Pattern.compile("insight");

            Matcher apodMatcher = apodPattern.matcher(queryString);
            Matcher insightApiMatcher = insigthApiPattern.matcher(queryString);

            if(apodMatcher.find()){//valid APOD request is made
                String year = apodMatcher.group(1);
                String month = apodMatcher.group(2);
                String day = apodMatcher.group(3);
                //make the request to the server
                queryAPOD(year, month, day);
                validQuery = !validQuery;
            }else if(insightApiMatcher.find()){//valid insight API request is made
                queryInsight();
                validQuery = !validQuery;
            }else{//non valid request is made
                System.out.println("Invalid request type. Try again.");
            }
        }



    }

    public void queryAPOD(String year, String month, String day) throws IOException {
        int phase = 1;
        int type = 0;
        //payload is token+the query param for APOD
        String replyPayload = token + year+"-"+month+"-"+day; //send token
        int replySize = replyPayload.length();

        TCPPayload replyMessage = new TCPPayload(phase, type, replySize, replyPayload);
        os.write(replyMessage.toStratonetProtocolByteArray()); // send the Auth_Success to client

    }

    public void queryInsight() throws IOException {
        int phase = 1;
        int type = 4;
        //payload is token
        String replyPayload = token;
        int replySize = replyPayload.length();

        TCPPayload replyMessage = new TCPPayload(phase, type, replySize, replyPayload);
        os.write(replyMessage.toStratonetProtocolByteArray()); // send the Auth_Success to client

    }

    //handle query msg coming from the server and reply accordingly
    public void queryRequestHandler(int type, int size, String payload, int dsSize) {
        //parse the payload
        String payloadToken = (String)payload.subSequence(0, token.length());//token send by the client
        String payloadHash = (String)payload.substring(token.length());
        switch(type){

            case 1://APOD image sent from server
                String payloadImageHash = payloadHash;

                //check if the token is valid
                if(tokenValidation(payloadToken)){//if token is valid
                    //read image from the output stream
                    try{
                        byte[] b = new byte[dsSize];//byte array to incoming socket input message
                        while ( (dis.read(b)) != 0 ) {

                            //compare hash values to validate correct delivery
                            ByteArrayInputStream ais = new ByteArrayInputStream(b);
                            BufferedImage apodFile = ImageIO.read(ais);

                            String imageHash = String.valueOf(getCRC32Checksum(b));

                            if(imageHash.equals(payloadImageHash)){//if hash values match
                                //create location of the image to be downloaded
                                String currentPath = new File("").getAbsolutePath();
                                File imageDestinationFile = new File(currentPath+"/APODImage.jpg");
                                //download the image
                                ImageIO.write(apodFile, "jpg", imageDestinationFile);
                                ais.close();

                                //display image on the screen
                                JFrame frame = new JFrame();
                                JLabel lbl = new JLabel(new ImageIcon(apodFile));
                                frame.getContentPane().add(lbl);
                                frame.setSize(apodFile.getWidth()+100, apodFile.getHeight()+100);
                                frame.setVisible(true);//make it appear

                                sendFileReceivedMsg();
                            }else{//hash values don't match request resending the file
                                resendFileMsg();
                            }

                            //clear the array
                            b = new byte[2000];
                        }

                    }catch (Exception e){
                        e.printStackTrace();
                    }

                }else{//token is not valid
                    //terminate the connection
                    System.out.println("Invalid token received from the StratoNetServer. \n Terminating connection");
                    terminateSocket();
                }

                break;
            case 5://insight api json arrived from the server
                    //TODO: başı sil bunu sonra

                //check if the token is valid
                if(tokenValidation(payloadToken)){//if token is valid
                    //read image from the output stream
                    try{

                        byte[] b = new byte[dsSize];//byte array to incoming socket input message
                        while ( (dis.read(b)) != 0 ) {
                            //compare hash values to validate correct delivery
                            String imageHash = String.valueOf(getCRC32Checksum(b));

                            if(imageHash.equals(payloadHash)){//if hash values match
                                //output received json
                                String jsonString = new String(b);
                                //convert jsonString jsonObject
                                JSONParser parser = new JSONParser();
                                JSONObject json = (JSONObject) parser.parse(jsonString);

                                //display the retained json
                                System.out.println("server response: "+ json);
                                sendFileReceivedMsg();
                                //terminate the connection
                                os.flush();
                                os.close();
                            }else{//hash values don't match request resending the file
                                resendFileMsg();
                            }

                            //clear the array
                            //b = new byte[2000];
                        }

                    }catch (Exception e){
                        System.err.println(e);
                    }

                }else{//token is not valid
                    //terminate the connection
                    System.out.println("Invalid token received from the StratoNetServer. \n Terminating connection");
                    terminateSocket();
                }

                break;

        }
    }


    //checks if the tokens match
    public boolean tokenValidation(String token){
        return token.equals(this.token);
    }

    //terminates the socket connection btw the server and the client
    public void terminateSocket(){
        try{//terminate socket
            is.close();
            os.close();
            s.close();
            ds.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //used to get hash value for the image being sent over the data socket
    public static long getCRC32Checksum(byte[] bytes) {
        Checksum crc32 = new CRC32();
        crc32.update(bytes, 0, bytes.length);
        return crc32.getValue();
    }

    //sends "file/JSON received" msg to the server
    public void sendFileReceivedMsg(){
        int replyPhase = 1;
        int replyType = 2;
        String replyPayload = token+"file/JSON received"; //send token + msg
        int replySize = replyPayload.length();

        TCPPayload replyMessage = new TCPPayload(replyPhase, replyType, replySize, replyPayload);
        try {
            os.write(replyMessage.toStratonetProtocolByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //sends resend file msg to the server
    public void resendFileMsg(){
        int replyPhase = 1;
        int replyType = 3;
        String replyPayload = token+"Resend File/JSON. Corrupted data received."; //send token + msg
        int replySize = replyPayload.length();

        TCPPayload replyMessage = new TCPPayload(replyPhase, replyType, replySize, replyPayload);
        try {
            os.write(replyMessage.toStratonetProtocolByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
