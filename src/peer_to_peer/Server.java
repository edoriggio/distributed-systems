package peer_to_peer;

import formats.MsgFormatWithLastPort;

import java.net.Socket;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.AbstractMap.SimpleEntry;

public class Server implements Runnable {
    private final ServerSocket server;
    private static Socket destSocket = null;

    private static int port;
    private final int id;

    static class SocketHandler implements Runnable {
        private final int peerId;
        private final Socket socket;

        private final InputStream in;

        public SocketHandler(Socket socket, int peerId) throws IOException {
            this.socket = socket;
            this.peerId = peerId;

            this.in = socket.getInputStream();
        }

        @Override
        public void run() {
            while (true) {
                try {
                    MsgFormatWithLastPort.Message fromPeer = MsgFormatWithLastPort.Message.parseDelimitedFrom(in);

                    if (fromPeer != null) {
                        if ((Peer.number > 1 || this.peerId == fromPeer.getFr()) && this.peerId != fromPeer.getTo()) {
                            for (SimpleEntry<String, Integer> peer : Peer.vector) {
                                if (peer.getValue() != fromPeer.getPrt()) {
                                    Server.destSocket = new Socket(peer.getKey(), peer.getValue());

                                    MsgFormatWithLastPort.Message.Builder toNextPeer = MsgFormatWithLastPort.Message.newBuilder();
                                    toNextPeer.setFr(fromPeer.getFr());
                                    toNextPeer.setTo(fromPeer.getTo());
                                    toNextPeer.setMsg(fromPeer.getMsg());
                                    toNextPeer.setPrt(port);
                                    toNextPeer.build().writeDelimitedTo(destSocket.getOutputStream());

                                    Server.destSocket.close();
                                }
                            }
                        } else if (this.peerId == fromPeer.getTo()) {
                            System.out.println("[Peer " + fromPeer.getFr() + "]: " + fromPeer.getMsg());
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Server(ServerSocket server, int port, int id) {
        this.server = server;
        this.id = id;

        Server.port = port;
    }

    @Override
    public void run() {
        System.out.println("Peer's server started");

        try {
            while (true) {
                Socket socket = server.accept();

                SocketHandler handler = new SocketHandler(socket, id);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}