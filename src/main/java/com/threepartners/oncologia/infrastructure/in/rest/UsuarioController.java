package com.threepartners.oncologia.infrastructure.in.rest;

import com.threepartners.oncologia.application.usuario.CambiarRolUsuarioUseCase;
import com.threepartners.oncologia.application.usuario.ConsultarUsuarioUseCase;
import com.threepartners.oncologia.application.usuario.CrearUsuarioUseCase;
import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.domain.usuario.Especialidad;
import com.threepartners.oncologia.domain.usuario.Usuario;
import com.threepartners.oncologia.infrastructure.in.rest.dto.PaginaResponseDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.auth.UsuarioResumenDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.usuario.CambiarRolRequestDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.usuario.CrearUsuarioRequestDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final CrearUsuarioUseCase crearUsuarioUseCase;
    private final CambiarRolUsuarioUseCase cambiarRolUsuarioUseCase;
    private final ConsultarUsuarioUseCase consultarUsuarioUseCase;

    @PostMapping
    public ResponseEntity<UsuarioResumenDto> crear(@Valid @RequestBody CrearUsuarioRequestDto dto) {
        Usuario nuevo = Usuario.builder()
                .nombres(dto.nombres())
                .email(dto.email())
                .rol(dto.rol())
                .especialidad(dto.especialidad())
                .build();

        Usuario creado = crearUsuarioUseCase.ejecutar(nuevo, dto.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(aResumen(creado));
    }

    @PutMapping("/{id}/rol")
    public UsuarioResumenDto cambiarRol(@PathVariable Long id, @Valid @RequestBody CambiarRolRequestDto dto, HttpServletRequest request) {
        Usuario actualizado = cambiarRolUsuarioUseCase.ejecutar(id, dto.rol(), AutenticacionActual.usuarioId(), AutenticacionActual.ipOrigen(request));
        return aResumen(actualizado);
    }

    @GetMapping("/{id}")
    public UsuarioResumenDto porId(@PathVariable Long id) {
        return aResumen(consultarUsuarioUseCase.porId(id));
    }

    @GetMapping
    public PaginaResponseDto<UsuarioResumenDto> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pagina = consultarUsuarioUseCase.listar(CriterioPaginacion.de(page, size));
        return PaginaResponseDto.de(pagina, this::aResumen);
    }

    @GetMapping("/medicos")
    public List<UsuarioResumenDto> medicosPorEspecialidad(@RequestParam Especialidad especialidad) {
        return consultarUsuarioUseCase.porEspecialidad(especialidad).stream().map(this::aResumen).toList();
    }

    private UsuarioResumenDto aResumen(Usuario usuario) {
        return new UsuarioResumenDto(usuario.getId(), usuario.getNombres(), usuario.getEmail(), usuario.getRol(), usuario.getEspecialidad());
    }
}
