package com.example.webviewshelldemo;

import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class QrCodeContentParser {
    private static final Pattern TOKEN_QUERY_PATTERN =
            Pattern.compile("(?:collectionToken|collectToken|token)=([^&\\s]+)", Pattern.CASE_INSENSITIVE);

    private QrCodeContentParser() {
    }

    static JSONObject parse(String rawText) {
        JSONObject parsed = new JSONObject();
        if (rawText == null) {
            return parsed;
        }
        String trimmed = rawText.trim();
        if (trimmed.isEmpty()) {
            return parsed;
        }

        if (trimmed.startsWith("{")) {
            JSONObject json = tryParseJson(trimmed);
            if (json != null) {
                copyKnownFields(json, parsed);
                return parsed;
            }
        }

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            copyUrlQuery(Uri.parse(trimmed), parsed);
            return parsed;
        }

        Matcher matcher = TOKEN_QUERY_PATTERN.matcher(trimmed);
        if (matcher.find()) {
            putIfPresent(parsed, "token", matcher.group(1));
            putIfPresent(parsed, "collectionToken", matcher.group(1));
        }

        return parsed;
    }

    private static JSONObject tryParseJson(String rawText) {
        try {
            return new JSONObject(rawText);
        } catch (JSONException ex) {
            return null;
        }
    }

    private static void copyUrlQuery(Uri uri, JSONObject target) {
        if (uri == null) {
            return;
        }
        for (String key : uri.getQueryParameterNames()) {
            putIfPresent(target, key, uri.getQueryParameter(key));
        }
        copyKnownFields(target, target);
    }

    private static void copyKnownFields(JSONObject source, JSONObject target) {
        copyField(source, target, "taskId");
        copyField(source, target, "candidateId");
        copyField(source, target, "collectionId");
        copyField(source, target, "examNo");
        copyField(source, target, "admissionNo");
        copyField(source, target, "candidateNo");
        copyField(source, target, "idCardNo");
        copyField(source, target, "token");
        copyField(source, target, "collectionToken");
        copyField(source, target, "collectToken");

        String token = firstNonEmpty(
                source.optString("collectionToken", ""),
                source.optString("collectToken", ""),
                source.optString("token", "")
        );
        if (!token.isEmpty()) {
            putIfPresent(target, "token", token);
        }

        String examNo = firstNonEmpty(
                source.optString("examNo", ""),
                source.optString("admissionNo", ""),
                source.optString("candidateNo", "")
        );
        if (!examNo.isEmpty()) {
            putIfPresent(target, "examNo", examNo);
        }
    }

    private static void copyField(JSONObject source, JSONObject target, String key) {
        putIfPresent(target, key, source.optString(key, ""));
    }

    private static void putIfPresent(JSONObject target, String key, String value) {
        if (value == null) {
            return;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        try {
            if ("token".equals(key) || key.toLowerCase(Locale.ROOT).contains("token")) {
                target.put(key, Uri.decode(trimmed));
                return;
            }
            target.put(key, trimmed);
        } catch (JSONException ignored) {
            // Best-effort parsing only.
        }
    }

    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }
}
