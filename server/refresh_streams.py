#!/usr/bin/env python3
"""
Builds the Live Feed registry from YouTube, once for everybody.

⚠️ WHY THIS RUNS HERE AND NOT IN THE APP:

`search.list` is capped at 100 calls per DAY for the whole project, shared
across every install — not per user. An app that searched for itself would
exhaust the global budget at about five users, and the key would be sitting
in the APK for anyone who unzipped it. Running it here makes the cost
O(channels) instead of O(users): the same ~25 calls a day whether ten people
use the app or ten million. That property, not the key hiding, is the real
reason this file exists (tech_stack.md §5.1).

⚠️ AND WHY IT IS ADAPTIVE:

Rediscovering every channel on every run would cost 25 search calls a run and
cap us at four runs a day. But rediscovery is only needed when a stream has
actually died. So each run first VERIFIES the streams we already know with
one cheap `videos.list` call (1 unit for 50 streams), and only spends a
search call on the channels that came up empty. A quiet run costs 1 unit. A
run where three streams restarted costs 3 search calls. We can afford to run
this every couple of hours.

Quota, per run:
    videos.list   1 unit per 50 video IDs
    channels.list 1 unit per unresolved handle (cached afterwards)
    search.list   1 call per channel that needs rediscovery, hard-capped below

Usage:
    YOUTUBE_API_KEY=… python server/refresh_streams.py
    YOUTUBE_API_KEY=… python server/refresh_streams.py --full   # ignore cache
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

API_ROOT = "https://www.googleapis.com/youtube/v3"

HERE = Path(__file__).parent
CHANNELS_FILE = HERE / "channels.json"
OUTPUT_FILE = HERE / "streams.json"
CHANNEL_ID_CACHE = HERE / "channel_ids.json"

# The project-wide ceiling is 100 search calls per DAY. The workflow runs
# every 6 hours, so 20 per run bounds us at 80/day even in the worst case
# where every channel needs rediscovering on every single run. Raise this and
# you must lower the schedule frequency to match, or a bad day exhausts the
# quota until midnight Pacific and the feed stops updating entirely.
MAX_SEARCH_CALLS_PER_RUN = 20


# --------------------------------------------------------------------------
# API plumbing
# --------------------------------------------------------------------------

class QuotaExceeded(RuntimeError):
    """Raised when YouTube says we're out of budget, so we stop cleanly."""


def api_get(endpoint: str, params: dict, key: str) -> dict:
    query = urllib.parse.urlencode({**params, "key": key})
    request = urllib.request.Request(
        f"{API_ROOT}/{endpoint}?{query}",
        headers={"Accept": "application/json"},
    )
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        body = error.read().decode("utf-8", "replace")
        if error.code == 403 and "quota" in body.lower():
            raise QuotaExceeded(body) from error
        raise RuntimeError(f"{endpoint} failed ({error.code}): {body}") from error


def resolve_channel_id(handle: str, key: str) -> str | None:
    """@handle → UC… channel ID. 1 unit, and cached, so it runs once ever."""
    response = api_get("channels", {"part": "id", "forHandle": handle}, key)
    items = response.get("items") or []
    return items[0]["id"] if items else None


def find_live_videos(channel_id: str, limit: int, key: str) -> list[str]:
    """
    Currently-live video IDs for a channel. This is the expensive call.

    `eventType=live` requires `type=video` — the API rejects it otherwise.
    """
    response = api_get(
        "search",
        {
            "part": "id",
            "channelId": channel_id,
            "eventType": "live",
            "type": "video",
            "maxResults": min(limit, 50),
            "order": "viewCount",
        },
        key,
    )
    return [
        item["id"]["videoId"]
        for item in response.get("items") or []
        if item.get("id", {}).get("videoId")
    ]


def inspect_videos(video_ids: list[str], key: str) -> dict[str, dict]:
    """
    Everything we need to decide whether a video is showable, 50 at a time.

    ⚠️ `status.embeddable` IS THE IMPORTANT ONE. A stream can be perfectly
    live and still refuse to play in the app because the uploader disabled
    embedding — which is very likely what was behind the error 152 failures
    documented in StreamPlayer.kt. Without this filter you find out when a
    user taps a black rectangle. With it, those streams never ship.
    """
    found: dict[str, dict] = {}
    for start in range(0, len(video_ids), 50):
        batch = video_ids[start:start + 50]
        response = api_get(
            "videos",
            {
                "part": "snippet,status,liveStreamingDetails,contentDetails",
                "id": ",".join(batch),
            },
            key,
        )
        for item in response.get("items") or []:
            found[item["id"]] = item
    return found


def is_showable(video: dict) -> tuple[bool, str]:
    """Returns (ok, reason-if-not). Reasons get logged, so failures are legible."""
    snippet = video.get("snippet") or {}
    status = video.get("status") or {}

    if snippet.get("liveBroadcastContent") != "live":
        return False, f"not live ({snippet.get('liveBroadcastContent')})"
    if not status.get("embeddable", False):
        return False, "embedding disabled by the uploader"
    if status.get("privacyStatus") not in ("public", None):
        return False, f"privacy: {status.get('privacyStatus')}"

    restriction = (video.get("contentDetails") or {}).get("regionRestriction") or {}
    if restriction.get("blocked"):
        return False, f"region-blocked in {len(restriction['blocked'])} countries"

    return True, ""


# --------------------------------------------------------------------------
# Registry assembly
# --------------------------------------------------------------------------

def load_json(path: Path, fallback):
    if not path.exists():
        return fallback
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as error:
        print(f"  ! {path.name} is not valid JSON ({error}); ignoring it")
        return fallback


