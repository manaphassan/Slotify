# 🗺️ Slotify — Product Development Roadmap

> **Target Release:** Q4 2026 – Q1 2027  
> **Platform:** Android Native (Jetpack Compose)  
> **Current Version:** `1.0.0-alpha01`

---

```mermaid
gantt
    title Pelan Pembangunan Slotify (Roadmap)
    dateFormat  YYYY-MM-DD
    section Fasa 1: Teras & UI
    Scaffolding Projek & Enjin Neumorphic Compose    :done, 2026-09-21, 7d
    Pangkalan Data Room & Data Awal Keluarga        :done, 2026-09-21, 7d
    section Fasa 2: Jadual & Beg
    Jadual Waktu Interaktif & Subjek KPM            :active, 2026-10-01, 14d
    Senarai Semak Beg Sekolah (Buku Kena Bawa)      :2026-10-15, 10d
    section Fasa 3: Logistik & Peta
    Integrasi Peta Vektor OSM & OSRM Routing        :2026-10-25, 14d
    Laluan Ambilan Berantai Abah & Geofens Van      :2026-11-08, 14d
    section Fasa 4: Ganjaran & Akademik
    Bank Ganjaran Bintang & Sistem Penebusan Hadiah :2026-11-22, 12d
    Rekod Pentaksiran PBD (TP1-TP6) & Markah UASA   :2026-12-04, 14d
    section Fasa 5: Kewangan & Takwim
    Pantauan Duit Poket Kantin & Tabung Simpanan    :2026-12-18, 10d
    Peringatan Yuran (Van, Taska, Tadika, PIBG)     :2026-12-28, 10d
    Penyelarasan Takwim Cuti Sekolah KPM 2026/2027  :2027-01-08, 10d
    section Fasa 6: Ujian & Pelancaran
    Ujian Beta Keluarga Keluarga (Real-world testing)   :2027-01-18, 14d
    Bina APK / AAB untuk Google Play Store          :2027-02-01, 7d
```

---

## 📌 Huraian Fasa Pembangunan

### 🔹 Fasa 1: Asas Teras & Seni Bina UI Neumorphic (SIAP ✅)
- [x] Inisialisasi projek Android Native dengan Kotlin 2.0 & Jetpack Compose BOM.
- [x] Reka cipta modifier Neumorphic (`NeumorphicModifier.kt`) dengan fizik bayang berkembar.
- [x] Skema pangkalan data Room (`StudentEntity`, `SchoolSessionEntity`, `TimetableSlotEntity`, `LocationNodeEntity`).
- [x] Seeder data sebenar keluarga (Adam, Aisyah, Danial).

### 🔹 Fasa 2: Pengurusan Jadual & Pembantu Beg Sekolah (SIAP ✅)
- [x] Paparan jadual penuh Isnin hingga Jumaat dengan penapis hari dan sesi.
- [x] Logik pertukaran sesi automatik Tahun 4 vs Tahun 5 (Pagi/Petang).
- [x] Ciri pintar "Buku Kena Bawa" berpandukan jadual hari esok berserta butang ganjaran `+5 ⭐`.
- [x] Panduan baju seragam harian (Isnin: Rasmi, Selasa: Sukan, Rabu: Koku, Khamis: Batik, Jumaat: Baju Melayu) dan sistem notifikasi berjadual.

### 🔹 Fasa 3: Logistik, Peta OpenStreetMap & Pengesanan Van (SIAP ✅)
- [x] Integrasi paparan peta OpenStreetMap (OSMDroid / MapLibre Native).
- [x] Pengiraan laluan berantai petang Abah (`Pejabat ➔ Tadika Danial ➔ Taska Aisyah ➔ Rumah`) berserta ETA masa pemanduan dan paparan garis jalan sebenar (GeoJSON).
- [x] Paparan status geofens Van Sekolah A dan Van Sekolah B serta amaran radar jarak dekat.

### 🔹 Fasa 4: Bank Ganjaran Bintang & Pentaksiran Akademik (SIAP ✅)
- [x] Pengumpulan syiling bintang secara automatik bila siap tugasan dan penilaian cemerlang.
- [x] Katalog penebusan hadiah dengan pengesahan PIN ibu bapa (SHA-256 + 3 Percubaan).
- [x] Modul rekod markah dan laporan PBD (Tahap Penguasaan TP1-TP6) berasaskan standard KPM.
- [x] Borang kerja rumah dengan fungsi tangkapan kamera bukti lembaran kerja (`AddHomeworkTaskDialog`).

### 🔹 Fasa 5: Pengurusan Duit Poket, Yuran & Takwim KPM (SIAP ✅)
- [x] Rekod belanja kantin harian & simpanan baki ke Tabung Maya dengan insentif +10 ⭐ Bintang.
- [x] Pusat pengurusan yuran bulanan (Van A, Taska, Tadika, PIBG) dengan status bayaran & dialog penambahan yuran.
- [x] Sinkronisasi kalendar takwim cuti sekolah KPM 2026/2027 (Kumpulan B - Selangor) dengan paparan dialog penuh & banner status sesi di dashboard.

### 🔹 Fasa 6: Ujian Beta & Pelancaran (Akan Datang ⚪)
- [ ] Pemasangan APK terus ke telefon bimbit Keluarga untuk ujian lapangan harian.
- [ ] Penambahbaikan berdasarkan pengalaman penggunaan sebenar.
- [ ] Pembungkusan AAB ditandatangani (*Signed Release AAB*) untuk Google Play Store.
