param(
  [Parameter(Position = 0)]
  [ValidateSet('start', 'status', 'stop', 'fault-redis', 'restart-api-1', 'restart-api-2', 'fault-rabbit')]
  [string]$Command = 'status'
)
Set-Location $PSScriptRoot\..
node .\ctl.mjs $Command
