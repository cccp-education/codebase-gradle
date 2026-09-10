package codebase.koog.svo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BacklogDetectorTest {

    @Test
    fun `parse extracts open and done checklist items`() {
        val lines = listOf(
            "== Active backlog",
            "* [ ] SVO-2 first open item",
            "* [x] SVO-1 done item",
            "* [X] RENAME-1 done uppercase item",
        )
        val items = BacklogDetector.parse(lines)
        assertEquals(3, items.size)
        assertFalse(items[0].done)
        assertTrue(items[1].done)
        assertTrue(items[2].done)
    }

    @Test
    fun `parse ignores non checklist lines`() {
        val lines = listOf(
            "= Backlog — Codebase",
            "== EPIC SVO",
            "Some prose line without checkbox",
            "| table | row |",
            "----",
            "code block content",
            "----",
            "* [ ] real open item",
        )
        val items = BacklogDetector.parse(lines)
        assertEquals(1, items.size)
        assertEquals("* [ ] real open item", items[0].line)
    }

    @Test
    fun `items under stale-marked heading are flagged stale`() {
        val lines = listOf(
            "== Backlog stale (résolu)",
            "* [ ] old stale item kept for traceability",
        )
        val items = BacklogDetector.parse(lines)
        assertEquals(1, items.size)
        assertTrue(items[0].stale)
    }

    @Test
    fun `items under heading with completion marker are flagged stale`() {
        val lines = listOf(
            "== EPIC CB-FPA-RAG — Ingestion corpus ✅ TERMINÉ (4/4 US)",
            "* [ ] historical unchecked item",
        )
        val items = BacklogDetector.parse(lines)
        assertTrue(items.single().stale)
    }

    @Test
    fun `status line with completion marker flags subsequent items stale until a new epic heading`() {
        val lines = listOf(
            "== EPIC V-9 — Vibecoding Gouvernance Agentique",
            "**Statut** : ✅ TERMINE (20/20 items)",
            "* [ ] historical unchecked item",
            "== EPIC SVO — Session Vibecoding Orchestrée 🟡 EN COURS",
            "* [ ] fresh open item",
        )
        val items = BacklogDetector.parse(lines)
        assertEquals(2, items.size)
        assertTrue(items[0].stale, "item under completed status line must be stale")
        assertFalse(items[1].stale, "item under a new active EPIC must not be stale")
    }

    @Test
    fun `neutral subsection heading inherits enclosing section status`() {
        val lines = listOf(
            "== EPIC V-9 — Gouvernance Agentique",
            "**Statut** : ✅ TERMINE (20/20)",
            "=== Prerequis",
            "* [ ] historical unchecked item",
            "=== Under-EPIC 1",
            "* [ ] historical unchecked item 2",
        )
        val items = BacklogDetector.parse(lines)
        assertEquals(2, items.size)
        assertTrue(items[0].stale, "subsection of closed EPIC inherits stale")
        assertTrue(items[1].stale, "second subsection of closed EPIC inherits stale")
    }

    @Test
    fun `active status line wins over completion marker inside same section`() {
        val lines = listOf(
            "== EPIC OCR-QUALITY — Détection auto 🟡 EN COURS (US-4 ✅ TERMINÉ S-213)",
            "* [ ] remaining US from other borough",
        )
        val items = BacklogDetector.parse(lines)
        assertEquals(1, items.size)
        assertFalse(items[0].stale, "section still EN COURS must stay open despite partial ✅")
    }

    @Test
    fun `table row status takes precedence over section staleness`() {
        val lines = listOf(
            "== EPIC X — closed section",
            "**Statut** : ✅ TERMINÉ (7/7)",
            "| SVO-4 | task autonomousSession | 3 | 🟡 À FAIRE |",
        )
        val items = BacklogDetector.parse(lines)
        assertEquals(1, items.size)
        assertFalse(
            items[0].stale,
            "explicit open table status overrides stale section (row owns its own status)"
        )
        assertFalse(items[0].done)
    }

    @Test
    fun `heading without completion marker resets stale flag`() {
        val lines = listOf(
            "== Backlog stale (résolu)",
            "* [ ] stale item",
            "== EPIC SVO — Session Vibecoding Orchestrée 🟡 EN COURS",
            "* [ ] active open item",
        )
        val items = BacklogDetector.parse(lines)
        assertTrue(items[0].stale)
        assertFalse(items[1].stale)
    }

    @Test
    fun `openItems excludes done and stale — keeps plain open`() {
        val items = listOf(
            BacklogItem(line = "* [ ] open item", done = false, stale = false),
            BacklogItem(line = "* [x] done item", done = true, stale = false),
            BacklogItem(line = "* [ ] stale item", done = false, stale = true),
        )
        val open = BacklogDetector.openItems(items)
        assertEquals(1, open.size)
        assertEquals("open item", open[0].summary())
    }

    @Test
    fun `isComplete true when no open non-stale items`() {
        assertTrue(BacklogDetector.isComplete(emptyList()))
        assertTrue(
            BacklogDetector.isComplete(
                listOf(
                    BacklogItem(line = "* [x] done", done = true, stale = false),
                    BacklogItem(line = "* [ ] stale", done = false, stale = true),
                )
            )
        )
    }

    @Test
    fun `isComplete false when at least one open non-stale item`() {
        assertFalse(
            BacklogDetector.isComplete(
                listOf(
                    BacklogItem(line = "* [x] done", done = true, stale = false),
                    BacklogItem(line = "* [ ] open", done = false, stale = false),
                )
            )
        )
    }

    @Test
    fun `summary strips the checkbox marker`() {
        val items = BacklogDetector.parse(listOf("* [ ] SVO-2 objects purs"))
        assertEquals("SVO-2 objects purs", items.single().summary())
    }

    @Test
    fun `table rows with open status are extracted as open items`() {
        val lines = listOf(
            "|===",
            "| US | Description | Pts | Statut",
            "| SVO-2 | SessionResumptionPlanner + BacklogDetector (objects purs) | 2 | 🟡 À FAIRE",
            "|===",
        )
        val items = BacklogDetector.parse(lines)
        assertEquals(1, items.size)
        val item = items.single()
        assertFalse(item.done)
        assertEquals("SVO-2 SessionResumptionPlanner + BacklogDetector (objects purs)", item.summary())
    }

    @Test
    fun `table rows with completed status are extracted as done items`() {
        val lines = listOf(
            "| SVO-1 | Inversion port PlannerPort | 3 | ✅ TERMINÉ (S-256) |",
        )
        val items = BacklogDetector.parse(lines)
        assertEquals(1, items.size)
        assertTrue(items.single().done)
        assertEquals("SVO-1 Inversion port PlannerPort", items.single().summary())
    }

    @Test
    fun `table header rows and block delimiters are skipped`() {
        val lines = listOf(
            "|===",
            "| US | Description | Pts | Statut",
            "| SVO-3 | Adapter planner | 2 | 🟡 À FAIRE",
            "|===",
        )
        val items = BacklogDetector.parse(lines)
        assertEquals(1, items.size)
        assertEquals("SVO-3 Adapter planner", items.single().summary())
    }
}