package com.example.localscreenanalyzer

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import android.graphics.Color

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var gameSpinner: Spinner
    private val requestCapture = 4102
    private val apps = mutableListOf<Pair<String, String>>() // label, package

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(28, 35, 45)
        window.navigationBarColor = Color.rgb(28, 35, 45)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(Color.rgb(245, 247, 250))
        }
        val title = TextView(this).apply {
            text = "Local Screen Analyzer"
            textSize = 24f
            setTextColor(Color.rgb(25, 35, 50))
        }
        val subtitle = TextView(this).apply {
            text = "Локальный тест захвата экрана. Интернет не нужен."
            textSize = 15f
            setTextColor(Color.DKGRAY)
            setPadding(0, 8, 0, 20)
        }
        gameSpinner = Spinner(this)
        val refresh = Button(this).apply { text = "Обновить список приложений" }
        val start = Button(this).apply { text = "Запустить захват экрана" }
        val stop = Button(this).apply { text = "Остановить захват" }
        status = TextView(this).apply {
            text = "Выбери приложение. Затем нажми запуск и подтверди системное окно Android."
            textSize = 15f
            setTextColor(Color.rgb(30, 40, 55))
            setPadding(0, 20, 0, 0)
        }
        root.addView(title)
        root.addView(subtitle)
        root.addView(TextView(this).apply { text = "Приложение (для справки):"; textSize = 16f })
        root.addView(gameSpinner)
        root.addView(refresh)
        root.addView(start)
        root.addView(stop)
        root.addView(status)
        setContentView(root)

        refresh.setOnClickListener { loadApps() }
        start.setOnClickListener {
            val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(mgr.createScreenCaptureIntent(), requestCapture)
        }
        stop.setOnClickListener {
            stopService(Intent(this, CaptureService::class.java))
            status.text = "Захват остановлен."
        }
        loadApps()
    }

    private fun loadApps() {
        apps.clear()
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        resolved.map {
            val label = it.loadLabel(pm).toString()
            label to it.activityInfo.packageName
        }.distinctBy { it.second }
            .filter { it.second != packageName }
            .sortedBy { it.first.lowercase() }
            .forEach { apps.add(it) }

        gameSpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            apps.map { it.first }
        )
        status.text = "Найдено приложений: ${apps.size}. Выбор пока справочный: Android сам показывает экран, который ты разрешишь захватывать."
    }

    @Deprecated("Deprecated in Android, retained for compatibility with this minimal starter")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestCapture) {
            if (resultCode == RESULT_OK && data != null) {
                val chosen = apps.getOrNull(gameSpinner.selectedItemPosition)
                val service = Intent(this, CaptureService::class.java).apply {
                    putExtra(CaptureService.EXTRA_RESULT_CODE, resultCode)
                    putExtra(CaptureService.EXTRA_DATA, data)
                    putExtra(CaptureService.EXTRA_APP_LABEL, chosen?.first ?: "Не выбрано")
                }
                startForegroundService(service)
                status.text = "Разрешение получено. Сервис захвата запущен. Этот прототип пока не распознаёт объекты и не управляет игрой."
            } else {
                status.text = "Разрешение на захват экрана не выдано."
            }
        }
    }
}
