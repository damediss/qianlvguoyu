$ErrorActionPreference = "Continue"

function Invoke-ToolCheck {
  param(
    [Parameter(Mandatory = $true)][string]$Name,
    [Parameter(Mandatory = $true)][string]$Command,
    [string[]]$Arguments = @()
  )

  $resolved = Get-Command $Command -ErrorAction SilentlyContinue
  if (-not $resolved) {
    Write-Host ("[MISS] {0}: command not found: {1}" -f $Name, $Command) -ForegroundColor Yellow
    return [pscustomobject]@{
      Name = $Name
      Status = "MISSING"
      Detail = "command not found: $Command"
    }
  }

  try {
    $output = & $resolved.Source @Arguments 2>&1 | Select-Object -First 3
    $detail = ($output | ForEach-Object { $_.ToString().Trim() } | Where-Object { $_ }) -join " | "
    if ([string]::IsNullOrWhiteSpace($detail)) {
      $detail = "command returned no output"
    }
    Write-Host ("[OK]   {0}: {1}" -f $Name, $detail) -ForegroundColor Green
    return [pscustomobject]@{
      Name = $Name
      Status = "OK"
      Detail = $detail
    }
  } catch {
    Write-Host ("[FAIL] {0}: {1}" -f $Name, $_.Exception.Message) -ForegroundColor Red
    return [pscustomobject]@{
      Name = $Name
      Status = "FAILED"
      Detail = $_.Exception.Message
    }
  }
}

Write-Host "Checking local AI / MCP development environment..."
Write-Host ""

$checks = @(
  @{ Name = "Docker"; Command = "docker"; Arguments = @("--version") },
  @{ Name = "Docker Compose"; Command = "docker"; Arguments = @("compose", "version") },
  @{ Name = "Git"; Command = "git"; Arguments = @("--version") },
  @{ Name = "Node.js"; Command = "node"; Arguments = @("--version") },
  @{ Name = "npm"; Command = "npm"; Arguments = @("--version") },
  @{ Name = "Java"; Command = "java"; Arguments = @("-version") },
  @{ Name = "Maven"; Command = "mvn"; Arguments = @("-version") },
  @{ Name = "winget"; Command = "winget"; Arguments = @("--version") }
)

$results = foreach ($check in $checks) {
  Invoke-ToolCheck @check
}

Write-Host ""
$okCount = @($results | Where-Object { $_.Status -eq "OK" }).Count
$missingCount = @($results | Where-Object { $_.Status -ne "OK" }).Count
Write-Host ("Summary: {0} OK, {1} missing or failed." -f $okCount, $missingCount)

if ($missingCount -gt 0) {
  Write-Host ""
  Write-Host "Missing tools do not stop this script. Install the missing items, then run this check again." -ForegroundColor Yellow
  Write-Host "Project-local hints:"
  Write-Host "- JDK 21: D:\Program Files\Java\oracleJdk-21"
  Write-Host "- Maven: C:\Program Files\Java\apache-maven-3.9.11\bin\mvn.cmd"
  Write-Host "- Node.js: D:\Program Files\nodejs"
}
