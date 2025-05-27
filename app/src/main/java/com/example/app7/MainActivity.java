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
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private EditText txtIP;
    private Button btnConectarServer;

    private ListView listViewContactos;

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

    // Variables para contactos
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
        btnConectarServer = findViewById(R.id.btnConectarServer);

        btnConectarServer.setOnClickListener(v -> {
            // Intentar conexión
            boolean conexionExitosa = conectarServer();

            // Si la conexión es exitosa, cambiar al layout de contactos
            if (conexionExitosa) {
                cambiarALayoutContactos();
            }
        });
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

    private void cambiarALayoutContactos() {
        setContentView(R.layout.contactos);

        // Inicializar elementos de contactos.xml
        listViewContactos = findViewById(R.id.lvContactos);

        // Configurar adaptador
        adaptadorContactos = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listaContactos);
        listViewContactos.setAdapter(adaptadorContactos);

        // Manejar clics en contactos
        listViewContactos.setOnItemClickListener((parent, view, position, id) -> {
            contactoActual = listaContactos.get(position);
            cambiarALayoutChat();
            tvNombreDispositivo.setText("Chat con " + contactoActual.getNombre());
        });

        // Iniciar escucha de contactos
        new Thread(this::escucharContactos).start();
    }

    private void escucharContactos() {
        try {
            while(isRunning) {
                if(socket != null && socket.getInputStream() != null) {
                    BufferedReader r = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String mensaje;
                    while ((mensaje = r.readLine()) != null) {
                        // Formato esperado: CONTACTO:nombre:ip
                        if (mensaje.startsWith("CONTACTO:")) {
                            String datos = mensaje.substring(9);
                            String[] partes = datos.split(":");
                            if (partes.length == 2) {
                                final Contacto nuevoContacto = new Contacto(partes[0], partes[1]);

                                handler.post(() -> {
                                    // Verificar si ya existe
                                    boolean existe = false;
                                    for (Contacto c : listaContactos) {
                                        if (c.getIp().equals(nuevoContacto.getIp())) {
                                            existe = true;
                                            break;
                                        }
                                    }

                                    if (!existe) {
                                        listaContactos.add(nuevoContacto);
                                        adaptadorContactos.notifyDataSetChanged();
                                    }
                                });
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this,
                    "Error al recibir contactos", Toast.LENGTH_SHORT).show());
        }
    }

    private void cambiarALayoutChat() {
        // Cambiar al layout de chat
        setContentView(R.layout.chat);

        // Inicializar elementos de chat.xml
        txtMensaje = findViewById(R.id.txtMensaje);
        tvMessageLog = findViewById(R.id.tvMensajes);
        btnEnviarMensaje = findViewById(R.id.btnEnviar);
        tvNombreDispositivo = findViewById(R.id.tvNombreDispositivo);

        // Configurar nombre del dispositivo si no estamos en chat privado
        if (contactoActual == null) {
            String nombreDispositivo = Build.MODEL;
            tvNombreDispositivo.setText(nombreDispositivo);
        }

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

    private void listenMessage() {
        try {
            while(isRunning) {
                if(socket != null && socket.getInputStream() != null) {
                    BufferedReader r = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String mensaje;
                    while ((mensaje = r.readLine()) != null) {
                        // Mensajes privados: PRIVADO:ipOrigen:contenido
                        if (mensaje.startsWith("PRIVADO:")) {
                            String[] partes = mensaje.substring(8).split(":", 2);
                            if (partes.length == 2) {
                                String ipOrigen = partes[0];
                                String contenido = partes[1];

                                // Buscar nombre del remitente
                                String nombreOrigen = ipOrigen;
                                for (Contacto c : listaContactos) {
                                    if (c.getIp().equals(ipOrigen)) {
                                        nombreOrigen = c.getNombre();
                                        break;
                                    }
                                }

                                final String nombreFinal = nombreOrigen;
                                final String mensajeFinal = contenido;

                                handler.post(() -> tvMessageLog.append(nombreFinal + ": " + mensajeFinal + "\n"));
                            }
                        } else {
                            // Mensajes normales (no privados)
                            final String mensajeNormal = mensaje;
                            handler.post(() -> tvMessageLog.append("<" + mensajeNormal + "\n"));
                        }
                    }
                }
            }
        } catch (IOException ex) {
            runOnUiThread(() -> Toast.makeText(this, "Error al leer los mensajes entrantes", Toast.LENGTH_SHORT).show());
        }
    }

    private void sendMessage(String message) {
        if(writer == null || message.isEmpty()) return;

        new Thread(() -> {
            try {
                // Si estamos en un chat privado, incluir destinatario en el mensaje
                String mensajeCompleto;
                if (contactoActual != null) {
                    mensajeCompleto = "PRIVADO:" + contactoActual.getIp() + ":" + message;
                } else {
                    mensajeCompleto = message;
                }

                writer.write(mensajeCompleto + "\n");
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