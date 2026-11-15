import pathlib, re
root = pathlib.Path("app/src/main/java/my/slotify/app")

def blocks_of(s, markers, end_idx):
    pos = []
    for name, m in markers:
        hits = [x.start() + 1 for x in re.finditer(re.escape("
" + m), s)]
        if len(hits) != 1:
            raise SystemExit(f"marker {name}: found {len(hits)}")
        pos.append((name, hits[0]))
    if [p for _, p in pos] != sorted(p for _, p in pos):
        raise SystemExit("markers out of order")
    out = {}
    for k, (name, start) in enumerate(pos):
        stop = pos[k + 1][1] if k + 1 < len(pos) else end_idx
        out[name] = s[start:stop]
    return pos[0][1], out

def indent(text, n=4):
    pad = " " * n
    return "".join((pad + l) if l.strip() else l for l in text.splitlines(keepends=True))

def section(ind, key, title, state, parts, blocks, summary=None):
    i = " " * ind
    summ = f",\n{i}    summary = {summary}" if summary else ""
    body = "".join(indent(blocks[p]) for p in parts).rstrip()
    return (f"{i}slotifySection(\n{i}    key = \"{key}\",\n{i}    title = {title},\n"
            f"{i}    expanded = {state},\n{i}    onToggle = {{ {state} = !{state} }}{summ}\n"
            f"{i}) {{\n{body}\n{i}}}\n\n")

def states(decls):
    return "".join(f"    var {n} by rememberSaveable {{ mutableStateOf({v}) }}\n" for n, v in decls)

def imports(s):
    for imp in ["my.slotify.app.core.designsystem.component.slotifySection",
                "androidx.compose.runtime.saveable.rememberSaveable"]:
        line = f"import {imp}\n"
        if line not in s:
            m = list(re.finditer(r"^import .*\n", s, re.M))
            s = s[:m[-1].end()] + line + s[m[-1].end():]
    return s

def must(s, a, b):
    if s.count(a) != 1:
        raise SystemExit(f"expected 1 of {a[:90]!r}, found {s.count(a)}")
    return s.replace(a, b)

# ---------------- Timetable
p = root / "feature/timetable/TimetableScreen.kt"
s = p.read_text(encoding="utf-8")
sp_line = "            item { Spacer(Modifier.height(16.dp)) }\n"
end = s.index(sp_line) + len(sp_line)
first, b = blocks_of(s, [
    ("header", "            // Header\n"), ("tabs", "            // View Mode Switcher\n"),
    ("tracker", "            // Realtime Tracker Card\n"), ("content", "            // View Content\n"),
    ("spacer", sp_line)], end)
# Title and child name are in the top bar now; keep the session badge and the actions.
b["header"] = must(b["header"], """                        Text(
                            text = strings.timetableTitle,
                            style = MaterialTheme.typography.headlineLarge,
                            color = SlotifyColors.TextPrimary
                        )
""", "")
b["header"] = must(b["header"], """                            Text(
                                text = "${selectedStudent?.nickname ?: ts.studentFallback} • ${selectedStudent?.currentLevel ?: "KPM"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlotifyColors.TextSecondary
                            )
""", "")
body = (b["header"] + b["tabs"] + b["content"]
        + section(12, "tracker", "ts.groupTracker", "trackerOpen", ["tracker"], b,
                  "activeSlot?.let { ts.trackerSummary(it.subjectName) }")
        + b["spacer"])
s = s[:first] + body + s[end:]
s = must(s, "    Box(modifier = Modifier.fillMaxSize()) {\n        LazyColumn(",
         states([("trackerOpen", "false")]) + "\n    Box(modifier = Modifier.fillMaxSize()) {\n        LazyColumn(")
p.write_text(imports(s), encoding="utf-8")
print("timetable ok")

# ---------------- Profile
p = root / "feature/profile/StudentProfileScreen.kt"
s = p.read_text(encoding="utf-8")
let_open = "        selectedStudent?.let { s ->\n"
li = s.index(let_open)
let_close = s.index("\n        }\n", s.index("            // ── Pentaksiran Bilik Darjah")) + 1
inner_start = li + len(let_open)
first, b = blocks_of(s, [
    ("hero", "            item {\n"), ("bio", "            // Biometrics & MyKid Card"),
    ("school", "            // School & Session Info Card"), ("transport", "            // Transport & Van Contact"),
    ("pbd", "            // ── Pentaksiran Bilik Darjah")] if False else [
    ("bio", "            // Biometrics & MyKid Card"),
    ("school", "            // School & Session Info Card"), ("transport", "            // Transport & Van Contact"),
    ("pbd", "            // ── Pentaksiran Bilik Darjah")], let_close)
hero = s[inner_start:first]
inner = (hero
         + section(12, "school", "ps.groupSchool", "schoolOpen", ["school", "transport"], b)
         + section(12, "assessment", "ps.groupAssessment", "assessmentOpen", ["pbd"], b)
         + section(12, "personal", "ps.groupPersonal", "personalOpen", ["bio"], b)).rstrip() + "\n"
s = s[:inner_start] + inner + s[let_close:]

# Header + child switcher -> one row: child chips and the edit button
hs = s.index("        // Top Header\n")
cs = s.index("        // Child Switcher Row\n")
ce = s.index("        // Selected Student Hero Card\n")
child = s[cs:ce]
lr_start = child.index("            LazyRow(")
lr_end = child.rindex("        }\n")
lazyrow = child[lr_start:lr_end]
new_header = ("        // Child switcher and edit button\n"
              "        item {\n"
              "            Row(verticalAlignment = Alignment.CenterVertically) {\n"
              "                Box(modifier = Modifier.weight(1f)) {\n"
              + indent(lazyrow, 8) +
              "                }\n"
              "                IconButton(onClick = { showEditDialog = true }) {\n"
              "                    Icon(\n"
              "                        imageVector = Icons.Default.Edit,\n"
              "                        contentDescription = strings.editProfileTitle,\n"
              "                        tint = SlotifyColors.KpmSkyBlue\n"
              "                    )\n"
              "                }\n"
              "            }\n"
              "        }\n\n")
s = s[:hs] + new_header + s[ce:]
s = must(s, "    LazyColumn(\n        modifier = Modifier\n            .fillMaxSize()\n            .background(SlotifyColors.CanvasBase)\n            .padding(horizontal = 14.dp, vertical = 10.dp),",
         states([("schoolOpen", "true"), ("assessmentOpen", "true"), ("personalOpen", "false")])
         + "\n    LazyColumn(\n        modifier = Modifier\n            .fillMaxSize()\n            .background(SlotifyColors.CanvasBase)\n            .padding(horizontal = 14.dp, vertical = 10.dp),")
p.write_text(imports(s), encoding="utf-8")
print("profile ok", "val ps = strings.profileUi" in s)
