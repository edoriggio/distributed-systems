package peer_to_peer;

import formats.MsgFormatWithLastPort;

import java.io.*;
import java.net.Socket;
import java.util.AbstractMap;
import java.net.ConnectException;

public class Client implements Runnable {
    private Socket destSocket;
    private final Socket socket;

    private InputStream in = null;
    private OutputStream out = null;
    private BufferedReader userInput = null;

    private final int id;
    private final int port;

    public Client(int port, int id) throws IOException {
        this.id = id;
        this.port = port;
        this.socket = new Socket("127.0.0.1", port);

        try {
            this.in = socket.getInputStream();
            this.out = socket.getOutputStream();
            this.userInput = new BufferedReader(new InputStreamReader(System.in));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            System.out.println("Peer " + id + " Client started");
            String msg = "";

            while (!msg.equals("end")) {
                msg = userInput.readLine();
                String[] parsedMsg = msg.split(" ", 2);

                int toId = Integer.parseInt(parsedMsg[0]);
                String message = parsedMsg[1];

                for (AbstractMap.SimpleEntry<String, Integer> peer : Peer.vector) {
                    try {
                        destSocket = new Socket(peer.getKey(), peer.getValue());

                        MsgFormatWithLastPort.Message.Builder toNextPeer = MsgFormatWithLastPort.Message.newBuilder();
                        toNextPeer.setFr(this.id);
                        toNextPeer.setTo(toId);
                        toNextPeer.setMsg(message);
                        toNextPeer.setPrt(this.port);

                        toNextPeer.build().writeDelimitedTo(destSocket.getOutputStream());
                    } catch (ConnectException e) {
                        System.out.println("Peer at port " + peer.getValue() + " is not up.");
                    }
                }

                System.out.println("Message was sent correctly");
            }

            userInput.close();
            out.close();

            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}