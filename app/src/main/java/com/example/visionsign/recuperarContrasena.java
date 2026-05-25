package com.example.visionsign;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class recuperarContrasena extends AppCompatActivity {

    private TextInputEditText etCorreo;
    private MaterialButton btnEnviarCorreo, btnVolver;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recuperar_contrasena);

        mAuth = FirebaseAuth.getInstance();

        etCorreo = findViewById(R.id.etCorreoRecuperacion);
        btnEnviarCorreo = findViewById(R.id.btnEnviarCorreo);
        btnVolver = findViewById(R.id.btnVolver);
        progressBar = findViewById(R.id.progressBar);

        btnEnviarCorreo.setOnClickListener(v -> enviarCorreo());
        btnVolver.setOnClickListener(v -> finish());
    }

    private void enviarCorreo() {

        String email = etCorreo.getText().toString().trim();

        if (email.isEmpty()) {
            etCorreo.setError("Ingresa tu correo");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnEnviarCorreo.setEnabled(false);

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {

                    progressBar.setVisibility(View.GONE);
                    btnEnviarCorreo.setEnabled(true);

                    if (task.isSuccessful()) {

                        Toast.makeText(this,
                                "📩 Revisa tu correo para restablecer la contraseña",
                                Toast.LENGTH_LONG).show();

                        finish();

                    } else {

                        String error = task.getException() != null
                                ? task.getException().getMessage()
                                : "Error desconocido";

                        Toast.makeText(this,
                                "❌ Error: " + error,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}