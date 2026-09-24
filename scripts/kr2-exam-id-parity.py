# -*- coding: utf-8 -*-
"""
O4-KR2：四路 examId 金标比对

对每个 (classId, expectedExamId) 拉取：
  1) 管理端 review-sync-config
  2) App follow review-sync
  3) 助教 bootstrap
  4) 精准学 precise/dashboard

断言四路 examId 与金标一致；entryVisible=true 时 deepLink.query.exam_id 同源。

用法（PowerShell）:
  $env:SAAS_BASE = "http://127.0.0.1:8080/api"   # 或生产同库网关
  $env:ADMIN_TOKEN = "<admin JWT>"
  $env:APP_ITOKEN = "<PHP /appauth/login 签发的 itoken>"
  $env:TENANT_ID = "10159"
  # 可选：覆盖试点学员（须为班成员且在考生名单）
  $env:PILOT_UID = "2651"
  python qianlvguoyu_saas/scripts/kr2-exam-id-parity.py

退出码：0 全绿；1 有 FAIL。
"""
from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.request
from typing import Any

sys.stdout.reconfigure(encoding="utf-8")

BASE = os.environ.get("SAAS_BASE", "http://127.0.0.1:8080/api").rstrip("/")
ADMIN_TOKEN = os.environ.get("ADMIN_TOKEN", "").strip()
APP_ITOKEN = os.environ.get("APP_ITOKEN", "").strip()
TENANT_ID = os.environ.get("TENANT_ID", "10159").strip()
PILOT_UID = os.environ.get("PILOT_UID", "").strip()

# 试点金标（gjjddbpro 当前绑定）
GOLDEN = [
    (30055, 476, "中级1班"),
    (30056, 481, "中级工2班"),
    (30057, 477, "高级工1班"),
    (30058, 482, "高级工2班"),
]

PASS: list[str] = []
FAIL: list[str] = []
ROWS: list[dict[str, Any]] = []


def ok(name: str, detail: str = "") -> None:
    PASS.append(name)
    print(f"[PASS] {name}" + (f" — {detail}" if detail else ""))


def fail(name: str, detail: str = "") -> None:
    FAIL.append(name)
    print(f"[FAIL] {name}" + (f" — {detail}" if detail else ""))


