import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.Semaphore;

public class Main {
    private static PriorityQueue<Map.Entry<Integer,String>> priorityQueue=new PriorityQueue<>(
            (e1, e2) -> Integer.compare(e1.getKey(), e2.getKey())
    );;
    public static String localIpAddress;
    public static ArrayList<String> eventHistory=new ArrayList<>();
    public static int localPort, port1, port2,port_cs;
    public static String add1, add2,add_cs;
    public static ServerSocket serverSocket;
    private static int resno;
    private static int timeStamp=0;
    private static final Object lock = new Object();
    private static final Object serverLock = new Object();
    private static final Object res_lock = new Object();

    private static int eventCount=0;
    private static boolean inCriticalSection=false;
    private static Semaphore semaphore = new Semaphore(2);
    private static int server_close =0;
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("google.com", 80));
        System.out.println("System running on"+socket.getLocalAddress().toString().substring(1));
        localIpAddress=socket.getLocalAddress().toString().substring(1);
        socket.close();
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter Portno:");
        localPort = scanner.nextInt();

        new Thread(() -> {
            try {
                server();
            } catch (IOException e) {
                e.printStackTrace(); // Log the exception instead of throwing a RuntimeException
            }
        }).start();

        System.out.println("Enter Portno's:");
        port1 = scanner.nextInt();
        port2 = scanner.nextInt();

        // can be changed to work in multiple devices
        add1 = "localhost";
        add2 = "localhost";
        add_cs="localhost";
        port_cs=8080;
        Communicate();

    }
    public static void enter_cs() throws IOException {
        Socket client= null;
        client = waitForServer(add_cs,port_cs);
        DataOutputStream write=new DataOutputStream(client.getOutputStream());
        DataInputStream read=new DataInputStream(client.getInputStream());
        write.writeUTF(localIpAddress+String.valueOf(localPort));
        String message = null;
        message=read.readUTF();
//        System.out.println(message+" CS");
    }
    public static void req_handler()  {
        int t;

        synchronized (lock) {
            eventCount++;
            timeStamp++;
            t = timeStamp;
            eventHistory.add(String.valueOf(eventCount)+" -> "+String.valueOf(timeStamp)+" -> sending REQ");
        }
        synchronized (res_lock){
            resno=0;
        }
        synchronized (priorityQueue) {
            priorityQueue.add(Map.entry(t, "self"));
        }

        client("REQ_"+t+"-"+localIpAddress+"&"+localPort);
        try {
            semaphore.acquire(2);
            semaphore.acquire(2);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        try {
//            System.out.println("Entering critical Section");
            enter_cs();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("exiting CS");
        semaphore.release();
        semaphore.release();


        synchronized (priorityQueue){
            priorityQueue.poll();
        }
        synchronized (lock){
            timeStamp++;
            eventCount++;
            t=timeStamp;
            eventHistory.add(String.valueOf(eventCount)+" -> "+String.valueOf(timeStamp)+" ->sending REL");
        }
        client("REL_"+t+"-"+localIpAddress+"&"+localPort);
    }
    public static void Communicate() throws IOException {
        while (true) {
            System.out.println("\n\n -------------------- \n Enter the number: \n 1.Create an Event \n 2.Request CS \n 3.Print Logs \n 4. print queue's top \n");
            Scanner scanner = new Scanner(System.in);
            int task = scanner.nextInt();
            if (task == 1) {
                synchronized (lock) {
                    eventCount++;
                    timeStamp++;
                    eventHistory.add(String.valueOf(eventCount)+" -> "+String.valueOf(timeStamp)+" -> new event");
                }
            } else if (task == 2) {
                inCriticalSection=true;
//                   new Thread(this::req_handler).start();
                req_handler();
                inCriticalSection=false;
            }
            else if(task==3)
            {
                for (int i = 0; i < eventHistory.size(); i++) {
                    System.out.println(eventHistory.get(i));
                }
            }
            else if(task==4){
                synchronized (priorityQueue){
                    if( priorityQueue.peek() != null)
                        System.out.println("top element in priorityQueue is:"+ priorityQueue.peek().getValue());
                    else
                        System.out.println("Queue is empty");
                }
            }
            else{
                break;
            }
        }
        System.out.println("Exiting cli interface");
        serverSocket.close();
    }

    public static void client(String message) {
        try (Socket client1 = waitForServer(add1, port1);
             Socket client2 = waitForServer(add2, port2);
             DataOutputStream dout1 = new DataOutputStream(client1.getOutputStream());
             DataOutputStream dout2 = new DataOutputStream(client2.getOutputStream())) {
//             System.out.println("Port no connected"+client1.getPort());
            dout1.writeUTF(message);
            dout1.flush();

            dout2.writeUTF(message);
            dout2.flush();
            client1.close();
            client2.close();
        } catch (IOException e) {
            e.printStackTrace(); // Handle the exception appropriately
        }
    }
    public static void write(String add,int port,String message){
        try {
            System.out.println("Sending RES request to "+port+add);
            Socket client=waitForServer(add,port);
            DataOutputStream dout = new DataOutputStream(client.getOutputStream());
            synchronized (lock) {
                eventCount++;
                timeStamp++;
                eventHistory.add(String.valueOf(eventCount)+" -> "+String.valueOf(timeStamp)+" -> RES");
            }
            dout.writeUTF(message);
            dout.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
    public static Socket waitForServer(String add,int port) throws IOException {
        boolean serverOpen = false;
        Socket socket=null;
        while (!serverOpen) {
            try {
                // Attempt to connect to the server
                socket = new Socket(add, port);
                serverOpen = true;
            } catch (IOException e) {
                // Server is not reachable or closed, wait and retry
                System.out.println("Server is not open yet. Retrying in " + 5000 + " ms.");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    System.err.println("Interrupted while waiting for server.");
                }
            }
        }
        return socket;

    }
    public static void server() throws IOException {
        serverSocket = new ServerSocket(localPort);
        while (true) {
            Socket clientSocket=null;
            try {
                clientSocket = serverSocket.accept();
            }
            catch(SocketException s){
                System.out.println("exiting programme");
                break;
            }
            synchronized(serverLock) {
                if (server_close == 1) {
                    break;
                }
            }
            Socket finalClientSocket = clientSocket;
            new Thread(() -> handleClient(finalClientSocket)).start();

            synchronized(serverLock) {
                if (server_close == 1) {
                    break;
                }
            }
        }
        serverSocket.close(); // Uncomment close the server socket
    }

    private static void handleClient(Socket clientSocket) {
        try (DataInputStream dis = new DataInputStream(clientSocket.getInputStream())) {
            String clientName ;
            String address;
            int clientport;
            // type_time-ip&port
            String message = dis.readUTF();
            if(message==null)
                return;
            int index1=message.indexOf("_");
            String type=message.substring(0,index1);
            int index2=message.indexOf("-");
            int index3=message.indexOf("&");
            address=message.substring(index2+1,index3);
            clientName=address+message.substring(index3+1);
            clientport=Integer.parseInt(message.substring(index3+1));
            int t,h;
            int client_clock;
            try {
                client_clock = Integer.parseInt(message.substring(index1+1,index2));
            }
            catch (NumberFormatException n){
                System.out.println("error in the message:"+message+" "+clientport);
                return;
            }

            synchronized (lock){
                eventCount++;
                h=timeStamp;
                timeStamp=Math.max(timeStamp,client_clock)+1;
                t=timeStamp;
                eventHistory.add(String.valueOf(eventCount)+" -> "+String.valueOf(t)+" -> "+type+"  "+client_clock);

            }
//            if(h>=client_clock)
//                System.out.println("Casuality broken");
            if(type.endsWith("REQ")){
                synchronized (priorityQueue)
                {
                    priorityQueue.add(Map.entry(client_clock,clientName));
                }
//                writer.println("RES"+t);
                write(address,clientport,"RES_"+(t+1)+"-"+localIpAddress+"&"+localPort);
            }
            else if(type.endsWith("RES")){

                synchronized (res_lock){
                    resno++;
                    System.out.println("res section");
                    if(resno==2) {
                        semaphore.release();
                        semaphore.release();
//                        cs_lock.unlock();
                        resno=0;
                        if (priorityQueue.peek().getValue().equals("self"))
                            semaphore.release();
                    }

                }

            }
            else if(type.endsWith("REL")){
                synchronized (priorityQueue){
                    if(!Objects.requireNonNull(priorityQueue.poll()).getValue().equals(clientName)){
                        System.out.println("Error in the queue");
                    }
                    if(inCriticalSection) {
                        assert priorityQueue.peek() != null;
                        if (priorityQueue.peek().getValue().equals("self")) {
                            semaphore.release();
                        }
                    }
                }
            }


            System.out.println("Message received: " + message+" from "+clientport);

        } catch (IOException e) {
            e.printStackTrace(); // Handle the exception appropriately
        }
    }
}