#!/usr/bin/env python3
"""Set Eureka hostname vars on a Railway service after deploy."""
from __future__ import annotations

import argparse
import subprocess
import sys


def _host_from_url(url: str) -> str:
    host = url.strip()
    for prefix in ("https://", "http://"):
        if host.startswith(prefix):
            host = host[len(prefix) :]
    return host.rstrip("/")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--service", required=True)
    parser.add_argument("--url", required=True)
    args = parser.parse_args()

    hostname = _host_from_url(args.url)
    if not hostname:
        print(f"::error::Invalid service URL for {args.service}: {args.url!r}", file=sys.stderr)
        sys.exit(1)

    result = subprocess.run(
        [
            "railway",
            "variables",
            "set",
            f"EUREKA_INSTANCE_HOSTNAME={hostname}",
            "EUREKA_INSTANCE_NON_SECURE_PORT_ENABLED=false",
            "EUREKA_INSTANCE_SECURE_PORT_ENABLED=true",
            "EUREKA_INSTANCE_SECURE_PORT=443",
            "--service",
            args.service,
        ],
        check=False,
    )
    if result.returncode != 0:
        print(f"::error::Failed to patch Eureka hostname on {args.service}.", file=sys.stderr)
        sys.exit(result.returncode)


if __name__ == "__main__":
    main()
