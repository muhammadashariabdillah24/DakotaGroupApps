package com.dakotagroupstaff.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.dakotagroupstaff.R
import com.dakotagroupstaff.databinding.ActivityHelpCenterBinding

class HelpCenterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHelpCenterBinding
    private lateinit var adapter: HelpGuideAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelpCenterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.help_center)
        }
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        val helpGuides = getHelpGuides()
        adapter = HelpGuideAdapter(helpGuides)
        
        binding.rvHelpGuides.apply {
            layoutManager = LinearLayoutManager(this@HelpCenterActivity)
            adapter = this@HelpCenterActivity.adapter
        }
    }

    private fun getHelpGuides(): List<HelpGuideItem> {
        return listOf(
            HelpGuideItem(
                title = "Fitur Absensi",
                description = "Fitur Absensi digunakan karyawan untuk melakukan Absen Masuk dan Absen Pulang dan serta fitur ini dilengkapi titik absen sesuai dengan koordinat. Pastikan GPS dan lokasi aktif saat melakukan absensi agar sistem dapat memvalidasi posisi Anda."
            ),
            HelpGuideItem(
                title = "Fitur Pengajuan Cuti",
                description = "Fitur ini memungkinkan karyawan untuk mengajukan permohonan cuti. Anda dapat memilih jenis cuti (Tahunan, Sakit, Izin, dll), menentukan tanggal mulai dan akhir cuti, serta menambahkan keterangan. Pengajuan akan dikirim ke atasan untuk persetujuan."
            ),
            HelpGuideItem(
                title = "Fitur Slip Gaji",
                description = "Melalui fitur ini, karyawan dapat melihat rincian gaji bulanan termasuk gaji pokok, tunjangan, potongan, dan total gaji bersih. Pilih bulan dan tahun untuk melihat slip gaji periode tertentu."
            ),
            HelpGuideItem(
                title = "Fitur Surat Tugas",
                description = "Fitur Surat Tugas menampilkan daftar penugasan yang diberikan kepada karyawan. Anda dapat melihat detail tugas seperti tanggal, lokasi, dan deskripsi tugas. Fitur ini membantu karyawan mengetahui jadwal dan tanggung jawab tugas mereka."
            ),
            HelpGuideItem(
                title = "Fitur Loper (Khusus Driver)",
                description = "Fitur khusus untuk driver/loper yang bertugas mengantarkan barang. Fitur ini mencakup:\n• Check Delivery Status - Mengecek status pengiriman\n• Get Delivery List - Melihat daftar pengiriman\n• Submit Delivery Data - Mengirim data pengiriman\n• Upload Foto Pengiriman - Mengunggah bukti foto barang yang dikirim\n• Upload Tanda Tangan - Mengunggah tanda tangan penerima sebagai bukti terima"
            ),
            HelpGuideItem(
                title = "Profil Karyawan",
                description = "Pada bagian atas halaman utama, terdapat kartu informasi karyawan yang menampilkan foto profil, nama, dan NIP. Klik pada foto profil untuk melihat dalam ukuran penuh dengan fitur zoom dan pan."
            ),
            HelpGuideItem(
                title = "Riwayat Menu",
                description = "Aplikasi secara otomatis menyimpan 5 menu terakhir yang Anda akses untuk memudahkan akses cepat. Riwayat menu ditampilkan di halaman utama dalam bentuk scroll horizontal."
            ),
            HelpGuideItem(
                title = "Informasi Akun",
                description = "Menu Pengaturan > Informasi Akun menampilkan detail lengkap data kepegawaian Anda meliputi: Nama, NIP, Area Kerja, BPJS Kesehatan, Jamsostek, NPWP, Status Sosial, Jam Kerja, dan Status Pegawai."
            ),
            HelpGuideItem(
                title = "Penggunaan Data",
                description = "Menu Pengaturan > Penggunaan Data menampilkan grafik konsumsi data aplikasi selama 3 bulan terakhir. Informasi ini membantu Anda memantau penggunaan kuota internet untuk aplikasi Dakota Group Staff."
            )
        )
    }
}
