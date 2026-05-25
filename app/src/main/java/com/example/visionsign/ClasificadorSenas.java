package com.example.visionsign;


public class ClasificadorSenas {

    private static final float UMBRAL_ALTO = 70f;
    private static final float UMBRAL_BAJO = 35f;

    public ClasificadorSenas(android.content.Context context) {
        // Sin inicialización adicional
    }

    public Resultado clasificar(float[] s) {
        if (s == null || s.length < 5) return new Resultado("ERROR", 0f);

        // Evaluación de los 5 dedos
        boolean s1A = s[0] >= UMBRAL_ALTO;
        boolean s2A = s[1] >= UMBRAL_ALTO;
        boolean s3A = s[2] >= UMBRAL_ALTO;
        boolean s4A = s[3] >= UMBRAL_ALTO;
        boolean s5A = s[4] >= UMBRAL_ALTO;

        boolean s1B = s[0] < UMBRAL_BAJO;
        boolean s2B = s[1] < UMBRAL_BAJO;
        boolean s3B = s[2] < UMBRAL_BAJO;
        boolean s4B = s[3] < UMBRAL_BAJO;
        boolean s5B = s[4] < UMBRAL_BAJO;

        // ✅ Campo de palma (s[5]):
        //   1  = palma hacia afuera/arriba
        //   0  = palma hacia adentro/abajo
        //  -1  = no disponible (ESP32 no manda el campo todavía)
        int palma = (s.length >= 6) ? Math.round(s[5]) : -1;
        boolean palmaFuera   = (palma == 1);
        boolean palmaDentro  = (palma == 0);
        boolean palmaDesconocida = (palma == -1);

        // -------------------------------------------------------
        // Señas con patrón único (no ambiguo) — precisión 95%
        // -------------------------------------------------------

        // A: pulgar+índice+anular altos, medio+meñique bajos
        if (s1A && s2A && s3B && s4A && s5B) return new Resultado("A", 95f);

        // B: pulgar+índice bajos, medio alto, anular bajo, meñique alto
        if (s1B && s2B && s3A && s4B && s5A) return new Resultado("B", 95f);

        // C: pulgar no bajo, índice+medio altos, anular+meñique bajos
        if (!s1B && s2A && s3A && s4B && s5B) return new Resultado("C", 95f);

        // I: pulgar+anular+meñique altos, índice+medio bajos
        if (s1A && s2B && s3B && s4A && s5A) return new Resultado("I", 95f);

        // L: pulgar alto, índice bajo, medio+anular+meñique altos
        if (s1A && s2B && s3A && s4A && s5A) return new Resultado("L", 95f);

        // 1 (puño cerrado): todos bajos
        if (s1B && s2B && s3B && s4B && s5B) return new Resultado("1", 95f);

        // 2: solo pulgar alto
        if (s1A && s2B && s3B && s4B && s5B) return new Resultado("2", 95f);

        // 3: pulgar+índice altos, resto bajos
        if (s1A && s2A && s3B && s4B && s5B) return new Resultado("3", 95f);

        // 5: pulgar+índice+medio+anular altos, meñique bajo
        if (s1A && s2A && s3A && s4A && s5B) return new Resultado("5", 95f);

        // 7: pulgar+índice bajos, medio+anular+meñique altos
        if (s1B && s2B && s3A && s4A && s5A) return new Resultado("7", 95f);

        // 9: pulgar+medio+anular altos, índice bajo, meñique bajo
        if (s1A && s2B && s3A && s4A && s5B) return new Resultado("9", 95f);

        // -------------------------------------------------------
        // ✅ CORREGIDO Bug 3: señas ambiguas — distinguir con palma
        // -------------------------------------------------------

        // E vs 6: pulgar bajo, índice+medio+anular+meñique altos
        if (s1B && s2A && s3A && s4A && s5A) {
            if (palmaFuera)       return new Resultado("E", 95f);
            if (palmaDentro)      return new Resultado("6", 95f);
            // Sin dato de palma: indicar ambigüedad con precisión reducida
            return new Resultado("E o 6", 50f);
        }

        // S vs 10: todos los dedos altos
        if (s1A && s2A && s3A && s4A && s5A) {
            if (palmaFuera)       return new Resultado("S", 95f);
            if (palmaDentro)      return new Resultado("10", 95f);
            // Sin dato de palma
            return new Resultado("S o 10", 50f);
        }

        // Seña no reconocida
        return new Resultado("?", 0f);
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