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
import com.applab.applab_backend.auth.model.UserModel;
import com.applab.applab_backend.auth.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class AiPageSelectionService {
    private final ObjectMapper mapper;
    private final UserService userService;
    private final Client client;
    private final JsonNode allPageOptions;
    private final JsonNode pageOptions;
    private final List<String> pageCodes;

    public AiPageSelectionService(ObjectMapper mapper, UserService userService,
            @Value("${GEMINI_API_KEY}") String apiKey)
            throws IOException {
        this.mapper = mapper;
        this.userService = userService;
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
    }

    public SseEmitter chat(AiPageSelectionRequest request, HttpServletRequest httpRequest) {
        SseEmitter emitter = new SseEmitter(0L);
        String currentUserName = getCurrentUserName(httpRequest);
        Thread.startVirtualThread(() -> {
            try {
                List<String> codes = selectPages(request, currentUserName);
                String assistantMessage = streamResponse(request, currentUserName, codes, emitter);
                String history = createHistory(request, assistantMessage);
                emitter.send(SseEmitter.event().name("history").data(history));
                emitter.complete();
            } catch (Exception exception) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(exception.getMessage()));
                    emitter.complete();
                } catch (IOException sendException) {
                    emitter.completeWithError(sendException);
                }
            }
        });
        return emitter;
    }

    private List<String> selectPages(AiPageSelectionRequest request, String currentUserName) throws IOException {
        String prompt = """
                Select one or more options that match the user's latest message.
                Use previous messages to understand follow-up messages.
                Select only UNCLEAR_MESSAGE when the request is unclear or incomplete.
                Select only FEATURE_NOT_AVAILABLE when the requested feature is not available.

                PAGE_OPTIONS:
                %s

                CURRENT_ROUTE: %s
                CURRENT_USER_NAME: %s
                PREVIOUS_MESSAGES: %s
                LATEST_MESSAGE: %s
                """.formatted(
                pageOptions,
                request.currentRoute(),
                currentUserName,
                request.history(),
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

    private String streamResponse(AiPageSelectionRequest request, String currentUserName,
            List<String> codes, SseEmitter emitter)
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
                If a route contains a dynamic parameter such as :username, return the route only when its actual value is known from the conversation.
                Replace every dynamic parameter with its actual value. Never return a route containing an unresolved :parameter.
                Keep the response around 150 words.

                SELECTED_PAGE_DETAILS:
                %s

                CURRENT_ROUTE: %s
                CURRENT_USER_NAME: %s
                PREVIOUS_MESSAGES: %s
                LATEST_MESSAGE: %s
                """.formatted(
                selectedOptions,
                request.currentRoute(),
                currentUserName,
                request.history(),
                request.message());

        StringBuilder assistantMessage = new StringBuilder();
        var config = GenerateContentConfig.builder()
                .maxOutputTokens(250)
                .build();
        try (var stream = client.models.generateContentStream("gemini-3.5-flash-lite", prompt, config)) {
            for (var part : stream) {
                if (part.text() != null) {
                    assistantMessage.append(part.text());
                    emitter.send(SseEmitter.event().data(part.text()));
                }
            }
        }
        return assistantMessage.toString();
    }

    private String createHistory(AiPageSelectionRequest request, String assistantMessage) {
        String prompt = """
                Create a short conversation history for the next AI request.
                Keep only important user intent,provided details,decisions and unresolved context.
                Remove greetings,repetition and unnecessary wording.
                Keep the history around 100 words.
                Return only the history text.

                PREVIOUS_HISTORY: %s
                LATEST_USER_MESSAGE: %s
                LATEST_ASSISTANT_MESSAGE: %s
                """.formatted(request.history(), request.message(), assistantMessage);

        var config = GenerateContentConfig.builder()
                .maxOutputTokens(150)
                .build();
        return client.models.generateContent("gemini-3.5-flash-lite", prompt, config).text();
    }

    private String getCurrentUserName(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session == null || !(session.getAttribute("userId") instanceof Long userId)) {
            return null;
        }

        UserModel user = userService.getUserById(userId);
        return user == null ? null : user.getName();
    }

    @PreDestroy
    public void close() {
        client.close();
    }
}
