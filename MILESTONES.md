# 🏆 Slotify — Release Milestones & Execution Checklist

> **Gating Criteria & Release Milestones for Slotify (v1.0.0)**

---

## 🎯 Milestone Overview

| Milestone | Target Tag | Focus Area | Status |
|---|---|---|:---:|
| **M1: Foundation & Seeding** | `v1.0.0-alpha01` | Architecture, Room Database, Neumorphic UI & Real Family Data | ✅ **SIAP** |
| **M2: Dynamic Timetable & Bag Helper** | `v1.0.0-alpha02` | Multi-Session Grid, Attire Guide & Packing Assistant | ✅ **SIAP** |
| **M3: OSM Logistics & Chained ETA** | `v1.0.0-beta01` | OpenStreetMap Map View, OSRM Route Calculator & Van Alerts | ✅ **SIAP** |
| **M4: Reward Vault & Academics** | `v1.0.0-beta02` | Star Coins, Parent PIN Authorization & PBD/UASA Records | 🟡 **Dalam Proses (75%)** |
| **M5: Finance, Takwim & Polish** | `v1.0.0-rc01` | Canteen Tracker, Fee Alerts & KPM Takwim 2026/2027 | ⚪ **Belum Mula** |
| **M6: Production Release** | `v1.0.0` | Signed AAB/APK, Google Play Store Publishing | ⚪ **Belum Mula** |

---

## 📋 Detailed Milestone Execution Checklist

### ✅ M1: Foundation & Seeding (`v1.0.0-alpha01`) — SIAP
- [x] Sediakan struktur direktori projek Android Native (`D:\HaNa_Innovation\slotify`).
- [x] Pasang konfigurasi Gradle, Kotlin 2.0 Compose Plugin & KSP.
- [x] Bina enjin bayang Neumorphic `NeumorphicModifier.kt` dengan kanvas dwi-bayang.
- [x] Cipta entiti Room Database (`StudentEntity`, `SchoolSessionEntity`, `TimetableSlotEntity`, `LocationNodeEntity`).
- [x] Tulis `DatabaseSeeder.kt` dengan data sebenar Adam (SRA/SK), Aisyah (SRAI), dan Danial (Tadika Ceria).
- [x] Wujudkan dwisistem bahasa (`res/values/strings.xml` dan `res/values-en/strings.xml`).

---

### ✅ M2: Dynamic Timetable & Bag Helper (`v1.0.0-alpha02`) — SIAP
- [x] Bina paparan jadual waktu penuh dengan pertukaran tab Isnin hingga Jumaat.
- [x] Hubungkan kod subjek KPM (`BM`, `MT`, `SN`, `PI`, dll.) dengan palet warna rasmi.
- [x] Tambah butang pertukaran automatik sesi Darjah 4 ➜ Darjah 5 (Pagi/Petang).
- [x] Jana senarai "Buku Kena Bawa" berdasarkan jadual hari esok berserta animasi `+5 ⭐ Bintang`.
- [x] Peringatan pagi hantar beg transit Adam/Aisyah ke Taska Ceria (07:00 AM) & notifikasi persediaan pakaian malam (08:30 PM).

---

### ✅ M3: OSM Logistics & Chained ETA (`v1.0.0-beta01`) — SIAP
- [x] Integrasikan `osmdroid-android` untuk memaparkan peta vektor OpenStreetMap luar talian/dalam talian.
- [x] Uji pengiraan jarak dan ETA bagi laluan berantai Abah (`Pejabat ➔ Tadika Danial ➔ Taska Aisyah ➔ Rumah`) menggunakan perkhidmatan OSRM dengan laluan jalan sebenar (GeoJSON polyline).
- [x] Paparkan lingkaran jejari geofens (300m, 1000m, 3000m) pada peta.
- [x] Paparkan status pergerakan Van Sekolah A dan Van Sekolah B serta notifikasi radar geofens berperingkat.

---

### 🟡 M4: Reward Vault & Academics (`v1.0.0-beta02`) — DALAM PROSES
- [x] Tambah modal dialog kebenaran PIN ibu bapa sebelum menebus ganjaran (SHA-256 dan kunci 3-kali percubaan).
- [ ] Cipta borang rekod kerja rumah dengan fungsi tangkapan gambar lembaran kerja.
- [x] Bina skrin rekod markah peperiksaan UASA dan Tahap Penguasaan PBD (TP1–TP6).
- [x] Sambungkan ganjaran bintang automatik apabila slip PBD/UASA dimasukkan (+15 ⭐ untuk TP5/TP6, +5 ⭐ untuk pencapaian asas).

---

### ⚪ M5: Finance, Takwim & Release Candidate (`v1.0.0-rc01`)
- [ ] Log belanja kantin harian dengan pengiraan baki tabung secara automatik.
- [ ] Pengurusan yuran bulanan (Van A, Taska Ceria, Tadika Danial, PIBG).
- [ ] Integrasikan kalendar Takwim KPM 2026/2027 (Kumpulan B) untuk cuti penggal dan cuti peristiwa.
- [ ] Ujian aplikasi pada peranti fizikal Android.

---

### ⚪ M6: Production Release (`v1.0.0`)
- [ ] Jana kunci tandatangan keluaran (*Release Signing Keystore*).
- [ ] Uji binaan keluaran ProGuard / R8 minification.
- [ ] Terbitkan fail APK / AAB keluaran akhir.
