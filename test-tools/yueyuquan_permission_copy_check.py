"""Check ReminderStatusCopy wording against capability semantics (no Android SDK)."""

from __future__ import annotations


def home_warning(*, any_enabled: bool, can_post: bool, can_exact: bool, last_error: str | None) -> str | None:
    if not any_enabled:
        return None
    if last_error:
        return f"提醒暂未安排完成，请前往设置重新检查：{last_error}"
    if not can_post:
        return "系统通知尚未开启或提醒通道已关闭。如需接收提醒，请前往“设置”检查通知权限与通道。"
    if not can_exact:
        return "当前使用近似提醒，通知可能延迟。可前往“设置”允许精确提醒。"
    return None


def settings_hint(can_post: bool) -> str | None:
    if not can_post:
        return "提醒开关对应的计划会保留，但在系统通知可用前不会弹出通知，也不会安排系统唤醒。"
    return None


def main() -> None:
    assert home_warning(any_enabled=False, can_post=False, can_exact=False, last_error=None) is None
    assert "通道" in home_warning(any_enabled=True, can_post=False, can_exact=True, last_error=None)
    assert "近似提醒" in home_warning(any_enabled=True, can_post=True, can_exact=False, last_error=None)
    assert "重新检查" in home_warning(any_enabled=True, can_post=True, can_exact=True, last_error="x")
    assert home_warning(any_enabled=True, can_post=True, can_exact=True, last_error=None) is None
    assert "系统唤醒" in (settings_hint(False) or "")
    assert settings_hint(True) is None
    # Old home copy only mentioned 权限; new copy must mention 通道 to match canPost.
    old = "系统通知尚未开启。如需接收提醒，请前往“设置”开启系统通知权限。"
    new = home_warning(any_enabled=True, can_post=False, can_exact=True, last_error=None)
    assert new != old and "通道" in new
    print("PASS permission/capability copy consistency")
    print("ALL_CHECKS_PASSED count=1 (ReminderStatusCopy)")


if __name__ == "__main__":
    main()
