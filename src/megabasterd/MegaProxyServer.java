package megabasterd;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Thanks to -> https://stackoverflow.com/users/6477541/sarvesh-agarwal
 */
public class MegaProxyServer extends Thread {

    private String _password;
    private int _port;
    private ServerSocket _serverSocket;

    public String getPassword() {
        return _password;
    }

    public int getPort() {
        return _port;
    }

    public MegaProxyServer(String password, int port) {

        super("Server Thread");
        _password = password;
        _port = port;

    }

    public synchronized void stopServer() throws IOException {

        this._serverSocket.close();
    }

    @Override
    public void run() {

        try {

            _serverSocket = new ServerSocket(_port);

            Socket socket;

            try {

                while ((socket = _serverSocket.accept()) != null) {
                    (new Handler(socket, this._password)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();  // TODO: implement catch
            }

        } catch (IOException ex) {
            Logger.getLogger(MegaProxyServer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {

            if (!_serverSocket.isClosed()) {
                try {
                    _serverSocket.close();
                } catch (IOException ex) {
                    Logger.getLogger(MegaProxyServer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public static class Handler extends Thread {

        public static final Pattern CONNECT_PATTERN = Pattern.compile("CONNECT (.*mega(?:\\.co)?\\.nz):(443) HTTP/(1\\.[01])", Pattern.CASE_INSENSITIVE);
        public static final Pattern AUTH_PATTERN = Pattern.compile("Proxy-Authorization: Basic +(.+)", Pattern.CASE_INSENSITIVE);

        private final Socket clientSocket;
        private boolean previousWasR = false;
        private String _password;

        public Handler(Socket clientSocket, String password) {
            this.clientSocket = clientSocket;
            this._password = password;
        }

        @Override
        public void run() {
            try {
                String request = readLine(clientSocket);
                System.out.println(request);
                Matcher matcher = CONNECT_PATTERN.matcher(request);
                boolean auth_ok = false;

                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8");

                if (matcher.matches()) {

                    String header;
                    String proxy_auth = null;

                    do {
                        header = readLine(clientSocket);

                        Matcher matcher_auth = AUTH_PATTERN.matcher(header);

                        if (matcher_auth.matches()) {

                            proxy_auth = new String(MiscTools.BASE642Bin(matcher_auth.group(1).trim()));

                            System.out.println(proxy_auth);
                        }

                    } while (!"".equals(header));

                    if (proxy_auth != null && proxy_auth.equals("megacrypter:" + this._password)) {
                        final Socket forwardSocket;

                        try {
                            forwardSocket = new Socket(matcher.group(1), Integer.parseInt(matcher.group(2)));
                            System.out.println(forwardSocket);
                        } catch (IOException | NumberFormatException e) {
                            e.printStackTrace();  // TODO: implement catch
                            outputStreamWriter.write("HTTP/" + matcher.group(3) + " 502 Bad Gateway\r\n");
                            outputStreamWriter.write("Proxy-agent: MegaBasterd/0.1\r\n");
                            outputStreamWriter.write("\r\n");
                            outputStreamWriter.flush();
                            return;
                        }
                        try {
                            outputStreamWriter.write("HTTP/" + matcher.group(3) + " 200 Connection established\r\n");
                            outputStreamWriter.write("Proxy-agent: MegaBasterd/0.1\r\n");
                            outputStreamWriter.write("\r\n");
                            outputStreamWriter.flush();

                            Thread remoteToClient = new Thread() {
                                @Override
                                public void run() {
                                    forwardData(forwardSocket, clientSocket);
                                }
                            };
                            remoteToClient.start();
                            try {
                                if (previousWasR) {
                                    int read = clientSocket.getInputStream().read();
                                    if (read != -1) {
                                        if (read != '\n') {
                                            forwardSocket.getOutputStream().write(read);
                                        }
                                        forwardData(clientSocket, forwardSocket);
                                    } else {
                                        if (!forwardSocket.isOutputShutdown()) {
                                            forwardSocket.shutdownOutput();
                                        }
                                        if (!clientSocket.isInputShutdown()) {
                                            clientSocket.shutdownInput();
                                        }
                                    }
                                } else {
                                    forwardData(clientSocket, forwardSocket);
                                }
                            } finally {
                                try {
                                    remoteToClient.join();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();  // TODO: implement catch
                                }
                            }
                        } finally {
                            forwardSocket.close();
                        }

                    } else {
                        outputStreamWriter.write("HTTP/1.1 403 Unauthorized\r\n");
                        outputStreamWriter.write("Proxy-agent: MegaBasterd/0.1\r\n");
                        outputStreamWriter.write("\r\n");
                        outputStreamWriter.flush();
                        return;
                    }

                } else {
                    outputStreamWriter.write("HTTP/1.1 403 Unauthorized\r\n");
                    outputStreamWriter.write("Proxy-agent: MegaBasterd/0.1\r\n");
                    outputStreamWriter.write("\r\n");
                    outputStreamWriter.flush();
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();  // TODO: implement catch
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();  // TODO: implement catch
                }
            }
        }

        private static void forwardData(Socket inputSocket, Socket outputSocket) {
            try {
                InputStream inputStream = inputSocket.getInputStream();
                try {
                    OutputStream outputStream = outputSocket.getOutputStream();
                    try {
                        byte[] buffer = new byte[4096];
                        int read;
                        do {
                            read = inputStream.read(buffer);
                            if (read > 0) {
                                outputStream.write(buffer, 0, read);
                                if (inputStream.available() < 1) {
                                    outputStream.flush();
                                }
                            }
                        } while (read >= 0);
                    } finally {
                        if (!outputSocket.isOutputShutdown()) {
                            outputSocket.shutdownOutput();
                        }
                    }
                } finally {
                    if (!inputSocket.isInputShutdown()) {
                        inputSocket.shutdownInput();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();  // TODO: implement catch
            }
        }

        private String readLine(Socket socket) throws IOException {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int next;
            readerLoop:
            while ((next = socket.getInputStream().read()) != -1) {
                if (previousWasR && next == '\n') {
                    previousWasR = false;
                    continue;
                }
                previousWasR = false;
                switch (next) {
                    case '\r':
                        previousWasR = true;
                        break readerLoop;
                    case '\n':
                        break readerLoop;
                    default:
                        byteArrayOutputStream.write(next);
                        break;
                }
            }
            return byteArrayOutputStream.toString("UTF-8");
        }
    }
}
