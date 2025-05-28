package com.example.app7;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private EditText txtIP, txtUsuario, txtMensaje;
    private Button btnConectarServer;
    private ListView listViewContactos;
    private TextView tvMessageLog, tvNombreDispositivo;
    private ImageButton btnEnviarMensaje;

    private ChatClient chatClient;
    private final ArrayList<Contacto> listaContactos = new ArrayList<>();
    private ArrayAdapter<Contacto> adaptadorContactos;
    private Contacto contactoActual;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        handler = new Handler(Looper.getMainLooper());

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

    private void conectarAlServidor(String ip, int puerto, String nombreUsuario) {
        chatClient = new ChatClient(ip, puerto, nombreUsuario, handler, new ChatClient.Listener() {
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
            }
        });
        chatClient.connect();
        cambiarALayoutContactos();
    }

    private void cambiarALayoutContactos() {
        setContentView(R.layout.contactos);
        listViewContactos = findViewById(R.id.lvContactos);

        adaptadorContactos = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listaContactos);
        listViewContactos.setAdapter(adaptadorContactos);

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
            chatClient.sendPrivateMessage(contactoActual.getNombre(), mensaje);
            txtMensaje.setText("");
        });
    }

    private void mostrarMensajeChat(String mensaje) {
        if (tvMessageLog != null)
            tvMessageLog.append(mensaje + "\n");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatClient != null) {
            chatClient.disconnect();
        }
    }
}