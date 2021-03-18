import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.*;

public class HttpGetRequest {

    public HttpGetRequest(){

    }

    //performs a HTTP GET request to the supplied url
    public static String makeHttpGetRequest(String urlAddr) throws IOException {
        // Create a neat value object to hold the URL
        URL url = new URL(urlAddr);

        // Open a connection(?) on the URL(??) and cast the response(???)
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Now it's "open", we can set the request method, headers etc.
        connection.setRequestProperty("accept", "application/json");

        // This line makes the request
        InputStream responseStream = connection.getInputStream();
        BufferedReader is = new BufferedReader(new InputStreamReader(responseStream));

        String txt ="";
        String jsonString = "";
        while (( txt = is.readLine()) != null){
            jsonString += txt;
        }

        return jsonString;

    }

   /* public static String makeHttpGetRequest(String host, int port, String urlExtension){
        try{
            InetAddress addr = InetAddress.getByName(host);
            //Connect to url
            Socket s = new Socket(addr, port);

            PrintWriter os = new PrintWriter(s.getOutputStream());
            BufferedReader is = new BufferedReader(new InputStreamReader(s.getInputStream()));

            //prompt the request string to the server
            os.println("GET /"+ urlExtension +" HTTP/1.1");
            os.println("Host: " + addr);
            os.println("");
            os.flush();

            String text;

            //Prints each line of the response
            while((text = is.readLine()) != null){
                System.out.println(text);
            }


            //Closes out buffer and writer
            is.close();
            os.close();
        }catch (Exception e){
            e.printStackTrace();
        }

        return "";
    }
*/

}
