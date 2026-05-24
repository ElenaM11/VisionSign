package com.example.visionsign;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MainActivity extends AppCompatActivity {

    private TextView tvEstadoMQTT, tvEstadoCamara;
    private ImageView ivIconoConexion, ivIconoCamara;
    private MaterialButton btnIniciarPractica, btnCerrarSesion;

    // MQTT con Eclipse Paho
    private MqttAndroidClient mqttClient;
    private final String brokerUrl = "tcp://192.168.16.110:1883";  // IP de tu laptop con Coreflux
    private final String topic = "guante/gesto";                 // Tópico donde publica el ESP32
    private final String username = null;
    private final String password = null;

    private boolean isConnecting = false;
    private boolean isConnected = false;
    private ConnectivityManager.NetworkCallback networkCallback;
    private SharedPreferences sharedPreferences;
    private ActivityResultLauncher<Intent> arLauncher;

    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final String TAG = "GUANTE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("GuanteJPrefs", MODE_PRIVATE);

        tvEstadoMQTT = findViewById(R.id.tvEstadoMQTT);
        tvEstadoCamara = findViewById(R.id.tvEstadoCamara);
        ivIconoConexion = findViewById(R.id.ivIconoConexion);
        ivIconoCamara = findViewById(R.id.ivIconoCamara);
        btnIniciarPractica = findViewById(R.id.btnIniciarPractica);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);

        tvEstadoMQTT.setText("Conectando...");
        tvEstadoCamara.setText("Camara no iniciada");
        ivIconoConexion.setColorFilter(Color.GRAY);
        ivIconoCamara.setColorFilter(Color.GRAY);
        btnIniciarPractica.setEnabled(false);

        // Launcher para AR
        arLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                boolean practicaCompletada = result.getData().getBooleanExtra("practicaCompletada", false);
                if (practicaCompletada) {
                    boolean encuestaCompletada = sharedPreferences.getBoolean("encuestaSatisfaccionCompletada", false);
                    if (!encuestaCompletada) {
                        sharedPreferences.edit().putBoolean("encuestaSatisfaccionCompletada", true).apply();
                        startActivity(new Intent(this, encuestaPostConocimiento.class));
                    } else {
                        Toast.makeText(this, "Encuesta ya completada", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        btnIniciarPractica.setOnClickListener(v -> verificarPermisoCamara());
        btnCerrarSesion.setOnClickListener(v -> finishAffinity());

        registrarCallbackRed();
        conectarMQTT();
    }

    private void verificarPermisoCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        else abrirAR();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            abrirAR();
        else Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
    }

    private void abrirAR() {
        tvEstadoCamara.setText("Camara activa");
        tvEstadoCamara.setTextColor(Color.GREEN);
        ivIconoCamara.setColorFilter(Color.GREEN);
        arLauncher.launch(new Intent(this, ar.class));
    }

    // -------------------- MQTT con Eclipse Paho --------------------
    private void registrarCallbackRed() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest request = new NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build();
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) { reconectarMQTT(); }
            @Override
            public void onLost(@NonNull Network network) { runOnUiThread(() -> actualizarEstadosMQTT(false)); isConnected = false; }
        };
        cm.registerNetworkCallback(request, networkCallback);
    }

    private void conectarMQTT() {
        if (isConnecting) return;
        isConnecting = true;
        tvEstadoMQTT.setText("Conectando...");
        ivIconoConexion.setColorFilter(Color.YELLOW);

        String clientId = "Android_" + System.currentTimeMillis();
        mqttClient = new MqttAndroidClient(getApplicationContext(), brokerUrl, clientId);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(60);
        if (username != null && password != null) {
            options.setUserName(username);
            options.setPassword(password.toCharArray());
        }

        try {
            mqttClient.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    isConnecting = false;
                    isConnected = true;
                    runOnUiThread(() -> actualizarEstadosMQTT(true));
                    suscribirse();
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    isConnecting = false;
                    isConnected = false;
                    runOnUiThread(() -> actualizarEstadosMQTT(false));
                    Log.e(TAG, "Conexión MQTT fallida", exception);
                }
            });
        } catch (MqttException e) {
            isConnecting = false;
            Log.e(TAG, "Error de conexión MQTT", e);
        }
    }

    private void suscribirse() {
        try {
            mqttClient.subscribe(topic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Suscrito a " + topic);
                    mqttClient.setCallback(new MqttCallback() {
                        @Override
                        public void connectionLost(Throwable cause) {
                            runOnUiThread(() -> actualizarEstadosMQTT(false));
                            isConnected = false;
                            reconectarMQTT();
                        }
                        @Override
                        public void messageArrived(String topic, MqttMessage message) {
                            String payload = new String(message.getPayload());
                            Log.d(TAG, "Mensaje recibido: " + payload);
                            // Enviar el JSON a MQTTManager (que lo pasará a AR)
                            MQTTManager.enviarMensaje(payload);
                        }
                        @Override
                        public void deliveryComplete(IMqttDeliveryToken token) {}
                    });
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Suscripción fallida", exception);
                }
            });
        } catch (MqttException e) {
            Log.e(TAG, "Error al suscribirse", e);
        }
    }

    private void reconectarMQTT() {
        if (isConnected || isConnecting) return;
        if (mqttClient != null) {
            try { mqttClient.disconnect(); } catch (Exception ignored) {}
            try { mqttClient.close(); } catch (Exception ignored) {}
        }
        conectarMQTT();
    }

    private void actualizarEstadosMQTT(boolean conectado) {
        if (conectado) {
            tvEstadoMQTT.setText("Conectado");
            tvEstadoMQTT.setTextColor(Color.GREEN);
            ivIconoConexion.setColorFilter(Color.GREEN);
            btnIniciarPractica.setEnabled(true);
        } else {
            tvEstadoMQTT.setText("No conectado");
            tvEstadoMQTT.setTextColor(Color.RED);
            ivIconoConexion.setColorFilter(Color.GRAY);
            btnIniciarPractica.setEnabled(false);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (!isConnected && !isConnecting) conectarMQTT();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (networkCallback != null) {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            cm.unregisterNetworkCallback(networkCallback);
        }
        if (mqttClient != null) {
            try { mqttClient.disconnect(); mqttClient.close(); } catch (Exception ignored) {}
        }
    }
}