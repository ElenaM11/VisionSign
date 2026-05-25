package com.example.visionsign;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class encuestaPostConocimiento extends AppCompatActivity {

    private MaterialButton btnEnviar;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseAnalytics mFirebaseAnalytics;
    private Map<String, Integer> respuestas = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encuesta_post_conocimiento);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Error: No hay usuario logueado", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        btnEnviar = findViewById(R.id.btnEnviarEncuesta);
        progressBar = findViewById(R.id.progressBar);

        // Configurar 4 preguntas post‑conocimiento
        configurarEstrellasPostConocimiento1();
        configurarEstrellasPostConocimiento2();
        configurarEstrellasPostConocimiento3();
        configurarEstrellasPostConocimiento4();

        // Configurar 8 preguntas de satisfacción
        configurarEstrellasSatisfaccion1();
        configurarEstrellasSatisfaccion2();
        configurarEstrellasSatisfaccion3();
        configurarEstrellasSatisfaccion4();
        configurarEstrellasSatisfaccion5();
        configurarEstrellasSatisfaccion6();
        configurarEstrellasSatisfaccion7();
        configurarEstrellasSatisfaccion8();

        btnEnviar.setOnClickListener(v -> enviarEncuesta());
    }

    // configurar estrellas
    private void configurarEstrellas(LinearLayout layout, String key) {
        if (layout == null) return;
        for (int i = 0; i < layout.getChildCount(); i++) {
            TextView star = (TextView) layout.getChildAt(i);
            final int index = i + 1;
            star.setOnClickListener(v -> {
                marcarEstrellas(layout, index);
                respuestas.put(key, index);
            });
        }
    }

    // ---------- Post‑conocimiento ----------
    private void configurarEstrellasPostConocimiento1() {
        configurarEstrellas(findViewById(R.id.estrellas_pc1), "pc1");
    }
    private void configurarEstrellasPostConocimiento2() {
        configurarEstrellas(findViewById(R.id.estrellas_pc2), "pc2");
    }
    private void configurarEstrellasPostConocimiento3() {
        configurarEstrellas(findViewById(R.id.estrellas_pc3), "pc3");
    }
    private void configurarEstrellasPostConocimiento4() {
        configurarEstrellas(findViewById(R.id.estrellas_pc4), "pc4");
    }

    // ---------- Satisfacción (8 preguntas) ----------
    private void configurarEstrellasSatisfaccion1() {
        configurarEstrellas(findViewById(R.id.estrellas_s1), "s1");
    }
    private void configurarEstrellasSatisfaccion2() {
        configurarEstrellas(findViewById(R.id.estrellas_s2), "s2");
    }
    private void configurarEstrellasSatisfaccion3() {
        configurarEstrellas(findViewById(R.id.estrellas_s3), "s3");
    }
    private void configurarEstrellasSatisfaccion4() {
        configurarEstrellas(findViewById(R.id.estrellas_s4), "s4");
    }
    private void configurarEstrellasSatisfaccion5() {
        configurarEstrellas(findViewById(R.id.estrellas_s5), "s5");
    }
    private void configurarEstrellasSatisfaccion6() {
        configurarEstrellas(findViewById(R.id.estrellas_s6), "s6");
    }
    private void configurarEstrellasSatisfaccion7() {
        configurarEstrellas(findViewById(R.id.estrellas_s7), "s7");
    }
    private void configurarEstrellasSatisfaccion8() {
        configurarEstrellas(findViewById(R.id.estrellas_s8), "s8");
    }

    private void marcarEstrellas(LinearLayout layout, int selected) {
        for (int j = 0; j < layout.getChildCount(); j++) {
            TextView s = (TextView) layout.getChildAt(j);
            if (j < selected) {
                s.setText("★");
                s.setTextColor(0xFFFFC107);
            } else {
                s.setText("☆");
                s.setTextColor(0xFF666666);
            }
        }
    }

    private void enviarEncuesta() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Error: Usuario no válido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (respuestas.size() < 12) {
            Toast.makeText(this, "Por favor, responde todas las preguntas (4 de autopercepción y 8 de satisfacción)", Toast.LENGTH_LONG).show();
            return;
        }

        // Calcular promedio post‑conocimiento (pc1 a pc4)
        final double postPromedio = (respuestas.get("pc1") + respuestas.get("pc2") +
                respuestas.get("pc3") + respuestas.get("pc4")) / 4.0;

        String userId = mAuth.getCurrentUser().getUid();

        // Leer prePromedio desde Firestore (documento del usuario)
        progressBar.setVisibility(View.VISIBLE);
        btnEnviar.setEnabled(false);
        btnEnviar.setText("Procesando...");

        db.collection("usuarios").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Double prePromedio = documentSnapshot.getDouble("prePromedio");
                    if (prePromedio == null) prePromedio = 0.0;
                    final double prePromedioFinal = prePromedio;   // ✅ COPIA FINAL

                    final double incremento = postPromedio - prePromedioFinal;

                    // 1. Guardar la encuesta final en Firestore
                    Map<String, Object> encuesta = new HashMap<>();
                    encuesta.put("usuarioId", userId);

                    Map<String, Integer> postMap = new HashMap<>();
                    postMap.put("p1", respuestas.get("pc1"));
                    postMap.put("p2", respuestas.get("pc2"));
                    postMap.put("p3", respuestas.get("pc3"));
                    postMap.put("p4", respuestas.get("pc4"));
                    encuesta.put("respuestasPostConocimiento", postMap);

                    Map<String, Integer> satMap = new HashMap<>();
                    satMap.put("p1", respuestas.get("s1"));
                    satMap.put("p2", respuestas.get("s2"));
                    satMap.put("p3", respuestas.get("s3"));
                    satMap.put("p4", respuestas.get("s4"));
                    satMap.put("p5", respuestas.get("s5"));
                    satMap.put("p6", respuestas.get("s6"));
                    satMap.put("p7", respuestas.get("s7"));
                    satMap.put("p8", respuestas.get("s8"));
                    encuesta.put("respuestasSatisfaccion", satMap);

                    encuesta.put("promedioPostConocimiento", postPromedio);
                    encuesta.put("incrementoAprendizaje", incremento);
                    encuesta.put("fechaCompletado", System.currentTimeMillis());
                    encuesta.put("tipo", "post_practica_completa");

                    db.collection("encuestas").document(userId + "_final")
                            .set(encuesta)
                            .addOnSuccessListener(aVoid -> {
                                // 2. Actualizar perfil del usuario
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("encuestaSatisfaccionCompletada", true);
                                updates.put("postPromedio", postPromedio);
                                updates.put("incrementoAprendizaje", incremento);

                                db.collection("usuarios").document(userId)
                                        .set(updates, SetOptions.merge())
                                        .addOnSuccessListener(aVoid2 -> {
                                            // 3. Registrar evento en Firebase Analytics
                                            Bundle bundle = new Bundle();
                                            bundle.putDouble("pre_promedio", prePromedioFinal);
                                            bundle.putDouble("post_promedio", postPromedio);
                                            bundle.putDouble("incremento", incremento);
                                            mFirebaseAnalytics.logEvent("aprendizaje_completado", bundle);

                                            // 4. Guardar en SharedPreferences local
                                            SharedPreferences prefs = getSharedPreferences("GuanteJPrefs", MODE_PRIVATE);
                                            prefs.edit().putBoolean("encuestaSatisfaccionCompletada", true).apply();

                                            Toast.makeText(encuestaPostConocimiento.this, "¡Gracias por tu participación!", Toast.LENGTH_SHORT).show();
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            progressBar.setVisibility(View.GONE);
                                            btnEnviar.setEnabled(true);
                                            btnEnviar.setText("Enviar cuestionario");
                                            Toast.makeText(encuestaPostConocimiento.this, "Error al actualizar perfil: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                btnEnviar.setEnabled(true);
                                btnEnviar.setText("Enviar cuestionario");
                                Toast.makeText(encuestaPostConocimiento.this, "Error al guardar encuesta: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnEnviar.setEnabled(true);
                    btnEnviar.setText("Enviar cuestionario");
                    Toast.makeText(encuestaPostConocimiento.this, "Error al leer datos previos: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}