package app.simplifymy.browser

import android.content.Intent
import mozilla.components.feature.media.service.AbstractMediaSessionService

class MediaSessionService : AbstractMediaSessionService() {
    override val store get() = components.store
    override val crashReporter = null
    override val notificationsDelegate get() = components.notificationsDelegate

    override fun onTaskRemoved(rootIntent: Intent?) {
        components.pauseMedia()
    }
}
