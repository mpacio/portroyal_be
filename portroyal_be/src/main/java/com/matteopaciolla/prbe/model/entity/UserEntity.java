package com.matteopaciolla.prbe.model.entity;

import com.matteopaciolla.portroyal.core.enums.BotDifficulty;
import com.matteopaciolla.prbe.constants.enums.UserRole;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "user_id_generator")
    @SequenceGenerator(name = "user_id_generator", sequenceName = "users_id_seq", initialValue = 1, allocationSize = 1)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(unique = true, nullable = false)
    private String username;

    private String password;

    private String firstName;

    private String lastName;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String telegramId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private List<UserRole> roles;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(nullable = false)
    private boolean emailConfirmed = false;

    private LocalDateTime lastLogin;

    /**
     * Set only for {@link UserRole#AI} users: the difficulty the backend uses when computing this
     * player's moves via {@code Match#calculateNextMoveRecord(BotDifficulty)}. Null for every
     * human-controlled account.
     */
    @Enumerated(EnumType.STRING)
    private BotDifficulty botDifficulty;

    @ManyToMany(fetch = FetchType.EAGER, cascade = {}, targetEntity = MatchEntity.class)
    @JoinTable(name = "match_user",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "match_id", referencedColumnName = "id"))
    private List<MatchEntity> matches;

//    @Transient
//    private String confirmPwd;
//    @Transient
//    private String confirmEmail;

    public UserEntity(String username, String password, List<UserRole> roles) {
        this.username = username;
        this.password = password;
        this.roles = roles;
        this.createdAt = LocalDateTime.now();
    }

    public UserEntity(String username, String password, List<UserRole> roles, String firstName, String lastName, String email, String telegramId) {
        this(username, password, roles);
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.telegramId = telegramId;
    }

    public UserEntity(String username, String password, List<UserRole> roles, String email, String telegramId) {
        this(username, password, roles);
        if (email != null) this.email = email;
        if (telegramId != null) this.telegramId = telegramId;
    }
}
