package com.banktracker.vn.ui

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.banktracker.vn.databinding.ActivitySettingsBinding
import com.banktracker.vn.data.model.BankCode
import com.banktracker.vn.utils.PreferencesManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var preferencesManager: PreferencesManager

    companion object {
        private const val REQUEST_CODE_SOUND_PICKER = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferencesManager = PreferencesManager(this)

        setupToolbar()
        loadSettings()
        setupListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Cài đặt"
    }

    private fun loadSettings() {
        // Âm thanh & rung
        binding.switchSound.isChecked = preferencesManager.isSoundEnabled()
        binding.switchVibration.isChecked = preferencesManager.isVibrationEnabled()
        binding.switchMaxVolume.isChecked = preferencesManager.isMaxVolumeEnabled()
        binding.switchTTS.isChecked = preferencesManager.isTTSEnabled()

        // Ngưỡng tiền
        binding.etMinAmount.setText(preferencesManager.getMinimumAmount().toInt().toString())
        binding.etLargeAmount.setText(preferencesManager.getLargeAmountThreshold().toInt().toString())

        // Ẩn/hiện phần âm thanh phụ
        updateSoundSettingsVisibility(binding.switchSound.isChecked)

        // Ngân hàng — bật/tắt theo Preferences
        BankCode.values().forEach { bank ->
            when (bank) {
                BankCode.MB -> binding.switchMB.isChecked = preferencesManager.isBankEnabled(bank.code)
                BankCode.VCB -> binding.switchVCB.isChecked = preferencesManager.isBankEnabled(bank.code)
                BankCode.TCB -> binding.switchTCB.isChecked = preferencesManager.isBankEnabled(bank.code)
                BankCode.VIETIN -> binding.switchVietin.isChecked = preferencesManager.isBankEnabled(bank.code)
                BankCode.ACB -> binding.switchACB.isChecked = preferencesManager.isBankEnabled(bank.code)
                BankCode.AGRI -> binding.switchAgri.isChecked = preferencesManager.isBankEnabled(bank.code)
                BankCode.BIDV -> binding.switchBIDV.isChecked = preferencesManager.isBankEnabled(bank.code)
                // --- 3 ngân hàng mới ---
                BankCode.MSB -> binding.switchMSB.isChecked = preferencesManager.isBankEnabled(bank.code)
                BankCode.TIMO -> binding.switchTimo.isChecked = preferencesManager.isBankEnabled(bank.code)
                BankCode.MOMO -> binding.switchMoMo.isChecked = preferencesManager.isBankEnabled(bank.code)
                else -> {}
            }
        }
    }

    private fun setupListeners() {
        // Âm thanh
        binding.switchSound.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setSoundEnabled(isChecked)
            updateSoundSettingsVisibility(isChecked)
        }
        binding.switchVibration.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setVibrationEnabled(isChecked)
        }
        binding.switchMaxVolume.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setMaxVolumeEnabled(isChecked)
        }
        binding.switchTTS.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setTTSEnabled(isChecked)
        }

        // Chọn âm thanh tùy chỉnh
        binding.btnChooseSound.setOnClickListener {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Chọn âm thanh thông báo")
                preferencesManager.getCustomSoundUri()?.let {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, it)
                }
            }
            startActivityForResult(intent, REQUEST_CODE_SOUND_PICKER)
        }

        // Lưu ngưỡng tiền
        binding.btnSaveMinAmount.setOnClickListener {
            val amount = binding.etMinAmount.text.toString().toDoubleOrNull() ?: 0.0
            preferencesManager.setMinimumAmount(amount)
            showToast("Đã lưu ngưỡng tối thiểu")
        }
        binding.btnSaveLargeAmount.setOnClickListener {
            val amount = binding.etLargeAmount.text.toString().toDoubleOrNull() ?: 10_000_000.0
            preferencesManager.setLargeAmountThreshold(amount)
            showToast("Đã lưu ngưỡng giao dịch lớn")
        }

        // Ngân hàng truyền thống
        binding.switchMB.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setBankEnabled(BankCode.MB.code, isChecked)
        }
        binding.switchVCB.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setBankEnabled(BankCode.VCB.code, isChecked)
        }
        binding.switchTCB.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setBankEnabled(BankCode.TCB.code, isChecked)
        }
        binding.switchVietin.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setBankEnabled(BankCode.VIETIN.code, isChecked)
        }
        binding.switchACB.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setBankEnabled(BankCode.ACB.code, isChecked)
        }
        binding.switchAgri.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setBankEnabled(BankCode.AGRI.code, isChecked)
        }
        binding.switchBIDV.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setBankEnabled(BankCode.BIDV.code, isChecked)
        }

        // --- 3 ngân hàng mới ---
        binding.switchMSB.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setBankEnabled(BankCode.MSB.code, isChecked)
        }
        binding.switchTimo.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setBankEnabled(BankCode.TIMO.code, isChecked)
        }
        binding.switchMoMo.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setBankEnabled(BankCode.MOMO.code, isChecked)
        }

        // Xóa dữ liệu
        binding.btnClearData.setOnClickListener { showClearDataDialog() }

        updateSoundSettingsVisibility(binding.switchSound.isChecked)
    }

    private fun updateSoundSettingsVisibility(enabled: Boolean) {
        binding.layoutSoundSettings.visibility =
            if (enabled) android.view.View.VISIBLE else android.view.View.GONE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SOUND_PICKER && resultCode == Activity.RESULT_OK) {
            val uri: Uri? = data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            preferencesManager.setCustomSoundUri(uri)
            binding.tvCurrentSound.text = if (uri != null) "Âm thanh tùy chỉnh" else "Mặc định"
            showToast("Đã cập nhật âm thanh thông báo")
        }
    }

    private fun showClearDataDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Xóa dữ liệu")
            .setMessage("Bạn có chắc muốn xóa toàn bộ dữ liệu? Hành động này không thể hoàn tác!")
            .setPositiveButton("Xóa") { _, _ ->
                preferencesManager.clearAll()
                showToast("Đã xóa toàn bộ dữ liệu")
                finish()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { finish(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
