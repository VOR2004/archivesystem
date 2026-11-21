package ru.itis.infrastructure;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import ru.itis.exception.ProcessingInterruptedException;
import ru.itis.exception.TokenAcquisitionException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Supplier;

public final class IamTokenProvider implements Supplier<String> {
    private static final URI TOKEN_URI = URI.create(
            "http://169.254.169.254/computeMetadata/v1/instance/service-accounts/default/token");
    private final HttpClient http;

    public IamTokenProvider(HttpClient http) {
        this.http = http;
    }

    @Override
    public String get() {
        HttpRequest request = HttpRequest.newBuilder(TOKEN_URI)
                .timeout(Duration.ofSeconds(5))
                .header("Metadata-Flavor", "Google")
                .GET()
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new TokenAcquisitionException("IAM token request failed: HTTP " + response.statusCode());
            }
            JsonObject credentials = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonElement token = credentials.get("access_token");
            if (token == null || !token.isJsonPrimitive() || !token.getAsJsonPrimitive().isString()
                    || token.getAsString().isBlank()) {
                throw new TokenAcquisitionException("Metadata service returned no IAM token");
            }
            return token.getAsString();
        } catch (IOException error) {
            throw new TokenAcquisitionException("Cannot get function service account token", error);
        } catch (JsonParseException | IllegalStateException error) {
            throw new TokenAcquisitionException("Metadata service returned invalid token JSON", error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new ProcessingInterruptedException("IAM token request interrupted", error);
        }
    }
}
