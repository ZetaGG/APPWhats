package com.example.app7;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    // Login views
    private EditText txtIP, txtUsuario;
    private Button btnConectarServer;
    private String nombreUsuarioActual;

    // Contactos views
    private RecyclerView rvContactos;
    private ContactoAdapter contactoAdapter;

    // Chat views
    private TextView tvMensajes, tvNombreDispositivo;
    private EditText txtMensaje;
    private ImageButton btnEnviar;

    // Datos
    private final ArrayList<Contacto> listaContactos = new ArrayList<>();
    private Contacto contactoActual;
    private ChatClient chatClient;
    private Handler handler;

    // Historial de chats [clave: nombre del contacto, valor: lista de mensajes]
    private final Map<String, List<String>> historialChats = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showLoginLayout();
        handler = new Handler(Looper.getMainLooper());
    }

    private void showLoginLayout() {
        setContentView(R.layout.activity_main); // Tu layout de login
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
            conectarAlServidor(ip, 5000, nombreUsuario);
        });
    }

    private void showContactosLayout() {
        setContentView(R.layout.contactos); // Tu layout de contactos con RecyclerView
        rvContactos = findViewById(R.id.rvContactos);

        if (contactoAdapter == null) {
            contactoAdapter = new ContactoAdapter(listaContactos, contacto -> {
                contactoActual = contacto;
                showChatLayout();
                tvNombreDispositivo.setText("Chat con " + contacto.getNombre());
            });
        }
        rvContactos.setLayoutManager(new LinearLayoutManager(this));
        rvContactos.setAdapter(contactoAdapter);
    }

    private void showChatLayout() {
        setContentView(R.layout.chat);

        ImageButton btnBack = findViewById(R.id.btnBackToContactos);
        btnBack.setOnClickListener(v -> showContactosLayout());

        txtMensaje = findViewById(R.id.txtMensaje);
        btnEnviar = findViewById(R.id.btnEnviar);
        tvMensajes = findViewById(R.id.tvMensajes);
        tvNombreDispositivo = findViewById(R.id.tvNombreDispositivo);

        tvNombreDispositivo.setText("Chat con " + (contactoActual != null ? contactoActual.getNombre() : ""));

        // Mostrar historial si existe
        if (contactoActual != null) {
            List<String> historial = historialChats.get(contactoActual.getNombre());
            tvMensajes.setText("");
            if (historial != null) {
                for (String m : historial) {
                    tvMensajes.append(m + "\n");
                }
            }
        }

        btnEnviar.setOnClickListener(v -> {
            String mensaje = txtMensaje.getText().toString().trim();
            if (mensaje.isEmpty() || contactoActual == null) return;
            chatClient.sendPrivateMessage(contactoActual.getNombre(), mensaje);
            guardarMensajeEnHistorial(contactoActual.getNombre(), "Yo: " + mensaje);
            tvMensajes.append("Yo: " + mensaje + "\n");
            txtMensaje.setText("");
        });
    }

    private void conectarAlServidor(String ip, int puerto, String nombreUsuario) {
        nombreUsuarioActual = nombreUsuario; // Guardas el nombre del usuario actual
        chatClient = new ChatClient(ip, puerto, nombreUsuario, new Handler(Looper.getMainLooper()), new ChatClient.Listener() {
            @Override
            public void onUserListUpdate(String rawUsers) {
                actualizarListaContactos(rawUsers);
            }
            @Override
            public void onPrivateMessage(String message) {
                mostrarMensajeChat(message);
            }
            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
            }
            @Override
            public void onDisconnected() {
                Toast.makeText(MainActivity.this, "Desconectado del servidor", Toast.LENGTH_SHORT).show();
                showLoginLayout();
            }
        });
        chatClient.connect();
        showContactosLayout();
    }

    private void actualizarListaContactos(String usuariosRaw) {
        runOnUiThread(() -> {
            listaContactos.clear();
            String users = usuariosRaw.substring("#usuarios:".length());
            if (!users.isEmpty()) {
                for (String u : users.split(",")) {
                    String[] partes = u.split("\\|");
                    if (partes.length == 2) {
                        // Solo agrega si NO es el usuario actual
                        if (!partes[0].equalsIgnoreCase(nombreUsuarioActual)) {
                            listaContactos.add(new Contacto(partes[0], partes[1]));
                        }
                    }
                }
            }
            if (contactoAdapter != null)
                contactoAdapter.notifyDataSetChanged();
        });
    }

    private void mostrarMensajeChat(String mensaje) {
        runOnUiThread(() -> {
            if (tvMensajes != null && contactoActual != null) {
                guardarMensajeEnHistorial(contactoActual.getNombre(), mensaje);
                tvMensajes.append(mensaje + "\n");
            }
        });
    }

    private void guardarMensajeEnHistorial(String nombreContacto, String mensaje) {
        List<String> historial = historialChats.get(nombreContacto);
        if (historial == null) {
            historial = new ArrayList<>();
            historialChats.put(nombreContacto, historial);
        }
        historial.add(mensaje);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatClient != null) {
            chatClient.disconnect();
        }
    }
}