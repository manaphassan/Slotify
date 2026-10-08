# 🚀 Slotify v1.0.0 — Official Release Notes

> **Versi:** `1.0.0` (Build `1`)  
> **Tarikh Keluaran:** 9 Oktober 2026  
> **Platform:** Android 8.0+ (API 26 – API 35)  
> **Pelesenan:** Hak Cipta Terpelihara © 2026 HaNa Innovation

---

## 🌟 Pengenalan & Sorotan Utama

**Slotify** ialah aplikasi pengurusan kehidupan persekolahan pintar berasaskan seni reka **Neumorphic** yang direka khas untuk keluarga Malaysia. Dibina menggunakan teknologi **Android Native (Kotlin 2.0 + Jetpack Compose)**, Slotify menyatukan pengurusan jadual waktu dwi-sesi KPM/JAIS, logistik pergerakan anak-anak di Bandar Contoh/Klang Valley, tabung simpanan maya, dan sistem ganjaran motivasi berteraskan bintang.

---

## 📦 Pakej Binaan Keluaran (Release Artifacts)

| Format Fail | Lokasi Direktori | Saiz | Kegunaan |
|---|---|:---:|---|
| **Standalone APK** | `app/build/outputs/apk/release/app-release.apk` | **3.89 MB** | Pemasangan terus ke telefon pintar Android (Sideloading / ADB) |
| **Play Store AAB** | `app/build/outputs/bundle/release/app-release.aab` | **6.76 MB** | Format rasmi penerbitan Google Play Console |

---

## ✨ Ciri-Ciri Utama Keluaran v1.0.0

### 1. 🏫 Pengurusan Jadual Waktu Dwi-Sesi (KPM & SRA)
* **Paparan Grid Isnin – Jumaat**: Jadual lengkap waktu persekolahan kebangsaan (SK) dan sekolah rendah agama (SRA / SRAI) berpandukan data jadual sebenar.
* **Palet Warna Subjek KPM**: Pengelasan visual automatik mengikut subjek (Bahasa Melayu, Bahasa Inggeris, Sains, Matematik, Pendidikan Islam, Bahasa Arab, Sejarah, RBT, Muzik, Seni Visual, dll.).
* **Pertukaran Sesi Automatik**: Butang peralihan sesi Darjah 4 (Petang) kepada Darjah 5 (Pagi) tanpa perlu memasukkan jadual semula.
* **Pembantu Beg "Buku Kena Bawa"**: Menjana senarai buku teks dan aktiviti yang perlu dibawa esok berdasarkan jadual hari seterusnya berserta animasi `+5 ⭐ Bintang` apabila beg siap dikemas.

---

### 2. ⏰ Automasi & Notifikasi Rutin Harian
* **07:00 AM Peringatan Beg Transit**: Memastikan beg transit Adam & Aisyah diturunkan di Taska Ceria sebelum ke sekolah.
* **08:30 PM Panduan Baju Seragam Esok**:
  * **Isnin**: Baju Rasmi Sekolah & Tali Leher
  * **Selasa**: Baju Sukan Lengkap
  * **Rabu**: Pakaian Unit Kokurikulum (Pengakap / Bulan Sabit Merah)
  * **Khamis**: Baju Batik Rasmi Sekolah
  * **Jumaat**: Baju Melayu Lengkap bersamping / Baju Kurung
* **Kad Pencetus Notifikasi Ujian**: Uji notifikasi secara langsung pada bila-bila masa melalui papan pemuka.

---

### 3. 🗺️ Logistik OpenStreetMap & Laluan Berantai OSRM
* **Peta Vektor OSMDroid Luar Talian/Dalam Talian**: Paparan peta vektor interaktif tanpa sebarang kebergantungan kepada perkhidmatan berbayar Google Maps API.
* **Laluan Berantai Petang Abah**: Pengiraan jarak dan masa perjalanan (*ETA*) bagi laluan:  
  `Pejabat ➔ Tadika Danial ➔ Taska Aisyah ➔ Rumah` menggunakan geometri jalan sebenar (GeoJSON polyline).
* **Radar Geofens Berperingkat**: Bulatan zon amaran 300m, 1,000m, dan 3,000m berserta simulasi radar pergerakan Van Sekolah A dan Van Sekolah B.

---

