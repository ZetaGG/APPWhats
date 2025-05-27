package com.example.app7;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    private EditText txtIP;
    private Button btnConectarServer;

    // Variables para elementos de chat.xml
    private EditText txtMensaje;
    private TextView tvMessageLog;
    private ImageButton btnEnviarMensaje;
    private TextView tvNombreDispositivo;

    // Variables para la conexión
    private Socket socket;
    private BufferedWriter writer;
    private boolean isRunning = false;
    private ServerSocket serverSocket;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txtIP = findViewById(R.id.txtIP);
        btnConectarServer = findViewById(R.id.btnConectarServer);

        btnConectarServer.setOnClickListener(v -> {
            // Intentar conexión
            boolean conexionExitosa = conectarServer();

            // Si la conexión es exitosa, cambiar al layout de chat
            if (conexionExitosa) {
                cambiarALayoutChat();
            }
        });
    }

    private void cambiarALayoutChat() {
        // Cambiar al layout de chat
        setContentView(R.layout.chat);

        // Inicializar elementos de chat.xml
        txtMensaje = findViewById(R.id.txtMensaje);
        tvMessageLog = findViewById(R.id.tvMensajes);
        btnEnviarMensaje = findViewById(R.id.btnEnviar);
        tvNombreDispositivo = findViewById(R.id.tvNombreDispositivo);

        // Configurar nombre del dispositivo
        String nombreDispositivo = Build.MODEL;
        tvNombreDispositivo.setText(nombreDispositivo);

        // Configurar listener para el botón de enviar
        btnEnviarMensaje.setOnClickListener(v ->
                sendMessage(txtMensaje.getText().toString()));

        // Iniciar thread para escuchar mensajes
        new Thread(this::listenMessage).start();
    }

    private boolean conectarServer() {
        String ip = txtIP.getText().toString();
        if(ip.isEmpty()) {
            Toast.makeText(this, "Ingresa la IP", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            socket = new Socket(ip, 5000);
            writer = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream()));
            isRunning = true;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al conectar", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void listenMessage(){
        try {
            while(isRunning){
                if(socket != null && socket.getInputStream() != null){
                    BufferedReader r = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String m;
                    while ((m = r.readLine()) != null) {
                        final String mensaje = m;
                        handler.post(() ->
                                tvMessageLog.append("<" + mensaje + "\n"));
                    }
                }
            }
        } catch (IOException ex) {
            runOnUiThread(() -> Toast.makeText(this, "Error al leer los mensajes entrantes", Toast.LENGTH_SHORT).show());
        }
    }

    private void sendMessage(String message){
        if(writer == null || message.isEmpty()) return;

        new Thread(() -> {
            try {
                writer.write(message + "\n");
                writer.flush();
                runOnUiThread(() -> {
                    tvMessageLog.append("Yo: " + message + "\n");
                    txtMensaje.setText("");
                });
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "Error al enviar mensaje", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRunning = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            if (socket != null) {
                socket.close();
            }
            if (writer != null) {
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}