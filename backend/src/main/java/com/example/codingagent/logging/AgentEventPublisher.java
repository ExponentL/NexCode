package com.example.codingagent.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class AgentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AgentEventPublisher.class);

    private final Map<String, List<AgentEvent>> taskEvents = new ConcurrentHashMap<>();
    private final Map<String, List<SseEmitter>> taskEmitters = new ConcurrentHashMap<>();
    private final List<java.util.function.Consumer<AgentEvent>> globalListeners = new CopyOnWriteArrayList<>();

    public void addGlobalListener(java.util.function.Consumer<AgentEvent> listener) {
        if (listener != null) {
            globalListeners.add(listener);
        }
    }

    public void removeGlobalListener(java.util.function.Consumer<AgentEvent> listener) {
        if (listener != null) {
            globalListeners.remove(listener);
        }
    }

    public void publish(AgentEvent event) {
        log.info("[Task: {}] [{}] {}", event.taskId(), event.type(), event.message());

        for (java.util.function.Consumer<AgentEvent> listener : globalListeners) {
            try {
                listener.accept(event);
            } catch (Exception ignored) {}
        }

        taskEvents.computeIfAbsent(event.taskId(), k -> new CopyOnWriteArrayList<>()).add(event);

        List<SseEmitter> emitters = taskEmitters.get(event.taskId());
        if (emitters != null) {
            List<SseEmitter> deadEmitters = new ArrayList<>();
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name(event.type().name())
                            .data(event));
                } catch (IOException e) {
                    deadEmitters.add(emitter);
                }
            }
            emitters.removeAll(deadEmitters);
        }
    }

    public List<AgentEvent> getEvents(String taskId) {
        return taskEvents.getOrDefault(taskId, Collections.emptyList());
    }

    public SseEmitter registerEmitter(String taskId) {
        SseEmitter emitter = new SseEmitter(180_000L); // 3 minute timeout
        taskEmitters.computeIfAbsent(taskId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(taskId, emitter));
        emitter.onTimeout(() -> removeEmitter(taskId, emitter));
        emitter.onError((e) -> removeEmitter(taskId, emitter));

        // Replay historical events to new subscriber
        List<AgentEvent> history = taskEvents.get(taskId);
        if (history != null) {
            for (AgentEvent event : history) {
                try {
                    emitter.send(SseEmitter.event()
                            .name(event.type().name())
                            .data(event));
                } catch (IOException e) {
                    emitter.complete();
                    removeEmitter(taskId, emitter);
                    break;
                }
            }
        }

        return emitter;
    }

    private void removeEmitter(String taskId, SseEmitter emitter) {
        List<SseEmitter> emitters = taskEmitters.get(taskId);
        if (emitters != null) {
            emitters.remove(emitter);
        }
    }
}
