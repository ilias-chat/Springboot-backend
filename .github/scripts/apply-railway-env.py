#!/usr/bin/env python3
"""Apply env JSON (from write-cloudrun-env.py) to a Railway service."""
from __future__ import annotations

import argparse
import json
import subprocess
import sys


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--service", required=True)
    parser.add_argument("--file", required=True)
    args = parser.parse_args()

    with open(args.file, encoding="utf-8") as handle:
        payload = json.load(handle)

    if not isinstance(payload, dict):
        print("::error::Env file must be a JSON object.", file=sys.stderr)
        sys.exit(1)

    for key, value in payload.items():
        if value is None:
            continue
        text = str(value)
        result = subprocess.run(
            ["railway", "variables", "set", f"{key}={text}", "--service", args.service],
            check=False,
        )
        if result.returncode != 0:
            print(f"::error::Failed to set Railway variable {key} on {args.service}.", file=sys.stderr)
            sys.exit(result.returncode)


if __name__ == "__main__":
    main()
