package com.matteopaciolla.prbe.cli;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public final class AdminBootstrapTool {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9_-]{5,30}");

    private AdminBootstrapTool() {
    }

    public static void main(String[] args) {
        String username = requiredEnvironmentValue("ADMIN_BOOTSTRAP_USERNAME");
        String password = requiredEnvironmentValue("ADMIN_BOOTSTRAP_PASSWORD");
        validateCredentials(username, password);

        JdbcTemplate jdbcTemplate = createJdbcTemplate();
        Integer existingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ?", Integer.class, username);
        if (existingCount == null || existingCount != 0) {
            throw new IllegalStateException("The requested administrator account already exists");
        }

        String encodedPassword = new BCryptPasswordEncoder().encode(password);
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO users (id, created_at, username, password, roles, enabled, email_confirmed)
                    VALUES (nextval('users_id_seq'), CURRENT_TIMESTAMP, ?, ?, ?, TRUE, TRUE)
                    """);
            statement.setString(1, username);
            statement.setString(2, encodedPassword);
            statement.setArray(3, connection.createArrayOf("varchar", new String[]{"ADMIN", "USER"}));
            return statement;
        });
        System.out.println("Provisioned the administrator account.");
    }

    private static void validateCredentials(String username, String password) {
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException("Administrator username must be 5-30 letters, digits, '_' or '-'");
        }
        int passwordBytes = password.getBytes(StandardCharsets.UTF_8).length;
        if (passwordBytes < 16 || passwordBytes > 72) {
            throw new IllegalArgumentException("Administrator password must be 16-72 UTF-8 bytes");
        }
    }

    private static JdbcTemplate createJdbcTemplate() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                requiredEnvironmentValue("SPRING_DATASOURCE_URL"),
                requiredEnvironmentValue("SPRING_DATASOURCE_USERNAME"),
                requiredEnvironmentValue("SPRING_DATASOURCE_PASSWORD"));
        return new JdbcTemplate(dataSource);
    }

    private static String requiredEnvironmentValue(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be configured");
        }
        return value;
    }
}
