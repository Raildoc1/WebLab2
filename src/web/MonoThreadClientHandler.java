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

    private long _sum = 0;

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

            _sum = 0;

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
                long bytes_read = 0;
                int bytes_to_read = getBytesLeft(bytes_read);
                while(((msg_len = _in.read(_buffer, 0, bytes_to_read)) != -1) && (bytes_read < _fileSize)) {
                    bytes_read += msg_len;
                    System.out.println("Bytes read = " + bytes_read);
                    bytes_to_read = getBytesLeft(bytes_read);
                    updateSum(msg_len);
                    _bytesPerTick += msg_len;
                    _bytesAmount += msg_len;
                    fout.write(_buffer, 0, msg_len);
                    if(_bytesPerTick > 10000)System.out.println("Current speed = " + getCurrentSpeed()+ "Bps");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println("Avg. Speed = " + (_bytesAmount / ((System.currentTimeMillis() - _startTime) / 1000f)) + "Bps");

            System.out.println("Sum = " + _sum);

            long clientSum = _in.readLong();

            if(clientSum != _sum) {
                _out.writeBoolean(false);
            } else {
                _out.writeBoolean(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                _out.flush();
                _out.close();
                _in.close();
            } catch (IOException e) {
                // IGNORE
            }
        }

    }

    private int getBytesLeft(long bytes_read) {
        return ((_fileSize - bytes_read) > (long)(_buffer.length)) ? _buffer.length : (int)(_fileSize - bytes_read);
    }

    private float getCurrentSpeed() {
        _prevTime = _currentTime;
        _currentTime = System.currentTimeMillis();
        _deltaTime = _currentTime - _prevTime;
        float currentSpeed = _bytesPerTick / ((_deltaTime / 1000f));
        _bytesPerTick = 0;
        return  currentSpeed;
    }

    private void updateSum(int msg_len) {
        for (int i = 0; i < msg_len; i++) {
            _sum += _buffer[i];
        }
    }
}
