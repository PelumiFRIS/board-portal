package com.fris.boardportal.meeting.recording;

import com.fris.boardportal.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class OpenAiTranscriptionService {

    private static final String TRANSCRIPTIONS_URL = "https://api.openai.com/v1/audio/transcriptions";

    private final String apiKey;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiTranscriptionService(@Value("${openai.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.create();
    }

    public String transcribe(byte[] audioBytes, String fileName, String contentType) {
        if (apiKey == null || apiKey.isBlank()) {
            throw ApiException.badRequest("Transcription is not configured — OPENAI_API_KEY is missing");
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        });
        body.add("model", "whisper-1");

        try {
            String responseJson = restClient.post()
                    .uri(TRANSCRIPTIONS_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode node = objectMapper.readTree(responseJson);
            JsonNode textNode = node.get("text");
            if (textNode == null) {
                throw ApiException.badRequest("Transcription provider returned an unexpected response");
            }
            return textNode.asText();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw ApiException.badRequest("Transcription failed: " + e.getMessage());
        }
    }
}
