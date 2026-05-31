#!/usr/bin/env python3
"""Resolve a Railway service public HTTPS URL (for deploy-railway.yml)."""
from __future__ import annotations

import argparse
import json
import subprocess
import sys


def _parse_domain(payload: object) -> str:
    if isinstance(payload, str):
        value = payload.strip().rstrip("/")
        for prefix in ("https://", "http://"):
            if value.startswith(prefix):
                value = value[len(prefix) :]
        return value.rstrip("/")

    if not isinstance(payload, dict):
        return ""

    for key in ("domain", "host", "hostname", "url"):
        raw = payload.get(key)
        if isinstance(raw, str) and raw.strip():
            return _parse_domain(raw)

    for key in ("serviceDomain", "publicDomain"):
        raw = payload.get(key)
        if isinstance(raw, str) and raw.strip():
            return _parse_domain(raw)

    nested = payload.get("data")
    if isinstance(nested, dict):
        return _parse_domain(nested)

    return ""


def get_url(service: str, *, optional: bool = False) -> str:
    commands = [
        ["railway", "domain", "--service", service, "--json", "-y"],
        ["railway", "domain", "--service", service, "--port", "8080", "--json", "-y"],
    ]

    for command in commands:
        result = subprocess.run(command, capture_output=True, text=True, check=False)
        if result.returncode != 0:
            continue
        stdout = (result.stdout or "").strip()
        if not stdout:
            continue
        try:
            domain = _parse_domain(json.loads(stdout))
        except json.JSONDecodeError:
            domain = _parse_domain(stdout)
        if domain:
            return f"https://{domain}"

    if optional:
        return ""

    print(
        f"::error::Could not resolve Railway URL for service '{service}'. "
        "Deploy the service and ensure it has a public *.up.railway.app domain.",
        file=sys.stderr,
    )
    sys.exit(1)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--service", required=True)
    parser.add_argument(
        "--optional",
        action="store_true",
        help="Return empty string instead of failing when no domain exists yet.",
    )
    args = parser.parse_args()
    print(get_url(args.service, optional=args.optional))


if __name__ == "__main__":
    main()
