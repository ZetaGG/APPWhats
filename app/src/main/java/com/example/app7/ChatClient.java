package com.example.app7;

import android.os.Handler;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ChatClient {
    private final String serverIp;
    private final int serverPort;
    private final String username;
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private Thread thread;
    private boolean running = false;

    public interface Listener {
        void onUserListUpdate(String rawUsers);
        void onPrivateMessage(String message);
        void onError(String error);
        void onDisconnected();
    }

    private final Listener listener;
    private final Handler uiHandler;

    public ChatClient(String serverIp, int serverPort, String username, Handler uiHandler, Listener listener) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.username = username;
        this.uiHandler = uiHandler;
        this.listener = listener;
    }

    public void connect() {
        thread = new Thread(() -> {
            try {
                socket = new Socket(serverIp, serverPort);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                writer.write(username + "\n");
                writer.flush();
                running = true;

                String line;
                while (running && (line = reader.readLine()) != null) {
                    final String msg = line;
                    if (msg.startsWith("#usuarios:")) {
                        uiHandler.post(() -> listener.onUserListUpdate(msg));
                    } else if (msg.startsWith("[Privado de ")) {
                        uiHandler.post(() -> listener.onPrivateMessage(msg));
                    }
                }
            } catch (IOException e) {
                uiHandler.post(() -> listener.onError("Error: " + e.getMessage()));
            } finally {
                disconnect();
                uiHandler.post(listener::onDisconnected);
            }
        });
        thread.start();
    }

    public void sendPrivateMessage(String to, String message) {
        new Thread(() -> {
            try {
                if (writer != null) {
                    writer.write("/para:" + to + " " + message + "\n");
                    writer.flush();
                }
            } catch (IOException ignored) {}
        }).start();
    }

    public void disconnect() {
        running = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
        socket = null;
    }
}