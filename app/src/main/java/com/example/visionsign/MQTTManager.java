package com.example.visionsign;

public class MQTTManager {

    private static MqttListener listener;
    private static String ultimoMensaje = null;

    public static void setListener(MqttListener l) {
        listener = l;
        if (l != null && ultimoMensaje != null) {
            l.onMensajeRecibido(ultimoMensaje);
        }
    }

    public static void enviarMensaje(String mensaje) {
        ultimoMensaje = mensaje;
        if (listener != null) {
            listener.onMensajeRecibido(mensaje);
        }
    }

    public static void limpiarUltimoMensaje() {
        ultimoMensaje = null;
    }
}