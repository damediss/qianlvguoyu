#Requires -RunAsAdministrator
$ErrorActionPreference = 'Continue'
$paths = @(
  'D:\qianlvguoyu\qianlvguoyu_saas-dev\gjjd_appnext',
  'D:\Program Files (x86)\HBuilder X',
  'D:\nodejs'
)
$procs = @('node.exe','esbuild.exe','HBuilderX.exe')
Write-Host 'Adding Windows Defender exclusions...' -ForegroundColor Cyan
foreach ($p in $paths) {
  try { Add-MpPreference -ExclusionPath $p; Write-Host "OK path: $p" -ForegroundColor Green }
  catch { Write-Host "FAIL path: $p :: $($_.Exception.Message)" -ForegroundColor Red }
}
foreach ($p in $procs) {
  try { Add-MpPreference -ExclusionProcess $p; Write-Host "OK proc: $p" -ForegroundColor Green }
  catch { Write-Host "FAIL proc: $p :: $($_.Exception.Message)" -ForegroundColor Red }
}
$pref = Get-MpPreference
Write-Host "
Current matching exclusions:" -ForegroundColor Cyan
$pref.ExclusionPath | Where-Object { $_ -match 'qianlvguoyu|HBuilder|nodejs' }
$pref.ExclusionProcess | Where-Object { $_ -match 'node|esbuild|HBuilder' }
Write-Host "
Done. You can close this window." -ForegroundColor Yellow
pause