#!/usr/bin/env python3
"""One-shot repair for gjjd_test Flyway schema history.

Removes failed migration records (success=0) that block application startup.
"""
from __future__ import annotations

import os
import sys

import pymysql

DEFAULT_HOST = "192.168.1.200"
DEFAULT_PORT = 3308
DEFAULT_USER = "root"
DEFAULT_PASSWORD = "guoyu_dev!513"
DEFAULT_DATABASE = "gjjd_test"


def connect():
    return pymysql.connect(
        host=os.environ.get("MYSQL_HOST", DEFAULT_HOST),
        port=int(os.environ.get("MYSQL_PORT", DEFAULT_PORT)),
        user=os.environ.get("MYSQL_USERNAME", DEFAULT_USER),
        password=os.environ.get("MYSQL_PASSWORD", DEFAULT_PASSWORD),
        database=os.environ.get("MYSQL_DATABASE", DEFAULT_DATABASE),
    )


def main() -> int:
    conn = connect()
    try:
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT installed_rank, version, script
                FROM flyway_schema_history
                WHERE success = 0
                ORDER BY installed_rank
                """
            )
            failed = cur.fetchall()
            if not failed:
                print("No failed Flyway migrations found.")
                return 0

            print("Failed migrations to remove:")
            for rank, version, script in failed:
                print(f"  rank={rank} version={version} script={script}")

            cur.execute("DELETE FROM flyway_schema_history WHERE success = 0")
            conn.commit()
            print(f"Deleted {cur.rowcount} failed record(s).")
    finally:
        conn.close()
    return 0


if __name__ == "__main__":
    sys.exit(main())
