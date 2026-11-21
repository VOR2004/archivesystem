package ru.itis.handler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;
import ru.itis.exception.InvalidTaskException;
import ru.itis.model.ArchiveTask;

final class ArchiveTaskParser {
    private static final Gson JSON = new GsonBuilder().setStrictness(Strictness.STRICT).create();

    private ArchiveTaskParser() {
    }

    static ArchiveTask parse(String body) {
        JsonObject task;
        try {
            task = JSON.fromJson(body, JsonObject.class);
        } catch (JsonParseException error) {
            throw new InvalidTaskException("Task body must be a valid JSON object", error);
        }
        if (task == null) {
            throw new InvalidTaskException("Task body is required");
        }
        return new ArchiveTask(requiredString(task, "name"), requiredString(task, "url"));
    }

    private static String requiredString(JsonObject object, String field) {
        JsonElement value = object.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new InvalidTaskException(field + " must be a string");
        }
        return value.getAsString();
    }
}
