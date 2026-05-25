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
import com.google.firebase.analytics.FirebaseAnalytics;

import org.json.JSONArray;
import org.json.JSONObject;
import androidx.activity.OnBackPressedCallback;


public class ar extends AppCompatActivity implements MQTTManager.MqttListener {

    private static final String TAG = "AR_ACTIVITY";
    private ArFragment arFragment;
    private TextView tvEstadoAR, tvMQTT;
    private AnchorNode currentAnchorNode;
    private ClasificadorSenas clasificador;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);
        tvEstadoAR = findViewById(R.id.tvEstadoAR);
        tvMQTT = findViewById(R.id.tvMQTT);
        tvEstadoAR.setText("AR Activo");
        tvMQTT.setText("Esperando señal...");

        // Inicializar clasificador ML
        clasificador = new ClasificadorSenas(this);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        ImageButton btnRegresar = findViewById(R.id.btnRegresar);
        if (btnRegresar == null) {
            Log.e(TAG, "Error: btnRegresar no encontrado en el layout");
            Toast.makeText(this, "Botón regresar no encontrado", Toast.LENGTH_SHORT).show();
        } else {
            btnRegresar.setOnClickListener(v -> verificarEncuesta());
        }
        //onback
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Log.d(TAG, "Botón atrás presionado, enviando resultado");
                enviarResultadoYCerrar();
            }
        });
    }

    private void enviarResultadoYCerrar() {
        Log.d(TAG, "Enviando resultado practicaCompletada=true");
        Intent resultIntent = new Intent();
        resultIntent.putExtra("practicaCompletada", true);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void verificarEncuesta() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() == null) {
            enviarResultadoYCerrar();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        db.collection("usuarios")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Boolean encuestaCompletada = documentSnapshot.getBoolean("encuestaSatisfaccionCompletada");
                    if (encuestaCompletada == null || !encuestaCompletada) {
                        Intent intent = new Intent(ar.this, encuestaPostConocimiento.class);
                        startActivity(intent);
                        finish();
                    } else {
                        enviarResultadoYCerrar();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error verificando encuesta", Toast.LENGTH_SHORT).show();
                    enviarResultadoYCerrar();
                });
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "Botón atrás presionado, enviando resultado");
        enviarResultadoYCerrar();
        super.onBackPressed();   // ← SOLO agrega esta línea
    }

    @Override
    protected void onResume() {
        super.onResume();
        MQTTManager.setListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MQTTManager.setListener(null);
    }

    @Override
    public void onMensajeRecibido(String mensaje) {
        runOnUiThread(() -> {
            tvMQTT.setText("Datos: " + mensaje);
            try {
                // Parsear JSON recibido del ESP32: {"dedos":[v1,v2,v3,v4,v5]}
                JSONObject json = new JSONObject(mensaje);
                JSONArray arr = json.getJSONArray("dedos");
                float[] angulos = new float[5];
                for (int i = 0; i < 5; i++) {
                    angulos[i] = (float) arr.getDouble(i);
                }

                // Clasificar con el modelo ML
                ClasificadorSenas.Resultado res = clasificador.clasificar(angulos);

                // Umbral de confianza (60% es un valor ajustable)
                if (res.precision >= 60) {
                    String textoMostrar = res.seña + " (" + Math.round(res.precision) + "%)";
                    mostrarTextoAR(textoMostrar);
                    tvEstadoAR.setText("Traducción: " + textoMostrar);

                    // Opcional: registrar evento de acierto en Analytics
                    Bundle bundle = new Bundle();
                    bundle.putString("seña", res.seña);
                    bundle.putDouble("precision", res.precision);
                    mFirebaseAnalytics.logEvent("acierto_practica", bundle);
                } else {
                    String textoMostrar = "Seña no reconocida (" + Math.round(res.precision) + "%)";
                    mostrarTextoAR(textoMostrar);
                    tvEstadoAR.setText(textoMostrar);

                    // Opcional: registrar evento de fallo
                    Bundle bundle = new Bundle();
                    bundle.putDouble("precision", res.precision);
                    mFirebaseAnalytics.logEvent("fallo_practica", bundle);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error al procesar JSON", e);
                tvEstadoAR.setText("Error en datos recibidos");
                mostrarTextoAR("Error: formato inválido");
            }
        });
    }

    private void mostrarTextoAR(String texto) {
        ViewRenderable.builder()
                .setView(this, R.layout.texto_ar)
                .build()
                .thenAccept(renderable -> {
                    if (currentAnchorNode != null) {
                        currentAnchorNode.setParent(null);
                        currentAnchorNode = null;
                    }
                    if (arFragment == null || arFragment.getArSceneView().getArFrame() == null) return;

                    Anchor anchor = arFragment.getArSceneView().getSession().createAnchor(
                            arFragment.getArSceneView().getArFrame()
                                    .getCamera()
                                    .getPose()
                                    .compose(com.google.ar.core.Pose.makeTranslation(0f, 0f, -0.6f))
                    );
                    currentAnchorNode = new AnchorNode(anchor);
                    Node node = new Node();
                    node.setRenderable(renderable);
                    TextView tv = (TextView) renderable.getView();
                    tv.setText(texto);
                    node.setParent(currentAnchorNode);
                    currentAnchorNode.setParent(arFragment.getArSceneView().getScene());
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Error al crear renderable", throwable);
                    return null;
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (currentAnchorNode != null) {
            currentAnchorNode.setParent(null);
            currentAnchorNode = null;
        }
    }
}