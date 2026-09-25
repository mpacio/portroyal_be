package com.matteopaciolla.prbe.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "matches")
public class MatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "match_id_generator")
    @SequenceGenerator(name = "match_id_generator", sequenceName = "matches_id_seq", initialValue = 1, allocationSize = 1)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @NotNull
    @Column(unique = true)
    private String keyCode;
    @NotNull
    private Integer configurationId;
    @NotNull
    @ManyToMany(fetch = FetchType.EAGER, cascade = {}, targetEntity = UserEntity.class)
    @JoinTable(name = "match_user",
            joinColumns = @JoinColumn(name = "match_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"))
    private List<UserEntity> players;
    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, cascade = {}, targetEntity = UserEntity.class, optional = false)
    @JoinColumn(name = "host_user_id", referencedColumnName = "id", nullable = false, unique = false)
    private UserEntity hostUser;
    @ManyToOne(fetch = FetchType.EAGER, cascade = {}, targetEntity = UserEntity.class)
    @JoinColumn(name = "winner_user_id", referencedColumnName = "id", nullable = true, unique = false)
    private UserEntity winner;
    @NotNull
    private Boolean started = false;
    @NotNull
    private Boolean ended = false;
    private LocalDateTime endedAt;
    @OneToMany(fetch = FetchType.EAGER, cascade = {}, targetEntity = MoveEntity.class)
    private List<MoveEntity> moves;
    private LocalDateTime startedAt;
    private LocalDateTime lastMoveAt;

    public void addPlayer(UserEntity player) {
        players.add(player);
    }

    public void addMove(MoveEntity entity) {
        if (moves == null) {
            moves = new LinkedList<>();
        }
        moves.add(entity);
    }
}
