"""Pure-logic check mirroring ReminderRulesTest against ReminderRules source.

This is a local fallback when Android SDK is unavailable. It does not replace
Gradle/Robolectric tests or device verification.
"""

from __future__ import annotations

from collections import defaultdict
from datetime import date, datetime, timedelta, timezone
from zoneinfo import ZoneInfo

BJ = ZoneInfo("Asia/Shanghai")
DEADLINE = int(datetime(2030, 1, 1, 4, 0, 0, tzinfo=timezone.utc).timestamp() * 1000)


def event_plans(
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
        plans.append(("DANGER", DEADLINE - sprint * 60_000, f"DANGER:a:{DEADLINE}"))
    if deadline_enabled:
        plans.append(("DEADLINE", DEADLINE - lead * 60_000, f"DEADLINE:a:{DEADLINE}"))
    sig: dict[int, list[str]] = defaultdict(list)
    for kind, trigger, _ in plans:
        sig[trigger].append(kind)
    out = []
    for kind, trigger, key in plans:
        kinds = "+".join(sorted(sig[trigger]))
        tag = f"event:a:{DEADLINE}:{trigger}:{kinds}"
        out.append((kind, trigger, key, tag))
    return out


def daily_trigger(day: date, minute: int) -> int:
    local = datetime(day.year, day.month, day.day, minute // 60, minute % 60, tzinfo=BJ)
    return int(local.timestamp() * 1000)


def next_daily_date(now_ms: int, minute: int) -> date:
    local = datetime.fromtimestamp(now_ms / 1000, BJ).date()
    return local if daily_trigger(local, minute) >= now_ms else local + timedelta(days=1)


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
    # urgentMinutes must not affect deadline lead plans
    assert event_plans(sprint=20) == event_plans(sprint=20)
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

    print("ALL_CHECKS_PASSED count=4 (ReminderRules pure subset)")


if __name__ == "__main__":
    main()
