# Training E2E API test: demand -> train info -> sign-up -> people
$ErrorActionPreference = 'Continue'
$Base = 'http://localhost:8080/api'
$results = @()

function Test-Step {
  param([string]$Name, [scriptblock]$Action)
  try {
    $data = & $Action
    $detail = if ($null -eq $data) { 'ok' } else { ($data | Out-String).Trim() }
    $script:results += [pscustomobject]@{ Step = $Name; Status = 'PASS'; Detail = $detail }
    Write-Host "[PASS] $Name" -ForegroundColor Green
    return $data
  } catch {
    $msg = $_.Exception.Message
    if ($_.ErrorDetails.Message) { $msg = $_.ErrorDetails.Message }
    $script:results += [pscustomobject]@{ Step = $Name; Status = 'FAIL'; Detail = $msg }
    Write-Host "[FAIL] $Name : $msg" -ForegroundColor Red
    return $null
  }
}

function Invoke-Api {
  param(
    [string]$Method = 'GET',
    [string]$Path,
    $Body = $null,
    [string]$Token = $null,
    [string]$TenantId = $null
  )
  if (-not $TenantId) { $TenantId = $script:TenantId }
  $headers = @{
    'Content-Type' = 'application/json'
    'X-Tenant-Id' = $TenantId
  }
  if ($Token) { $headers['Authorization'] = "Bearer $Token" }
  $uri = "$Base$Path"
  $params = @{ Method = $Method; Uri = $uri; Headers = $headers }
  if ($Body -ne $null) { $params['Body'] = ($Body | ConvertTo-Json -Depth 10 -Compress) }
  $resp = Invoke-WebRequest @params -UseBasicParsing
  $json = $resp.Content | ConvertFrom-Json
  if ($json.code -and $json.code -ne '0' -and $json.code -ne 0) {
    throw "$Method $Path => code=$($json.code) msg=$($json.message)"
  }
  return $json.data
}

Write-Host "`n=== Training E2E API Test ===" -ForegroundColor Cyan

$login = Test-Step '01 login' {
  Invoke-Api -Method POST -Path '/auth/login' -Body @{
    tenantCode = 'demo'; username = 'admin'; password = 'Admin@123456'
  }
}
if (-not $login) { $results | Format-Table -AutoSize; exit 1 }
$token = $login.accessToken
$script:TenantId = [string]$login.tenantId

Test-Step '02 demand-trains list' {
  $d = Invoke-Api -Path '/training/demand-trains?page=1&pageSize=5' -Token $token
  "total=$($d.total)"
}

Test-Step '03 demand-trains pendings' {
  $d = Invoke-Api -Path '/training/demand-trains/pendings?page=1&pageSize=5&del=0' -Token $token
  "total=$($d.total)"
}

$publicData = Test-Step '04 public-data' {
  $d = Invoke-Api -Path '/training/demand-trains/public-data?tags=company&tags=worktype&tags=traininfo&tags=condition' -Token $token
  "companies=$($d.company_list.Count) worktypes=$($d.worktype_list.Count)"
}

$companyId = 100
$workTypeId = 2
if ($publicData) {
  if ($publicData.company_list -and $publicData.company_list.Count -gt 0) {
    $companyId = [long]$publicData.company_list[0].id
  }
  if ($publicData.worktype_list -and $publicData.worktype_list.Count -gt 0) {
    $opts = $publicData.worktype_list[0].options
    if ($opts -and $opts.Count -gt 0) { $workTypeId = [long]$opts[0].id }
  }
}

$trainInfo = Test-Step '05 demand convert to train info' {
  $title = "E2E-TrainInfo-$(Get-Date -Format 'yyyyMMddHHmmss')"
  $d = Invoke-Api -Method POST -Path '/training/demand-trains/convert' -Token $token -Body @{
    title = $title; companyId = $companyId; workTypeId = $workTypeId
    workTypeLevel = 0; num = 10; traindemandId = 10001
  }
  $script:createdTrainInfoId = [long]$d.id
  "trainInfoId=$($d.id)"
}

