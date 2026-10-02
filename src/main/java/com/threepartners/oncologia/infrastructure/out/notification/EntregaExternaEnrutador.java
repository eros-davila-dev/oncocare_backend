package com.threepartners.oncologia.infrastructure.out.notification;

import com.threepartners.oncologia.application.notificacion.EncolarCorreo;
import com.threepartners.oncologia.domain.notificacion.EntregaExternaPort;
import com.threepartners.oncologia.domain.notificacion.TipoCorreo;
import com.threepartners.oncologia.infrastructure.out.correo.CorreoProperties;
import com.threepartners.oncologia.infrastructure.out.correo.PlantillasCorreo;
import com.threepartners.oncologia.infrastructure.out.correo.ServicioCorreo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Entrega de cada aviso del outbox segun su destino: "correo:TIPO" va al
 * servicio de correo (plantilla + API/SMTP); cualquier otro destino es un
 * webhook de n8n (por ejemplo, la alerta de consulta escalada al personal).
 */
@Component
@RequiredArgsConstructor
public class EntregaExternaEnrutador implements EntregaExternaPort {

    private final N8nNotificationAdapter n8n;
    private final ServicioCorreo servicioCorreo;
    private final CorreoProperties correoProperties;

    @Override
    public void entregar(String destino, Map<String, Object> payload) {
        if (destino.startsWith(EncolarCorreo.PREFIJO_DESTINO)) {
            TipoCorreo tipo = TipoCorreo.valueOf(destino.substring(EncolarCorreo.PREFIJO_DESTINO.length()));
            servicioCorreo.enviar(PlantillasCorreo.renderizar(tipo, texto(payload, "email"),
                    texto(payload, "nombre"), texto(payload, "enlace"), correoProperties.logoUrl()));
            return;
        }
        n8n.entregar(destino, payload);
    }

    private static String texto(Map<String, Object> payload, String clave) {
        Object valor = payload.get(clave);
        return valor != null ? valor.toString() : "";
    }
}
