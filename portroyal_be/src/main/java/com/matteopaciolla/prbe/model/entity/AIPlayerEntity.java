package com.matteopaciolla.prbe.model.entity;

import com.matteopaciolla.portroyal.core.enums.BotDifficulty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * An autonomous, per-match player operated by the backend. Distinct from the Telegram {@code BOT}
 * technical account (which mediates real human users, see {@link com.matteopaciolla.prbe.constants.enums.UserRole#BOT}),
 * an AI player never authenticates and is not persisted in the {@code users} table: it only exists
 * to occupy a seat in a {@link MatchEntity} and to carry the {@link BotDifficulty} the backend uses
 * to compute its moves via {@code Match#calculateNextMoveRecord(BotDifficulty)}. It is created when
 * the host adds an AI player to a not-yet-started match and is deleted when it is removed or the
 * match itself is deleted.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "ai_players")
public class AIPlayerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "ai_player_id_generator")
    @SequenceGenerator(name = "ai_player_id_generator", sequenceName = "ai_players_id_seq", initialValue = 1, allocationSize = 1)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {}, targetEntity = MatchEntity.class, optional = false)
    @JoinColumn(name = "match_id", referencedColumnName = "id", nullable = false)
    private MatchEntity match;

    @Column(nullable = false, unique = true)
    private String username;

    private String displayName;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private BotDifficulty difficulty;

    public AIPlayerEntity(String username, String displayName, BotDifficulty difficulty) {
        this.username = username;
        this.displayName = displayName;
        this.difficulty = difficulty;
    }
}
