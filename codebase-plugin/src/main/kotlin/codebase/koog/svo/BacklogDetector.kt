package codebase.koog.svo

/**
 * A single work item extracted from a BACKLOG.adoc — either a checklist line
 * (`* [ ]` / `* [x]`) or an AsciiDoc table row (`| ID | desc | pts | statut |`).
 *
 * [stale] marks items that live under sections closed by history — headings
 * or status lines carrying a completion marker (EPIC ✅ TERMINÉ / CADUQUE,
 * "stale (résolu)", "ABANDONNÉ", "historique") — so the detector can tell a
 * real remaining work item apart from an unchecked-but-archived checklist
 * entry (the recurrent BACKLOG-sync drift, see SESSION_CHECKLIST step 7).
 */
data class BacklogItem(
    val line: String,
    val done: Boolean,
    val stale: Boolean,
) {
    fun summary(): String = when {
        line.startsWith("|") -> {
            val cells = line.trim('|').split('|').map { it.trim() }
            if (cells.size >= 2) "${cells[0]} ${cells[1]}" else line.trim('|').trim()
        }
        else -> line.replace(CHECKBOX_MARK, "").trim()
    }

    private companion object {
        val CHECKBOX_MARK = Regex("^\\*\\s*\\[.\\]\\s*")
    }
}

/**
 * Pure heuristic deciding whether a BACKLOG is complete (EPIC SVO D8):
 * a backlog is finished when no *non-stale* `[ ]` item remains.
 *
 * Supports both BACKLOG formats found in real boroughs (dogfooding S-257):
 * checkboxes (`* [ ]`) and AsciiDoc US tables (`| US | desc | pts | Statut |`).
 */
object BacklogDetector {

    private val CHECKBOX = Regex("^\\*\\s*\\[( |x|X)\\]\\s*(.*)$")

    private val HEADING = Regex("^(=+)\\s+(.*)$")

    private val COMPLETION_MARKER = Regex(
        "(?i)(✅|stale|termin[eé]s?|termine|caduque|abandonn[eé]s?|cl[oô]tur[eé]s?|retrait[eé]s?|migr[eé]s?|historique)"
    )

    private val ACTIVE_MARKER = Regex(
        "(?i)(🟡|⏳|en cours|à faire|a faire|à planifier|a planifier|en pause)"
    )

    private val TABLE_DELIMITER = Regex("^\\|={3,}$")

    private val TABLE_ROW =
        Regex("^\\|\\s*(\\S[^|]*?)\\s*\\|\\s*(\\S[^|]*?)\\s*\\|\\s*[^|]*\\|\\s*([^|]*?)\\s*\\|?\\s*$")

    fun parse(lines: List<String>): List<BacklogItem> {
        val items = mutableListOf<BacklogItem>()
        var stale = false
        var inCodeBlock = false
        var skipNextTableRow = false

        for (raw in lines) {
            val line = raw.trim()
            if (line.startsWith("----")) {
                inCodeBlock = !inCodeBlock
                continue
            }
            if (inCodeBlock) continue

            if (TABLE_DELIMITER.matchEntire(line) != null) {
                skipNextTableRow = !skipNextTableRow
                continue
            }

            if (HEADING.matchEntire(line) != null || line.startsWith("**Statut**")) {
                if (COMPLETION_MARKER.containsMatchIn(line)) {
                    stale = !ACTIVE_MARKER.containsMatchIn(line)
                } else if (ACTIVE_MARKER.containsMatchIn(line)) {
                    stale = false
                }
                // Neutral heading (e.g. "=== Prerequisites") inherits the
                // enclosing section status — only explicit markers flip it.
                continue
            }

            if (line.startsWith("* [")) {
                val match = CHECKBOX.matchEntire(line)
                if (match != null) {
                    items += BacklogItem(
                        line = line,
                        done = !match.groupValues[1].isBlank(),
                        stale = stale,
                    )
                }
                continue
            }

            if (line.startsWith("| ")) {
                if (skipNextTableRow) {
                    skipNextTableRow = false
                    continue
                }
                val match = TABLE_ROW.matchEntire(line)
                if (match != null) {
                    val status = match.groupValues[3]
                    val done = COMPLETION_MARKER.containsMatchIn(status)
                    items += BacklogItem(
                        line = line,
                        done = done,
                        stale = stale && !ACTIVE_MARKER.containsMatchIn(status),
                    )
                }
            }
        }
        return items
    }

    fun openItems(items: List<BacklogItem>): List<BacklogItem> =
        items.filter { !it.done && !it.stale }

    fun isComplete(items: List<BacklogItem>): Boolean = openItems(items).isEmpty()
}