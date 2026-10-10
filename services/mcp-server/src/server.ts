import { randomUUID, timingSafeEqual } from "node:crypto";
import express, { type NextFunction, type Request, type Response } from "express";
import { SSEServerTransport } from "@modelcontextprotocol/sdk/server/sse.js";
import { StreamableHTTPServerTransport } from "@modelcontextprotocol/sdk/server/streamableHttp.js";
import { isInitializeRequest } from "@modelcontextprotocol/sdk/types.js";
import { loadConfig } from "./config.js";
import { log, sanitizeError } from "./logger.js";
import { PlatformClient } from "./platformClient.js";
import { createCertificationMcpServer } from "./tools.js";
import { decodeBotActorContext } from "./context/botActorContext.js";

const config = loadConfig();
const platformClient = new PlatformClient(config);
const app = express();
app.use(express.json({ limit: "1mb" }));

type ServerTransport = StreamableHTTPServerTransport | SSEServerTransport;
const transports: Record<string, ServerTransport> = {};

app.get("/health", (_req, res) => {
  res.json({
    status: "UP",
    service: "certification-data-assistant"
  });
});

// Internal binary bridge for WeKnora's delivery worker. Binary bytes never enter an MCP tool
// result or model context; this route only reuses the MCP service's platform credentials and
// signs the verified actor context before streaming the platform-file response.
app.get(
  "/internal/artifacts/platform-file/:fileId/content",
  requireBearerToken,
  async (req: Request, res: Response) => {
    const startedAt = Date.now();
    try {
      const encodedActor = req.header("x-weknora-bot-actor-context") || "";
      const actor = decodeBotActorContext(encodedActor);
      const content = await platformClient.getFileArtifactContent(req.params.fileId, actor);
      res.setHeader("Content-Type", content.mimeType || "application/octet-stream");
      res.setHeader("Content-Length", String(content.size));
      res.setHeader("X-File-Id", content.fileId);
      // HTTP header values are Latin-1 constrained in Node. Base64 keeps Chinese filenames intact.
      res.setHeader("X-File-Name-Base64", Buffer.from(content.filename, "utf8").toString("base64"));
      res.setHeader("X-File-Mime-Type", content.mimeType);
      res.setHeader("X-File-Size", String(content.size));
      if (content.sha256) {
        res.setHeader("X-File-SHA256", content.sha256);
      }
      res.status(200).send(Buffer.from(content.data));
      log("info", "artifact content proxied to delivery worker", {
        fileId: content.fileId,
        requestId: actor.request_id,
        tenantId: actor.tenant_id,
        size: content.size,
        durationMs: Date.now() - startedAt
      });
    } catch (error) {
      const status = error instanceof Error && "status" in error && typeof error.status === "number"
        ? error.status
        : 400;
      log("warn", "artifact content proxy rejected request", {
        fileId: req.params.fileId,
        status,
        durationMs: Date.now() - startedAt,
        error: sanitizeError(error)
      });
      if (!res.headersSent) {
        res.status(status).json({ error: status === 400 ? "Invalid artifact request" : "Artifact fetch failed" });
      }
    }
  }
);

