package peer_to_peer;

import java.io.*;
import java.net.*;
import java.util.Vector;
import java.util.AbstractMap.SimpleEntry;

public class Peer {
    private ServerSocket server = null;

    public static Integer number;
    public static Vector<SimpleEntry<String, Integer>> vector = new Vector<>();

    public Peer(Integer port, Integer id, String[] args) {
        System.out.println(port);

        for (int i = 2; i < args.length-1; i+=2) {
            System.out.println(args[i] + " " + args[i+1]);
            Peer.vector.add(new SimpleEntry<>(args[i], Integer.parseInt(args[i + 1])));
        }

        Peer.number = Peer.vector.size();

        try {
            Server incomingHandler = new Server(new ServerSocket(port), port, id);
            new Thread(incomingHandler).start();

            Client outgoingHandler = new Client(port, id);
            new Thread(outgoingHandler).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("Usage: peer [port] [id] [neighbour_hostname] [neighbour_port]");
            System.exit(0);
        }

        Peer peer = new Peer(Integer.parseInt(args[0]), Integer.parseInt(args[1]), args);
    }
}
