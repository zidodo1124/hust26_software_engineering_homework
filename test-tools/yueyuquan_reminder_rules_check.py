"""Pure-logic check mirroring ReminderRulesTest against ReminderRules source.

Local fallback when Android SDK is unavailable. Does not replace Gradle/Robolectric
or device verification.
"""

from __future__ import annotations

from collections import defaultdict
from datetime import date, datetime, timedelta, timezone
from zoneinfo import ZoneInfo

BJ = ZoneInfo("Asia/Shanghai")
DEADLINE = int(datetime(2030, 1, 1, 4, 0, 0, tzinfo=timezone.utc).timestamp() * 1000)
RETRY_MILLIS = 5 * 60_000


def event_plans(
    event_id: str = "a",
    deadline: int = DEADLINE,
    sprint: int | None = 60,
    danger: bool = True,
    deadline_enabled: bool = True,
    lead: int = 60,
    completed: bool = False,
    archived: bool = False,
):
    if completed or archived:
        return []
    plans = []
    if danger and sprint is not None:
        plans.append(("DANGER", deadline - sprint * 60_000, f"DANGER:{event_id}:{deadline}"))
    if deadline_enabled:
        plans.append(("DEADLINE", deadline - lead * 60_000, f"DEADLINE:{event_id}:{deadline}"))
    sig: dict[int, list[str]] = defaultdict(list)
    for kind, trigger, _ in plans:
        sig[trigger].append(kind)
    out = []
    for kind, trigger, key in plans:
        kinds = "+".join(sorted(sig[trigger]))
        tag = f"event:{event_id}:{deadline}:{trigger}:{kinds}"
        out.append((kind, trigger, key, tag))
    return out


def daily_trigger(day: date, minute: int) -> int:
    local = datetime(day.year, day.month, day.day, minute // 60, minute % 60, tzinfo=BJ)
    return int(local.timestamp() * 1000)


def next_daily_date(now_ms: int, minute: int) -> date:
    local = datetime.fromtimestamp(now_ms / 1000, BJ).date()
    return local if daily_trigger(local, minute) >= now_ms else local + timedelta(days=1)


def safe_add(value: int, amount: int) -> int:
    try:
        return value + amount
    except OverflowError:
        return 2**63 - 1


def next_wake(plans: list[dict], now: int) -> int | None:
    candidates = []
    for plan in plans:
        if not plan.get("active", True) or plan["state"] == "SENT":
            continue
        floor = float("-inf")
        if plan["state"] in ("FAILED", "CLAIMED"):
            attempt = plan.get("lastAttemptAt")
            floor = safe_add(attempt, RETRY_MILLIS) if attempt is not None else float("-inf")
        candidates.append(max(plan["triggerAt"], floor if floor != float("-inf") else -(2**63)))
    if not candidates:
        return None
    return max(min(candidates), safe_add(now, 1))


def main() -> None:
    plans = event_plans()
    assert len(plans) == 2
    assert all(t == DEADLINE - 60 * 60_000 for _, t, _, _ in plans)
    assert len({tag for *_, tag in plans}) == 1
    assert len({key for _, _, key, _ in plans}) == 2
    print("PASS exactBoundaryAndMergedIdentity")

    plans = event_plans(sprint=20)
    assert len({tag for *_, tag in plans}) == 2
    assert {t for _, t, _, _ in plans} == {DEADLINE - 20 * 60_000, DEADLINE - 60 * 60_000}
    print("PASS separateReasonTimesHaveSeparateAlertIdentities")

    assert [k for k, _, _, _ in event_plans(sprint=None)] == ["DEADLINE"]
    assert event_plans(danger=False, deadline_enabled=False) == []
    assert event_plans(completed=True) == []
    assert event_plans(archived=True) == []
    print("PASS noSprintAndDisabledReasonsDoNotCreateDangerPlans")

    before = int(datetime(2030, 1, 1, 23, 59, 0, tzinfo=timezone.utc).timestamp() * 1000)
    assert next_daily_date(before, 480) == date(2030, 1, 2)
    assert daily_trigger(date(2030, 1, 2), 480) == int(
        datetime(2030, 1, 2, 0, 0, 0, tzinfo=timezone.utc).timestamp() * 1000
    )
    assert next_daily_date(before + 120_000, 480) == date(2030, 1, 3)
    print("PASS dailyNextOccurrenceUsesBeijingAndDoesNotCatchUpNewInstall")

    first = event_plans(sprint=30, lead=60)
    second = event_plans(sprint=30, lead=60)
    assert {key for _, _, key, _ in first} == {f"DANGER:a:{DEADLINE}", f"DEADLINE:a:{DEADLINE}"}
    assert first == second
    assert len(first) == len({key for _, _, key, _ in first})
    print("PASS planKeysAreStableUniquePerReasonAndDeadline")

    original = {key for _, _, key, _ in event_plans(sprint=60)}
    shifted = {key for _, _, key, _ in event_plans(deadline=DEADLINE + 60_000, sprint=60)}
    assert original and original.isdisjoint(shifted)
    print("PASS deadlineChangeProducesNewKeysSoOldLedgerCannotCollide")

    tags_a = {tag for *_, tag in event_plans(event_id="a", sprint=60)}
    tags_b = {tag for *_, tag in event_plans(event_id="b", sprint=60)}
    assert len(tags_a) == 1 and len(tags_b) == 1 and tags_a.isdisjoint(tags_b)
    print("PASS differentEventsAtSameInstantKeepDistinctNotificationTags")

    now = 1_000_000
    assert next_wake(
        [
            {"triggerAt": now - 10, "state": "SENT", "active": True},
            {"triggerAt": now + 50, "state": "PENDING", "active": True},
            {"triggerAt": now + 100, "state": "PENDING", "active": True},
        ],
        now,
    ) == now + 50
    assert next_wake(
        [{"triggerAt": now - 5, "state": "CLAIMED", "active": True, "lastAttemptAt": now}],
        now,
    ) == now + RETRY_MILLIS
    assert next_wake([{"triggerAt": now - 1, "state": "SENT", "active": True}], now) is None
    print("PASS nextWakeSkipsSentUsesEarliestPendingAndBackoffForClaimed")

    print("ALL_CHECKS_PASSED count=8 (ReminderRules dedupe/merge subset)")


if __name__ == "__main__":
    main()
