package my.slotify.app.core.designsystem.theme

/** Student profile screen and its dialogs. Accessed as `strings.profileUi`. */
data class ProfileStrings(
    // Section headers
    val groupPersonal: String,
    val groupSchool: String,
    val groupAssessment: String,

    // Hero card
    val classWithName: (String) -> String,
    val classFallback: String,

    // MyKid card
    val hideMyKid: String,
    val showMyKid: String,

    // School & session card
    val secondarySchoolLabel: String,
    val sessionDual: String,
    val sessionIntegratedIslamic: String,
    val sessionTadikaDaycare: String,

    // Transport card
    val dropOffByParents: String,
    val ownVehicle: String,
    val transportActive: String,

    // PBD section
    val pbdTitle: String,
    val pbdSubtitle: String,
    val recordTpButton: String,
    val pbdEmpty: (String) -> String,
    val tpLevel1: String,
    val tpLevel2: String,
    val tpLevel3: String,
    val tpLevel4: String,
    val tpLevel5: String,
    val tpLevel6: String,
    val teacherCommentQuote: (String) -> String,

    // PBD record result
    val pbdRecordedWithStars: (Int, String, Int) -> String,
    val pbdUpdated: (Int, String) -> String,

    // Add PBD dialog
    val addPbdTitle: String,
    val addPbdSubtitle: (String) -> String,
    val subjectLabel: String,
    val masteryLevelLabel: String,
    val teacherCommentLabel: String,
    val bonusRewardHint: (Int) -> String,
    val saveTpButton: String
)

val BmProfileStrings = ProfileStrings(
    groupPersonal = "Maklumat Peribadi",
    groupSchool = "Sekolah & Pengangkutan",
    groupAssessment = "Pentaksiran (PBD)",

    // Hero card
    classWithName = { name -> "Kelas: $name" },
    classFallback = "Kelas KPM",

    // MyKid card
    hideMyKid = "Sembunyikan",
    showMyKid = "Tunjukkan",

    // School & session card
    secondarySchoolLabel = "Sekolah Agama / Transit:",
    sessionDual = "Sesi Dual (SRA + SK)",
    sessionIntegratedIslamic = "Sesi Bersepadu Islam",
    sessionTadikaDaycare = "Tadika & Daycare",

    // Transport card
    dropOffByParents = "Dihantar Ibu Bapa",
    ownVehicle = "Kenderaan Sendiri",
    transportActive = "Pengangkutan Aktif",

    // PBD section
    pbdTitle = "Pentaksiran Bilik Darjah (PBD)",
    pbdSubtitle = "Tahap Penguasaan TP1 – TP6 KPM",
    recordTpButton = "Rekod TP",
    pbdEmpty = { name ->
        "Belum ada rekod PBD untuk $name. Ketik 'Rekod TP' untuk memasukkan tahap penguasaan."
    },
    tpLevel1 = "TP 1 • Permulaan",
    tpLevel2 = "TP 2 • Asas",
    tpLevel3 = "TP 3 • Memuaskan",
    tpLevel4 = "TP 4 • Baik",
    tpLevel5 = "TP 5 • Terpuji",
    tpLevel6 = "TP 6 • Cemerlang",
    teacherCommentQuote = { comment -> "Ulasan: \"$comment\"" },

    // PBD record result
    pbdRecordedWithStars = { tp, subject, stars -> "🎉 TP$tp $subject direkodkan! +$stars ⭐" },
    pbdUpdated = { tp, subject -> "TP$tp $subject dikemas kini." },

    // Add PBD dialog
    addPbdTitle = "Rekod Tahap Penguasaan (PBD)",
    addPbdSubtitle = { name -> "Pilih subjek KPM dan Tahap Penguasaan (TP1-TP6) untuk $name" },
    subjectLabel = "Subjek:",
    masteryLevelLabel = "Tahap Penguasaan (TP 1 - 6):",
    teacherCommentLabel = "Ulasan Guru / Catatan",
    bonusRewardHint = { tp -> "⭐ Ganjaran Bonus: Pencapaian TP$tp melayakkan +15 Bintang!" },
    saveTpButton = "Simpan TP"
)

val EnProfileStrings = ProfileStrings(
    groupPersonal = "Personal Details",
    groupSchool = "School & Transport",
    groupAssessment = "Assessments (PBD)",

    // Hero card
    classWithName = { name -> "Class: $name" },
    classFallback = "KPM Class",

    // MyKid card
    hideMyKid = "Hide",
    showMyKid = "Show",

    // School & session card
    secondarySchoolLabel = "Religious / Transit School:",
    sessionDual = "Dual Session (SRA + SK)",
    sessionIntegratedIslamic = "Integrated Islamic Session",
    sessionTadikaDaycare = "Kindergarten & Daycare",

    // Transport card
    dropOffByParents = "Dropped off by parents",
    ownVehicle = "Own vehicle",
    transportActive = "Transport Active",

    // PBD section
    pbdTitle = "Classroom Assessment (PBD)",
    pbdSubtitle = "KPM mastery levels TP1 – TP6",
    recordTpButton = "Record TP",
    pbdEmpty = { name ->
        "No PBD records for $name yet. Tap 'Record TP' to enter a mastery level."
    },
    tpLevel1 = "TP 1 • Beginning",
    tpLevel2 = "TP 2 • Basic",
    tpLevel3 = "TP 3 • Satisfactory",
    tpLevel4 = "TP 4 • Good",
    tpLevel5 = "TP 5 • Commendable",
    tpLevel6 = "TP 6 • Excellent",
    teacherCommentQuote = { comment -> "Comment: \"$comment\"" },

    // PBD record result
    pbdRecordedWithStars = { tp, subject, stars -> "🎉 TP$tp $subject recorded! +$stars ⭐" },
    pbdUpdated = { tp, subject -> "TP$tp $subject updated." },

    // Add PBD dialog
    addPbdTitle = "Record Mastery Level (PBD)",
    addPbdSubtitle = { name -> "Choose a KPM subject and mastery level (TP1-TP6) for $name" },
    subjectLabel = "Subject:",
    masteryLevelLabel = "Mastery Level (TP 1 - 6):",
    teacherCommentLabel = "Teacher's Comment / Notes",
    bonusRewardHint = { tp -> "⭐ Bonus Reward: Reaching TP$tp earns +15 Stars!" },
    saveTpButton = "Save TP"
)
