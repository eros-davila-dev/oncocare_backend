package com.threepartners.oncologia.infrastructure.in.rest.advice;

import com.threepartners.oncologia.domain.estudio.Fase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Un error del cliente (parametro faltante o mal escrito, JSON roto, metodo
 * equivocado) es 4xx con un mensaje util, nunca un 500 "error inesperado".
 */
class GlobalExceptionHandlerTest {

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ControladorDePrueba())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void unParametroObligatorioFaltanteEs400ConSuNombre() throws Exception {
        mvc.perform(get("/prueba/fase"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"))
                .andExpect(jsonPath("$.message").value(containsString("'fase'")));
    }

    @Test
    void unValorDeEnumInvalidoEs400YListaLosPermitidos() throws Exception {
        mvc.perform(get("/prueba/fase").param("fase", "INTERMEDIA"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("PRETEST, POSTEST")));
    }

    @Test
    void unJsonMalFormadoEs400() throws Exception {
        mvc.perform(post("/prueba/cuerpo").contentType(MediaType.APPLICATION_JSON).content("{roto"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"));
    }

    @Test
    void unMetodoNoSoportadoConservaSu405() throws Exception {
        mvc.perform(delete("/prueba/fase"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void unErrorInesperadoEs500SinExponerElDetalle() throws Exception {
        mvc.perform(get("/prueba/falla"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Ocurrio un error inesperado"));
    }

    @RestController
    static class ControladorDePrueba {

        @GetMapping("/prueba/fase")
        String fase(@RequestParam Fase fase) {
            return fase.name();
        }

        @PostMapping("/prueba/cuerpo")
        Map<String, Object> cuerpo(@RequestBody Map<String, Object> cuerpo) {
            return cuerpo;
        }

        @GetMapping("/prueba/falla")
        String falla() {
            throw new IllegalStateException("detalle interno que no debe salir");
        }
    }
}
