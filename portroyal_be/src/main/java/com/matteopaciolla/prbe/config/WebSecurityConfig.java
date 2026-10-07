package com.matteopaciolla.prbe.config;

import com.matteopaciolla.prbe.constants.Paths;
import com.matteopaciolla.prbe.constants.enums.UserRole;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests((authorizeRequests) -> authorizeRequests
                            .requestMatchers("/").permitAll() //fallback controller
                            .requestMatchers("/*").permitAll() //static files
                            .requestMatchers("/css/**").permitAll() //static css files
                            .requestMatchers("/js/**").permitAll() //static js files
                            .requestMatchers("/login").permitAll() //login page
                            .requestMatchers(Paths.BASE_API_PATH + Paths.PUBLIC_PATH + "/**").permitAll() //public API paths
                            .requestMatchers(Paths.PUBLIC_PATH + "/confirmEmail").permitAll() //HTML email confirmation page

                            .requestMatchers(HttpMethod.GET,"/swagger-ui/**").permitAll()
                            .requestMatchers(HttpMethod.GET,Paths.BASE_API_PATH + "/api-docs/**").permitAll()
                            .requestMatchers(HttpMethod.GET,Paths.BASE_API_PATH + "/api-docs.yaml").permitAll()

                            .requestMatchers(HttpMethod.POST,   Paths.BASE_API_PATH + Paths.USER_PATH + "/unify").hasRole(UserRole.BOT.name())
                            .requestMatchers(HttpMethod.POST,   Paths.BASE_API_PATH + Paths.USER_PATH + "/register/telegram").hasRole(UserRole.BOT.name())
//                            .requestMatchers(HttpMethod.PUT,    Paths.BASE_API_PATH + Paths.USER_PATH + "/**").hasAnyRole(UserRole.ADMIN.name(), UserRole.BOT.name())
                            .requestMatchers(HttpMethod.DELETE, Paths.BASE_API_PATH + Paths.USER_PATH + "/**").hasAnyRole(UserRole.ADMIN.name())// only admin can delete users

                            .requestMatchers(Paths.BASE_API_PATH + "/**").authenticated()
                            .anyRequest().denyAll()
//                .anyRequest().authenticated()
//                .anyRequest().permitAll()
            )
//            .formLogin(Customizer.withDefaults())
            .formLogin(form -> form
                    .loginPage("/login") // Custom login page URL
                    .loginProcessingUrl("/perform_login") // URL to submit the username and password
                    .defaultSuccessUrl("/", true) // URL to redirect to after successful login
                    .failureUrl("/login?error=true") // URL to redirect to after failed login
                    .usernameParameter("username") // Custom username parameter name
                    .passwordParameter("password") // Custom password parameter name
                    .permitAll() // Allow everyone to see the login page
            )
            .logout(logout -> logout.logoutSuccessUrl("/login?logout=true").invalidateHttpSession(true).permitAll())
            .httpBasic(Customizer.withDefaults())
            .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
                .expiredUrl("/login?session=expired")
            )
        ;
        return http.build();
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
