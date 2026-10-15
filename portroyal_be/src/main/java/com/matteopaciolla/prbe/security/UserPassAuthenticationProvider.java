package com.matteopaciolla.prbe.security;

import com.matteopaciolla.prbe.constants.enums.UserRole;
import com.matteopaciolla.prbe.model.entity.UserEntity;
import com.matteopaciolla.prbe.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class UserPassAuthenticationProvider implements AuthenticationProvider {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptRateLimiter loginAttemptRateLimiter;
    private final String dummyPasswordHash;

    public UserPassAuthenticationProvider(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            LoginAttemptRateLimiter loginAttemptRateLimiter) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptRateLimiter = loginAttemptRateLimiter;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials() == null
                ? ""
                : authentication.getCredentials().toString();
        String remoteAddress = authentication.getDetails() instanceof WebAuthenticationDetails details
                ? details.getRemoteAddress()
                : "unknown";
        if (!loginAttemptRateLimiter.isAllowed(remoteAddress, username)) {
            throw new BadCredentialsException("Invalid credentials!");
        }
        UserEntity user = userRepository.findByUsernameAndEnabledAndEmailConfirmed(username, true, true).orElse(null);
        String encodedPassword = user == null || user.getPassword() == null
                ? dummyPasswordHash
                : user.getPassword();
        boolean passwordMatches = passwordEncoder.matches(password, encodedPassword);
        if (user != null && user.getPassword() != null && passwordMatches) {
            if (passwordEncoder.upgradeEncoding(user.getPassword())) {
                user.setPassword(passwordEncoder.encode(password));
            }
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
            loginAttemptRateLimiter.recordSuccess(username);
            List<GrantedAuthority> authorities = getGrantedAuthorities(
                    user.getRoles().stream().map(UserRole::toString).toList());
            authorities.add(FactorGrantedAuthority.withAuthority(FactorGrantedAuthority.PASSWORD_AUTHORITY)
                    .issuedAt(java.time.Instant.now())
                    .build());
            return new UsernamePasswordAuthenticationToken(
                    user.getUsername(), null, authorities);
        } else {
            loginAttemptRateLimiter.recordFailure(remoteAddress, username);
            throw new BadCredentialsException("Invalid credentials!");
        }
    }

    private List<GrantedAuthority> getGrantedAuthorities(List<String> roles) {
        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        roles.forEach(role -> grantedAuthorities.add(new SimpleGrantedAuthority(role)));
        return grantedAuthorities;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
