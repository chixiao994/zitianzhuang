package com.example.fontinstaller

import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var btnOpen: Button
    private lateinit var btnTest: Button
    private lateinit var btnUninstall: Button
    private lateinit var tvStatus: TextView
    private lateinit var etPreview: EditText

    private var currentTypeface: Typeface? = null
    private var currentFile: File? = null

    private val fontDir: File get() = File(filesDir, "fonts")

    /** 选择字体文件 */
    private val pickFont = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) installFont(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnOpen = findViewById(R.id.btnOpen)
        btnTest = findViewById(R.id.btnTest)
        btnUninstall = findViewById(R.id.btnUninstall)
        tvStatus = findViewById(R.id.tvStatus)
        etPreview = findViewById(R.id.etPreview)

        etPreview.setText("永和九年，岁在癸丑，暮春之初。\nThe quick brown fox jumps over the lazy dog.\n0123456789")

        btnOpen.setOnClickListener {
            // 字体文件的 MIME 类型五花八门，直接放开所有类型
            pickFont.launch(arrayOf("*/*"))
        }
        btnTest.setOnClickListener { showTestDialog() }
        btnUninstall.setOnClickListener { uninstallFont() }

        restoreInstalledFont()
    }

    // ---------------------------------------------------------------- 安装

    private fun installFont(uri: Uri) {
        try {
            val rawName = queryDisplayName(uri) ?: "font_${System.currentTimeMillis()}.ttf"
            val name = rawName.replace("/", "_").replace("\\", "_")

            val dir = fontDir
            if (!dir.exists() && !dir.mkdirs()) {
                toast("无法创建字体目录")
                return
            }

            val temp = File(dir, "tmp_$name")
            if (temp.exists()) temp.delete()

            contentResolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            } ?: run {
                toast("无法读取该文件")
                return
            }

            if (!isFontFile(temp)) {
                temp.delete()
                toast("这不是有效的字体文件（支持 ttf / otf / ttc）")
                return
            }

            // 只保留一个已安装字体
            dir.listFiles()?.forEach { f ->
                if (f.absolutePath != temp.absolutePath) f.delete()
            }

            val target = File(dir, name)
            if (target.exists()) target.delete()
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }

            currentFile = target
            currentTypeface = Typeface.createFromFile(target)
            applyFont()
            toast("已安装：${target.name}")
        } catch (e: Exception) {
            toast("安装失败：${e.message}")
        }
    }

    /** 通过文件头判断是否为字体文件 */
    private fun isFontFile(file: File): Boolean {
        if (!file.exists() || file.length() < 12) return false
        return try {
            val head = ByteArray(4)
            file.inputStream().use { it.read(head) }
            val tag = String(head, Charsets.US_ASCII)
            val isTrueType = head[0].toInt() == 0x00 && head[1].toInt() == 0x01 &&
                    head[2].toInt() == 0x00 && head[3].toInt() == 0x00
            isTrueType || tag == "OTTO" || tag == "true" ||
                    tag == "ttcf" || tag == "wOFF" || tag == "wOF2"
        } catch (e: Exception) {
            false
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    // ---------------------------------------------------------------- 应用

    private fun applyFont() {
        val tf = currentTypeface
        if (tf != null) {
            etPreview.typeface = tf
            val f = currentFile
            val sizeText = if (f != null && f.exists()) {
                val kb = f.length() / 1024.0
                if (kb >= 1024) String.format("%.2f MB", kb / 1024) else String.format("%.1f KB", kb)
            } else ""
            tvStatus.text = "当前字体：${f?.name ?: "未知"}  $sizeText"
        } else {
            etPreview.typeface = Typeface.DEFAULT
            tvStatus.text = "当前字体：系统默认"
        }
    }

    private fun restoreInstalledFont() {
        val f = fontDir.listFiles()
            ?.filter { !it.name.startsWith("tmp_") }
            ?.sortedBy { it.name }
            ?.firstOrNull()
        if (f != null && isFontFile(f)) {
            currentFile = f
            currentTypeface = Typeface.createFromFile(f)
        }
        applyFont()
    }

    // ---------------------------------------------------------------- 卸载

    private fun uninstallFont() {
        val f = currentFile
        if (f == null) {
            toast("当前没有已安装的字体")
            return
        }
        AlertDialog.Builder(this)
            .setTitle("卸载字体")
            .setMessage("确定要卸载「${f.name}」吗？")
            .setPositiveButton("卸载") { _, _ ->
                f.delete()
                fontDir.listFiles()?.forEach { it.delete() }
                currentFile = null
                currentTypeface = null
                applyFont()
                toast("已卸载")
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // ---------------------------------------------------------------- 测试

    private fun showTestDialog() {
        val tf = currentTypeface
        if (tf == null) {
            toast("请先点击「打开」安装一个字体")
            return
        }

        val pad = (16 * resources.displayMetrics.density).toInt()
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        fun label(text: String) {
            container.addView(TextView(this).apply {
                this.text = text
                textSize = 12f
                setTextColor(0xFF888888.toInt())
                setPadding(0, pad / 2, 0, 4)
            })
        }

        fun sample(text: String, size: Float, face: Typeface = tf) {
            container.addView(TextView(this).apply {
                this.text = text
                textSize = size
                typeface = face
                setTextColor(0xFF222222.toInt())
                setPadding(0, 4, 0, 4)
            })
        }

        label("常规")
        sample("天地玄黄，宇宙洪荒。日月盈昃，辰宿列张。", 20f)
        sample("ABCDEFG abcdefg 0123456789", 20f)

        label("字号")
        sample("14sp 小字预览效果", 14f)
        sample("20sp 正文预览效果", 20f)
        sample("28sp 大字预览效果", 28f)
        sample("36sp 标题字", 36f)

        label("样式")
        sample("Bold 粗体", 20f, Typeface.create(tf, Typeface.BOLD))
        sample("Italic 斜体", 20f, Typeface.create(tf, Typeface.ITALIC))
        sample("Bold Italic 粗斜体", 20f, Typeface.create(tf, Typeface.BOLD_ITALIC))

        label("标点与符号")
        sample("，。、；：？！“”‘’（）《》【】—…·", 20f)
        sample("!@#\$%^&*()_+-=[]{}|;:',.<>/?~`", 20f)

        val scroll = ScrollView(this).apply { addView(container) }

        AlertDialog.Builder(this)
            .setTitle("字体测试：${currentFile?.name ?: ""}")
            .setView(scroll)
            .setPositiveButton("关闭", null)
            .show()
    }

    // ---------------------------------------------------------------- 工具

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
