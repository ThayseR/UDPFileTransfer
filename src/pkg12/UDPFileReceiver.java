package pkg12;
import java.io.*;
import java.net.*;
import java.util.*;

public class UDPFileReceiver {
    private byte[] buffer;
    private Message receiveMSG;
    private DatagramSocket socket;
    private String filename, initString;
    private FileOutputStream fileWriter;
    private DatagramPacket initPacket, receivedPacket;
    private int bytesReceived, bytesToReceive, simulateBadConnection, expectedSegmentID;
    private final boolean simulateMessageFail = true;//true se quiser simular conexão ruim

    public UDPFileReceiver(DatagramSocket socket) throws IOException
    {
        this.socket = socket;
        buffer = new byte[1130];  //618 é o tamanho total da mensagem (1024 payload, 106 overhead)

        System.out.println("*** Ready to receive file on port: " + socket.getLocalPort() + " ***");

        initPacket = receivePacket();
        initString = "Recieved-" + new String(initPacket.getData(), 0, initPacket.getLength());

        //pega o tamanho e o nome do arquivo
        StringTokenizer t = new StringTokenizer(initString, "::");
        filename = t.nextToken();
        bytesToReceive = new Integer(t.nextToken()).intValue();

        System.out.println("*** O arquivo será salvo como: " + filename + " ***");
        System.out.println("*** Espera receber: " + bytesToReceive + " bytes ***");

        //tell the sender OK to send data
        send(initPacket.getAddress(), initPacket.getPort(), ("OK").getBytes());

        fileWriter = new FileOutputStream(filename);
        
        //Verifica se tem mais dados para receber
        while (bytesReceived < bytesToReceive)
        {
            receiveMSG = new Message();
            
            //verifica erros no recebimento de pacotes e Ack perdidos
            do
            {
                receivedPacket = receivePacket();
                try
                {
                    receiveMSG = (Message) deserialize(receivedPacket.getData());
                } catch (ClassNotFoundException ex)
                {
                    System.out.println("*** Message packet failed. ***");
                }

                //Se o ultimo Ack enviado falhou e não foi recebido o UDPSender reenvia o ultimo seguimento.
                //Uma verificação simples do segemntID mostra se é igual ao expectedID - 1 e reenvia o ACK
            
                if ((expectedSegmentID - 1) == receiveMSG.getSegmentID())
                {
                    String ACK = Integer.toString(receiveMSG.getSegmentID());
                    send(initPacket.getAddress(), initPacket.getPort(), (ACK).getBytes());
                    System.out.println("*** Resending ACK for segment " + ACK + " ***");
                }

                if (simulateMessageFail)
                {
                    simulateBadConnection = (Math.random() < 0.95) ? 0 : 1; //simulate a 5% chance a message object is lost
                }

                //Adicionando 1 no segmentIDExpected podemos fazer o receptor determinar que a message foi perdida
            } while (receiveMSG.getSegmentID() != (expectedSegmentID + simulateBadConnection));

            expectedSegmentID++;

            //Obtém o tamanho do ultimo segmentID .getBytesToWrite()
            fileWriter.write(receiveMSG.getPacket(), 0, receiveMSG.getBytesToWrite());

            System.out.println("Received segmentID " + receiveMSG.getSegmentID());

            //adiciona o tamanho do payload
            bytesReceived = bytesReceived + 1024;

            //simula 5% de chance de perda
            if (simulateMessageFail)
            {
                if ((Math.random() < 0.95))
                {
                    String ACK = Integer.toString(receiveMSG.getSegmentID());
                    send(initPacket.getAddress(), initPacket.getPort(), (ACK).getBytes());
                } else
                {
                    System.out.println("*** falha ao enviar ACK ***");
                }
            } else
            {
                String ACK = Integer.toString(receiveMSG.getSegmentID());
                send(initPacket.getAddress(), initPacket.getPort(), (ACK).getBytes());
            }
        }
        System.out.println("*** Transferencia de arquivo completa. ***");
        fileWriter.close();
    }

    private DatagramPacket receivePacket() throws IOException
    {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);

        return packet;
    }

    private void send(InetAddress recv, int port, byte[] message) throws IOException
    {
        DatagramPacket packet = new DatagramPacket(message, message.length, recv, port);
        socket.send(packet);
    }

    private Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException
    {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
        ObjectInputStream objectStream = new ObjectInputStream(byteStream);
        return (Message) objectStream.readObject();
    }
}
