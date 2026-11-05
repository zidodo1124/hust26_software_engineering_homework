package cn.edu.hust.ddlsaver.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.math.RoundingMode

/** Units offered when typing a duration. Storage stays in whole minutes. */
internal enum class DurationUnit(val label: String, val minutes: Long) {
    MINUTE("分钟", 1L),
    HOUR("小时", 60L),
    DAY("天", 24L * 60L);

    companion object {
        fun fromStored(name: String?): DurationUnit = entries.firstOrNull { it.name == name } ?: HOUR
    }
}

/**
 * Upper bound accepted by the input fields. The domain layer accepts larger values, but nothing a
 * student plans is longer than a year, and huge values only produce unreadable calendar blocks.
 */
internal const val MaxInputDurationMinutes: Long = 365L * 24L * 60L

/** Text shown in a duration field: the typed amount plus the selected unit. */
internal data class DurationText(val amount: String, val unit: DurationUnit)

/** Shows a stored value with the largest unit that divides it exactly, e.g. 120 → "2" 小时, 90 → "90" 分钟. */
internal fun splitDuration(minutes: Long?, emptyUnit: DurationUnit = DurationUnit.HOUR): DurationText = when {
    minutes == null -> DurationText("", emptyUnit)
    minutes > 0 && minutes % DurationUnit.DAY.minutes == 0L -> DurationText((minutes / DurationUnit.DAY.minutes).toString(), DurationUnit.DAY)
    minutes > 0 && minutes % DurationUnit.HOUR.minutes == 0L -> DurationText((minutes / DurationUnit.HOUR.minutes).toString(), DurationUnit.HOUR)
    else -> DurationText(minutes.toString(), DurationUnit.MINUTE)
}

internal sealed interface DurationParse {
    data object Blank : DurationParse
    data class Valid(val minutes: Long) : DurationParse
    data class Invalid(val message: String) : DurationParse
}

private val DurationPattern = Regex("""\d{1,9}(\.\d{0,2})?""")

/**
 * Converts typed text to whole minutes. Up to two decimals are allowed ("1.5" 小时 = 90 分钟);
 * the result is rounded to the nearest minute. BigDecimal keeps the conversion free of overflow.
 */
internal fun parseDuration(text: String, unit: DurationUnit, maxMinutes: Long = MaxInputDurationMinutes): DurationParse {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return DurationParse.Blank
    if (!DurationPattern.matches(trimmed)) return DurationParse.Invalid("请填写数字，最多保留两位小数。")
    val minutes = BigDecimal(trimmed).multiply(BigDecimal.valueOf(unit.minutes)).setScale(0, RoundingMode.HALF_UP)
    if (minutes < BigDecimal.ONE) return DurationParse.Invalid("时长至少 1 分钟。")
    if (minutes > BigDecimal.valueOf(maxMinutes)) return DurationParse.Invalid("时长不能超过 ${formatDuration(maxMinutes)}。")
    return DurationParse.Valid(minutes.longValueExact())
}

/** Number field plus a unit choice; [tag] names the text field and `$tag-unit-<UNIT>` the unit chips. */
@Composable
internal fun DurationField(
    amount: String,
    unit: DurationUnit,
    onAmountChange: (String) -> Unit,
    onUnitChange: (DurationUnit) -> Unit,
    label: String,
    tag: String,
    enabled: Boolean,
    isError: Boolean,
    supportingText: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = amount, onValueChange = onAmountChange, enabled = enabled,
            label = { Text(label) }, singleLine = true, isError = isError,
            supportingText = { Text(supportingText) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth().testTag(tag), shape = RoundedCornerShape(16.dp),
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("单位", color = Muted, style = MaterialTheme.typography.bodySmall)
            DurationUnit.entries.forEach { option ->
                FilterChip(
                    selected = unit == option, onClick = { onUnitChange(option) }, enabled = enabled,
                    label = { Text(option.label) }, modifier = Modifier.testTag("$tag-unit-${option.name}"),
                )
            }
        }
    }
}
