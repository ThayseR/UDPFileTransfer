package pkg12;
import java.io.*;
import java.net.*;

public class Server {
    private static int clientID = 1;
    private static DatagramSocket serverSocket;

    public static void main(String[] args) throws IOException
    {
        System.out.println("Server started.");
        byte[] buffer = new byte[1024];

        /**
         * O servidor precisa ser multi-threaded, e e ter uma thread por conexão.
         */
        serverSocket = new DatagramSocket(8550);
        while (true)
        {
            try
            {
                DatagramPacket packet =  new DatagramPacket(buffer, buffer.length );
                serverSocket.receive(packet);
                System.out.println("SERVER: Conexão aceita.");
                System.out.println("SERVER: recebido "+new String(packet.getData(), 0, packet.getLength()));

                //novo socket criada com porta aleatória para thread
                DatagramSocket threadSocket = new DatagramSocket();

                Thread t = new Thread(new CLIENTConnection(threadSocket, packet, clientID++));

                t.start();

            } catch (Exception e)
            {
                System.err.println("Erro na conexão.");
            }
        }
    }
}
