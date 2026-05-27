package com.example.visionsign;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.json.JSONArray;
import org.json.JSONObject;

//  Import correcto para la clase interna Resultado
import com.example.visionsign.ClasificadorSenas.Resultado;

public class ar extends AppCompatActivity implements MqttListener {

    private static final String TAG = "AR_ACTIVITY";

    private ArFragment arFragment;
    private TextView tvEstadoAR, tvMQTT;

    private AnchorNode currentAnchorNode;
    private Anchor currentAnchor;

    private ClasificadorSenas clasificador;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ArCoreApk.Availability availability =
                ArCoreApk.getInstance().checkAvailability(this);
        if (availability.isUnsupported()) {
            Toast.makeText(this,
                    "Este dispositivo no soporta ARCore",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_ar);

        arFragment = (ArFragment) getSupportFragmentManager()
                .findFragmentById(R.id.arFragment);
        tvEstadoAR = findViewById(R.id.tvEstadoAR);
        tvMQTT     = findViewById(R.id.tvMQTT);
        tvEstadoAR.setText("AR Activo");
        tvMQTT.setText("Esperando señal...");

        clasificador       = new ClasificadorSenas(this);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        ImageButton btnRegresar = findViewById(R.id.btnRegresar);
        if (btnRegresar == null) {
            Log.e(TAG, "Error: btnRegresar no encontrado en el layout");
        } else {
            btnRegresar.setOnClickListener(v -> verificarEncuesta());
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                enviarResultadoYCerrar();
            }
        });
    }

    // -------------------- Navegación --------------------

    private void enviarResultadoYCerrar() {
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
        db.collection("usuarios").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Boolean encuestaCompletada =
                            documentSnapshot.getBoolean("encuestaSatisfaccionCompletada");
                    if (encuestaCompletada == null || !encuestaCompletada) {
                        startActivity(new Intent(ar.this, encuestaPostConocimiento.class));
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

    // -------------------- Ciclo de vida MQTT --------------------

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

    // -------------------- Procesamiento de mensajes MQTT --------------------

    @Override
    public void onMensajeRecibido(String mensaje) {
        runOnUiThread(() -> {
            tvMQTT.setText("Datos: " + mensaje);
            try {
                JSONObject json = new JSONObject(mensaje);
                JSONArray arr = json.getJSONArray("dedos");

                float[] angulos = new float[6];
                for (int i = 0; i < 5; i++) {
                    angulos[i] = (float) arr.getDouble(i);
                }
                angulos[5] = json.optInt("palma", -1);

                // CORREGIDO: usar instancia clasificador, no llamada estática
                Resultado res = clasificador.clasificar(angulos);

                if (res.precision >= 60) {
                    String textoMostrar = res.seña + " (" + Math.round(res.precision) + "%)";
                    mostrarTextoAR(textoMostrar);
                    tvEstadoAR.setText("Traducción: " + textoMostrar);

                    Bundle bundle = new Bundle();
                    bundle.putString("seña", res.seña);
                    bundle.putDouble("precision", res.precision);
                    mFirebaseAnalytics.logEvent("acierto_practica", bundle);
                } else {
                    String textoMostrar = "Seña no reconocida (" + Math.round(res.precision) + "%)";
                    mostrarTextoAR(textoMostrar);
                    tvEstadoAR.setText(textoMostrar);

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

    // -------------------- ARCore: mostrar texto --------------------

    private void mostrarTextoAR(String texto) {
        ViewRenderable.builder()
                .setView(this, R.layout.texto_ar)
                .build()
                .thenAccept(renderable -> {

                    if (currentAnchorNode != null) {
                        currentAnchorNode.setParent(null);
                        currentAnchorNode = null;
                    }
                    if (currentAnchor != null) {
                        currentAnchor.detach();
                        currentAnchor = null;
                    }

                    if (arFragment == null
                            || arFragment.getArSceneView() == null
                            || arFragment.getArSceneView().getArFrame() == null) return;

                    currentAnchor = arFragment.getArSceneView().getSession().createAnchor(
                            arFragment.getArSceneView().getArFrame()
                                    .getCamera()
                                    .getPose()
                                    .compose(com.google.ar.core.Pose.makeTranslation(0f, 0f, -0.6f))
                    );

                    currentAnchorNode = new AnchorNode(currentAnchor);

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

    // -------------------- Limpieza --------------------

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (currentAnchorNode != null) {
            currentAnchorNode.setParent(null);
            currentAnchorNode = null;
        }
        if (currentAnchor != null) {
            currentAnchor.detach();
            currentAnchor = null;
        }
    }
}