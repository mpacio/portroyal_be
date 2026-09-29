package com.matteopaciolla.prbe.repository.cachingproxy;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.matteopaciolla.portroyal.confs.Configuration;
import com.matteopaciolla.portroyal.core.Match;
import com.matteopaciolla.portroyal.core.MatchCreator;
import com.matteopaciolla.portroyal.core.MoveRecord;
import com.matteopaciolla.portroyal.core.Player;
import com.matteopaciolla.portroyal.exceptions.internal.InternalGameException;
import com.matteopaciolla.prbe.converter.MatchConfigConverter;
import com.matteopaciolla.prbe.converter.MoveConverter;
import com.matteopaciolla.prbe.model.entity.ConfigPropertyEntity;
import com.matteopaciolla.prbe.model.entity.MatchEntity;
import com.matteopaciolla.prbe.repository.ConfigPropertyRepository;
import com.matteopaciolla.prbe.repository.MatchRepository;
import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
public class MatchRetainer {

    @Autowired
    private ConfigPropertyRepository configPropertyRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Value("${be_app.cache.match.seconds-to-expire:300}")
    private int secondsToExpire;

    private LoadingCache<String, Match> matchCache;

    // Built in @PostConstruct (not as a field initializer) so that secondsToExpire
    // is already populated by @Value injection, which happens after construction.
    @PostConstruct
    private void initMatchCache() {
        matchCache = Caffeine.newBuilder()
                .expireAfterAccess(secondsToExpire, TimeUnit.SECONDS)
                .build(new CacheLoader<String, Match>() {
                            @Override
                            public Match load(@NonNull String key) {
                                Optional<MatchEntity> matchEntity = matchRepository.findByKeyCode(key);
                                return matchEntity.map(MatchRetainer.this::createMatch)
                                        .orElseThrow(() -> new IllegalArgumentException("Match not found"));
                            }
                        });
    }

    public Match getMatch(MatchEntity matchEntity) {
        if (matchEntity == null) {
            throw new IllegalArgumentException("Match entity cannot be null");
        }
        if (matchEntity.getStarted() == null || !matchEntity.getStarted()) {
            return null;
        }
        String keyCode = matchEntity.getKeyCode();
        Match match = matchCache.get(keyCode);
        // if match moves are not synchronized with the database, invalidate the cached match
        if (matchEntity.getMoves().size() != match.getMovesCount()) {
            log.warn("Match moves are not synchronized with the database, invalidating match with keyCode {}", keyCode);
            matchCache.invalidate(keyCode);
            return matchCache.get(keyCode);
        } else {
            log.debug("Match with keyCode {} found in cache", keyCode);
            return match;
        }
    }

    private Match createMatch(MatchEntity matchEntity) {
        List<Player> players = matchEntity.getAllPlayerUsernames().stream()
                .map(Player::new)
                .collect(LinkedList::new, LinkedList::add, LinkedList::addAll);

        Configuration configuration = createConfiguration(matchEntity.getConfigurationId());

        List<MoveRecord> moveRecords = matchEntity.getMoves().stream()
                .map(MoveConverter::toRecord)
                .collect(LinkedList::new, LinkedList::add, LinkedList::addAll);

        try {
            Match match = MatchCreator.createMatch(matchEntity.getId(), players, configuration, moveRecords);
            log.debug("Match with keyCode {} created, moves count: {}", matchEntity.getKeyCode(), match.getMovesCount());
            return match;
        } catch (InternalGameException e) {
            throw new RuntimeException(e);
        }
    }

    private Configuration createConfiguration(int configurationId) {
        List<ConfigPropertyEntity> configProperties = configPropertyRepository.findByConfigId(configurationId);
        return MatchConfigConverter.toLibEntity(configProperties);
    }
}
