package io.github.pethaven.notification;

/**
 * Correo listo para enviar.
 *
 * @param to      destinatario
 * @param subject asunto
 * @param text    cuerpo en texto plano
 * @param html    cuerpo en HTML (con los datos del usuario ya escapados)
 */
public record EmailMessage(String to, String subject, String text, String html) { }
