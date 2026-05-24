package com.example.visionsign;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class ClasificadorSenas {
    private static final String TAG = "ClasificadorSenas";
    private Interpreter tflite;
    private final int NUM_CLASES = 7;  // Ajusta según tus señas: HOLA, PAZ, TE QUIERO, 1, 3, 4, A
    private final String[] etiquetas = {"HOLA", "PAZ", "TE QUIERO", "1", "3", "4", "A"};

    public ClasificadorSenas(Context context) {
        try {
            tflite = new Interpreter(cargarModelo(context));
            Log.d(TAG, "Modelo cargado correctamente");
        } catch (IOException e) {
            Log.e(TAG, "Error al cargar modelo", e);
        }
    }

    private MappedByteBuffer cargarModelo(Context context) throws IOException {
        AssetFileDescriptor fd = context.getAssets().openFd("modelo_senas.tflite");
        FileInputStream inputStream = new FileInputStream(fd.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fd.getStartOffset();
        long declaredLength = fd.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public Resultado clasificar(float[] angulos) {
        if (tflite == null) return new Resultado("ERROR", 0f);
        float[][] input = new float[1][5];
        input[0] = angulos;
        float[][] output = new float[1][NUM_CLASES];
        tflite.run(input, output);
        int idx = argmax(output[0]);
        float confianza = output[0][idx];
        return new Resultado(etiquetas[idx], confianza * 100);
    }

    private int argmax(float[] arr) {
        int maxIdx = 0;
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > arr[maxIdx]) maxIdx = i;
        }
        return maxIdx;
    }

    public static class Resultado {
        public final String seña;
        public final float precision;
        public Resultado(String seña, float precision) {
            this.seña = seña;
            this.precision = precision;
        }
    }
}