import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class CriticalSection {
    public static void Communicate(Socket client) throws IOException {
        DataInputStream dis;
        try {
            dis=new DataInputStream(client.getInputStream());
            BufferedWriter writer = new BufferedWriter(new FileWriter("Critical_Section.txt", true));
            writer.write(dis.readUTF());
            writer.newLine();
            writer.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
//        try {
//            System.out.println(dis.readUTF());
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//
//        }
        try {
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        DataOutputStream write=new DataOutputStream(client.getOutputStream());
        write.writeUTF("done");
        try {
            client.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
    public static void main(String[] args){
        ServerSocket server= null;
        try {
            server = new ServerSocket(8080);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        while(true){
            Socket client=null;
            try {
                client=server.accept();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Socket finalClient = client;
            new Thread(()-> {
                try {
                    Communicate(finalClient);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
    }
}