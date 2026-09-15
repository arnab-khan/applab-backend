package com.applab.applab_backend.ai.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.applab.applab_backend.ai.dto.AiPageSelectionRequest;
import com.applab.applab_backend.ai.service.AiPageSelectionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/ai")
public class AiPageSelectionController {
    private final AiPageSelectionService service;

    public AiPageSelectionController(AiPageSelectionService service) {
        this.service = service;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@Valid @RequestBody AiPageSelectionRequest request,
            HttpServletRequest httpRequest) {
        return service.chat(request, httpRequest);
    }
}
