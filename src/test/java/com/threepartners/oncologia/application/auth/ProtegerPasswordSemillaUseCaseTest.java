package com.threepartners.oncologia.application.auth;

import com.threepartners.oncologia.application.auth.ProtegerPasswordSemillaUseCase.Resultado;
import com.threepartners.oncologia.domain.auditoria.event.OperacionAuditadaEvent;
import com.threepartners.oncologia.domain.usuario.PasswordEncoderPort;
import com.threepartners.oncologia.domain.usuario.SesionRefreshTokenRepositoryPort;
import com.threepartners.oncologia.domain.usuario.Usuario;
import com.threepartners.oncologia.domain.usuario.UsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProtegerPasswordSemillaUseCaseTest {

    /** Simula BCrypt: "hash(x)" coincide solo con x. */
    private final PasswordEncoderPort encoder = new PasswordEncoderPort() {
        @Override
        public String encriptar(String plano) {
            return "hash(" + plano + ")";
        }

        @Override
        public boolean coincide(String plano, String hash) {
            return hash.equals("hash(" + plano + ")");
        }
    };

    private final UsuarioRepositoryPort usuarios = mock(UsuarioRepositoryPort.class);
    private final SesionRefreshTokenRepositoryPort sesiones = mock(SesionRefreshTokenRepositoryPort.class);
    private final ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);
    private final ProtegerPasswordSemillaUseCase useCase =
            new ProtegerPasswordSemillaUseCase(usuarios, encoder, sesiones, eventos);

    private Usuario admin;

    @BeforeEach
    void adminConLaSemilla() {
        admin = Usuario.builder().id(1L).email(ProtegerPasswordSemillaUseCase.EMAIL_ADMIN_SEMILLA)
                .passwordHash("hash(" + ProtegerPasswordSemillaUseCase.PASSWORD_SEMILLA + ")").intentosFallidos(3).build();
        when(usuarios.buscarPorEmail(ProtegerPasswordSemillaUseCase.EMAIL_ADMIN_SEMILLA)).thenReturn(Optional.of(admin));
    }

    @Test
    void conPasswordInicialReemplazaLaSemillaCierraSesionesYAudita() {
        Resultado r = useCase.ejecutar("Fundacion-2026-segura", true);

        assertThat(r).isEqualTo(Resultado.REEMPLAZADA);
        assertThat(admin.getPasswordHash()).isEqualTo("hash(Fundacion-2026-segura)");
        assertThat(admin.getIntentosFallidos()).isZero();
        verify(usuarios).guardar(admin);
        verify(sesiones).revocarTodasDeUsuario(1L);
        verify(eventos).publishEvent(any(OperacionAuditadaEvent.class));
    }

    @Test
    void enProduccionSinPasswordInicialImpideElArranque() {
        assertThatThrownBy(() -> useCase.ejecutar("", true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_PASSWORD_INICIAL");
    }

    @Test
    void enDesarrolloSoloAdvierte() {
        assertThat(useCase.ejecutar(null, false)).isEqualTo(Resultado.SEMILLA_ACTIVA);
        verify(usuarios, never()).guardar(any());
    }

    @Test
    void rechazaUnaPasswordInicialCortaOIgualALaSemilla() {
        assertThatThrownBy(() -> useCase.ejecutar("corta", true)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> useCase.ejecutar(ProtegerPasswordSemillaUseCase.PASSWORD_SEMILLA, true))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void siYaSeCambioNuncaPisaLaPasswordElegida() {
        admin.setPasswordHash("hash(la-que-eligio-el-admin)");

        assertThat(useCase.ejecutar("Fundacion-2026-segura", true)).isEqualTo(Resultado.SIN_PASSWORD_SEMILLA);
        verify(usuarios, never()).guardar(any());
    }
}
