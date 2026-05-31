#!/usr/bin/env python3
"""Resolve a Railway service public HTTPS URL (for deploy-railway.yml)."""
from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
import time

RAILWAY_DOMAIN_RE = re.compile(
    r"(?:https?://)?([a-zA-Z0-9-]+\.up\.railway\.app)",
    re.IGNORECASE,
)


def _parse_domain(payload: object) -> str:
    if isinstance(payload, str):
        match = RAILWAY_DOMAIN_RE.search(payload.strip())
        if match:
            return match.group(1).rstrip("/")
        value = payload.strip().rstrip("/")
        for prefix in ("https://", "http://"):
            if value.startswith(prefix):
                value = value[len(prefix) :]
        return value.rstrip("/")

    if isinstance(payload, list):
        for item in payload:
            domain = _parse_domain(item)
            if domain:
                return domain
        return ""

    if not isinstance(payload, dict):
        return ""

    for key in ("domain", "host", "hostname", "url", "serviceDomain", "publicDomain"):
        raw = payload.get(key)
        if isinstance(raw, str) and raw.strip():
            domain = _parse_domain(raw)
            if domain:
                return domain

    for key in ("domains", "serviceDomains", "customDomains"):
        raw = payload.get(key)
        if isinstance(raw, list):
            domain = _parse_domain(raw)
            if domain:
                return domain

    nested = payload.get("data")
    if isinstance(nested, dict):
        return _parse_domain(nested)

    return ""


def _run_domain_command(service: str, *, port: int | None, json_output: bool) -> tuple[int, str, str]:
    command = ["railway", "domain", "--service", service, "-y"]
    if port is not None:
        command.extend(["--port", str(port)])
    if json_output:
        command.append("--json")
    result = subprocess.run(command, capture_output=True, text=True, check=False)
    stdout = (result.stdout or "").strip()
    stderr = (result.stderr or "").strip()
    combined = "\n".join(part for part in (stdout, stderr) if part)
    return result.returncode, combined, stderr


def get_url(
    service: str,
    *,
    optional: bool = False,
    retries: int = 12,
    retry_delay_seconds: float = 5.0,
) -> str:
    attempts = [
        (8080, True),
        (8080, False),
        (None, True),
        (None, False),
    ]

    last_stderr = ""
    for attempt in range(retries):
        for port, json_output in attempts:
            returncode, combined, stderr = _run_domain_command(service, port=port, json_output=json_output)
            if stderr:
                last_stderr = stderr

            if not combined:
                continue

            domain = ""
            if json_output:
                try:
                    domain = _parse_domain(json.loads(combined))
                except json.JSONDecodeError:
                    domain = _parse_domain(combined)
            else:
                domain = _parse_domain(combined)

            if domain:
                return f"https://{domain}"

        if attempt < retries - 1:
            time.sleep(retry_delay_seconds)

    if optional:
        return ""

    if last_stderr:
        print(f"::warning::railway domain stderr for '{service}': {last_stderr}", file=sys.stderr)

    print(
        f"::error::Could not resolve Railway URL for service '{service}'. "
        "Ensure the service deployed successfully and has a public *.up.railway.app domain "
        "(Networking → Generate Domain in Railway, or let this workflow create one).",
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
