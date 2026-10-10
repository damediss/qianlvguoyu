param(
  [Parameter(ValueFromRemainingArguments = $true)]
  [string[]]$InspectorArgs
)

$ErrorActionPreference = "Stop"

function Require-Command {
  param(
    [Parameter(Mandatory = $true)][string]$Command,
    [Parameter(Mandatory = $true)][string]$Tip
  )

  $resolved = Get-Command $Command -ErrorAction SilentlyContinue
  if (-not $resolved) {
    Write-Host ("Missing command: {0}" -f $Command) -ForegroundColor Red
    Write-Host $Tip
    exit 1
  }
  return $resolved
}

Require-Command -Command "node" -Tip "Install Node.js 20+ before running MCP Inspector." | Out-Null
Require-Command -Command "npm" -Tip "Install npm before running MCP Inspector." | Out-Null
$npx = Require-Command -Command "npx" -Tip "Install npm/npx before running MCP Inspector."

Write-Host "Starting MCP Inspector..."
Write-Host ""
Write-Host "After the Inspector UI opens, select the transport that matches the MCP Server."
Write-Host "Common local addresses:"
Write-Host "- Streamable HTTP: http://localhost:3001/mcp"
Write-Host "- SSE:             http://localhost:3001/sse"
Write-Host "- Docker to host:  http://host.docker.internal:3001/mcp"
Write-Host ""
Write-Host "If your MCP Server requires auth, configure: Authorization: Bearer <MCP_BEARER_TOKEN>"
Write-Host ""

& $npx.Source @("@modelcontextprotocol/inspector") @InspectorArgs
