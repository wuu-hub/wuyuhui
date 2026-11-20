package com.example.wordapp

import android.Manifest
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.wordapp.data.WordRepository
import com.example.wordapp.databinding.ActivityMainBinding
import com.example.wordapp.export.BitmapReport
import com.example.wordapp.export.CsvUtil
import com.example.wordapp.export.ShareHelper
import com.example.wordapp.net.OnlineDict
import com.example.wordapp.notify.NotificationHelper
import com.example.wordapp.ui.QuizActivity
import com.example.wordapp.ui.ReviewActivity
import com.example.wordapp.ui.WordListActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: WordRepository

    /** 选择 CSV 文件导入 */
    private val openCsvLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) importCsv(uri)
        }

    /** “另存为” CSV */
    private val createCsvLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
            if (uri != null) saveCsvTo(uri)
        }

    /** 通知权限请求后继续设置提醒 */
    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            pickReminderTime()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = WordRepository.get(this)
        NotificationHelper.ensureChannel(this)

        with(binding) {
            btnReview.setOnClickListener {
                startActivity(Intent(this@MainActivity, ReviewActivity::class.java))
            }
            btnQuiz.setOnClickListener {
                startActivity(Intent(this@MainActivity, QuizActivity::class.java))
            }
            btnList.setOnClickListener {
                startActivity(Intent(this@MainActivity, WordListActivity::class.java))
            }
            btnImport.setOnClickListener { showImportOptions() }
            btnReminder.setOnClickListener { onReminderClick() }
            btnImage.setOnClickListener { shareReportImage() }
            btnCsv.setOnClickListener { showCsvOptions() }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStats()
    }

    private fun refreshStats() {
        val s = repository.stats()
        with(binding) {
            tvDue.text = "今日待复习 ${s.due} 个"
            tvStatTotal.text = s.total.toString()
            tvStatLearned.text = s.learned.toString()
            tvStatMastered.text = s.mastered.toString()
            tvStatRate.text = "${s.correctRate}%"
        }
    }

    // ---------- 词库导入 ----------

    /** 导入入口：本地 CSV 还是联网查词 */
    private fun showImportOptions() {
        AlertDialog.Builder(this)
            .setTitle("导入词库")
            .setItems(arrayOf("从本地 CSV 文件导入", "联网查词并添加")) { _, which ->
                if (which == 0) {
                    openCsvLauncher.launch(arrayOf("text/*", "text/comma-separated-values", "*/*"))
                } else {
                    showOnlineLookupDialog()
                }
            }
            .show()
    }

    /** 输入要查的单词，确认后联网查释义 */
    private fun showOnlineLookupDialog() {
        val padding = (16 * resources.displayMetrics.density).toInt()
        val input = EditText(this).apply {
            hint = "输入英文单词，例如 benefit"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setSingleLine(true)
            setPadding(padding, padding, padding, padding)
        }
        val container = FrameLayout(this).apply { addView(input) }

        val dialog = AlertDialog.Builder(this)
            .setTitle("联网查词")
            .setView(container)
            .setPositiveButton("查询", null)
            .setNegativeButton("取消", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val term = input.text.toString().trim()
                if (term.isEmpty()) {
                    input.error = "先输入一个英文单词"
                    return@setOnClickListener
                }
                dialog.dismiss()
                doOnlineLookup(term)
            }
        }
        dialog.show()
    }

    /** 子线程请求词典，回到主线程提示并入库 */
    private fun doOnlineLookup(term: String) {
        if (!isOnline()) {
            toast("当前没有网络连接，联网查词需要先连网")
            return
        }
        val loading = AlertDialog.Builder(this)
            .setMessage("正在联网查询“$term”…")
            .setCancelable(false)
            .create()
        loading.show()

        Thread {
            val result = OnlineDict.lookup(term)
            runOnUiThread {
                if (isFinishing || isDestroyed) {
                    loading.dismiss()
                    return@runOnUiThread
                }
                loading.dismiss()
                when (result) {
                    is OnlineDict.Result.Success -> {
                        val added = repository.importWords(listOf(result.word))
                        if (added > 0) {
                            toast("已加入词库：${result.word.word}\n${result.word.meaning}")
                            refreshStats()
                        } else {
                            toast("词库里已经有“${result.word.word}”了")
                        }
                    }
                    is OnlineDict.Result.Failure -> toast(result.reason)
                }
            }
        }.start()
    }

    /** 当前是否有可用网络，避免在断网时白等超时 */
    private fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun importCsv(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                val words = CsvUtil.parse(input)
                if (words.isEmpty()) {
                    toast("未解析到有效单词，格式应为：单词,释义,例句")
                    return
                }
                val added = repository.importWords(words)
                toast("解析 ${words.size} 个，新增 $added 个（重复已跳过）")
                refreshStats()
            } ?: toast("无法读取该文件")
        } catch (e: Exception) {
            toast("导入失败：${e.message}")
        }
    }

    // ---------- 复习提醒 ----------

    private fun onReminderClick() {
        if (NotificationHelper.isEnabled(this)) {
            AlertDialog.Builder(this)
                .setTitle("复习提醒")
                .setMessage("提醒已开启，要修改时间还是关闭提醒？")
                .setPositiveButton("修改时间") { _, _ -> requestNotifPermissionThenPick() }
                .setNegativeButton("关闭提醒") { _, _ ->
                    NotificationHelper.cancel(this)
                    toast("已关闭每日提醒")
                }
                .setNeutralButton("取消", null)
                .show()
        } else {
            requestNotifPermissionThenPick()
        }
    }

    private fun requestNotifPermissionThenPick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            pickReminderTime()
        }
    }

    private fun pickReminderTime() {
        TimePickerDialog(
            this,
            { _, hour, minute ->
                NotificationHelper.scheduleDaily(this, hour, minute)
                val hh = "%02d".format(hour)
                val mm = "%02d".format(minute)
                if (!canExactAlarm()) {
                    toast("已设置每日 $hh:$mm 提醒（未授予精确闹钟，使用非精确提醒）")
                } else {
                    toast("已设置每日 $hh:$mm 提醒")
                }
            },
            19, 0, true
        ).show()
    }

    private fun canExactAlarm(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return am.canScheduleExactAlarms()
    }

    // ---------- 导出与分享 ----------

    private fun shareReportImage() {
        val bitmap = BitmapReport.generate(repository.stats())
        ShareHelper.shareImage(this, bitmap)
    }

    private fun showCsvOptions() {
        val csv = CsvUtil.toCsv(repository.getAllWords())
        AlertDialog.Builder(this)
            .setTitle("导出 CSV")
            .setItems(arrayOf("分享给其他 App", "保存到本地")) { _, which ->
                if (which == 0) {
                    ShareHelper.shareCsv(this, csv)
                } else {
                    createCsvLauncher.launch("words.csv")
                }
            }
            .show()
    }

    private fun saveCsvTo(uri: Uri) {
        try {
            val csv = CsvUtil.toCsv(repository.getAllWords())
            ShareHelper.saveToUri(this, uri, csv.toByteArray(Charsets.UTF_8))
            toast("已保存 CSV")
        } catch (e: Exception) {
            toast("保存失败：${e.message}")
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
