package app.simplifymy.browser.core.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import app.simplifymy.browser.MainActivity
import app.simplifymy.browser.ui.theme.AppIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

private val ALIAS_PREFIX = MainActivity::class.java.name.substringBeforeLast('.') + ".icon."

class AppIconManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val pending = AtomicReference<AppIcon?>(null)

    fun request(icon: AppIcon) = pending.set(icon)

    fun flush() {
        val icon = pending.getAndSet(null) ?: return
        scope.launch { swap(icon) }
    }

    private fun swap(icon: AppIcon) {
        val packages = context.packageManager
        if (AppIcon.entries.all { packages.isEnabled(it) == (it == icon) }) return
        packages.setEnabled(icon, PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
        AppIcon.entries
            .filter { it != icon }
            .forEach { packages.setEnabled(it, PackageManager.COMPONENT_ENABLED_STATE_DISABLED) }
    }

    private fun PackageManager.setEnabled(icon: AppIcon, state: Int) =
        setComponentEnabledSetting(component(icon), state, PackageManager.DONT_KILL_APP)

    private fun PackageManager.isEnabled(icon: AppIcon) =
        when (getComponentEnabledSetting(component(icon))) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> false
            else -> icon.enabledByDefault
        }

    private fun component(icon: AppIcon) = ComponentName(context.packageName, ALIAS_PREFIX + icon.name)
}
