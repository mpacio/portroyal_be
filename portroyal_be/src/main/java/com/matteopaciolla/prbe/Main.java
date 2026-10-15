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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import java.util.List;

@SecuritySchemes({
		@SecurityScheme(
				name = "bearerAuth",
				type = SecuritySchemeType.HTTP,
				scheme = "bearer",
				bearerFormat = "JWT"
		)
})
@OpenAPIDefinition(
		info = @Info(
				title = "Port Royal Back End",
				description = "Port Royal game backend API. The service exposes the game state, match lifecycle, authentication, user management and bot-mediated operations for the Port Royal game. " +
					"JSON API endpoints under /api/v1 use OAuth2 bearer access tokens unless explicitly marked as public. " +
					"Human clients use OpenID Connect Authorization Code with PKCE; the Telegram bot uses client credentials and must send tgId when acting for a player.",
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
			UserRepository userRepository,
			@Value("${pr.auth.initial-admin-password:}") String initialAdminPassword,
			@Value("${pr.auth.bootstrap-admin-enabled:false}") boolean bootstrapAdminEnabled
	) {
		return args -> {
			UserEntity bot = userRepository.findByUsername("shaslabot")
					.orElseGet(() -> new UserEntity("shaslabot", null, List.of(UserRole.BOT)));
			bot.setPassword(null);
			bot.setRoles(List.of(UserRole.BOT));
			bot.setEnabled(true);
			bot.setEmailConfirmed(true);
			userRepository.save(bot);

			UserEntity admin = userRepository.findByUsername("admin").orElse(null);
			if (admin != null && admin.getPassword() != null
					&& passwordEncoder.matches("password", admin.getPassword())) {
				admin.setPassword(null);
				admin.setEnabled(false);
				userRepository.save(admin);
				log.warn("Disabled the legacy default admin credential; provision a new password out of band.");
			}

			if (bootstrapAdminEnabled) {
				if (!StringUtils.hasText(initialAdminPassword)
						|| initialAdminPassword.length() < 15
						|| initialAdminPassword.length() > 128) {
					throw new IllegalStateException(
							"PORTROYAL_INITIAL_ADMIN_PASSWORD must be between 15 and 128 characters when bootstrapping.");
				}
				if (admin == null) {
					admin = new UserEntity("admin", passwordEncoder.encode(initialAdminPassword),
							List.of(UserRole.ADMIN, UserRole.USER));
				} else if (admin.getPassword() == null && !admin.isEnabled()) {
					admin.setPassword(passwordEncoder.encode(initialAdminPassword));
				} else {
					admin = null;
				}
				if (admin != null) {
					admin.setEnabled(true);
					admin.setEmailConfirmed(true);
					userRepository.save(admin);
					log.info("Provisioned the initial administrator account.");
				}
			}
		};
	}
}
