package web;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private final int PORT;
    private ServerSocket _serverSocket;
    private ArrayList<MonoThreadClientHandler> _clientThreads;
    private ExecutorService _executorService = Executors.newFixedThreadPool(256);

    public Server(int port) throws IOException {
        this.PORT = port;
        _serverSocket = new ServerSocket(port);
        _clientThreads = new ArrayList<>();
    }

    public void start() throws IOException {
        BufferedReader systemInReader = new BufferedReader(new InputStreamReader(System.in));

        while(!_serverSocket.isClosed()) {
            try {
                _serverSocket.setSoTimeout(1000);

                Socket client = _serverSocket.accept();
                MonoThreadClientHandler clientThread = new MonoThreadClientHandler(client);

                _clientThreads.add(clientThread);

                _executorService.execute(clientThread);

            } catch (IOException e) { }
            if(System.in.available() != 0) {
                Scanner sc = new Scanner(System.in);
                String s = sc.nextLine();
                if(s.equals("stop")) close();
            }
        }

        _executorService.shutdown();
    }

    public void close() throws IOException {
        _serverSocket.close();
    }

}
