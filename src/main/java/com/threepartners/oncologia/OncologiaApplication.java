package com.threepartners.oncologia;

import me.paulschwarz.springdotenv.spring.DotenvApplicationInitializer;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Excluye UserDetailsServiceAutoConfiguration: la autenticacion es JWT manual
 * (JwtAuthFilter + AutenticarUsuarioUseCase), no usa AuthenticationManager ni
 * UserDetailsService de Spring Security, asi que el usuario en memoria que
 * autoconfigura por defecto solo generaria ruido (y una contrasena aleatoria)
 * en cada arranque sin cumplir ninguna funcion real.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class OncologiaApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder(OncologiaApplication.class)
				.initializers(new DotenvApplicationInitializer())
				.run(args);
	}

}
