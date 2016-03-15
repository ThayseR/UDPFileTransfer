package pkg12;
import java.io.*;
import java.net.*;

public class UDPFileSender {
    private int segmentID;
    private int reSendCount;
    private byte[] msg, buffer;
    private FileInputStream fileReader;
    private DatagramSocket datagramSocket;
    private int fileLength, currentPos, bytesRead;
    private final int packetOverhead = 106; // pacote overhead

    public UDPFileSender(DatagramSocket socket, DatagramPacket initPacket) throws IOException
    {
        msg = new byte[1024];
        buffer = new byte[1024];
        datagramSocket = socket;

        //Adiciona DatagramSocket com o ip e porta do receptor
        datagramSocket.connect(initPacket.getAddress(), initPacket.getPort());
        segmentID = 0;
        currentPos = 0;
        reSendCount = 0;
    }

    public void sendFile(File theFile) throws IOException
    {
        fileReader = new FileInputStream(theFile);
        fileLength = fileReader.available();

        System.out.println("*** Nome do arquivo: " + theFile.getName() + " ***");
        System.out.println("*** Bytes a serem enviados: " + fileLength + " ***");

        send((theFile.getName() + "::" + fileLength).getBytes());

        DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
        datagramSocket.receive(reply);

        //aguarda mensagem de Ok do receptor
        if (new String(reply.getData(), 0, reply.getLength()).equals("OK"))
        {
            System.out.println("*** Got OK from receiver - sending the file ***");

            //Enquanto o envio não está completo executa esse while
            while (currentPos < fileLength)
            {
                bytesRead = fileReader.read(msg);

                Message message = new Message(segmentID, msg, bytesRead);
                System.out.println("Sending segment " + message.getSegmentID() + " with " + bytesRead + " byte payload.");
                byte[] test = serialize(message);

                boolean receiveACK = false;
                send(test, bytesRead + packetOverhead); //

                currentPos = currentPos + bytesRead;

                datagramSocket.setSoTimeout(2000); //timeout de 2 segundos, após este tempo o segmento é reenviado
                
                //Controla recebimento de acks
                while (!receiveACK)
                {
                    try
                    {
                        datagramSocket.receive(reply);
                        //System.out.println("Recebeu reply no try " + reply.getData().toString());
                    } catch (SocketTimeoutException e)
                    {
                    	//System.out.println("Entrou no catch para reenviar");
                        send(test, bytesRead + packetOverhead);
                        System.out.println("*** Enviando Segmento " + message.getSegmentID() + " com " + bytesRead + " payload. ***");
                        reSendCount++;
                    }
                    if (new String(reply.getData(), 0, reply.getLength()).equals(Integer.toString(message.getSegmentID())))
                    {
           
                        System.out.println("Ack recebido para o segmento" + new String(reply.getData(), 0, reply.getLength()));
                        segmentID++;
                        receiveACK = true;
                    }
                }
            }
            System.out.println("*** Transferencia do arquivo completa...");
            System.out.println(reSendCount + " Segmentos precisaram ser reenviados. ***");
        } else
        {
            System.out.println("Recebeu outra mensagem que não é OK... encerrando");
        }
    }

    private void send(byte[] message, int length) throws IOException
    {
        DatagramPacket packet = new DatagramPacket(message, length);
        datagramSocket.send(packet);
    }

    private void send(byte[] message) throws IOException
    {
        DatagramPacket packet = new DatagramPacket(message, message.length);
        datagramSocket.send(packet);
    }

    public byte[] serialize(Object obj) throws IOException
    {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
        objectStream.writeObject(obj);
        objectStream.flush();
        return byteStream.toByteArray();
    }
}
