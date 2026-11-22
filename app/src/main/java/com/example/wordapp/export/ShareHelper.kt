package com.example.wordapp.export

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.OutputStream

/**
 * 导出与跨 App 分享。
 *
 * 文件先写入应用缓存目录 share/，再通过 FileProvider 生成 content:// URI，
 * 配合 Intent.ACTION_SEND + 临时读权限分享。
 * Android 7.0（API 24）起禁止在跨 App Intent 中直接使用 file://，
 * 因此本类全程不出现 file:// URI。
 */
object ShareHelper {

    private const val SHARE_DIR = "share"

    private fun shareDir(context: Context): File {
        val dir = File(context.cacheDir, SHARE_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun authority(context: Context): String =
        "${context.packageName}.fileprovider"

    /** 保存报告 Bitmap 为 PNG，返回文件 */
    fun writeBitmap(context: Context, bitmap: Bitmap): File {
        val file = File(shareDir(context), "learning_report_${System.currentTimeMillis()}.png")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
        }
        return file
    }

    /** 保存 CSV 文本，返回文件 */
    fun writeCsv(context: Context, csv: String): File {
        val file = File(shareDir(context), "words_${System.currentTimeMillis()}.csv")
        file.writeText(csv, Charsets.UTF_8)
        return file
    }

    /** FileProvider 把文件转换为可安全分享的 content:// URI */
    fun contentUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, authority(context), file)

    /**
     * 调起系统分享面板分享单个文件。
     * @param mimeType 例如 image/png、text/csv
     * @return 是否成功调起；没有可接收的 App 时返回 false，由调用方提示
     */
    fun share(context: Context, file: File, mimeType: String, title: String): Boolean {
        // 每次分享前清理一天前的旧文件，避免缓存无限增长
        clearOldShares(context)

        if (!canShare(context, mimeType)) return false

        return try {
            val uri = contentUri(context, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                // 临时授予接收 App 读取该 URI 的权限
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, title))
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    /** 系统里是否有能接收该类型的 App（Android 11+ 需配合 Manifest 的 queries 声明） */
    fun canShare(context: Context, mimeType: String): Boolean {
        val probe = Intent(Intent.ACTION_SEND).apply { type = mimeType }
        return context.packageManager.queryIntentActivities(probe, 0).isNotEmpty()
    }

    /** 删除 share 目录下超过一天的旧文件 */
    fun clearOldShares(context: Context) {
        val dir = shareDir(context)
        val cutoff = System.currentTimeMillis() - 24L * 60 * 60 * 1000
        dir.listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < cutoff) file.delete()
        }
    }

    fun shareImage(context: Context, bitmap: Bitmap): Boolean =
        share(context, writeBitmap(context, bitmap), "image/png", "分享学习报告")

    fun shareCsv(context: Context, csv: String): Boolean =
        share(context, writeCsv(context, csv), "text/csv", "分享词库 CSV")

    /**
     * 写入用户通过“另存为”（SAF ACTION_CREATE_DOCUMENT）选定的位置。
     */
    fun saveToUri(context: Context, uri: Uri, bytes: ByteArray) {
        val out: OutputStream = context.contentResolver.openOutputStream(uri)
            ?: throw IllegalStateException("无法打开目标位置")
        out.use { it.write(bytes) }
    }
}
