package app.simplifymy.browser

import android.app.Activity
import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.provider.Browser

private val FORWARDED_EXTRAS = listOf(Intent.EXTRA_TEXT, SearchManager.QUERY, Browser.EXTRA_APPLICATION_ID)

class IntentReceiverActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val incoming = intent
        val forward = Intent(this, MainActivity::class.java)
            .setAction(incoming.action)
            .setDataAndType(incoming.data, incoming.type)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        FORWARDED_EXTRAS.forEach { key ->
            runCatching { incoming.getCharSequenceExtra(key) }.getOrNull()?.let { forward.putExtra(key, it.toString()) }
        }
        referrer?.let { forward.putExtra(Intent.EXTRA_REFERRER, it) }
        startActivity(forward)
        finish()
    }
}
