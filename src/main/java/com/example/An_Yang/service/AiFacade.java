package com.example.An_Yang.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * GptService의 메서드명이 프로젝트마다 다른 문제를 해결하기 위해
 * 리플렉션으로 대표적인 메서드명을 순차 시도한다.
 * 시도 순서: complete, chat, generate, ask, create, createChat, createCompletion
 */
@Service
public class AiFacade {

    private final Optional<GptService> gptService;

    @Autowired(required = false)
    public AiFacade(GptService gptService) {
        this.gptService = Optional.ofNullable(gptService);
    }

    public String generate(String prompt) {
        return gptService
                .map(svc -> tryInvoke(svc, prompt))
                .orElse("[AI 모듈 미연결] " + prompt);
    }

    public String summarize(String title, String dataBlock, String ask) {
        String prompt = """
                [TASK] %s
                [DATA]
                %s

                [REQUEST]
                %s

                [OUTPUT RULES]
                - 글머리표/소제목으로 핵심만 7줄 이내
                - 숫자는 단위와 함께
                - 리스크/기회 1줄씩 포함
                """.formatted(title, dataBlock, ask);
        return generate(prompt);
    }

    public Mono<String> generateMono(String prompt) {
        return Mono.fromCallable(() -> generate(prompt))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<String> summarizeMono(String title, String dataBlock, String ask) {
        return Mono.fromCallable(() -> summarize(title, dataBlock, ask))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private String tryInvoke(Object svc, String prompt) {
        String[] candidates = {
                "complete", "chat", "generate", "ask",
                "create", "createChat", "createCompletion"
        };
        for (String name : candidates) {
            try {
                Method m = svc.getClass().getMethod(name, String.class);
                Object res = m.invoke(svc, prompt);
                if (res != null) return res.toString();
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                return "[AI 호출 오류:" + e.getClass().getSimpleName() + "] " + prompt;
            }
        }
        return "[AI 메서드 미탐색] " + prompt;
    }
}
