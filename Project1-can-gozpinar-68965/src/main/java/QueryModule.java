import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class QueryModule {

    private RequestHandler requestHandlerInstance;
    //command socket's streams
    protected DataInputStream is;
    protected DataOutputStream os;
    protected Socket s;//command socket
    protected Socket ds;//data socket
    //data socket's streams
    protected DataInputStream dis;
    protected DataOutputStream dos;
    private String token;
    private String APODKEY;

    public QueryModule(DataInputStream is, DataOutputStream os, Socket s, Socket ds, String token){
        this.is = is;
        this.os = os;
        this.s = s;
        this.ds = ds;
        this.token = token;

        APODKEY = "";
        try{
            //set streams of dataSocket
            dos = new DataOutputStream(ds.getOutputStream());
            dis = new DataInputStream(ds.getInputStream());

            //set API key
            String currentPath = new File("").getAbsolutePath();
            File file = new File(currentPath+"/apodApiKey.txt");
            Scanner sc = null;
            sc = new Scanner(file);

            String text = "";//text of validUsers.txt

            // /read the apodApiKey.txt
            while (sc.hasNextLine()){
                text += sc.nextLine()+"\n";
            }

            Pattern apodKeyPattern = Pattern.compile("API key:(.*)\\b");
            Matcher apodKeyMatcher = apodKeyPattern.matcher(text);

            if(apodKeyMatcher.find()){
                this.APODKEY = apodKeyMatcher.group(1);
            }

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    //interprets the incoming msg from the client and responds accordingly
    public void queryRequestHandler(int type, int size, String payload, RequestHandler requestHandlerInstance) throws IOException {
        this.requestHandlerInstance = requestHandlerInstance;
        //parse the load into token and query parameter
        String payloadToken = (String)payload.subSequence(0, token.length());//token send by the client
        String payloadMsg = (String)payload.substring(token.length());
        switch (type){

            case 0://Query params received from client for APOD query
                String queryParam = payloadMsg;
                //check token validity
                if(tokenValidation(payloadToken)){//if user is valid
                    String host = "https://api.nasa.gov";
                    String urlExtension = "/planetary/apod?api_key="+APODKEY+"&date="+queryParam;
                    //make HTTP GET request to apodGetUrl
                    String jsonString = "";
                    BufferedImage image;
                    try {
                        jsonString = HttpGetRequest.makeHttpGetRequest(host+urlExtension);
                        //parse the json Object and attain the image url
                        /*JSONObject json = new JSONObject(jsonString);*/
                        JSONParser parser = new JSONParser();
                        JSONObject json = (JSONObject) parser.parse(jsonString);
                        String imageUrl = (String) json.get("url");
                        //download the image from the image url
                        URL url = new URL(imageUrl);
                        image = ImageIO.read(url);
                        //convert downloaded image to String
                        ByteArrayOutputStream aos = new ByteArrayOutputStream();
                        ImageIO.write(image, "jpg", aos);
                        byte[] bytes = aos.toByteArray();

                        //send command socket
                        int replyPhase = 1;
                        int replyType = 1;
                        String imageHash = String.valueOf(getCRC32Checksum(bytes));
                        String replyPayload = token + imageHash; //send image as payload
                        int replySize = replyPayload.length();

                        TCPPayload replyMessage = new TCPPayload(replyPhase, replyType, replySize, replyPayload, bytes.length);

                        //take backups of the packets
                        requestHandlerInstance.setsBackUpFile(replyMessage.toAPIStratonetProtocolByteArray());
                        requestHandlerInstance.setDsBackUpFile(bytes);

                        os.write(replyMessage.toAPIStratonetProtocolByteArray()); // send the Auth_Success to client

                        //send the image through the data Socket
                        dos.write(bytes); // send the Auth_Success to client

                    }catch (IOException | ParseException e) {
                        e.printStackTrace();
                    }

                }else{//if user is not valid terminate connection
                    terminateSocket();
                }

                break;
            case 2://"file/JSON received" msg
                //check for the token validity
                if(tokenValidation(payloadToken)){//token is valid. output msg
                    System.out.println(payloadMsg);
                    //terminate the connection


                }else{//if token is not valid
                    System.out.println("Token is not valid. Terminating connection.");
                    terminateSocket();
                }
                break;

            case 3://resend msg due to corrupted file transfer
                //check for the token validity
                if(tokenValidation(payloadToken)){//token is valid. resend msg
                    System.out.println("Resending msg to client due to corrupted file transfer.");
                    //resend backup msg
                    try{
                        os.write(requestHandlerInstance.getsBackUpFile());
                        dos.write(requestHandlerInstance.getDsBackUpFile());
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                }else{//if token is not valid
                    System.out.println("Token is not valid. Terminating connection.");
                    terminateSocket();
                }
                break;
            case 4://Insight API query
                //TODO: implement this functionality
                //payloadMsg
                //check for the token validity
                if(tokenValidation(payloadToken)) {//token is valid. resend msg
                    String host = "https://api.nasa.gov";
                    String urlExtension = "/insight_weather/?api_key=DEMO_KEY&feedtype=json&ver=1.0";
                    //make HTTP GET request to insight API
                    String jsonString = "";
                    try {
                        jsonString = HttpGetRequest.makeHttpGetRequest(host+urlExtension);
                        //parse the json Object and attain the PRE(atm pressure) info
                        JSONParser parser = new JSONParser();
                        JSONObject json = (JSONObject) parser.parse(jsonString);
                        //choose a random sol
                        // create instance of Random class
                        Random rand = new Random();

                        // Generate random to get a random sol
                        JSONArray solKeys = (JSONArray) json.get("sol_keys");
                        int randomInt = rand.nextInt(solKeys.size());

                        //choose the random sol's PRE
                        JSONObject randomSol = (JSONObject) json.get(solKeys.get(randomInt));
                        JSONObject atmPre = (JSONObject) randomSol.get("PRE");

                        //send through command socket
                        int replyPhase = 1;
                        int replyType = 5;
                        String atmPreJsonString = atmPre.toJSONString();
                        String jsonHash = String.valueOf(getCRC32Checksum(atmPreJsonString.getBytes()));
                        String replyPayload = token + jsonHash; //send image as payload
                        int replySize = replyPayload.length();

                        TCPPayload replyMessage = new TCPPayload(replyPhase, replyType, replySize, replyPayload, atmPreJsonString.length());

                        //take backups of the packets
                        requestHandlerInstance.setsBackUpFile(replyMessage.toAPIStratonetProtocolByteArray());
                        requestHandlerInstance.setDsBackUpFile(atmPreJsonString.getBytes());

                        os.write(replyMessage.toAPIStratonetProtocolByteArray()); // send the Auth_Success to client

                        //send the PRE JSON through the data Socket
                        dos.write(atmPreJsonString.getBytes()); // send the Auth_Success to client

                    }catch (IOException | ParseException e) {
                        System.err.println(e);
                    }

                }else{//if user token is not valid terminate connection
                    System.out.println("Token is not valid. Terminating connection.");
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
}
