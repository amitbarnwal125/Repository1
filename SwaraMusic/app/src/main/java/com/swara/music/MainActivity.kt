package com.swara.music

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.swara.music.ui.SwaraRoot
import com.swara.music.ui.theme.SwaraTheme

class MainActivity : ComponentActivity() {

    private val notifPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val c = container
        c.player.connect()
        if (Build.VERSION.SDK_INT >= 33 && !c.settings.askedNotifications) {
            c.settings.askedNotifications = true
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { SwaraTheme { SwaraRoot() } }
    }
}
