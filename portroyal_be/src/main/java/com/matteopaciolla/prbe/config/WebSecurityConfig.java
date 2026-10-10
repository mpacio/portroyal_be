package com.matteopaciolla.prbe.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matteopaciolla.prbe.constants.Paths;
import com.matteopaciolla.prbe.constants.enums.UserRole;
import com.matteopaciolla.prbe.dto.response.ErrorResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        AuthenticationEntryPoint authenticationEntryPoint = (request, response, exception) -> {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"PortRoyal\"");
            writeSecurityError(response, objectMapper, HttpStatus.UNAUTHORIZED,
                    "Authentication is required to access this resource. Provide valid credentials and try again.");
        };
        AccessDeniedHandler accessDeniedHandler = (request, response, exception) ->
                writeSecurityError(response, objectMapper, HttpStatus.FORBIDDEN,
                        "You are authenticated but do not have permission to access this resource.");

        http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .exceptionHandling(exceptionHandling -> exceptionHandling
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
            .authorizeHttpRequests((authorizeRequests) -> authorizeRequests
                            .requestMatchers("/").permitAll() //fallback controller
                            .requestMatchers("/*").permitAll() //static files
                            .requestMatchers("/css/**").permitAll() //static css files
                            .requestMatchers("/js/**").permitAll() //static js files
                            .requestMatchers("/login").permitAll() //login page
                            .requestMatchers(Paths.BASE_API_PATH + Paths.PUBLIC_PATH + "/**").permitAll() //public API paths
                            .requestMatchers(Paths.PUBLIC_PATH + "/confirm-email").permitAll() //HTML email confirmation page

                            .requestMatchers(HttpMethod.GET,"/swagger-ui/**").permitAll()
                            .requestMatchers(HttpMethod.GET,Paths.BASE_API_PATH + "/api-docs/**").permitAll()
                            .requestMatchers(HttpMethod.GET,Paths.BASE_API_PATH + "/api-docs.yaml").permitAll()

                            .requestMatchers(HttpMethod.POST,   Paths.BASE_API_PATH + Paths.USER_PATH + "/unify").hasRole(UserRole.BOT.name())
                            .requestMatchers(HttpMethod.POST,   Paths.BASE_API_PATH + Paths.USER_PATH + "/register/telegram").hasRole(UserRole.BOT.name())
//                            .requestMatchers(HttpMethod.PATCH,  Paths.BASE_API_PATH + Paths.USER_PATH + "/**").hasAnyRole(UserRole.ADMIN.name(), UserRole.BOT.name())
                            .requestMatchers(HttpMethod.DELETE, Paths.BASE_API_PATH + Paths.USER_PATH + "/**").hasAnyRole(UserRole.ADMIN.name())// only admin can delete users

                            .requestMatchers(Paths.BASE_API_PATH + "/**").authenticated()
                            .anyRequest().denyAll()
//                .anyRequest().authenticated()
//                .anyRequest().permitAll()
            )
//            .formLogin(Customizer.withDefaults())
            .formLogin(form -> form
                    .loginPage("/login") // Custom login page URL
                    .loginProcessingUrl("/perform-login") // URL to submit the username and password
                    .defaultSuccessUrl("/", true) // URL to redirect to after successful login
                    .failureUrl("/login?error=true") // URL to redirect to after failed login
                    .usernameParameter("username") // Custom username parameter name
                    .passwordParameter("password") // Custom password parameter name
                    .permitAll() // Allow everyone to see the login page
            )
            .logout(logout -> logout.logoutSuccessUrl("/login?logout=true").invalidateHttpSession(true).permitAll())
            .httpBasic(httpBasic -> httpBasic.authenticationEntryPoint(authenticationEntryPoint))
            .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
                .expiredUrl("/login?session=expired")
            )
        ;
        return http.build();
    }

    private static void writeSecurityError(
            jakarta.servlet.http.HttpServletResponse response,
            ObjectMapper objectMapper,
            HttpStatus status,
            String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                new ErrorResponse(status.value(), status.getReasonPhrase(), message));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true); // Allow credentials if needed
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}
