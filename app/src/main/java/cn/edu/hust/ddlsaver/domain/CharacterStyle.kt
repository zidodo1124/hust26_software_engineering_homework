package cn.edu.hust.ddlsaver.domain

/** Presentation preference only; both styles use the same event state and reminder rules. */
enum class CharacterStyle(val label: String) {
    CLASSIC("经典立绘"),
    CHIBI("Q 版形象");

    companion object {
        fun fromStored(value: String?): CharacterStyle = entries.firstOrNull { it.name == value } ?: CLASSIC
    }
}
