package com.guoyu.platform.competition.exam.realtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.util.StringUtils;

/**
 * Redis fan-out envelope. Local hub delivery uses the raw Outbox JSON; Redis
 * messages carry publisherNodeId so the publishing node can ignore the loopback.
 */
public final class FormalExamRealtimeBusMessages {

  private FormalExamRealtimeBusMessages() {
  }

  public record Parsed(String publisherNodeId, FormalExamRealtimeEnvelope envelope, String eventJson) {
  }

  public static String wrap(ObjectMapper objectMapper, String publisherNodeId, String payloadJson)
      throws Exception {
    JsonNode event = objectMapper.readTree(payloadJson);
    ObjectNode root = objectMapper.createObjectNode();
    if (StringUtils.hasText(publisherNodeId)) {
      root.put("publisherNodeId", publisherNodeId);
    }
    root.set("event", event);
    return objectMapper.writeValueAsString(root);
  }

  public static Parsed parse(ObjectMapper objectMapper, String json) throws Exception {
    JsonNode root = objectMapper.readTree(json);
    if (root != null && root.isObject() && root.hasNonNull("event") && root.has("publisherNodeId")) {
      JsonNode eventNode = root.get("event");
      String eventJson = objectMapper.writeValueAsString(eventNode);
      FormalExamRealtimeEnvelope envelope =
          objectMapper.treeToValue(eventNode, FormalExamRealtimeEnvelope.class);
      String publisherNodeId = root.get("publisherNodeId").asText(null);
      return new Parsed(publisherNodeId, envelope, eventJson);
    }
    FormalExamRealtimeEnvelope envelope =
        objectMapper.readValue(json, FormalExamRealtimeEnvelope.class);
    return new Parsed(null, envelope, json);
  }

  public static boolean shouldSkipLocalEcho(String publisherNodeId, String currentNodeId) {
    return StringUtils.hasText(publisherNodeId)
        && StringUtils.hasText(currentNodeId)
        && publisherNodeId.equals(currentNodeId);
  }
}
