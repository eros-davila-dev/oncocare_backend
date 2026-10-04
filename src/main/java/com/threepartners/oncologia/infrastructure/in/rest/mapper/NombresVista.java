package com.threepartners.oncologia.infrastructure.in.rest.mapper;

import java.util.Map;

/**
 * Nombres ya resueltos para una pagina de resultados (ver NombresService):
 * los mappers los usan como @Context para que la respuesta lleve
 * "Jasmin Arnao Fretel" junto a pacienteId, en vez de que la pantalla muestre
 * un id crudo.
 */
public record NombresVista(Map<Long, String> pacientes, Map<Long, String> usuarios, Map<String, String> entidades) {

    public NombresVista {
        pacientes = pacientes == null ? Map.of() : pacientes;
        usuarios = usuarios == null ? Map.of() : usuarios;
        entidades = entidades == null ? Map.of() : entidades;
    }

    public String paciente(Long id) {
        return id == null ? null : pacientes.get(id);
    }

    public String usuario(Long id) {
        return id == null ? null : usuarios.get(id);
    }

    public String entidad(String tipo, String id) {
        return tipo == null || id == null ? null : entidades.get(tipo + ":" + id);
    }
}
