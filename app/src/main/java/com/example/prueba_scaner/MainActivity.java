package com.example.prueba_scaner;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class MainActivity extends AppCompatActivity {

    private Button btnScan;
    private int contadorQR = 0;

    DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("scans");

    private final androidx.activity.result.ActivityResultLauncher<ScanOptions> qrLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    contadorQR++;

                    String qrTexto = result.getContents();

                    Toast.makeText(MainActivity.this,
                            "QR leído: " + qrTexto, Toast.LENGTH_SHORT).show();

                    // Guardar en Firebase
                    String id = dbRef.push().getKey();
                    dbRef.child(id).child("codigo").setValue(qrTexto);
                    dbRef.child(id).child("numero").setValue(contadorQR);

                    Toast.makeText(MainActivity.this,
                            "Guardado en Firebase. Total: " + contadorQR,
                            Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar Firebase
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            dbRef = database.getReference("scans");

            // Verificar conexión
            dbRef.child("test").setValue("connection_test")
                    .addOnSuccessListener(aVoid -> Log.d("Firebase", "Conexión exitosa"))
                    .addOnFailureListener(e -> Log.e("Firebase", "Error conexión: " + e.getMessage()));

        } catch (Exception e) {
            Toast.makeText(this, "Error Firebase: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        btnScan = findViewById(R.id.btnScan);
        btnScan.setOnClickListener(v -> abrirEscaner());
    }

    private void abrirEscaner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Escanea un código QR");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        // options.setCaptureActivity(CaptureAct.class); // ← COMENTA o ELIMINA esta línea
        // NO uses setCaptureActivity o usa la clase por defecto:

        qrLauncher.launch(options);
    }
}
