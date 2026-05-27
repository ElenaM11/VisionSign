package com.example.visionsign;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
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

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    // =====================================================
    // UI
    // =====================================================
    private TextView tvEstadoMQTT;
    private TextView tvEstadoCamara;

    private ImageView ivIconoConexion;
    private ImageView ivIconoCamara;

    private MaterialButton btnIniciarPractica;
    private MaterialButton btnCerrarSesion;

    // =====================================================
    // MQTT
    // =====================================================
    private MqttClient mqttClient;

    private final String brokerUrl = "tcp://172.26.143.153:1883";
    private final String topic = "guante/gesto";

    private volatile boolean isConnecting = false;
    private volatile boolean isConnected = false;

    // =====================================================
    // OTROS
    // =====================================================
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final String TAG = "GUANTE";

    private ClasificadorSenas clasificador;

    private ActivityResultLauncher<Intent> arLauncher;

    // =====================================================
    // ON CREATE
    // =====================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // =====================================================
        // INICIALIZAR CLASIFICADOR
        // =====================================================
        clasificador = new ClasificadorSenas(this);

        // =====================================================
        // UI
        // =====================================================
        tvEstadoMQTT = findViewById(R.id.tvEstadoMQTT);
        tvEstadoCamara = findViewById(R.id.tvEstadoCamara);

        ivIconoConexion = findViewById(R.id.ivIconoConexion);
        ivIconoCamara = findViewById(R.id.ivIconoCamara);

        btnIniciarPractica = findViewById(R.id.btnIniciarPractica);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);

        // =====================================================
        // ESTADO INICIAL
        // =====================================================
        tvEstadoMQTT.setText("Conectando MQTT...");
        tvEstadoMQTT.setTextColor(Color.YELLOW);

        tvEstadoCamara.setText("Camara no iniciada");
        tvEstadoCamara.setTextColor(Color.GRAY);

        ivIconoConexion.setColorFilter(Color.YELLOW);
        ivIconoCamara.setColorFilter(Color.GRAY);

        btnIniciarPractica.setEnabled(false);

        // =====================================================
        // BOTONES
        // =====================================================
        btnIniciarPractica.setOnClickListener(v -> verificarPermisoCamara());

        btnCerrarSesion.setOnClickListener(v -> finishAffinity());

        // =====================================================
        // AR LAUNCHER
        // =====================================================
        arLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "AR finalizado");
                }
        );

        // =====================================================
        // MQTT
        // =====================================================
        conectarMQTT();
    }

    // =====================================================
    // MQTT CONNECT
    // =====================================================
    private void conectarMQTT() {

        if (isConnecting) return;

        isConnecting = true;

        new Thread(() -> {

            try {

                String clientId =
                        "Android_" + System.currentTimeMillis();

                mqttClient = new MqttClient(
                        brokerUrl,
                        clientId,
                        new MemoryPersistence()
                );

                MqttConnectOptions options =
                        new MqttConnectOptions();

                options.setCleanSession(true);
                options.setAutomaticReconnect(true);
                options.setConnectionTimeout(10);
                options.setKeepAliveInterval(20);

                mqttClient.setCallback(new MqttCallback() {

                    @Override
                    public void connectionLost(Throwable cause) {

                        isConnected = false;

                        Log.e(TAG,
                                "MQTT desconectado",
                                cause);

                        runOnUiThread(() -> {

                            tvEstadoMQTT.setText("MQTT desconectado");
                            tvEstadoMQTT.setTextColor(Color.RED);

                            ivIconoConexion.setColorFilter(Color.RED);

                            btnIniciarPractica.setEnabled(false);
                        });
                    }

                    @Override
                    public void messageArrived(
                            String topic,
                            MqttMessage message
                    ) {

                        String payload =
                                new String(message.getPayload());

                        Log.d(TAG,
                                "RAW MQTT: " + payload);

                        procesarMensaje(payload);
                    }

                    @Override
                    public void deliveryComplete(
                            IMqttDeliveryToken token
                    ) {

                    }
                });

                // =====================================================
                // CONECTAR
                // =====================================================
                mqttClient.connect(options);

                // =====================================================
                // SUSCRIBIR
                // =====================================================
                mqttClient.subscribe(topic, 1);

                isConnected = true;
                isConnecting = false;

                Log.d(TAG, "MQTT conectado");

                runOnUiThread(() -> {

                    tvEstadoMQTT.setText("MQTT conectado");
                    tvEstadoMQTT.setTextColor(Color.GREEN);

                    ivIconoConexion.setColorFilter(Color.GREEN);

                    // ✅ HABILITAR BOTÓN
                    btnIniciarPractica.setEnabled(true);
                });

            } catch (Exception e) {

                isConnecting = false;
                isConnected = false;

                Log.e(TAG,
                        "Error MQTT",
                        e);

                runOnUiThread(() -> {

                    tvEstadoMQTT.setText("Error MQTT");
                    tvEstadoMQTT.setTextColor(Color.RED);

                    ivIconoConexion.setColorFilter(Color.RED);

                    btnIniciarPractica.setEnabled(false);
                });
            }

        }).start();
    }

    // =====================================================
    // PROCESAR MENSAJES MQTT
    // =====================================================
    private void procesarMensaje(String json) {

        try {

            JSONObject obj = new JSONObject(json);

            JSONArray arr =
                    obj.getJSONArray("dedos");

            float[] senal = new float[5];

            for (int i = 0; i < 5; i++) {

                senal[i] =
                        (float) arr.getDouble(i);
            }

            // =====================================================
            // CLASIFICAR
            // =====================================================
            ClasificadorSenas.Resultado resultado =
                    clasificador.clasificar(senal);

            Log.d(TAG,
                    "SEÑA: "
                            + resultado.seña
                            + " | "
                            + resultado.precision);

            runOnUiThread(() -> {

                tvEstadoMQTT.setText(
                        "Seña: " + resultado.seña
                );

                tvEstadoMQTT.setTextColor(Color.GREEN);
            });

        } catch (Exception e) {

            Log.e(TAG,
                    "Error procesando JSON",
                    e);
        }
    }

    // =====================================================
    // PERMISO CAMARA
    // =====================================================
    private void verificarPermisoCamara() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION
            );

        } else {

            abrirAR();
        }
    }

    // =====================================================
    // RESULTADO PERMISO
    // =====================================================
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == REQUEST_CAMERA_PERMISSION) {

            if (grantResults.length > 0
                    && grantResults[0]
                    == PackageManager.PERMISSION_GRANTED) {

                abrirAR();

            } else {

                Toast.makeText(
                        this,
                        "Permiso de cámara denegado",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    // =====================================================
    // ABRIR AR
    // =====================================================
    private void abrirAR() {

        tvEstadoCamara.setText("Camara activa");
        tvEstadoCamara.setTextColor(Color.GREEN);

        ivIconoCamara.setColorFilter(Color.GREEN);

        Intent intent =
                new Intent(this, ar.class);

        arLauncher.launch(intent);
    }

    // =====================================================
    // ON DESTROY
    // =====================================================
    @Override
    protected void onDestroy() {

        super.onDestroy();

        try {

            if (mqttClient != null
                    && mqttClient.isConnected()) {

                mqttClient.disconnect();
            }

        } catch (Exception e) {

            Log.e(TAG,
                    "Error cerrando MQTT",
                    e);
        }
    }
}