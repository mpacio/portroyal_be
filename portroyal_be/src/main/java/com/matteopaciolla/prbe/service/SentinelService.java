package com.matteopaciolla.prbe.service;

import com.matteopaciolla.prbe.constants.enums.SentinelAlertMessage;
import com.matteopaciolla.prbe.dto.AlertCallbackWrapperDto;
import com.matteopaciolla.prbe.dto.request.CallbackSentinelRequest;
import com.matteopaciolla.prbe.dto.AlertDto;
import com.matteopaciolla.prbe.exceptions.common.ResourceNotFoundException;
import com.matteopaciolla.prbe.model.SentinelSubscription;
import com.matteopaciolla.prbe.model.entity.CallbackEntity;
import com.matteopaciolla.prbe.model.entity.UserEntity;
import com.matteopaciolla.prbe.repository.CallbackRepository;
import com.matteopaciolla.prbe.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class SentinelService {

    private final CallbackRepository callbackRepository;
    private final MatchRepository matchRepository;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final RestTemplate restTemplate;

    private final Map<String, Set<SentinelSubscription>> matchSubsMap = new ConcurrentHashMap<>();

    private void checkMatchExists(String keyCode) {
        matchRepository.findByKeyCode(keyCode)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found"));
    }

    private void logRemoving(boolean removed, int newSize) {
        if (removed) {
            log.trace("Subscription removed, new size: {}", newSize);
        } else {
            log.trace("Subscription not found, size: {}", newSize);
        }
    }

    public String addCallbackSentinel(Long userId, String keyCode, String url, String secret) {
        log.info("Requested callback sentinel for match: {}", keyCode);
        checkMatchExists(keyCode);
        Set<SentinelSubscription> subs = matchSubsMap.get(keyCode);
        if (subs != null) {
            subs.forEach(sub -> {
                if (sub.getUser().getId().equals(userId)) {
                    log.warn("Subscription already exists for user: {}, updating it with a callback", sub.getUser().getUsername());
                    sub.stop();
                    boolean found = subs.remove(sub);
                    logRemoving(found, subs.size());
                }
            });
        }
        callbackRepository.findByUserIdAndMatchKeyCode(userId, keyCode)
                .ifPresentOrElse(
                        callback -> {
                            if (!callback.getUrl().equals(url)) {
                                callback.setUrl(url);
                                callback.setSecret(secret);
                                callbackRepository.save(callback);
                            }
                        },
                        () -> callbackRepository.save(new CallbackEntity(keyCode, userId, url, secret))
                );
        return "Callback sentinel added";
    }

    public DeferredResult<AlertDto> addLongPollingSentinel(UserEntity user, String keyCode, int secondsTimeout) {
        log.info("Adding long polling sentinel for keyCode: {}", keyCode);
        checkMatchExists(keyCode);
        DeferredResult<AlertDto> result = new DeferredResult<>();
        Set<SentinelSubscription> clients = matchSubsMap.getOrDefault(keyCode, ConcurrentHashMap.newKeySet());
        SentinelSubscription subscription = new SentinelSubscription(user, keyCode, null, result);
        Optional<SentinelSubscription> existingSub = clients.stream()
                .filter(sub -> sub.equals(subscription)).findFirst();
        if (existingSub.isPresent()) {
            log.warn("Subscription already exists for user: {}, updating it with a long polling", user.getUsername());
            existingSub.get().stop();
            boolean found = clients.remove(existingSub.get());
            logRemoving(found, clients.size());
        }
        clients.add(subscription);
        matchSubsMap.put(keyCode, clients);

        executorService.execute(() -> {
            try {
                Thread.sleep(secondsTimeout * 1000L);
                if (!result.hasResult()) {
                    AlertDto alert = new AlertDto(SentinelAlertMessage.SUBSCRIPTION_TIMEOUT, keyCode);
                    result.setResult(alert);
                    subscription.stop();
                    boolean found = clients.remove(subscription);
                    log.debug("DeferredResult completed, removing from list. subscription: {}", subscription);
                    logRemoving(found, clients.size());
                    Thread.currentThread().interrupt();
                }
            } catch (InterruptedException e) {
                log.warn("Long Polling Thread interrupted, stopping it. subscription: {}", subscription);
                Thread.currentThread().interrupt();
            }
        });
        removeCallbackSubscription(user.getId(), keyCode);
        return result;
    }

    public SseEmitter addSSESentinel(UserEntity user, String keyCode, long timeout) {
        log.info("Subscribing to events for keyCode: {}", keyCode);
        checkMatchExists(keyCode);
        SseEmitter emitter = new SseEmitter(timeout);
        Set<SentinelSubscription> clients = matchSubsMap.getOrDefault(keyCode, ConcurrentHashMap.newKeySet());
        SentinelSubscription subscription = new SentinelSubscription(user, keyCode, emitter, null);
        Optional<SentinelSubscription> existingSub = clients.stream()
                .filter(sub -> sub.equals(subscription)).findFirst();
        if (existingSub.isPresent()) {
            log.warn("Subscription already exists for user: {}, updating it with a SSE", user.getUsername());
            existingSub.get().stop();
            clients.remove(existingSub.get());
        }
        clients.add(subscription);
        matchSubsMap.put(keyCode, clients);

        try {
            emitter.onCompletion(() -> {
                boolean found = clients.removeIf(sub -> sub.getEmitter() == emitter);
                log.debug("SSE Emitter completed, removed from list");
                logRemoving(found, clients.size());
            });
            emitter.onTimeout(() -> {
                boolean found = clients.removeIf(sub -> sub.getEmitter() == emitter);
                log.debug("SSE Emitter timed out, removed from list");
                logRemoving(found, clients.size());
            });
            emitter.onError(throwable -> {
                boolean found = clients.removeIf(sub -> sub.getEmitter() == emitter);
                log.debug("SSE Emitter failed with error: {}, removed from list", throwable.getMessage());
                logRemoving(found, clients.size());
            });
            // just for testing
//            Thread thread = new Thread(() -> {
//                while (true) {
//                    try {
//                        Thread.sleep(Duration.ofSeconds(1));
//                        sendEvents();
//                    } catch (InterruptedException e) {
//                        log.warn("SSE Thread interrupted, stopping it");
//                        Thread.currentThread().interrupt();
//                    }
//                }
//            });
//            thread.start();
            // Send initial event to test the connection
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (Exception e) {
            log.error("Error while creating emitter", e);
            emitter.completeWithError(e);
        }
        removeCallbackSubscription(user.getId(), keyCode);
        return emitter;
    }

    private void removeCallbackSubscription(Long userId, String keyCode) {
        callbackRepository.findByUserIdAndMatchKeyCode(userId, keyCode)
                .ifPresent(callbackRepository::delete);
    }

    public void sendUpdate(String keyCode, String username, SentinelAlertMessage message) {
        boolean lastMessage = isLastMessage(message);
        Set<CallbackRepository.UrlSecret> urlSecrets = callbackRepository.findDistinctUrlSecretByMatchKeyCode(keyCode);
        if (!urlSecrets.isEmpty()) {
            sendCallbackUpdate(urlSecrets, keyCode, username, message);
        }
        Set<SentinelSubscription> subs = matchSubsMap.get(keyCode);
        if (subs != null) {
            subs.forEach(sub -> {
                boolean toBeRemoved = false;
                if (sub.getEmitter() != null) {
                    try {
                        sendSSEEvents(sub.getUser().getId(), keyCode, sub.getEmitter(), message);
                    } catch (IOException e) {
                        toBeRemoved = true;
                        log.warn("IOException encountered. Removing emitter for user: {}", sub.getUser().getUsername());
                    } catch (Exception e) {
                        toBeRemoved = true;
                        log.debug("Failed to send event to emitter, removing it: {}", e.getMessage());
                    }
                }
                if (sub.getDeferredResult() != null) {
                    sendLongPollingEvents(sub.getDeferredResult(), keyCode, message);
                    toBeRemoved = true;
                }
                if (toBeRemoved) {
                    subs.remove(sub);
                }
            });
            log.trace("After update sending subs size: {}", subs.size());
        } else {
            log.trace("No subscriptions found for keyCode: {}", keyCode);
        }
        if (lastMessage) {
            removeAllMatchSubscriptions(keyCode);
        }
    }

    @Async
    private void sendCallbackUpdate(Set<CallbackRepository.UrlSecret> urlSecrets, String keyCode, String username, SentinelAlertMessage message) {
        urlSecrets.forEach(urlSecret -> {
            String url = urlSecret.getUrl();
            String secret = urlSecret.getSecret();
            log.debug("Sending notification to {}", url);
            try {
                AlertDto alert = new AlertDto(message, keyCode);
                AlertCallbackWrapperDto request = new AlertCallbackWrapperDto(username, alert, secret);
                ResponseEntity<Object> response = restTemplate.postForEntity(url, request, Object.class);
                log.debug("Notification sent to {} with status code {}", url, response.getStatusCode());
            } catch (ResourceNotFoundException e) {
                log.warn("Error sending notification to {} because of ResourceNotFoundException {}", url, e.getMessage());
            } catch (ResourceAccessException e) {
                log.warn("Error sending notification to {} because of ResourceAccessException {}", url, e.getMessage());
            } catch (RestClientException e) {
                log.warn("Error sending notification to {} because of RestClientException {}", url, e.getMessage());
            } catch (Exception e) {
                log.warn("Error sending notification to {} with message {}", url, e.getMessage());
            }
        });
    }

    public void removeSentinelSubscription(Long userId, String keyCode) {
        callbackRepository.findByUserIdAndMatchKeyCode(userId, keyCode)
                .ifPresent(callbackRepository::delete);
        Set<SentinelSubscription> subs = matchSubsMap.get(keyCode);
        if (subs != null) {
            Iterator<SentinelSubscription> iterator = subs.iterator();
            while (iterator.hasNext()) {
                SentinelSubscription sub = iterator.next();
                if (sub.getUser().getId().equals(userId)) {
                    sub.stop();
                    iterator.remove();
                    log.debug("Subscription removed for user: {}", sub.getUser().getUsername());
                    logRemoving(true, subs.size());
                }
            }
        }
    }

    private void sendSSEEvents(Long userId, String keyCode, SseEmitter sseEmitter, SentinelAlertMessage message) throws IOException {
        String id = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        id = String.valueOf(userId) + "_"
                + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("_%05d", LocalTime.now().toSecondOfDay());
        AlertDto alert = new AlertDto(message, keyCode);
        sseEmitter.send(SseEmitter.event()
                .name("alert")
                .id(id)
                .data(alert));
        log.debug("Event with id: {} sent to user: {}", id, userId);
    }

    private void sendLongPollingEvents(DeferredResult<AlertDto> deferredResult, String keyCode, SentinelAlertMessage message) {
        if (!deferredResult.hasResult()) {
            AlertDto alert = new AlertDto(message, keyCode);
            deferredResult.setResult(alert);
        }
    }

    private boolean isLastMessage(SentinelAlertMessage message) {
        return message == SentinelAlertMessage.MATCH_ENDED
                || message == SentinelAlertMessage.MATCH_CLOSED
                || message == SentinelAlertMessage.SUBSCRIPTION_TIMEOUT;
    }

    private void removeAllMatchSubscriptions(String keyCode) {
        List<CallbackEntity> callbacks = callbackRepository.findByMatchKeyCode(keyCode);
        if (!callbacks.isEmpty()) {
            callbackRepository.deleteAll(callbacks);
        }
        Set<SentinelSubscription> subs = matchSubsMap.get(keyCode);
        if (subs != null) {
            subs.forEach(SentinelSubscription::stop);
            matchSubsMap.remove(keyCode);
            log.debug("Removed all subscriptions for keyCode: {}", keyCode);
        }
    }
}
