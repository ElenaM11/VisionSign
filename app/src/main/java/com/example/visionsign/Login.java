package com.example.visionsign;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class Login extends AppCompatActivity {

    private TextInputEditText etUsuario, etContrasena;
    private MaterialButton btnIniciar, btnCancelar, btnRegistrar, btnSalir;
    private TextView tvMensaje, tvOlvideContrasena;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();



        initViews();
        setupListeners();
    }

    private void initViews() {
        etUsuario = findViewById(R.id.etUsuario);
        etContrasena = findViewById(R.id.etContrasena);
        btnIniciar = findViewById(R.id.btnIniciar);
        btnCancelar = findViewById(R.id.btnCancelar);
        tvMensaje = findViewById(R.id.tvMensaje);
        tvOlvideContrasena = findViewById(R.id.tvOlvideContrasena);


        btnRegistrar = new MaterialButton(this);
        btnRegistrar.setText("REGISTRARSE");
        btnRegistrar.setCornerRadius(8);
        btnRegistrar.setBackgroundColor(getColor(R.color.teal_700));
        btnRegistrar.setTextColor(getColor(android.R.color.white));


        LinearLayout parent = (LinearLayout) etUsuario.getParent().getParent().getParent();
        parent.addView(btnRegistrar);

        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);
        parent.addView(progressBar);
    }

    private void setupListeners() {
        btnIniciar.setOnClickListener(v -> validarLogin());
        btnCancelar.setOnClickListener(v -> limpiarCampos());

        btnRegistrar.setOnClickListener(v ->
                startActivity(new Intent(Login.this, register.class)));
        tvOlvideContrasena.setOnClickListener(v -> mostrarDialogoRecuperacion());

        btnSalir.setOnClickListener(v -> finish());
    }

    private void validarLogin() {
        String email = etUsuario.getText().toString().trim();
        String password = etContrasena.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            tvMensaje.setText(" Correo y contraseña son obligatorios");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnIniciar.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    btnIniciar.setEnabled(true);

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            tvMensaje.setText(" Inicio exitoso...");
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                verificarEncuestaYRedirigir(user.getUid());
                            }, 1000);
                        }
                    } else {
                        tvMensaje.setText(" " + task.getException().getMessage());
                        etContrasena.setText("");
                    }
                });
    }

    private void verificarEncuestaYRedirigir(String userId) {
        if (userId == null || userId.isEmpty()) {
            irAMainActivity();
            return;
        }

        db.collection("usuarios").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    // Si el documento no existe, asumimos que es primera vez (encuesta no completada)
                    boolean encuestaCompletada = documentSnapshot.exists() &&
                            Boolean.TRUE.equals(documentSnapshot.getBoolean("encuestaCompletada"));
                    if (!encuestaCompletada) {
                        Intent intent = new Intent(Login.this, encuestaConocimiento.class);
                        startActivity(intent);
                    } else {
                        irAMainActivity();
                    }
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al verificar encuesta", Toast.LENGTH_SHORT).show();
                    irAMainActivity();
                    finish();
                });
    }

    private void irAMainActivity() {
        startActivity(new Intent(Login.this, MainActivity.class));
        finish();
    }

    //
    private void mostrarDialogoRecuperacion() {
        startActivity(new Intent(Login.this, recuperarContrasena.class));
    }

    private void enviarRecuperacion(String email) {
        progressBar.setVisibility(View.VISIBLE);
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        new AlertDialog.Builder(Login.this)
                                .setTitle("REVISÁ TU CORREO")
                                .setMessage("Enviamos un enlace de recuperación a:\n" + email +
                                        "\n\nHacé clic en el enlace y creá una nueva contraseña.")
                                .setPositiveButton("ACEPTAR", (dialog, which) -> dialog.dismiss())
                                .show();
                    } else {
                        Toast.makeText(Login.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void limpiarCampos() {
        etUsuario.setText("");
        etContrasena.setText("");
        tvMensaje.setText("");
    }
}