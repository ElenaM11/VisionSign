package com.example.visionsign;

public class MQTTManager {

    public interface MqttListener {
        void onMensajeRecibido(String mensaje);
    }

    private static MqttListener listener;

    public static void setListener(MqttListener l) {
        listener = l;
    }

    public static void enviarMensaje(String mensaje) {
        if (listener != null) {
            listener.onMensajeRecibido(mensaje);
        }
    }
}