# Map old Chinese names to new English names
$renameMap = @{
  'data闈㈡澘.html' = 'data-panel.html'
  '鏁版嵁闈㈡澘.html' = 'data-dashboard.html'
  '杩涘害璁″垝-缂栬緫.html' = 'progress-plan-edit.html'
  '杩涘害璁″垝.html' = 'progress-plan.html'
  '椤圭洰WBS-缂栬緫.html' = 'project-wbs-edit.html'
  '椤圭洰WBS.html' = 'project-wbs.html'
  '椤圭洰绠＄悊棣栭〉.html' = 'project-management-home.html'
  '椤圭洰鎴愬憳-缂栬緫.html' = 'project-members-edit.html'
  '椤圭洰鎴愬憳.html' = 'project-members.html'
  '浠诲姟妗ｆ.html' = 'task-archive.html'
  '浠诲姟閫夋嫨.html' = 'task-select.html'
  '瀵煎叆璧勬枡琛ㄥ崟.html' = 'import-material-form.html'
  '瀹炴搷鑰冭瘯.html' = 'practical-exam.html'
  '璁ゅ畾娴佺▼棣栭〉.html' = 'certification-process-home.html'
  '璁ゅ畾绛瑰-鑰冨満璧勬枡.html' = 'certification-prep-exam-material.html'
  '璁ゅ畾绛瑰.html' = 'certification-prep.html'
  '璁ゅ畾褰掓。.html' = 'certification-archive.html'
  '鎴戣涓嬪崟.html' = 'request-order.html'
  '鏁翠綋宸ヤ綔娴佺▼.md' = 'overall-workflow.md'
  '鐞嗚绗旇瘯.html' = 'theory-exam.html'
  '鐢抽琛ヨ创.html' = 'apply-subsidy.html'
  '鐢熸垚鐢虫姤鏉愭枡.html' = 'generate-application-material.html'
  '鑰冨満淇℃伅绠＄悊.html' = 'exam-site-info-management.html'
  '鑰冨満璧勬枡.html' = 'exam-site-material.html'
  '鑰冪敓璧勬枡.html' = 'examinee-material.html'
  '鑰冭瘯璁惧绠＄悊.html' = 'exam-equipment-management.html'
  '鑰冭瘯鏉愭枡.html' = 'exam-material.html'
  '閫夋嫨椤圭洰.html' = 'select-project.html'
  '1璁ゅ畾閮ㄤ骇鍝侀渶姹傛枃妗RD.md' = '1-certification-product-requirement-BRD.md'
}

$dir = ''D:\workspace\qianlvguoyu_saas\prototypes\certification''

# First, update all HTML file references
Get-ChildItem $dir -Filter *.html | ForEach-Object {
  $content = Get-Content $_.FullName -Raw
  $updated = $content
  foreach ($old in $renameMap.Keys) {
    $new = $renameMap[$old]
    $updated = $updated -Replace [regex]::Escape($old), $new
  }
  if ($updated -ne $content) {
    Set-Content $_.FullName -Value $updated -NoNewline
    Write-Output "Updated references in $($_.Name)"
  }
}

# Then rename the files
$renameMap.GetEnumerator() | ForEach-Object {
  $oldPath = Join-Path $dir $_.Key
  $newPath = Join-Path $dir $_.Value
  if (Test-Path $oldPath) {
    Rename-Item -LiteralPath $oldPath -NewName $_.Value
    Write-Output "Renamed: $($_.Key) -> $($_.Value)"
  }
}

Write-Output "Done!"

