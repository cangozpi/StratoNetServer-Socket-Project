import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

class ServerThread extends Thread
{
    protected DataInputStream is;
    protected DataOutputStream os;
    protected Socket s;
    protected ServerSocket dataSocket;
    private boolean authorized = false;

    /**
     * Creates a server thread on the input socket
     *
     * @param s input socket to create a thread on
     */
    public ServerThread(Socket s, ServerSocket dataSocket)
    {
        this.s = s;
        this.dataSocket = dataSocket;
    }

    /**
     * The server thread, communicates with the client according to the StratoNet protocol
     */
    public void run(){
        try{
            is = new DataInputStream(s.getInputStream());
            os = new DataOutputStream(s.getOutputStream());

        }
        catch (IOException e){
            System.err.println("Server Thread. Run. IO error in server thread");
        }

        try{
            //Handle Authentication functionality by passing it to Authentication instance
            RequestHandler requestHandler = new RequestHandler(is,os,s, dataSocket);
            requestHandler.handleRequest();
        }catch (NullPointerException e){
            System.err.println("Server Thread. Run.Client Closed"+e);
        } finally{
            try{
                System.out.println("Closing the connection");
                if (is != null){
                    is.close();
                    System.err.println(" Socket Input Stream Closed");
                }

                if (os != null){
                    os.close();
                    System.err.println("Socket Out Closed");
                }
                if (s != null){
                    s.close();
                    System.err.println("Socket Closed");
                }

            }catch (IOException ie){
                System.err.println("Socket Close Error");
            }
        }//end finally
    }
}
