package com.example.webviewshelldemo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public final class NativeFileUploadService {
    static final class UploadRequest {
        final String uploadUrl;
        final String authorization;
        final String tenantId;
        final String accessPolicy;

        private UploadRequest(String uploadUrl, String authorization, String tenantId, String accessPolicy) {
            this.uploadUrl = uploadUrl;
            this.authorization = authorization;
            this.tenantId = tenantId;
            this.accessPolicy = accessPolicy;
        }

        static UploadRequest fromPayload(JSONObject payload) throws UploadException {
            String uploadUrl = trim(payload.optString("uploadUrl", ""));
            String authorization = trim(payload.optString("authorization", ""));
            String tenantId = trim(payload.optString("tenantId", ""));
            String accessPolicy = trim(payload.optString("accessPolicy", ""));
            if (accessPolicy == null) {
                accessPolicy = trim(payload.optString("visibility", ""));
            }
            if (accessPolicy == null) {
                accessPolicy = "private";
            }
            if (uploadUrl == null) {
                throw new UploadException("NATIVE_UPLOAD_CONFIG_INVALID", "缺少文件上传地址");
            }
            if (authorization == null) {
                throw new UploadException("NATIVE_UPLOAD_CONFIG_INVALID", "缺少上传认证信息");
            }
            if (tenantId == null) {
                throw new UploadException("NATIVE_UPLOAD_CONFIG_INVALID", "缺少租户信息");
            }
            return new UploadRequest(uploadUrl, authorization, tenantId, accessPolicy);
        }
    }

    static final class UploadResult {
        final String fileId;
        final String sha256Hex;
        final String format;
        final String uploadedAt;

        UploadResult(String fileId, String sha256Hex, String format, String uploadedAt) {
            this.fileId = fileId;
            this.sha256Hex = sha256Hex;
            this.format = format;
            this.uploadedAt = uploadedAt;
        }
    }

    static final class UploadException extends Exception {
        final String code;

        UploadException(String code, String message) {
            super(message);
            this.code = code;
        }

        UploadException(String code, String message, Throwable cause) {
            super(message, cause);
            this.code = code;
        }
    }

    UploadResult uploadPng(byte[] pngBytes, String fileName, UploadRequest request) throws UploadException {
        if (pngBytes == null || pngBytes.length == 0) {
            throw new UploadException("NATIVE_UPLOAD_FILE_EMPTY", "PNG 文件为空");
        }
        String sha256Hex = sha256Hex(pngBytes);
        String boundary = "----GuoyuAndroid" + UUID.randomUUID().toString().replace("-", "");
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(request.uploadUrl).openConnection();
            connection.setConnectTimeout(20_000);
            connection.setReadTimeout(120_000);
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", request.authorization);
            connection.setRequestProperty("X-Tenant-Id", request.tenantId);
            connection.setRequestProperty("X-Request-Id", UUID.randomUUID().toString());
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream output = connection.getOutputStream()) {
                writeTextPart(output, boundary, "accessPolicy", request.accessPolicy);
                writeFilePart(output, boundary, "file", safeFileName(fileName), "image/png", pngBytes);
                writeUtf8(output, "--" + boundary + "--\r\n");
            }

            int httpCode = connection.getResponseCode();
            String responseText = readResponseText(httpCode >= 200 && httpCode < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream());
            if (httpCode < 200 || httpCode >= 300) {
                throw new UploadException("NATIVE_UPLOAD_HTTP_FAILED",
                        "文件上传失败，HTTP " + httpCode + responseMessage(responseText));
            }

            String fileId = parseFileId(responseText);
            return new UploadResult(fileId, sha256Hex, "PNG", formatNow());
        } catch (UploadException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new UploadException("NATIVE_UPLOAD_IO_FAILED", "文件上传网络异常", ex);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String parseFileId(String responseText) throws UploadException {
        try {
            JSONObject response = new JSONObject(responseText);
            String code = trim(response.optString("code", "0"));
            if (code != null && !"0".equals(code)) {
                String message = trim(response.optString("message", ""));
                throw new UploadException("NATIVE_UPLOAD_BUSINESS_FAILED",
                        message == null ? "文件上传失败" : message);
            }
            JSONObject data = response.optJSONObject("data");
            if (data == null || data.isNull("id")) {
                throw new UploadException("NATIVE_UPLOAD_RESPONSE_INVALID", "文件上传成功但未返回文件 ID");
            }
            String fileId = String.valueOf(data.get("id"));
            if (trim(fileId) == null) {
                throw new UploadException("NATIVE_UPLOAD_RESPONSE_INVALID", "文件上传成功但文件 ID 为空");
            }
            return fileId;
        } catch (JSONException ex) {
            throw new UploadException("NATIVE_UPLOAD_RESPONSE_INVALID", "文件上传响应不是合法 JSON", ex);
        }
    }

    private String sha256Hex(byte[] bytes) throws UploadException {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                builder.append(String.format(Locale.US, "%02x", value & 0xff));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new UploadException("NATIVE_UPLOAD_HASH_FAILED", "SHA-256 不可用", ex);
        }
    }

    private void writeTextPart(OutputStream output, String boundary, String name, String value) throws IOException {
        writeUtf8(output, "--" + boundary + "\r\n");
        writeUtf8(output, "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
        writeUtf8(output, value == null ? "" : value);
        writeUtf8(output, "\r\n");
    }

    private void writeFilePart(
            OutputStream output,
            String boundary,
            String name,
            String fileName,
            String contentType,
            byte[] content
    ) throws IOException {
        writeUtf8(output, "--" + boundary + "\r\n");
        writeUtf8(output, "Content-Disposition: form-data; name=\"" + name
                + "\"; filename=\"" + fileName + "\"\r\n");
        writeUtf8(output, "Content-Type: " + contentType + "\r\n\r\n");
        output.write(content);
        writeUtf8(output, "\r\n");
    }

    private void writeUtf8(OutputStream output, String value) throws IOException {
        output.write(value.getBytes(StandardCharsets.UTF_8));
    }

    private String readResponseText(InputStream input) throws IOException {
        if (input == null) {
            return "";
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int read;
        while ((read = input.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toString(StandardCharsets.UTF_8.name());
    }

    private String responseMessage(String responseText) {
        if (responseText == null || responseText.trim().isEmpty()) {
            return "";
        }
        try {
            JSONObject response = new JSONObject(responseText);
            String message = trim(response.optString("message", ""));
            return message == null ? "" : "：" + message;
        } catch (JSONException ignored) {
            return "";
        }
    }

    private String safeFileName(String fileName) {
        String name = trim(fileName);
        if (name == null) {
            return "native-upload.png";
        }
        return name.replace("\\", "_")
                .replace("/", "_")
                .replace("\"", "_")
                .replace("\r", "_")
                .replace("\n", "_");
    }

    static String trim(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    static String formatNow() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).format(new Date());
    }
}
