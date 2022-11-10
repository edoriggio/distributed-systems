package server_clients;

import formats.MsgFormatWithHandshake;

import java.io.*;
import java.net.Socket;

public class Client {
    private Socket socket = null;

    private InputStream in = null;
    private OutputStream out = null;

    private BufferedReader userInput = null;

    class MessageHandler implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    MsgFormatWithHandshake.Message fromServer = MsgFormatWithHandshake.Message.parseDelimitedFrom(in);

                    if (fromServer.getFr() == 1) {
                        System.out.println("[Server]: " + fromServer.getMsg());
                    } else {
                        System.out.println("[Client " + fromServer.getFr() + "]: " + fromServer.getMsg());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Client(String address, int port, int clientId) {
        try {
            socket = new Socket(address, port);
            System.out.println("Client" + clientId + "started");

            in = socket.getInputStream();
            out = socket.getOutputStream();

            userInput = new BufferedReader(new InputStreamReader(System.in));

            MsgFormatWithHandshake.Handshake.Builder handshakeOut = MsgFormatWithHandshake.Handshake.newBuilder();
            handshakeOut.setId(clientId);
            handshakeOut.setError(false);
            handshakeOut.build().writeDelimitedTo(out);

            MsgFormatWithHandshake.Handshake handshakeIn = MsgFormatWithHandshake.Handshake.parseDelimitedFrom(in);

            if (handshakeIn.getError()) {
                System.out.println("Could not connect with the server");
            } else {
                System.out.println("Connection with the server established");

                Runnable clientWorker = new MessageHandler();
                new Thread(clientWorker).start();

                String msg = "";

                while (!msg.equals("end")) {
                    msg = userInput.readLine();
                    String[] parsedMsg = msg.split(" ", 2);
                    int toId = Integer.parseInt(parsedMsg[0]);

                    MsgFormatWithHandshake.Message.Builder toServer = MsgFormatWithHandshake.Message.newBuilder();
                    toServer.setFr(clientId);
                    toServer.setTo(toId);
                    toServer.setMsg(parsedMsg[1]);

                    if (toId != clientId) {
                        toServer.build().writeDelimitedTo(out);
                    } else {
                        System.out.println("You must send a message to another client, not yourself!");
                    }
                }
            }

            userInput.close();
            out.close();
            in.close();

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: client [id]");
            System.exit(0);
        }

        Client client = new Client("127.0.0.1", 8080, Integer.parseInt(args[0]));
    }
}