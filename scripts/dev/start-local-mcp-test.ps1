$ErrorActionPreference = "Continue"

$Root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Set-Location $Root

function Test-RepoPath {
  param([Parameter(Mandatory = $true)][string]$Path)
  return Test-Path -LiteralPath (Join-Path $Root $Path)
}

function Write-Step {
  param(
    [Parameter(Mandatory = $true)][int]$Number,
    [Parameter(Mandatory = $true)][string]$Title
  )
  Write-Host ""
  Write-Host ("{0}. {1}" -f $Number, $Title) -ForegroundColor Cyan
}

Write-Host "Qianlv Guoyu local AI / MCP test prompt script"
Write-Host ("Repo: {0}" -f $Root)
Write-Host "This script prints the recommended sequence only. It does not force-start every service."

Write-Step 1 "Start database / Redis / dependencies"
if (Test-RepoPath "infra\docker\docker-compose.platform-api-dev.yml") {
  Write-Host "Suggested:"
  Write-Host "docker compose -f infra/docker/docker-compose.platform-api-dev.yml up -d"
} else {
  Write-Host "No local docker compose file was detected. Start MySQL/Redis according to your local environment."
}

Write-Step 2 "Start platform-api"
if (Test-RepoPath "services\platform-api\pom.xml") {
  Write-Host "Suggested Maven command:"
  Write-Host '$env:JAVA_HOME = "D:\Program Files\Java\oracleJdk-21"'
  Write-Host '& "C:\Program Files\Java\apache-maven-3.9.11\bin\mvn.cmd" spring-boot:run -f services/platform-api/pom.xml -s scripts/custom-maven-settings.xml'
  Write-Host "Health check:"
  Write-Host "http://localhost:8080/api/actuator/health"
} else {
  Write-Host "services/platform-api/pom.xml was not detected. Use the actual backend startup command."
}

Write-Step 3 "Test platform-api with Postman / IDEA HTTP Client"
Write-Host "Postman Collection:"
Write-Host "docs/postman/qianlvguoyu-platform-api-local.postman_collection.json"
Write-Host "IDEA HTTP file:"
Write-Host "docs/http/platform-api-local-test.http"
Write-Host "Fill variables: baseUrl, token, tenantId, taskId, candidateId."

Write-Step 4 "Start MCP Server"
if (Test-RepoPath "services\mcp-server\package.json") {
  Write-Host "Suggested:"
  Write-Host "cd services/mcp-server"
  Write-Host "npm install"
  Write-Host "npm run build"
  Write-Host "npm start"
  Write-Host "Health check: http://localhost:3001/health"
  Write-Host "Required env vars are listed in services/mcp-server/.env.example."
} else {
  Write-Host "services/mcp-server/package.json was not detected. Use the actual MCP Server startup command."
}

Write-Step 5 "Test tools/list and tools/call with MCP Inspector"
Write-Host "Suggested:"
Write-Host ".\scripts\dev\start-mcp-inspector.ps1"
Write-Host "Common addresses:"
Write-Host "- http://localhost:3001/mcp"
Write-Host "- http://localhost:3001/sse"
Write-Host "- http://host.docker.internal:3001/mcp"

Write-Step 6 "Start local WeKnora"
Write-Host "Recommended external path:"
Write-Host "D:/qianlvguoyu/tools/WeKnora"
Write-Host "Suggested:"
Write-Host "git clone https://github.com/Tencent/WeKnora.git D:/qianlvguoyu/tools/WeKnora"
Write-Host "cd D:/qianlvguoyu/tools/WeKnora"
Write-Host "cp .env.example .env"
Write-Host "docker compose up -d"

Write-Step 7 "Configure MCP Server in WeKnora"
Write-Host "If WeKnora runs in Docker and MCP Server runs on Windows host, use:"
Write-Host "http://host.docker.internal:3001/mcp"
Write-Host "Do not use http://localhost:3001/mcp inside the WeKnora container unless MCP Server is in the same container/network."

Write-Step 8 "Run WeKnora Agent Q&A tests"
Write-Host "Knowledge-only: What should be done after importing the practical scoring table?"
Write-Host "MCP-only: What stage is certification task T001 currently in?"
Write-Host "Knowledge + MCP: Can task T001 start tablet scoring now? If not, why, and what is the next action?"

Write-Host ""
Write-Host "Detailed flow: docs/mcp-local-test-flow.md"