### 4. 🎓 Pentaksiran Akademik PBD & Tugasan Sekolah
* **Pentaksiran Bilik Darjah (PBD TP1–TP6)**: Merekod dan memantau Tahap Penguasaan mengikut format Kurikulum Standard Sekolah Rendah (KSSR) KPM berserta bar kemajuan berwarna.
* **Ganjaran Bintang Automatik**: Penguasaan tahap cemerlang (TP5 & TP6) memberikan `+15 ⭐ Bintang`, manakala tahap asas memberikan `+5 ⭐ Bintang`.
* **Pengurus Kerja Rumah & Foto Lembaran**: Borang pendaftaran kerja rumah berserta simulasi tangkapan gambar foto lembaran kerja yang telah diselesaikan.

---

### 5. 🔒 Keselamatan Ibu Bapa (Parent PIN Vault)
* **Penyulitan Kriptografi SHA-256**: Pengesahan PIN 4-digit ibu bapa sebelum anak-anak boleh menebus ganjaran di Bank Ganjaran (*Reward Vault*).
* **Kunci Keselamatan Automatik**: Mengunci akses selama 30 saat sekiranya berlaku 3 kali percubaan PIN yang salah secara berturut-turut.

---

### 6. 🪙 Duit Poket Kantin, Tabung Maya & Yuran Bulanan
* **Pantauan Belanja Kantin Harian**: Pelarasan pantas duit belanja harian (RM 3, RM 5, atau pelarasan ±RM 0.50) dengan kiraan baki automatik.
* **Tabung Maya Simpanan**: Mengumpul semua wang saku yang dijimatkan sepanjang bulan.
* **Tukar Simpanan ke Bintang**: Menukarkan setiap RM 10 penjimatan tabung kepada `+10 ⭐ Bintang` ganjaran untuk memupuk disiplin kewangan anak.
* **Pengurusan Yuran Komitmen**: Menjejaki status yuran Van Sekolah, Taska, Tadika, dan PIBG berserta dialog penambahan yuran baharu dan penapis status bayaran.

---

### 7. 🗓️ Takwim Persekolahan KPM 2026/2027 (Kumpulan B - Selangor)
* **Kalendar Rasmi Akademik KPM**: Penggal 1, Penggal 2, Penggal 3, Cuti Penggal, dan Cuti Akhir Sesi 2026/2027 bagi sekolah-sekolah di Negeri Selangor.
* **Cuti Kelepasan Am Negeri Selangor**: Hari Keputeraan Sultan Selangor, Hari Kebangsaan, Hari Raya Aidilfitri, Thaipusam, dll.
* **Dialog Takwim Interaktif & Banner Status Harian**: Menunjukkan status persekolahan hari ini serta undur hari ke cuti sekolah yang berikutnya.

---

### 8. 🎨 Seni Bina & Pengalaman Pengguna (UX)
* **Enjin Bayang Neumorphic**: Fizik dwi-bayang (*soft light & ambient shadow*) dengan interaksi sentuhan nyata.
* **Sokongan Tema Dwi-Bahasa & Nord**: Bahasa Melayu, English, serta mod palet warna Artik Nord.
* **Integriti Luar Talian Penuh**: Pangkalan data tempatan Room SQLite memastikan aplikasi beroperasi sepenuhnya walaupun tanpa sambungan internet.
* **Pengoptimuman R8 / ProGuard**: Kod sumber dipadatkan dan dilindungi dengan saiz fail APK hanya **3.89 MB**.

---

## 🛠️ Panduan Pemasangan & Ujian

### Pemasangan Menggunakan ADB (Kabel USB)
1. Sambungkan telefon pintar Android ke komputer dan aktifkan **USB Debugging** dalam Tetapan Pembangun (*Developer Options*).
2. Jalankan arahan berikut dari terminal projek:
   ```bash
   adb install -r app/build/outputs/apk/release/app-release.apk
   ```

### Pemasangan Manual (Sideloading)
1. Salin fail `app/build/outputs/apk/release/app-release.apk` ke dalam storan telefon atau hantar melalui Telegram / WhatsApp Web.
2. Buka fail tersebut pada telefon dan pilih **Pasang (Install)** (Benarkan pemasangan daripada sumber tidak dikenali jika diminta).

---

## 📈 Sejarah Komit Keluaran Utama

* `e229ade` — *feat(release): complete M6 production release with R8 minification, signed APK, and AAB bundle*
* `4fe0974` — *feat(homework): add worksheet photo proof dialog and mark M4 and M5 as SIAP*
* `f794be3` — *feat(finance-takwim): complete M5 canteen savings tracker, fee management, and KPM 2026/2027 calendar*
* `618ee54` — *feat: complete M2 routine alerts, M3 OSRM live routing, and M4 PBD assessment tracking*
* `96c1fd8` — *feat(timetable): parse and seed real SK 4A timetable from ICS*
* `7eb2e18` — *feat(ui): initialize feature screens, core services, and gradle wrapper*
