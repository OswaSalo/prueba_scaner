package com.example.prueba_scaner;


import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Button btnScan, btnCalendar;
    private int contadorQR = 0;
    private DatabaseReference dbRef;

    private final androidx.activity.result.ActivityResultLauncher<ScanOptions> qrLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    contadorQR++;

                    String qrTexto = result.getContents();

                    Toast.makeText(MainActivity.this,
                            "QR leído: " + qrTexto, Toast.LENGTH_SHORT).show();

                    // Obtener fecha y hora actual
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
                    String fechaHora = sdf.format(new Date());

                    SimpleDateFormat sdfDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                    String fecha = sdfDate.format(new Date());

                    // Guardar en Firebase
                    String id = dbRef.push().getKey();
                    if (id != null) {
                        Map<String, Object> scanData = new HashMap<>();
                        scanData.put("codigo", qrTexto);
                        scanData.put("numero", contadorQR);
                        scanData.put("fechaHora", fechaHora);
                        scanData.put("fecha", fecha);
                        scanData.put("timestamp", System.currentTimeMillis());

                        dbRef.child(id).setValue(scanData)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(MainActivity.this,
                                            "Guardado en Firebase. Total: " + contadorQR,
                                            Toast.LENGTH_LONG).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(MainActivity.this,
                                            "Error: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar Firebase
        dbRef = FirebaseDatabase.getInstance().getReference("scans");

        btnScan = findViewById(R.id.btnScan);
        btnCalendar = findViewById(R.id.btnCalendar); // Nuevo botón

        btnScan.setOnClickListener(v -> abrirEscaner());
        btnCalendar.setOnClickListener(v -> abrirCalendario());
    }

    private void abrirEscaner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Escanea un código QR");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        qrLauncher.launch(options);
    }

    private void abrirCalendario() {
        // Mostrar diálogo de calendario
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    // Formatear fecha seleccionada
                    String fechaSeleccionada = String.format(Locale.getDefault(),
                            "%02d-%02d-%04d", dayOfMonth, month + 1, year);
                    filtrarEscaneosPorFecha(fechaSeleccionada);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePicker.show();
    }

    private void filtrarEscaneosPorFecha(String fecha) {
        Query query = dbRef.orderByChild("fecha").equalTo(fecha);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int totalDia = 0;
                StringBuilder detalles = new StringBuilder();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    totalDia++;
                    String codigo = snapshot.child("codigo").getValue(String.class);
                    String fechaHora = snapshot.child("fechaHora").getValue(String.class);

                    detalles.append("• ").append(fechaHora)
                            .append(" - ").append(codigo).append("\n");
                }

                mostrarResultados(fecha, totalDia, detalles.toString());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MainActivity.this,
                        "Error: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void mostrarResultados(String fecha, int total, String detalles) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Escaneos del " + fecha)
                .setMessage("Total: " + total + " escaneos\n\n" +
                        (detalles.isEmpty() ? "No hay escaneos este día" : detalles))
                .setPositiveButton("Aceptar", null)
                .show();
    }
}
