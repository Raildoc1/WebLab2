package web;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MonoThreadClientHandler implements Runnable {

    private Socket _client;
    private DataInputStream _in;
    private DataOutputStream _out;

    private int _fileNameSize;
    private String _fileName;
    private long _fileSize;
    private Path _newFilePath;
    private byte[] _buffer = new byte[1024];

    private int _bytesAmount = 0;
    private int _bytesPerTick = 0;

    private long _deltaTime = 0;
    private long _currentTime = 0;
    private long _prevTime = 0;
    private long _startTime = 0;

    public MonoThreadClientHandler(Socket socket) {
        _client = socket;
    }

    @Override
    public void run() {

        try {
            _out = new DataOutputStream(_client.getOutputStream());
            _in = new DataInputStream(_client.getInputStream());

            _currentTime = System.currentTimeMillis();
            _startTime = System.currentTimeMillis();

            _fileNameSize = _in.readInt();
            _fileName = _in.readUTF();
            _fileSize = _in.readLong();

            _bytesAmount += Integer.BYTES + _fileName.length() + Long.BYTES;
            _bytesPerTick += Integer.BYTES + _fileName.length() + Long.BYTES;

            try {
                _newFilePath = Files.createFile(Paths.get("files", _fileName));
            } catch (FileAlreadyExistsException e) {
                _out.writeBoolean(false);
            }

            _out.writeBoolean(true);

            int msg_len;

            System.out.println("Current speed = " + getCurrentSpeed());

            try(OutputStream fout = Files.newOutputStream(_newFilePath)) {
                while((msg_len = _in.read(_buffer)) != -1) {
                    _bytesPerTick += msg_len;
                    _bytesAmount += msg_len;
                    fout.write(_buffer, 0, msg_len);
                    if(_bytesPerTick > 10000)System.out.println("Current speed = " + getCurrentSpeed()+ "Bps");
                }
            }

            System.out.println("Avg. Speed = " + (_bytesAmount / ((System.currentTimeMillis() - _startTime) / 1000f)) + "Bps");

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private float getCurrentSpeed() {
        _prevTime = _currentTime;
        _currentTime = System.currentTimeMillis();
        _deltaTime = _currentTime - _prevTime;
        float currentSpeed = _bytesPerTick / ((_deltaTime / 1000f));
        _bytesPerTick = 0;
        return  currentSpeed;
    }
}
