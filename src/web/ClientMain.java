package web;

import java.io.IOException;

public class ClientMain {
    public static void main(String[] args) throws IOException {
        Client client = new Client("localhost", 2020, "test.txt");
        client.start();
        client.closeClient();
    }
}
