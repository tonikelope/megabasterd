package com.tonikelope.megabasterd;

import static com.tonikelope.megabasterd.MiscTools.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author tonikelope
 *
 *
 * Thanks to -> https://stackoverflow.com/users/6477541/sarvesh-agarwal
 */
public class MegaProxyServer implements Runnable {

    private static final Logger LOG = Logger.getLogger(MegaProxyServer.class.getName());

    private final String _password;
    private final int _port;
    private ServerSocket _serverSocket;
    private final MainPanel _main_panel;

    public MegaProxyServer(MainPanel main_panel, String password, int port) {

        _main_panel = main_panel;
        _password = password;
        _port = port;

    }

    public String getPassword() {
        return _password;
    }

    public int getPort() {
        return _port;
    }

    public synchronized void stopServer() throws IOException {

        _serverSocket.close();
    }

    @Override
    public void run() {

        _main_panel.getView().updateMCReverseStatus(LabelTranslatorSingleton.getInstance().translate("MC reverse mode: ON (port ") + _port + ")");

        try {

            _serverSocket = new ServerSocket(_port);

            Socket socket;

            try {

                while ((socket = _serverSocket.accept()) != null) {
                    (new Handler(socket, _password)).start();
                }
            } catch (IOException e) {

            }

        } catch (IOException ex) {
            LOG.log(Level.SEVERE, ex.getMessage());
        } finally {

            if (!_serverSocket.isClosed()) {
                try {
                    _serverSocket.close();
                } catch (IOException ex) {
                    LOG.log(Level.SEVERE, ex.getMessage());
                }
            }
        }

        _main_panel.getView().updateMCReverseStatus("MC reverse mode: OFF");
    }

    public static class Handler extends Thread {

        public static final Pattern CONNECT_PATTERN = Pattern.compile("CONNECT (.*mega(?:\\.co)?\\.nz):(443) HTTP/(1\\.[01])", Pattern.CASE_INSENSITIVE);
        public static final Pattern AUTH_PATTERN = Pattern.compile("Proxy-Authorization: Basic +(.+)", Pattern.CASE_INSENSITIVE);

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

            }
        }

        private final Socket _clientSocket;
        private boolean _previousWasR = false;
        private final String _password;

        public Handler(Socket clientSocket, String password) {
            _clientSocket = clientSocket;
            _password = password;
        }

        @Override
        public void run() {
            try {
                String request = readLine(_clientSocket);

                LOG.log(Level.INFO, request);

                Matcher matcher = CONNECT_PATTERN.matcher(request);

                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(_clientSocket.getOutputStream(), "UTF-8");

                if (matcher.matches()) {

                    String header;
                    String proxy_auth = null;

                    do {
                        header = readLine(_clientSocket);

                        Matcher matcher_auth = AUTH_PATTERN.matcher(header);

                        if (matcher_auth.matches()) {

                            proxy_auth = new String(BASE642Bin(matcher_auth.group(1).trim()), "UTF-8");

                        }

                        LOG.log(Level.INFO, header);

                    } while (!"".equals(header));

                    if (proxy_auth != null && proxy_auth.matches(".*?: *?" + _password)) {
                        final Socket forwardSocket;

                        try {
                            forwardSocket = new Socket(matcher.group(1), Integer.parseInt(matcher.group(2)));

                        } catch (IOException | NumberFormatException e) {

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
                                    forwardData(forwardSocket, _clientSocket);
                                }
                            };
                            remoteToClient.start();
                            try {
                                if (_previousWasR) {
                                    int read = _clientSocket.getInputStream().read();
                                    if (read != -1) {
                                        if (read != '\n') {
                                            forwardSocket.getOutputStream().write(read);
                                        }
                                        forwardData(_clientSocket, forwardSocket);
                                    } else {
                                        if (!forwardSocket.isOutputShutdown()) {
                                            forwardSocket.shutdownOutput();
                                        }
                                        if (!_clientSocket.isInputShutdown()) {
                                            _clientSocket.shutdownInput();
                                        }
                                    }
                                } else {
                                    forwardData(_clientSocket, forwardSocket);
                                }
                            } finally {
                                try {
                                    remoteToClient.join();
                                } catch (InterruptedException e) {

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

                    }

                } else {
                    outputStreamWriter.write("HTTP/1.1 403 Unauthorized\r\n");
                    outputStreamWriter.write("Proxy-agent: MegaBasterd/0.1\r\n");
                    outputStreamWriter.write("\r\n");
                    outputStreamWriter.flush();

                }

            } catch (IOException e) {

            } finally {

                try {
                    _clientSocket.close();
                } catch (IOException e) {

                }
            }
        }

        private String readLine(Socket socket) throws IOException {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int next;
            readerLoop:
            while ((next = socket.getInputStream().read()) != -1) {
                if (_previousWasR && next == '\n') {
                    _previousWasR = false;
                    continue;
                }
                _previousWasR = false;
                switch (next) {
                    case '\r':
                        _previousWasR = true;
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
