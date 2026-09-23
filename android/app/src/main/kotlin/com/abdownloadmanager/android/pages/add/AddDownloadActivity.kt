package com.abdownloadmanager.android.pages.add

import android.content.Intent
import android.os.Bundle
import arrow.core.firstOrNone
import arrow.core.getOrElse
import arrow.core.None
import arrow.core.Some
import com.abdownloadmanager.android.pages.add.multiple.AddMultiDownloadActivity
import com.abdownloadmanager.android.pages.add.single.AddSingleDownloadActivity
import com.abdownloadmanager.android.pages.onboarding.permissions.PermissionManager
import com.abdownloadmanager.android.ui.MainActivity
import com.abdownloadmanager.android.util.activity.ABDMActivity
import com.abdownloadmanager.shared.pages.adddownload.AddDownloadConfig
import com.abdownloadmanager.shared.pages.adddownload.AddDownloadCredentialsInUiProps
import com.abdownloadmanager.shared.pages.adddownload.ImportOptions
import com.abdownloadmanager.shared.util.extractors.linkextractor.DefaultDownloadCredentialsExtractor
import ir.amirab.downloader.downloaditem.IDownloadCredentials
import ir.amirab.downloader.downloaditem.http.HttpDownloadCredentials
import kotlinx.serialization.json.Json
import org.koin.core.component.inject

class AddDownloadActivity : ABDMActivity() {
    val json: Json by inject()
    val permissionManager: PermissionManager by inject()
    private fun createDownloaderInUiProps(
        credentials: IDownloadCredentials
    ): AddDownloadCredentialsInUiProps {
        return AddDownloadCredentialsInUiProps(
            credentials,
            AddDownloadCredentialsInUiProps.Configs(),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!permissionManager.isReady()) {
            // user not opened the app at least once. we must redirect it to the permission page first
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        val credentials = getDownloadCredentialsFromIntent(intent)
        val intent = if (credentials.size > 1) {
            AddMultiDownloadActivity.createIntent(
                this,
                AddDownloadConfig.MultipleAddConfig(
                    newDownloads = credentials.map(::createDownloaderInUiProps),
                    importOptions = ImportOptions(),
                ),
                json = json,
            )
        } else {
            AddSingleDownloadActivity.createIntent(
                this,
                AddDownloadConfig.SingleAddConfig(
                    newDownload = credentials
                        .firstOrNone()
                        .getOrElse { HttpDownloadCredentials("") }
                        .let(::createDownloaderInUiProps),
                    importOptions = ImportOptions(),
                ),
                json = json,
            ).putExtra(AddSingleDownloadActivity.EXTRA_FROM_EXTERNAL, true)
        }
        startActivity(intent)
        finish()
    }

    private fun getDownloadCredentialsFromIntent(intent: Intent): List<IDownloadCredentials> {
        val links = when (intent.action) {
            Intent.ACTION_SEND -> {
                intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            }

            else -> {
                // action view etc...
                intent.data?.toString().orEmpty()
            }
        }
        val referrerUrl = getReferrerUrl(intent)
        return DefaultDownloadCredentialsExtractor
            .extract(links)
            .distinctBy { it.link }
            .map { credentials ->
                if (referrerUrl == null) {
                    credentials
                } else {
                    // keep the page url so the downloader can send it as Referer (anti-hotlink sites)
                    credentials.copy(
                        link = None,
                        downloadPage = Some(referrerUrl),
                    )
                }
            }
    }

    private fun getReferrerUrl(intent: Intent): String? {
        // browsers usually pass something like "android-app://com.android.chrome/" here,
        // which is useless as an http Referer (and would pollute the download page field),
        // so only accept real http(s) urls
        return listOfNotNull(
            intent.getStringExtra(Intent.EXTRA_REFERRER_NAME),
            referrer?.toString(),
        ).firstOrNull { it.isHttpUrl() }
    }

    private fun String.isHttpUrl(): Boolean {
        return startsWith("http://", ignoreCase = true) ||
            startsWith("https://", ignoreCase = true)
    }
}
