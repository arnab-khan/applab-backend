package com.applab.applab_backend.ai.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.applab.applab_backend.ai.dto.AiPageSelectionRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import jakarta.annotation.PreDestroy;

@Service
public class AiPageSelectionService {
    private final ObjectMapper mapper;
    private final Client client;
    private final JsonNode allPageOptions;
    private final JsonNode pageOptions;
    private final List<String> pageCodes;

    public AiPageSelectionService(ObjectMapper mapper, @Value("${GEMINI_API_KEY}") String apiKey)
            throws IOException {
        this.mapper = mapper;
        this.client = Client.builder().apiKey(apiKey).build();
        try (var input = new ClassPathResource(
                "com/applab/applab_backend/ai/options/ai-page-options.json").getInputStream()) {
            JsonNode allOptions = mapper.readTree(input);
            this.allPageOptions = allOptions;
            var selectionOptions = mapper.createArrayNode();
            allOptions.forEach(option -> selectionOptions.add(mapper.createObjectNode()
                    .put("code", option.get("code").asText())
                    .put("summary", option.get("summary").asText())));
            this.pageOptions = selectionOptions;
        }
        this.pageCodes = new ArrayList<>();
        pageOptions.forEach(option -> pageCodes.add(option.get("code").asText()));
        pageCodes.add("NONE");
    }

    public SseEmitter chat(AiPageSelectionRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        Thread.startVirtualThread(() -> {
            try {
                List<String> codes = selectPages(request);
                streamResponse(request, codes, emitter);
                emitter.complete();
            } catch (Exception exception) {
                emitter.completeWithError(exception);
            }
        });
        return emitter;
    }

    private List<String> selectPages(AiPageSelectionRequest request) throws IOException {
        String prompt = """
                Select one or more pages that match the user's latest message.
                Use previous messages to understand follow-up messages.
                Return only NONE when no page matches.

                PAGE_OPTIONS:
                %s

                CURRENT_ROUTE: %s
                USER_TYPE: %s
                PREVIOUS_MESSAGES: %s
                LATEST_MESSAGE: %s
                """.formatted(
                pageOptions,
                request.currentRoute(),
                request.userType(),
                mapper.writeValueAsString(request.history() == null ? List.of() : request.history()),
                request.message());

        var config = GenerateContentConfig.builder()
                .responseMimeType("application/json")
                .responseJsonSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "codes", Map.of(
                                        "type", "array",
                                        "items", Map.of("type", "string", "enum", pageCodes))),
                        "required", List.of("codes")))
                .build();

        String response = client.models.generateContent("gemini-3.5-flash-lite", prompt, config).text();
        List<String> codes = new ArrayList<>();
        mapper.readTree(response).get("codes").forEach(code -> codes.add(code.asText()));
        return codes;
    }

    private void streamResponse(AiPageSelectionRequest request, List<String> codes, SseEmitter emitter)
            throws IOException {
        var selectedOptions = mapper.createArrayNode();
        allPageOptions.forEach(option -> {
            if (codes.contains(option.get("code").asText())) {
                selectedOptions.add(option);
            }
        });

        String prompt = """
                Answer the user's latest message as the AppLab assistant using the selected page details.
                Treat only capabilities explicitly present in the selected descriptions as available.
                If the requested feature is not present in those descriptions, clearly state that the feature is not available.
                When directing the user to an option whose route is not null, write it as router__<route>.
                Include the option's params in the route when present. Options with a null route must not use this format.

                SELECTED_PAGE_DETAILS:
                %s

                CURRENT_ROUTE: %s
                USER_TYPE: %s
                PREVIOUS_MESSAGES: %s
                LATEST_MESSAGE: %s
                """.formatted(
                selectedOptions,
                request.currentRoute(),
                request.userType(),
                mapper.writeValueAsString(request.history() == null ? List.of() : request.history()),
                request.message());

        try (var stream = client.models.generateContentStream("gemini-3.5-flash-lite", prompt, null)) {
            for (var part : stream) {
                if (part.text() != null) {
                    emitter.send(SseEmitter.event().data(part.text()));
                }
            }
        }
    }

    @PreDestroy
    public void close() {
        client.close();
    }
}
