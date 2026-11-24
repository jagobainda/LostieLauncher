package dev.jagoba.lostielauncher.service.download

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface NotificationAccess {
    fun isGranted(): Boolean
}

internal class AndroidNotificationAccess @Inject constructor(@ApplicationContext private val context: Context) :
    NotificationAccess {
    override fun isGranted(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
}
