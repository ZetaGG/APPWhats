package com.example.app7;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private EditText txtIP, txtUsuario, txtMensaje;
    private Button btnConectarServer;
    private ListView listViewContactos;
    private TextView tvMessageLog, tvNombreDispositivo;
    private ImageButton btnEnviarMensaje;

    private Socket socket;
    private BufferedWriter writer;
    private boolean isRunning = false;
    private Handler handler = new Handler(Looper.getMainLooper());

    private ArrayList<Contacto> listaContactos = new ArrayList<>();
    private ArrayAdapter<Contacto> adaptadorContactos;
    private Contacto contactoActual;

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
        txtUsuario = findViewById(R.id.txtUsuario);
        btnConectarServer = findViewById(R.id.btnConectarServer);

        btnConectarServer.setOnClickListener(v -> {
            String ip = txtIP.getText().toString().trim();
            String nombreUsuario = txtUsuario.getText().toString().trim();
            if (ip.isEmpty() || nombreUsuario.isEmpty()) {
                Toast.makeText(this, "IP y usuario requeridos", Toast.LENGTH_SHORT).show();
                return;
            }
            conectarAlServidor(ip, 5000, nombreUsuario); // Cambia 5059 si tu puerto es otro
        });
    }

    private void conectarAlServidor(String ip, int puerto, String nombreUsuario) {
        new Thread(() -> {
            try {
                socket = new Socket(ip, puerto);
                writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

                // Enviar solo el nombre de usuario como primer mensaje
                writer.write(nombreUsuario + "\n");
                writer.flush();

                isRunning = true;

                handler.post(this::cambiarALayoutContactos);

                while (isRunning) {
                    String line = reader.readLine();
                    if (line == null) break;
                    if (line.startsWith("#usuarios:")) {
                        handler.post(() -> actualizarListaContactos(line));
                    } else if (line.startsWith("[Privado de ")) {
                        handler.post(() -> mostrarMensajeChat(line));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                handler.post(() -> Toast.makeText(MainActivity.this, "No se pudo conectar al servidor: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void cambiarALayoutContactos() {
        setContentView(R.layout.contactos);

        listViewContactos = findViewById(R.id.lvContactos);
        if (adaptadorContactos == null) {
            adaptadorContactos = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listaContactos);
            listViewContactos.setAdapter(adaptadorContactos);
        } else {
            adaptadorContactos.notifyDataSetChanged();
        }

        listViewContactos.setOnItemClickListener((parent, view, position, id) -> {
            contactoActual = listaContactos.get(position);
            cambiarALayoutChat();
            tvNombreDispositivo.setText("Chat con " + contactoActual.getNombre());
        });
    }

    private void actualizarListaContactos(String usuariosRaw) {
        listaContactos.clear();
        String users = usuariosRaw.substring("#usuarios:".length());
        if (!users.isEmpty()) {
            for (String u : users.split(",")) {
                String[] partes = u.split("\\|");
                if (partes.length == 2) {
                    listaContactos.add(new Contacto(partes[0], partes[1]));
                }
            }
        }
        if (adaptadorContactos != null)
            adaptadorContactos.notifyDataSetChanged();
    }

    private void cambiarALayoutChat() {
        setContentView(R.layout.chat);

        txtMensaje = findViewById(R.id.txtMensaje);
        btnEnviarMensaje = findViewById(R.id.btnEnviar);
        tvMessageLog = findViewById(R.id.tvMensajes);
        tvNombreDispositivo = findViewById(R.id.tvNombreDispositivo);

        btnEnviarMensaje.setOnClickListener(v -> {
            String mensaje = txtMensaje.getText().toString().trim();
            if (mensaje.isEmpty() || contactoActual == null) return;
            enviarMensajePrivado(contactoActual.getNombre(), mensaje);
            txtMensaje.setText("");
        });
    }

    private void enviarMensajePrivado(String destinatario, String mensaje) {
        new Thread(() -> {
            try {
                writer.write("/para:" + destinatario + " " + mensaje + "\n");
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }



    private void mostrarMensajeChat(String mensaje) {
        if (tvMessageLog != null)
            tvMessageLog.append(mensaje + "\n");
    }

    public class Contacto {
        private String nombre;
        private String ip;

        public Contacto(String nombre, String ip) {
            this.nombre = nombre;
            this.ip = ip;
        }

        public String getNombre() {
            return nombre;
        }

        public String getIp() {
            return ip;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRunning = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException e) { /* Ignorar */ }
    }
}