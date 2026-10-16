package app.simplifymy.browser

import mozilla.components.feature.downloads.AbstractFetchDownloadService
import mozilla.components.feature.downloads.DefaultFileSizeFormatter
import mozilla.components.feature.downloads.DefaultPackageNameProvider
import mozilla.components.feature.downloads.DownloadEstimator
import mozilla.components.feature.downloads.filewriter.DefaultDownloadFileWriter

class DownloadService : AbstractFetchDownloadService() {
    override val store get() = components.store
    override val httpClient get() = components.client
    override val notificationsDelegate get() = components.notificationsDelegate
    override val downloadFileUtils get() = components.downloadFileUtils
    override val packageNameProvider by lazy { DefaultPackageNameProvider(applicationContext) }
    override val fileSizeFormatter by lazy { DefaultFileSizeFormatter(applicationContext) }
    override val downloadEstimator by lazy { DownloadEstimator(dateTimeProvider) }
    override val downloadFileWriter by lazy { DefaultDownloadFileWriter(applicationContext, downloadFileUtils) }
}
