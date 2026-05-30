#!/usr/bin/env python3
"""Build Cloud Run env JSON for DWSC microservices (used by cd.yml)."""
from __future__ import annotations

import argparse
import json
import os
import sys


def _strip(s: str | None) -> str:
    return (s or "").strip()


def _host_from_url(url: str) -> str:
    u = url.strip()
    for prefix in ("https://", "http://"):
        if u.startswith(prefix):
            u = u[len(prefix) :]
    return u.rstrip("/")


def _eureka_instance(hostname: str) -> dict[str, str]:
    return {
        "EUREKA_INSTANCE_HOSTNAME": hostname,
        "EUREKA_INSTANCE_NON_SECURE_PORT_ENABLED": "false",
        "EUREKA_INSTANCE_SECURE_PORT_ENABLED": "true",
        "EUREKA_INSTANCE_SECURE_PORT": "443",
    }


def _firebase(out: dict[str, str]) -> None:
    fb = _strip(os.environ.get("FIREBASE_SERVICE_ACCOUNT_JSON"))
    if not fb:
        print("::warning::FIREBASE_SERVICE_ACCOUNT_JSON is not set.")
        return
    try:
        json.loads(fb)
    except json.JSONDecodeError as exc:
        print(f"::error::FIREBASE_SERVICE_ACCOUNT_JSON is not valid JSON: {exc}")
        sys.exit(1)
    out["FIREBASE_SERVICE_ACCOUNT_JSON"] = fb


def _api_football(out: dict[str, str]) -> None:
    af = _strip(os.environ.get("API_FOOTBALL_KEY"))
    if af.upper().startswith("API_FOOTBALL_KEY="):
        af = af.split("=", 1)[1].strip()
        print("::warning::API_FOOTBALL_KEY had a prefix; stripped for Cloud Run.")
    if af:
        out["API_FOOTBALL_KEY"] = af
    else:
        print("::warning::API_FOOTBALL_KEY is not set.")


def _grok(out: dict[str, str]) -> None:
    """AI lineup key (POST /api/lineup/suggest). Provider auto-detected from prefix (gsk_ vs xai-)."""
    found = False
    for name in ("GROK_API_KEY", "GROQ_API_KEY"):
        value = _strip(os.environ.get(name))
        if value.upper().startswith(f"{name}="):
            value = value.split("=", 1)[1].strip()
            print(f"::warning::{name} had a prefix; stripped for Cloud Run.")
        if value:
            out[name] = value
            found = True
    if not found:
        print("::warning::GROK_API_KEY is not set; /api/lineup/suggest will return 503.")


def _datasource(out: dict[str, str], url_env: str) -> None:
    url = _strip(os.environ.get(url_env))
    user = _strip(os.environ.get("SPRING_DATASOURCE_USERNAME"))
    password = _strip(os.environ.get("SPRING_DATASOURCE_PASSWORD"))
    if not url:
        print(f"::error::{url_env} is empty. Add repository Actions secret.")
        sys.exit(1)
    if not user or not password:
        print("::error::SPRING_DATASOURCE_USERNAME and SPRING_DATASOURCE_PASSWORD are required.")
        sys.exit(1)
    out["SPRING_DATASOURCE_URL"] = url
    out["SPRING_DATASOURCE_USERNAME"] = user
    out["SPRING_DATASOURCE_PASSWORD"] = password


def build(
    service: str,
    eureka_url: str,
    config_url: str,
    hostname: str,
    comment_service_url: str = "",
    player_service_url: str = "",
) -> dict[str, str]:
    # Do not set PORT — Cloud Run injects it automatically (reserved env var).
    out: dict[str, str] = {}

    if service == "discovery":
        return out

    if not eureka_url:
        print("::error::EUREKA_URL is required for config/comment/player deploy.")
        sys.exit(1)

    out["EUREKA_CLIENT_SERVICEURL_DEFAULTZONE"] = eureka_url

    if service == "config":
        out["SPRING_CLOUD_CONFIG_SERVER_NATIVE_SEARCH_LOCATIONS"] = "file:/app/config-repo"
        if hostname:
            out.update(_eureka_instance(hostname))
        return out

    if not config_url:
        print("::error::CONFIG_URL is required for comment/player deploy.")
        sys.exit(1)

    out["SPRING_CLOUD_CONFIG_URI"] = config_url
    if hostname:
        out.update(_eureka_instance(hostname))
    elif service in ("comment", "player"):
        print(f"::warning::No hostname for {service}; CD will patch EUREKA_INSTANCE_HOSTNAME after deploy.")

    if service == "comment":
        url_env = "SPRING_DATASOURCE_URL_COMMENT"
        fallback = _strip(os.environ.get("SPRING_DATASOURCE_URL"))
        if not _strip(os.environ.get(url_env)) and fallback:
            os.environ[url_env] = fallback
        _datasource(out, url_env)
        _firebase(out)
        player_url = _strip(player_service_url) or _strip(os.environ.get("PLAYER_SERVICE_URL"))
        if player_url:
            out["PLAYER_SERVICE_URL"] = player_url.rstrip("/")
        else:
            print("::warning::PLAYER_SERVICE_URL not set; comment author names may fall back to Firebase only.")
        return out

    if service == "player":
        url_env = "SPRING_DATASOURCE_URL_PLAYER"
        if not _strip(os.environ.get(url_env)):
            legacy = _strip(os.environ.get("SPRING_DATASOURCE_URL"))
            if legacy:
                os.environ[url_env] = legacy
        _datasource(out, url_env)
        _firebase(out)
        _api_football(out)
        _grok(out)
        comment_url = _strip(comment_service_url) or _strip(os.environ.get("COMMENT_SERVICE_URL"))
        if comment_url:
            out["COMMENT_SERVICE_URL"] = comment_url.rstrip("/")
        else:
            print("::warning::COMMENT_SERVICE_URL not set; player Feign may rely on Eureka for dwsc-comment.")
        return out

    print(f"::error::Unknown service: {service}")
    sys.exit(1)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--service", required=True, choices=["discovery", "config", "comment", "player"])
    parser.add_argument("--out", required=True)
    parser.add_argument("--eureka-url", default="")
    parser.add_argument("--config-url", default="")
    parser.add_argument("--hostname", default="")
    parser.add_argument("--comment-url", default="")
    parser.add_argument("--player-url", default="")
    args = parser.parse_args()

    payload = build(
        args.service,
        args.eureka_url,
        args.config_url,
        args.hostname,
        args.comment_url,
        args.player_url,
    )

    with open(args.out, "w", encoding="utf-8") as handle:
        json.dump(payload, handle)


if __name__ == "__main__":
    main()
