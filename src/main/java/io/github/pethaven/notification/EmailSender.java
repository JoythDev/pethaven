package io.github.pethaven.notification;

/** Entrega correos transaccionales (avisos de citas). Usa el mismo proveedor que el código de inicio de sesión. */
public interface EmailSender {

    /**
     * Envía el correo.
     *
     * @throws IllegalStateException si el proveedor no lo pudo entregar
     */
    void send(EmailMessage message);
}
