package com.matteopaciolla.prbe.repository.cachingproxy;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
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
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
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

    private final LoadingCache<String, Match> matchCache = CacheBuilder.newBuilder()
            .expireAfterAccess(secondsToExpire, TimeUnit.SECONDS)
            .build(new CacheLoader<String, Match>() {
                        @Override
                        public Match load(@NonNull String key) {
                            Optional<MatchEntity> matchEntity = matchRepository.findByKeyCode(key);
                            return matchEntity.map(MatchRetainer.this::createMatch)
                                    .orElseThrow(() -> new IllegalArgumentException("Match not found"));
                        }
                    });

    public Match getMatch(MatchEntity matchEntity) throws ExecutionException {
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
        List<Player> players = matchEntity.getPlayers().stream()
                .map(playerEntity -> new Player(playerEntity.getUsername()))
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
