import pathlib, re
root = pathlib.Path("app/src/main/java/my/slotify/app")

def edit(rel, pairs):
    p = root / rel
    s = p.read_text(encoding="utf-8")
    for a, b in pairs:
        if s.count(a) != 1:
            raise SystemExit(f"{rel}: expected 1 of {a[:90]!r}, found {s.count(a)}")
        s = s.replace(a, b)
    p.write_text(s, encoding="utf-8")

# Shared labels
edit("core/designsystem/theme/SlotifyStrings.kt", [
    ("    val close: String,\n", "    val close: String,\n    val expandSection: String,\n    val collapseSection: String,\n"),
    ('    close = "Tutup",\n', '    close = "Tutup",\n    expandSection = "Buka bahagian",\n    collapseSection = "Tutup bahagian",\n'),
    ('    close = "Close",\n', '    close = "Close",\n    expandSection = "Expand section",\n    collapseSection = "Collapse section",\n'),
])
edit("core/designsystem/theme/DashboardStrings.kt", [
    ("    val expandSection: String,\n    val collapseSection: String,\n", ""),
    ('    expandSection = "Buka bahagian",\n', ""), ('    collapseSection = "Tutup bahagian",\n', ""),
    ('    expandSection = "Expand section",\n', ""), ('    collapseSection = "Collapse section",\n', ""),
])

sec = root / "core/designsystem/component/SlotifySection.kt"
t = sec.read_text(encoding="utf-8")
t = t.replace("""    onToggle: () -> Unit,
    expandLabel: String,
    collapseLabel: String,
    modifier: Modifier = Modifier,
    summary: String? = null
) {""", """    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null
) {
    val expandLabel = strings.expandSection
    val collapseLabel = strings.collapseSection""")
t = t.replace("""    onToggle: () -> Unit,
    expandLabel: String,
    collapseLabel: String,
    summary: String? = null,
    content: LazyListScope.() -> Unit""", """    onToggle: () -> Unit,
    summary: String? = null,
    content: LazyListScope.() -> Unit""")
t = t.replace("SlotifySectionHeader(title, expanded, onToggle, expandLabel, collapseLabel, summary = summary)",
              "SlotifySectionHeader(title, expanded, onToggle, summary = summary)")
t = t.replace("import my.slotify.app.core.designsystem.theme.SlotifyColors\n",
              "import my.slotify.app.core.designsystem.theme.SlotifyColors\nimport my.slotify.app.core.designsystem.theme.strings\n")
assert "expandLabel: String" not in t
sec.write_text(t, encoding="utf-8")

d = root / "feature/dashboard/TodayDashboardScreen.kt"
u = d.read_text(encoding="utf-8")
n = u.count("                expandLabel = ds.expandSection,\n                collapseLabel = ds.collapseSection")
assert n == 5, n
u = re.sub(r",\n                expandLabel = ds\.expandSection,\n                collapseLabel = ds\.collapseSection", "", u)
d.write_text(u, encoding="utf-8")
print("ok")
