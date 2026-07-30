# Workspace Directory Rules

## Canonical development directory

- The only project development workspace is:
  `D:\qianlvguoyu\qianlvguoyu_saas-dev`
- Application source files currently live under:
  `D:\qianlvguoyu\qianlvguoyu_saas-dev\qianlvguoyu_saas-dev-c0c8c9aec83ff243d28d42cf263e4f6deaf1bd5b`
- Keep all code reads, searches, edits, builds, and generated files inside
  `D:\qianlvguoyu\qianlvguoyu_saas-dev` unless the user explicitly names
  another directory.

## Forbidden duplicate directory

- Never edit, format, generate files in, or use as a fallback repository:
  `D:\qianlvguoyu\qianlvguoyu_saas-git-dev`
- Do not select that directory merely because it contains `.git`.
- Treat it as a stale duplicate containing historical accidental changes.
- If a task requires Git metadata that is unavailable in the canonical
  workspace, report that fact instead of switching directories.

Before changing a file, verify that its resolved absolute path starts with
`D:\qianlvguoyu\qianlvguoyu_saas-dev\`.
