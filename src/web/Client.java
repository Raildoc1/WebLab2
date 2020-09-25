package web;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Client extends Socket {

    private DataOutputStream _out;
    private DataInputStream _in;

    private final Path FILE_PATH;
    private final long FILE_SIZE;
    private final int FILE_NAME_SIZE;
    private final String FILE_NAME;
    private byte[] _buffer = new byte[1024];
    private long real_file_size = 0;

    private long _sum = 0;

    public Client(String host, int port, String filePath) throws IOException {
        super(host, port);

        this.FILE_PATH = Paths.get(filePath);

        if (!Files.exists(this.FILE_PATH)) {
            throw new FileNotFoundException(filePath);
        }

        this.FILE_NAME = this.FILE_PATH.getFileName().toString();
        this.FILE_NAME_SIZE = FILE_NAME.getBytes().length;
        this.FILE_SIZE = Files.size(this.FILE_PATH);
    }

    public void start() throws IOException {

        try(BufferedInputStream fileReader = new BufferedInputStream(Files.newInputStream(FILE_PATH));) {

            _out = new DataOutputStream(this.getOutputStream());
            _in = new DataInputStream(this.getInputStream());

            _out.writeInt(FILE_NAME_SIZE);
            _out.writeUTF(FILE_NAME);
            _out.writeLong(FILE_SIZE);
            System.out.println("FILE_SIZE = " + FILE_SIZE);

            _out.flush();

            _sum = 0;

            if(_in.readBoolean()) {
                int msg_len;
                while ((msg_len = fileReader.read(_buffer)) != -1) {
                    updateSum(msg_len);
                    _out.write(_buffer, 0, msg_len);
                }
                _out.flush();
            } else {
                System.out.println("The file has already been created!");
            }

            _out.writeLong(_sum);
            _out.flush();

            System.out.println("Sum = " + _sum);
            System.out.println("File Size = " + FILE_SIZE);
            System.out.println("Real = " + real_file_size);

            if(_in.readBoolean()) {
                System.out.println("File successfully uploaded!");
            } else {
                System.out.println("File upload failed!");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeClient() throws IOException {
        this.close();
    }

    private void updateSum(int msg_len) {
        for (int i = 0; i < msg_len; i++) {
            _sum += _buffer[i];
            real_file_size++;
        }
    }
}
