package app.simplifymy.browser.core.settings

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri

fun shouldOfferDefaultBrowser(offered: Boolean, isDefault: Boolean, openedFromLink: Boolean, otherDialogVisible: Boolean): Boolean =
    !offered && !isDefault && !openedFromLink && !otherDialogVisible

object DefaultBrowser {

    fun isDefault(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roles = context.getSystemService(RoleManager::class.java) ?: return false
            return roles.isRoleAvailable(RoleManager.ROLE_BROWSER) && roles.isRoleHeld(RoleManager.ROLE_BROWSER)
        }
        val probe = Intent(Intent.ACTION_VIEW, "https://example.com".toUri())
        return context.packageManager.resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName == context.packageName
    }

    fun requestIntent(context: Context): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roles = context.getSystemService(RoleManager::class.java)
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_BROWSER) && !roles.isRoleHeld(RoleManager.ROLE_BROWSER)) {
                return roles.createRequestRoleIntent(RoleManager.ROLE_BROWSER)
            }
        }
        return manageIntent(context)
    }

    fun manageIntent(context: Context): Intent {
        val defaultApps = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        return if (defaultApps.resolveActivity(context.packageManager) != null) defaultApps else Intent(Settings.ACTION_SETTINGS)
    }
}