$trainInfoId = $script:createdTrainInfoId
if (-not $trainInfoId) {
  Test-Step '05b fallback info-lists' {
    $d = Invoke-Api -Path '/training/info-lists?page=1&pageSize=1' -Token $token
    if ($d.items -and $d.items.Count -gt 0) {
      $script:createdTrainInfoId = [long]$d.items[0].id
      "fallback=$($script:createdTrainInfoId)"
    } else { throw 'no train info' }
  }
  $trainInfoId = $script:createdTrainInfoId
}

Test-Step '06 info-lists detail' {
  $d = Invoke-Api -Path "/training/info-lists/$trainInfoId" -Token $token
  "id=$($d.id)"
}

Test-Step '07 solutions list' {
  $d = Invoke-Api -Path '/training/solutions/list?page=1&pageSize=5' -Token $token
  "total=$($d.total)"
}

$signUp = Test-Step '08 create sign-up plan' {
  $title = "E2E-SignUp-$(Get-Date -Format 'HHmmss')"
  $d = Invoke-Api -Method POST -Path '/training/sign-ups' -Token $token -Body @{
    title = $title
    trainTime = '2026-06-01 to 2026-06-30'
    examTime = '2026-07-01'
    orgId = $companyId
    trainInfoId = $trainInfoId
    sortNumber = 0
  }
  $script:signUpId = [long]$d.id
  "signUpId=$($d.id)"
}

$signUpId = $script:signUpId
if (-not $signUpId) { $results | Format-Table -Wrap; exit 1 }

Test-Step '09 sign-ups collection' {
  $d = Invoke-Api -Path '/training/sign-ups/collection?page=1&pageSize=5' -Token $token
  "total=$($d.total)"
}

Test-Step '10 sign-up qrcode' {
  $d = Invoke-Api -Path "/training/sign-ups/$signUpId/qrcode" -Token $token
  "path=$($d.path)"
}

$person = Test-Step '11 create sign-up person' {
  $d = Invoke-Api -Method POST -Path '/training/sign-up-people' -Token $token -Body @{
    signUpId = $signUpId
    name = 'E2E Tester'
    phone = '13800138001'
    idCardNumber = '450108199001011234'
    education = 'college'
    status = 0
  }
  $script:personId = [long]$d.id
  "personId=$($d.id)"
}

$personId = $script:personId
if ($personId) {
  Test-Step '12 sign-up person detail' {
    $d = Invoke-Api -Path "/training/sign-up-people/$personId" -Token $token
    "status=$($d.status)"
  }

  Test-Step '13 batch approve status' {
    $d = Invoke-Api -Method PUT -Path '/training/sign-up-people/batch' -Token $token -Body @{
      ids = @($personId); field = 'status'; value = 1
    }
    "updated=$d"
  }

  Test-Step '14 update person hasPay' {
    $d = Invoke-Api -Method PUT -Path "/training/sign-up-people/$personId" -Token $token -Body @{
      name = 'E2E Tester'; phone = '13800138001'; status = 1; hasPay = 1
    }
    "hasPay=$($d.hasPay)"
  }
}

Test-Step '15 sign-up people list' {
  $d = Invoke-Api -Path "/training/sign-up-people?signUpId=$signUpId&page=1&pageSize=10" -Token $token
  "total=$($d.total)"
}

Test-Step '16 import students candidates' {
  $d = Invoke-Api -Path '/training/sign-up-people/import/students?page=1&pageSize=5' -Token $token
  "total=$($d.total)"
}

Test-Step '17 platform-users list' {
  $d = Invoke-Api -Path '/training/platform-users?page=1&pageSize=5' -Token $token
  "total=$($d.total)"
}

Write-Host "`n=== Summary ===" -ForegroundColor Cyan
$results | Format-Table -AutoSize -Wrap
$fail = ($results | Where-Object { $_.Status -eq 'FAIL' }).Count
$pass = ($results | Where-Object { $_.Status -eq 'PASS' }).Count
Write-Host "PASS: $pass  FAIL: $fail  TOTAL: $($results.Count)"
exit $(if ($fail -gt 0) { 1 } else { 0 })
