package com.matteopaciolla.prbe.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "game_configurations")
public class ConfigPropertyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "game_configurations_id_generator")
    @SequenceGenerator(name = "game_configurations_id_generator", sequenceName = "game_configurations_id_seq", initialValue = 1, allocationSize = 1)
    @EqualsAndHashCode.Include
    private Integer id;
    private Integer configId;
    private String configName;
    @Column(unique = true)
    private String propName;
    private String propValue;

    public ConfigPropertyEntity(Integer configId, String name, String propName, String propValue) {
        this.configId = configId;
        this.configName = name;
        this.propName = propName;
        this.propValue = propValue;
    }
}
