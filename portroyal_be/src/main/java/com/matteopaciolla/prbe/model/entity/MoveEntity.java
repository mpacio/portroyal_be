package com.matteopaciolla.prbe.model.entity;

import com.matteopaciolla.portroyal.core.enums.MoveAction;
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
@Table(name = "moves")
public class MoveEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "move_id_generator")
    @SequenceGenerator(name = "move_id_generator", sequenceName = "moves_id_seq", initialValue = 1, allocationSize = 1)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
    private String libVersion;
    private Integer configurationId;
    @ManyToOne(fetch = FetchType.LAZY, cascade = {}, targetEntity = MatchEntity.class, optional = false)
    @JoinColumn(name = "match_id", referencedColumnName = "id",nullable = false)
    private MatchEntity match;
    private Integer timeIndex;
    private String activePlayerUsername;
    private Integer activePlayerIndex;
    private String runningPlayerUsername;
    private Integer runningPlayerIndex;
    @Enumerated(EnumType.STRING)
    private MoveAction moveName;
    private Integer parameterIndex;
    private Integer pickPlayerIndex;
    private List<String> expeditionEmployeesList;
    private String notes;
}
