package server_clients;

import formats.MsgFormatWithHandshake;

import java.io.*;
import java.net.Socket;
import java.util.Vector;
import java.net.ServerSocket;

public class Server {
    private ServerSocket server = null;
    private int clientCount = 0;
    private final Vector<SocketHandler> clients = new Vector<>();
    private final Vector<MsgFormatWithHandshake.Message> queue = new Vector<>();

    class SocketHandler implements Runnable {
        private final Socket socket;

        private final InputStream in;
        private final OutputStream out;

        private int clientId;
        private final String prefix;

        public SocketHandler(Socket socket, int clientId) throws IOException {
            this.socket = socket;
            this.clientId = clientId;
            this.prefix = "Client[" + clientId + "]: ";

            this.in = socket.getInputStream();
            this.out = socket.getOutputStream();
        }

        @Override
        public void run() {
            try {
                clients.add(this);
                int serverId = 1;

                MsgFormatWithHandshake.Handshake handshakeIn = MsgFormatWithHandshake.Handshake.parseDelimitedFrom(in);
                boolean isUnreachable = handshakeIn.getError();

                MsgFormatWithHandshake.Handshake.Builder handshakeOut = MsgFormatWithHandshake.Handshake.newBuilder();
                handshakeOut.setId(handshakeIn.getId());
                handshakeOut.setError(isUnreachable);
                handshakeOut.build().writeDelimitedTo(out);
                this.clientId = handshakeIn.getId();

                if(isUnreachable){
                    System.out.println("Error while connecting with " + handshakeIn.getId());
                } else {
                    System.out.println("Connection with " + handshakeIn.getId() + " established");

                    if (queue.size() > 0) {
                        Vector<MsgFormatWithHandshake.Message> toRemove = new Vector<>();

                        for (MsgFormatWithHandshake.Message message : queue) {
                            if (message.getTo() == clientId) {
                                MsgFormatWithHandshake.Message.Builder toClientOk = MsgFormatWithHandshake.Message.newBuilder();
                                toClientOk.setFr(message.getFr());
                                toClientOk.setTo(message.getTo());
                                toClientOk.setMsg(message.getMsg());
                                toClientOk.build().writeDelimitedTo(out);

                                toRemove.add(message);
                            }
                        }

                        queue.removeAll(toRemove);
                    }

                    String msg = "";
                    while (!msg.equals("end")) {
                        try {
                            MsgFormatWithHandshake.Message fromClient = MsgFormatWithHandshake.Message.parseDelimitedFrom(in);
                            msg = fromClient.getMsg();

                            if (fromClient.getTo() == 1) {
                                System.out.println(prefix + msg + ", From: " + fromClient.getFr());
                            } else {
                                boolean clientFound = false;

                                for (SocketHandler client : clients) {
                                    if (client.clientId == fromClient.getTo()) {
                                        clientFound = true;

                                        MsgFormatWithHandshake.Message.Builder toClientOk = MsgFormatWithHandshake.Message.newBuilder();
                                        toClientOk.setFr(fromClient.getFr());
                                        toClientOk.setTo(fromClient.getTo());
                                        toClientOk.setMsg(fromClient.getMsg());
                                        toClientOk.build().writeDelimitedTo(client.out);
                                    }
                                }

                                if (!clientFound) {
                                    System.out.println("Client is not online, adding message to queue");

                                    MsgFormatWithHandshake.Message.Builder toClient1 = MsgFormatWithHandshake.Message.newBuilder();
                                    toClient1.setFr(serverId);
                                    toClient1.setTo(fromClient.getFr());
                                    toClient1.setMsg("The client " + fromClient.getTo() + " is not connected, adding message to queue");
                                    toClient1.build().writeDelimitedTo(out);

                                    queue.add(fromClient);
                                    continue;
                                }
                            }

                            MsgFormatWithHandshake.Message.Builder toClient = MsgFormatWithHandshake.Message.newBuilder();
                            toClient.setFr(serverId);
                            toClient.setTo(fromClient.getFr());
                            toClient.setMsg("Received the message " + msg);
                            toClient.build().writeDelimitedTo(out);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                System.out.println("Closing connection with " + prefix);
                clients.remove(this);

                in.close();
                out.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class ServerOperator implements Runnable {
        private BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

        @Override
        public void run() {
            String command = "";
            while (true) {
                try {
                    command = input.readLine();
                    if (command.equals("num_users")) {
                        System.out.println("Users Connected: " + clients.size());
                    } else if (command.equals("get_queue")) {
                        if (queue.size() == 0) {
                            System.out.println("Queue is empty");
                        } else {
                            System.out.println("Queue contains the following messages:");

                            for (MsgFormatWithHandshake.Message message : queue) {
                                System.out.println("From: " + message.getFr() + ", To: " + message.getTo() + ", Message: " + message.getMsg());
                            }
                        }
                    }else {
                        System.out.println("Invalid Command");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Server(int port) {
        try {
            server = new ServerSocket(port);
            System.out.println("Server started");

            ServerOperator operator = new ServerOperator();
            new Thread(operator).start();

            while (true) {
                Socket socket = server.accept();
                System.out.println("Client accepted");
                SocketHandler handler = new SocketHandler(socket, ++clientCount);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: server [port]");
            System.exit(0);
        }

        Server server = new Server(Integer.parseInt(args[0]));
    }
}