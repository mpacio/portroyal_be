package com.matteopaciolla.prbe;

import com.matteopaciolla.prbe.constants.enums.UserRole;
import com.matteopaciolla.prbe.model.entity.UserEntity;
import com.matteopaciolla.prbe.repository.UserRepository;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import io.swagger.v3.oas.annotations.servers.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@SecuritySchemes({
		@SecurityScheme(
				name = "basicAuth",
				type = SecuritySchemeType.HTTP,
				scheme = "basic"
		)
})
@OpenAPIDefinition(
		info = @Info(
				title = "Port Royal Back End",
				description = "Port Royal game backend API. The service exposes the game state, match lifecycle, authentication, user management and bot-mediated operations for the Port Royal game. " +
					"JSON API endpoints under /api/v1 use HTTP Basic Authentication unless explicitly marked as public; HTML pages are outside this prefix. " +
					"Bot integrations must send the mandatory tgId header when acting as the shaslabot technical account.",
				version = "1.0"
		),
		servers = {
				@Server(url = "http://localhost:8080", description = "Local development environment"),
				@Server(url = "https://prbe.matteopaciolla.com", description = "Production / hosted API")
		}
)
@Slf4j
@SpringBootApplication
public class Main {

	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
	}

	@Bean
	CommandLineRunner commandLineRunner(
			PasswordEncoder passwordEncoder,
			UserRepository userRepository
	) {
		return args -> {
			log.info("Application started successfully");

			String basePassword = "password";
			String adminUsername = "admin";
			String botUsername = "shaslabot";

			userRepository.findByUsername(adminUsername).ifPresentOrElse(
					user -> log.debug("Admin user already registered"),
					() -> {
						log.info("Registering admin user");
						// Register an admin user
						UserEntity user = new UserEntity(
								adminUsername,
								passwordEncoder.encode(basePassword),
								List.of(UserRole.ADMIN, UserRole.USER));
						user.setEnabled(true);
						user.setEmailConfirmed(true);
						userRepository.save(user);
					}
			);
			userRepository.findByUsername(botUsername).ifPresentOrElse(
					user -> log.debug("Bot user already registered"),
					() -> {
						log.info("Registering bot user");
						// Register a bot user
						UserEntity user = new UserEntity(
								botUsername,
								passwordEncoder.encode(basePassword),
								List.of(UserRole.BOT));
						user.setEnabled(true);
						user.setEmailConfirmed(true);
						userRepository.save(user);
					}
			);
		};
	}
}
