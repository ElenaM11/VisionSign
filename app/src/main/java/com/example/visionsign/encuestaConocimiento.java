package com.example.visionsign;

import android.content.Intent;
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

public class encuestaConocimiento extends AppCompatActivity {

    private MaterialButton btnEnviar;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseAnalytics mFirebaseAnalytics;
    private Map<String, Integer> respuestas = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encuesta_conocimiento);

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

        configurarEstrellasPregunta1();
        configurarEstrellasPregunta2();
        configurarEstrellasPregunta3();
        configurarEstrellasPregunta4();
        configurarEstrellasPregunta5();

        btnEnviar.setOnClickListener(v -> enviarEncuesta());
    }

    private void configurarEstrellas(LinearLayout layout, String preguntaKey) {
        if (layout == null) return;
        for (int i = 0; i < layout.getChildCount(); i++) {
            TextView star = (TextView) layout.getChildAt(i);
            final int starIndex = i + 1;
            star.setOnClickListener(v -> {
                for (int j = 0; j < layout.getChildCount(); j++) {
                    TextView s = (TextView) layout.getChildAt(j);
                    if (j < starIndex) {
                        s.setText("★");
                        s.setTextColor(0xFFFFC107);
                    } else {
                        s.setText("☆");
                        s.setTextColor(0xFF666666);
                    }
                }
                respuestas.put(preguntaKey, starIndex);
            });
        }
    }

    private void configurarEstrellasPregunta1() {
        configurarEstrellas(findViewById(R.id.estrellas_p1), "p1");
    }
    private void configurarEstrellasPregunta2() {
        configurarEstrellas(findViewById(R.id.estrellas_p2), "p2");
    }
    private void configurarEstrellasPregunta3() {
        configurarEstrellas(findViewById(R.id.estrellas_p3), "p3");
    }
    private void configurarEstrellasPregunta4() {
        configurarEstrellas(findViewById(R.id.estrellas_p4), "p4");
    }
    private void configurarEstrellasPregunta5() {
        configurarEstrellas(findViewById(R.id.estrellas_p5), "p5");
    }

    private void enviarEncuesta() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Error: Usuario no valido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (respuestas.size() < 5) {
            Toast.makeText(this, "Por favor, responde todas las preguntas con estrellas", Toast.LENGTH_LONG).show();
            return;
        }

        // Calcular promedio de respuestas
        int sum = 0;
        for (int val : respuestas.values()) {
            sum += val;
        }
        double promedio = sum / (double) respuestas.size();

        String userId = mAuth.getCurrentUser().getUid();

        Map<String, Object> encuesta = new HashMap<>();
        encuesta.put("usuarioId", userId);
        encuesta.put("respuestasConocimiento", respuestas);
        encuesta.put("promedioConocimiento", promedio);
        encuesta.put("fechaCompletado", System.currentTimeMillis());
        encuesta.put("tipo", "conocimiento_inicial");

        progressBar.setVisibility(View.VISIBLE);
        btnEnviar.setEnabled(false);
        btnEnviar.setText("Enviando...");

        // Guardar en Firestore
        db.collection("encuestas").document(userId + "_conocimiento")
                .set(encuesta)
                .addOnSuccessListener(aVoid -> {
                    // Actualizar perfil del usuario
                    Map<String, Object> updateData = new HashMap<>();
                    updateData.put("encuestaCompletada", true);
                    updateData.put("prePromedio", promedio);

                    db.collection("usuarios").document(userId)
                            .set(updateData, SetOptions.merge())
                            .addOnSuccessListener(aVoid2 -> {
                                // Registrar evento en Analytics
                                Bundle bundle = new Bundle();
                                bundle.putDouble("promedio_conocimiento_inicial", promedio);
                                mFirebaseAnalytics.logEvent("encuesta_conocimiento_completada", bundle);

                                Toast.makeText(encuestaConocimiento.this,
                                        "Encuesta enviada. Gracias.", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(encuestaConocimiento.this, MainActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                btnEnviar.setEnabled(true);
                                btnEnviar.setText("Enviar encuesta");
                                Toast.makeText(encuestaConocimiento.this,
                                        "Error al actualizar perfil: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnEnviar.setEnabled(true);
                    btnEnviar.setText("Enviar encuesta");
                    Toast.makeText(encuestaConocimiento.this,
                            "Error al guardar la encuesta: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}