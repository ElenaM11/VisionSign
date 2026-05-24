package com.example.visionsign;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import org.json.JSONArray;
import org.json.JSONObject;

public class ar extends AppCompatActivity implements MQTTManager.MqttListener {

    private static final String TAG = "AR_ACTIVITY";
    private ArFragment arFragment;
    private TextView tvEstadoAR, tvMQTT;
    private AnchorNode currentAnchorNode;
    private ClasificadorSenas clasificador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);
        tvEstadoAR = findViewById(R.id.tvEstadoAR);
        tvMQTT = findViewById(R.id.tvMQTT);
        tvEstadoAR.setText("AR Activo");
        tvMQTT.setText("Esperando señal...");

        // Inicializar el clasificador ML
        clasificador = new ClasificadorSenas(this);

        ImageButton btnRegresar = findViewById(R.id.btnRegresar);
        btnRegresar.setOnClickListener(v -> verificarEncuesta());
    }

    // Los métodos enviarResultadoYCerrar, verificarEncuesta, onBackPressed, onResume, onPause
    // se mantienen exactamente igual que antes (no los modifico por espacio, pero debes copiarlos).

    @Override
    public void onMensajeRecibido(String mensaje) {
        runOnUiThread(() -> {
            tvMQTT.setText("Datos: " + mensaje);
            try {
                JSONObject json = new JSONObject(mensaje);
                JSONArray arr = json.getJSONArray("dedos");  // espera {"dedos":[v1,v2,v3,v4,v5]}
                float[] angulos = new float[5];
                for (int i = 0; i < 5; i++) {
                    angulos[i] = (float) arr.getDouble(i);
                }
                ClasificadorSenas.Resultado res = clasificador.clasificar(angulos);
                if (res.precision >= 60) {  // umbral mínimo para considerar que es una seña válida
                    String textoMostrar = res.seña + " (" + Math.round(res.precision) + "%)";
                    mostrarTextoAR(textoMostrar);
                    tvEstadoAR.setText("Traducción: " + textoMostrar);
                } else {
                    tvEstadoAR.setText("Seña no reconocida (" + Math.round(res.precision) + "%)");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error procesando JSON", e);
                tvEstadoAR.setText("Error en datos recibidos");
            }
        });
    }

    private void mostrarTextoAR(String texto) {
        // El código de mostrarTextoAR se mantiene igual al que ya tenías (con ViewRenderable)
        // ... (copia el que tienes actualmente)
    }

    // ... resto de métodos onDestroy, etc.
}