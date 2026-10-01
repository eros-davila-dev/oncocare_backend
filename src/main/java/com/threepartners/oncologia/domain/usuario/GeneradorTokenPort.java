package com.threepartners.oncologia.domain.usuario;

/**
 * Generacion y hash de tokens de un solo uso (verificacion de email, reset de
 * password, identificadores de sesion de refresh). El valor en texto plano
 * solo se usa para construir el enlace enviado al usuario; unicamente el hash
 * se persiste.
 */
public interface GeneradorTokenPort {

    String generarToken();

    String hash(String valorEnTextoPlano);
}
