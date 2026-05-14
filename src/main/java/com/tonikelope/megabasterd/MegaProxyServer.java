/*
 __  __                  _               _               _ 
|  \/  | ___  __ _  __ _| |__   __ _ ___| |_ ___ _ __ __| |
| |\/| |/ _ \/ _` |/ _` | '_ \ / _` / __| __/ _ \ '__/ _` |
| |  | |  __/ (_| | (_| | |_) | (_| \__ \ ||  __/ | | (_| |
|_|  |_|\___|\__, |\__,_|_.__/ \__,_|___/\__\___|_|  \__,_|
             |___/                                         
© Perpetrated by tonikelope since 2016
 */
package com.tonikelope.megabasterd;

import static com.tonikelope.megabasterd.MiscTools.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
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

    private static final int MAX_PROXY_THREADS = 64;

    private final String _password;
    private final int _port;
    private volatile ServerSocket _serverSocket;
    private final MainPanel _main_panel;
    private volatile ExecutorService _handler_pool;

    public MegaProxyServer(MainPanel main_panel, String password, int port) {

        _main_panel = main_panel;
        _password = password;
        _port = port;

    }

    private static ThreadFactory _daemonFactory(final String name_prefix) {
        return new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, name_prefix + counter.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        };
    }

    public String getPassword() {
        return _password;
    }

    public int getPort() {
        return _port;
    }

    public synchronized void stopServer() throws IOException {

        if (_serverSocket != null) {
            _serverSocket.close();
        }

        if (_handler_pool != null) {
            _handler_pool.shutdownNow();
        }
    }

    @Override
    public void run() {

        _handler_pool = Executors.newFixedThreadPool(MAX_PROXY_THREADS, _daemonFactory("MegaProxyHandler-"));

        try {

            _serverSocket = new ServerSocket(_port, 50, InetAddress.getLoopbackAddress());

            Socket socket;

            try {

                while ((socket = _serverSocket.accept()) != null) {
                    socket.setSoTimeout(30000);
                    final Handler handler = new Handler(socket, _password, _handler_pool);
                    try {
                        _handler_pool.execute(handler);
                    } catch (java.util.concurrent.RejectedExecutionException rex) {
                        LOG.log(Level.WARNING, "MegaProxyServer rejected connection (pool saturated)");
                        try {
                            socket.close();
                        } catch (IOException ignore) {
                        }
                    }
                }
            } catch (IOException e) {
                LOG.log(Level.FINE, "MegaProxyServer accept loop ended: {0}", e.getMessage());
            }

        } catch (IOException ex) {
            LOG.log(Level.SEVERE, ex.getMessage());
        } finally {

            if (_serverSocket != null && !_serverSocket.isClosed()) {
                try {
                    _serverSocket.close();
                } catch (IOException ex) {
                    LOG.log(Level.SEVERE, ex.getMessage());
                }
            }

            if (_handler_pool != null) {
                _handler_pool.shutdownNow();
            }
        }
    }

    public static class Handler implements Runnable {

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
        private final ExecutorService _pool;

        public Handler(Socket clientSocket, String password, ExecutorService pool) {
            _clientSocket = clientSocket;
            _password = password;
            _pool = pool;
        }

        private boolean _checkProxyAuth(String proxy_auth) {

            int sep = proxy_auth.indexOf(':');
            if (sep < 0) {
                return false;
            }

            byte[] expected = _password.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] given = proxy_auth.substring(sep + 1).trim().getBytes(java.nio.charset.StandardCharsets.UTF_8);

            return java.security.MessageDigest.isEqual(expected, given);
        }

        @Override
        public void run() {
            try {
                String request = readLine(_clientSocket);

                LOG.log(Level.FINE, request);

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

                            LOG.log(Level.FINE, "Proxy-Authorization: [REDACTED]");

                        } else {

                            LOG.log(Level.FINE, header);
                        }

                    } while (!"".equals(header));

                    if (proxy_auth != null && _checkProxyAuth(proxy_auth)) {
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

                            // Dedicated daemon thread for the remote->client direction.
                            // Previously this submitted to _pool and blocked .get() on it;
                            // with a bounded pool (64 threads) that meant each Handler
                            // occupied 2 pool slots and saturated at 32 concurrent
                            // connections -- and beyond that, every new connection
                            // deadlocked because the Handler held one slot, queued its
                            // remoteToClient, and blocked waiting for a slot that would
                            // never come free until SO_TIMEOUT fired.
                            final Thread remoteToClient = new Thread(() -> forwardData(forwardSocket, _clientSocket),
                                    "MegaProxyHandler-remote-" + _clientSocket.getRemoteSocketAddress());
                            remoteToClient.setDaemon(true);
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
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
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
