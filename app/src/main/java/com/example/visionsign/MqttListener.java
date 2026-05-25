package com.example.visionsign;



public interface MqttListener {
    void onMensajeRecibido(String mensaje);
}