def stream_entry(video_id: str, video: dict, channel: dict) -> dict:
    """
    One registry row, shaped for Stream.kt.

    The title comes from the video because multi-camera channels name each
    camera there; place and coordinates come from our table because YouTube
    has no idea where the camera is pointing.
    """
    snippet = video.get("snippet") or {}
    return {
        "id": f"yt-{video_id}",
        "title": snippet.get("title", "").strip() or channel.get("place", "Live"),
        "place": channel.get("place", ""),
        "lat": channel.get("lat", 0.0),
        "lng": channel.get("lng", 0.0),
        "category": channel.get("category", "CITY"),
        "moods": channel.get("moods", []),
        "source": "YOUTUBE",
        "streamRef": video_id,
        "hasAudio": True,
        "timezone": channel.get("timezone"),
        "attribution": channel.get("attribution") or snippet.get("channelTitle"),
        "lastVerified": datetime.now(timezone.utc).strftime("%Y-%m-%d"),
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--full",
        action="store_true",
        help="Rediscover every channel instead of only the ones that went dark.",
    )
    args = parser.parse_args()

    key = os.environ.get("YOUTUBE_API_KEY", "").strip()
    if not key:
        print("YOUTUBE_API_KEY is not set.", file=sys.stderr)
        return 2

    config = load_json(CHANNELS_FILE, {})
    channels = config.get("channels") or []
    if not channels:
        print(f"No channels in {CHANNELS_FILE.name}; nothing to do.", file=sys.stderr)
        return 2

    id_cache = load_json(CHANNEL_ID_CACHE, {})
    previous = load_json(OUTPUT_FILE, {})
    previous_streams = previous.get("streams") or []

    # Which channel did each stream we already know come from? Verified
    # streams count against that channel's quota so we don't re-search it.
    owner_of = {
        stream["streamRef"]: stream.get("_channel")
        for stream in previous_streams
        if stream.get("streamRef")
    }

    print(f"{len(channels)} channels, {len(previous_streams)} streams from last run")

    # ---- Phase 1: verify what we already have. One cheap call. ----
    kept: dict[str, list[dict]] = {}
    if previous_streams and not args.full:
        known_ids = [s["streamRef"] for s in previous_streams if s.get("streamRef")]
        try:
            details = inspect_videos(known_ids, key)
        except QuotaExceeded:
            print("! Out of quota during verification; leaving the registry as-is.")
            return 1

        for video_id in known_ids:
            video = details.get(video_id)
            if not video:
                print(f"  - {video_id}: gone")
                continue
            ok, reason = is_showable(video)
            if not ok:
                print(f"  - {video_id}: {reason}")
                continue
            handle = owner_of.get(video_id)
            kept.setdefault(handle, []).append((video_id, video))

        still_live = sum(len(v) for v in kept.values())
        print(f"  {still_live}/{len(known_ids)} still live")

    # ---- Phase 2: rediscover only the channels that came up short. ----
    searches = 0
    discovered: list[tuple[str, dict, dict]] = []

    for channel in channels:
        handle = channel.get("handle") or channel.get("channelId")
        if not handle:
            continue

        wanted = int(channel.get("maxStreams", 1))
        have = len(kept.get(handle, []))
        if have >= wanted:
            continue

        if searches >= MAX_SEARCH_CALLS_PER_RUN:
            print(f"  ! search budget spent; {handle} waits for the next run")
            continue

        channel_id = channel.get("channelId") or id_cache.get(handle)
        if not channel_id:
            try:
                channel_id = resolve_channel_id(handle, key)
            except QuotaExceeded:
                print("! Out of quota while resolving handles; stopping here.")
                break
            if not channel_id:
                print(f"  ! {handle}: no such channel — check the handle")
                continue
            id_cache[handle] = channel_id
            print(f"  + resolved {handle} → {channel_id}")

        try:
            video_ids = find_live_videos(channel_id, wanted - have, key)
        except QuotaExceeded:
            print("! Out of search quota; keeping what we have.")
            break
        searches += 1

        if not video_ids:
            print(f"  · {handle}: nothing live right now")
            continue

        try:
            details = inspect_videos(video_ids, key)
        except QuotaExceeded:
            print("! Out of quota while inspecting; keeping what we have.")
            break

        for video_id in video_ids:
            video = details.get(video_id)
            if not video:
                continue
            ok, reason = is_showable(video)
            if not ok:
                print(f"  - {video_id} ({handle}): {reason}")
                continue
            discovered.append((video_id, video, channel))
            print(f"  + {handle}: {video.get('snippet', {}).get('title', '')[:60]}")

    # ---- Phase 3: write it out. ----
    by_handle = {c.get("handle") or c.get("channelId"): c for c in channels}
    streams: list[dict] = []

    for handle, entries in kept.items():
        channel = by_handle.get(handle)
        if not channel:
            continue  # channel was removed from the table since last run
        for video_id, video in entries:
            entry = stream_entry(video_id, video, channel)
            entry["_channel"] = handle
            streams.append(entry)

    for video_id, video, channel in discovered:
        entry = stream_entry(video_id, video, channel)
        entry["_channel"] = channel.get("handle") or channel.get("channelId")
        streams.append(entry)

    if not streams:
        # Publishing an empty registry would blank the feed for everyone. The
        # app falls back to its bundled asset only when the fetch fails, not
        # when it succeeds and is empty — so refuse to write instead.
        print("! Nothing live anywhere. Leaving the previous registry in place.")
        return 1

    payload = {
        "version": 6,
        "generated": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "note": "Generated by server/refresh_streams.py. Do not edit by hand — "
                "edit server/channels.json instead.",
        "streams": streams,
    }

    OUTPUT_FILE.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
    CHANNEL_ID_CACHE.write_text(json.dumps(id_cache, indent=2) + "\n", encoding="utf-8")

    print(f"\nWrote {len(streams)} live streams ({searches} search calls used)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