def http_json(method: str, path: str, token: str) -> dict[str, Any]:
    url = f"{BASE}{path}"
    req = urllib.request.Request(
        url,
        method=method,
        headers={
            "Authorization": f"Bearer {token}",
            "X-Tenant-Id": TENANT_ID,
            "Accept": "application/json",
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=25) as resp:
            body = resp.read().decode("utf-8", errors="replace")
            return json.loads(body) if body else {}
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", errors="replace") if e.fp else ""
        try:
            data = json.loads(raw) if raw else {}
        except json.JSONDecodeError:
            data = {"message": raw[:300]}
        data["_http"] = e.code
        return data
    except Exception as e:
        return {"_error": str(e)}


def unwrap(data: dict[str, Any]) -> Any:
    if data.get("_http") or data.get("_error"):
        return None
    code = data.get("code")
    if code in (0, "0", None):
        return data.get("data")
    return None


def norm_exam(v: Any) -> str | None:
    if v is None or v == "":
        return None
    s = str(v).strip()
    return s or None


def extract_admin(data: Any) -> str | None:
    if not isinstance(data, dict):
        return None
    for key in ("examId",):
        e = norm_exam(data.get(key))
        if e:
            return e
    resolve = data.get("resolve")
    if isinstance(resolve, dict):
        return norm_exam(resolve.get("examId"))
    return None


def extract_app_sync(data: Any) -> tuple[str | None, bool | None, str | None]:
    if not isinstance(data, dict):
        return None, None, None
    exam = norm_exam(data.get("examId"))
    visible = data.get("entryVisible")
    if isinstance(visible, str):
        visible = visible.lower() == "true"
    deep = None
    dl = data.get("deepLink")
    if isinstance(dl, dict):
        q = dl.get("query") or {}
        if isinstance(q, dict):
            deep = norm_exam(q.get("exam_id") or q.get("examId"))
    return exam, visible if isinstance(visible, bool) else None, deep


def extract_bootstrap(data: Any) -> str | None:
    if not isinstance(data, dict):
        return None
    e = norm_exam(data.get("examId"))
    if e:
        return e
    summary = data.get("summary")
    if isinstance(summary, dict):
        e = norm_exam(summary.get("examId"))
        if e:
            return e
    # actions 深链兜底
    for key in ("chips", "actions"):
        arr = data.get(key)
        if not isinstance(arr, list):
            continue
        for a in arr:
            if not isinstance(a, dict):
                continue
            url = str(a.get("url") or "")
            if "exam_id=" in url:
                part = url.split("exam_id=", 1)[1]
                part = part.split("&", 1)[0]
                e = norm_exam(part)
                if e:
                    return e
    return None


def extract_precise(data: Any) -> str | None:
    if not isinstance(data, dict):
        return None
    entries = data.get("entriesSync")
    if isinstance(entries, dict):
        e = norm_exam(entries.get("examId"))
        if e:
            return e
    sync = data.get("syncEntry") or data.get("reviewSync")
    if isinstance(sync, dict):
        return norm_exam(sync.get("examId"))
    return None


def main() -> int:
    print(f"BASE={BASE} TENANT_ID={TENANT_ID}")
    if not ADMIN_TOKEN:
        fail("环境", "缺少 ADMIN_TOKEN")
    if not APP_ITOKEN:
        fail("环境", "缺少 APP_ITOKEN（须 PHP itoken，勿用 admin JWT）")
    if FAIL:
        print("\n请先设置令牌后再跑。")
        return 1

    print("\n## O4-KR2 examId 金标比对\n")
    print("| 班级 | classId | 金标 | admin | app | 助教 | 精准学 | deepLink | entryVisible |")
    print("|---|---|---|---|---|---|---|---|---|")

    for class_id, golden, name in GOLDEN:
        row: dict[str, Any] = {
            "name": name,
            "classId": class_id,
            "golden": str(golden),
        }

        admin_raw = http_json("GET", f"/training/info-classes/{class_id}/review-sync-config", ADMIN_TOKEN)
        admin_data = unwrap(admin_raw)
        admin_exam = extract_admin(admin_data)
        row["admin"] = admin_exam or (
            f"ERR:{admin_raw.get('_http') or admin_raw.get('_error') or admin_raw.get('message')}"
        )

        app_raw = http_json("GET", f"/training/follow/classes/{class_id}/review-sync", APP_ITOKEN)
        app_data = unwrap(app_raw)
        app_exam, visible, deep = extract_app_sync(app_data)
        row["app"] = app_exam or (
            f"ERR:{app_raw.get('_http') or app_raw.get('_error') or app_raw.get('message')}"
        )
        row["deepLink"] = deep
        row["entryVisible"] = visible

        boot_raw = http_json(
            "GET", f"/training/follow/classes/{class_id}/assistant/bootstrap", APP_ITOKEN
        )
        boot_data = unwrap(boot_raw)
        boot_exam = extract_bootstrap(boot_data)
        row["assistant"] = boot_exam or (
            f"ERR:{boot_raw.get('_http') or boot_raw.get('_error') or boot_raw.get('message')}"
        )

        prec_raw = http_json(
            "GET", f"/training/follow/classes/{class_id}/precise/dashboard", APP_ITOKEN
        )
        prec_data = unwrap(prec_raw)
        prec_exam = extract_precise(prec_data)
        row["precise"] = prec_exam or (
            f"ERR:{prec_raw.get('_http') or prec_raw.get('_error') or prec_raw.get('message')}"
        )

        print(
            f"| {name} | {class_id} | {golden} | {row['admin']} | {row['app']} | "
            f"{row['assistant']} | {row['precise']} | {row.get('deepLink')} | {visible} |"
        )
        ROWS.append(row)

        g = str(golden)
        label = f"{name}({class_id})"
        if admin_exam != g:
            fail(f"{label} admin==金标", f"got={admin_exam}")
        else:
            ok(f"{label} admin==金标")

        if app_exam != g:
            fail(f"{label} app review-sync==金标", f"got={app_exam}")
        else:
            ok(f"{label} app review-sync==金标")

        # 助教：学员模式才有 examId；非成员/非学员可能为空
        if boot_exam is None:
            fail(f"{label} 助教 examId", "为空（确认 APP_ITOKEN 为该班学员）")
        elif boot_exam != g:
            fail(f"{label} 助教==金标", f"got={boot_exam}")
        else:
            ok(f"{label} 助教==金标")

        if prec_exam is None:
            # entryVisible=false 时精准学可能不下发 entriesSync
            if visible is False:
                ok(f"{label} 精准学无 entriesSync（入口不可见，可接受）")
            else:
                fail(f"{label} 精准学 examId", "为空")
        elif prec_exam != g:
            fail(f"{label} 精准学==金标", f"got={prec_exam}")
        else:
            ok(f"{label} 精准学==金标")

        if visible is True:
            if deep != g:
                fail(f"{label} deepLink.exam_id", f"got={deep}")
            else:
                ok(f"{label} deepLink 同源")
        elif visible is False:
            ok(f"{label} entryVisible=false（核对名单/灰度）", f"reason 见 API")

    if PILOT_UID:
        print(f"\nPILOT_UID={PILOT_UID}（仅备注，本脚本不按 uid 鉴权；依赖 APP_ITOKEN 主体）")

    print("\n========== SUMMARY ==========")
    print(f"PASS {len(PASS)}  FAIL {len(FAIL)}")
    if FAIL:
        print("Failed:")
        for x in FAIL:
            print(" -", x)
        return 1
    print("All KR2 examId parity checks passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
