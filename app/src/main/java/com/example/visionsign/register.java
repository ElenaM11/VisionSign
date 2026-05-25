package com.example.visionsign;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class register extends AppCompatActivity {

    private TextInputEditText etNombre, etEmail, etPassword, etConfirmarPassword;
    private MaterialButton btnRegistrar, btnCancelar;
    private TextView tvMensaje;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etNombre = findViewById(R.id.etNombre);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmarPassword = findViewById(R.id.etConfirmarPassword);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        btnCancelar = findViewById(R.id.btnCancelar);
        tvMensaje = findViewById(R.id.tvMensaje);

        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);

        btnRegistrar.setOnClickListener(v -> registrarUsuario());
        btnCancelar.setOnClickListener(v -> finish());
    }

    private void registrarUsuario() {
        String nombre = etNombre.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmarPassword.getText().toString().trim();

        if (nombre.isEmpty()) {
            tvMensaje.setText(" Ingresa tu nombre");
            return;
        }
        if (email.isEmpty()) {
            tvMensaje.setText(" Ingresa tu email");
            return;
        }
        if (password.isEmpty()) {
            tvMensaje.setText(" Ingresa una contraseña");
            return;
        }
        if (password.length() < 6) {
            tvMensaje.setText(" Contraseña debe tener al menos 6 caracteres");
            return;
        }
        if (!password.equals(confirmPassword)) {
            tvMensaje.setText(" Las contraseñas no coinciden");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnRegistrar.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();

                        Map<String, Object> usuario = new HashMap<>();
                        usuario.put("uid", userId);
                        usuario.put("nombre", nombre);
                        usuario.put("email", email);
                        usuario.put("encuestaCompletada", false);

                        db.collection("usuarios").document(userId)
                                .set(usuario)
                                .addOnSuccessListener(aVoid -> {
                                    // Cerrar sesión para que el usuario tenga que iniciar sesión manualmente
                                    mAuth.signOut();

                                    progressBar.setVisibility(View.GONE);
                                    tvMensaje.setText(" Registro exitoso. Ahora inicia sesión.");

                                    // Esperar 2 segundos y volver al Login
                                    new Handler().postDelayed(() -> finish(), 2000);
                                })
                                .addOnFailureListener(e -> {
                                    progressBar.setVisibility(View.GONE);
                                    btnRegistrar.setEnabled(true);
                                    tvMensaje.setText(" Error al guardar: " + e.getMessage());
                                });
                    } else {
                        progressBar.setVisibility(View.GONE);
                        btnRegistrar.setEnabled(true);
                        tvMensaje.setText(" " + task.getException().getMessage());
                    }
                });
    }
}