app.all("/mcp", requireBearerToken, async (req: Request, res: Response) => {
  const startedAt = Date.now();
  try {
    const sessionId = req.headers["mcp-session-id"] as string | undefined;
    let transport: StreamableHTTPServerTransport;

    if (sessionId && transports[sessionId] instanceof StreamableHTTPServerTransport) {
      transport = transports[sessionId] as StreamableHTTPServerTransport;
    } else if (!sessionId && req.method === "POST" && isInitializeRequest(req.body)) {
      transport = new StreamableHTTPServerTransport({
        sessionIdGenerator: () => randomUUID(),
        onsessioninitialized: (initializedSessionId) => {
          transports[initializedSessionId] = transport;
          log("info", "mcp streamable session initialized", {
            transport: "streamable-http",
            sessionId: initializedSessionId
          });
        }
      });
      const server = createCertificationMcpServer(platformClient);
      transport.onclose = () => {
        const sid = transport.sessionId;
        if (sid) {
          delete transports[sid];
        }
        void server.close();
      };
      await server.connect(transport);
    } else {
      res.status(400).json({
        jsonrpc: "2.0",
        error: {
          code: -32000,
          message: "Bad Request: no valid MCP session was provided."
        },
        id: null
      });
      return;
    }

    await transport.handleRequest(req, res, req.body);
    log("info", "mcp request handled", {
      transport: "streamable-http",
      method: req.method,
      status: res.statusCode,
      durationMs: Date.now() - startedAt
    });
  } catch (error) {
    log("error", "mcp streamable request failed", {
      transport: "streamable-http",
      method: req.method,
      durationMs: Date.now() - startedAt,
      error: sanitizeError(error)
    });
    if (!res.headersSent) {
      res.status(500).json({
        jsonrpc: "2.0",
        error: {
          code: -32603,
          message: "Internal server error"
        },
        id: null
      });
    }
  }
});

app.get("/sse", requireBearerToken, async (_req: Request, res: Response) => {
  const transport = new SSEServerTransport("/messages", res);
  transports[transport.sessionId] = transport;
  const server = createCertificationMcpServer(platformClient);
  res.on("close", () => {
    delete transports[transport.sessionId];
    void server.close();
  });
  await server.connect(transport);
  log("info", "mcp sse session initialized", {
    transport: "sse",
    sessionId: transport.sessionId
  });
});

app.post("/messages", requireBearerToken, async (req: Request, res: Response) => {
  const sessionId = req.query.sessionId as string | undefined;
  const existingTransport = sessionId ? transports[sessionId] : undefined;
  if (!(existingTransport instanceof SSEServerTransport)) {
    res.status(400).json({
      jsonrpc: "2.0",
      error: {
        code: -32000,
        message: "Bad Request: no SSE transport found for sessionId."
      },
      id: null
    });
    return;
  }
  await existingTransport.handlePostMessage(req, res, req.body);
});

app.use((error: unknown, _req: Request, res: Response, _next: NextFunction) => {
  log("error", "unhandled http error", {
    error: sanitizeError(error)
  });
  if (!res.headersSent) {
    res.status(500).json({
      error: "Internal server error"
    });
  }
});

const httpServer = app.listen(config.port, () => {
  log("info", "certification MCP server started", {
    port: config.port,
    streamableHttpPath: "/mcp",
    ssePath: "/sse"
  });
});

process.on("SIGINT", () => {
  void shutdown("SIGINT");
});

process.on("SIGTERM", () => {
  void shutdown("SIGTERM");
});

async function shutdown(signal: string): Promise<void> {
  log("info", "shutting down certification MCP server", { signal });
  for (const [sessionId, transport] of Object.entries(transports)) {
    try {
      await transport.close();
      delete transports[sessionId];
    } catch (error) {
      log("warn", "failed to close MCP transport", {
        sessionId,
        error: sanitizeError(error)
      });
    }
  }
  httpServer.close(() => process.exit(0));
}

function requireBearerToken(req: Request, res: Response, next: NextFunction): void {
  const header = req.header("authorization") || "";
  const prefix = "Bearer ";
  if (!header.startsWith(prefix) || !tokensEqual(header.slice(prefix.length), config.inboundBearerToken)) {
    log("warn", "mcp authentication failed", {
      path: req.path,
      method: req.method
    });
    res.status(401).json({
      error: "Unauthorized"
    });
    return;
  }
  next();
}

function tokensEqual(actual: string, expected: string): boolean {
  const actualBuffer = Buffer.from(actual);
  const expectedBuffer = Buffer.from(expected);
  if (actualBuffer.length !== expectedBuffer.length) {
    return false;
  }
  return timingSafeEqual(actualBuffer, expectedBuffer);
}
