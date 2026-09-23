package com.abdownloadmanager.android.pages.add.single

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.abdownloadmanager.android.pages.browser.BrowserActivity
import com.abdownloadmanager.android.pages.category.CategorySheet
import com.abdownloadmanager.android.pages.newqueue.NewQueueSheet
import com.abdownloadmanager.android.pages.singledownload.SingleDownloadPageActivity
import com.abdownloadmanager.android.util.ABDMAppManager
import com.abdownloadmanager.android.util.AndroidDownloadItemOpener
import com.abdownloadmanager.android.util.activity.ABDMActivity
import com.abdownloadmanager.android.util.activity.HandleActivityEffects
import com.abdownloadmanager.android.util.activity.RetainedComponentContainer
import com.abdownloadmanager.android.util.activity.getSerializedExtra
import com.abdownloadmanager.android.util.activity.putSerializedExtra
import com.abdownloadmanager.android.util.pagemanager.AndroidDownloadErrorPageManager
import com.abdownloadmanager.shared.downloaderinui.DownloaderInUiRegistry
import com.abdownloadmanager.shared.pages.adddownload.AddDownloadConfig
import com.abdownloadmanager.shared.pages.adddownload.AddDownloadCredentialsInUiProps
import com.abdownloadmanager.shared.storage.ILastSavedLocationsStorage
import com.abdownloadmanager.shared.storage.ISelectQueueStorage
import com.abdownloadmanager.shared.util.DownloadSystem
import com.abdownloadmanager.shared.util.FileIconProvider
import com.abdownloadmanager.shared.util.OnFullyDismissed
import com.abdownloadmanager.shared.util.ResponsiveDialog
import com.abdownloadmanager.shared.util.category.CategoryManager
import com.abdownloadmanager.shared.util.mvi.HandleEffects
import com.abdownloadmanager.shared.util.rememberChild
import com.abdownloadmanager.shared.util.rememberResponsiveDialogState
import ir.amirab.downloader.downloaditem.http.HttpDownloadCredentials
import ir.amirab.downloader.queue.QueueManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.milliseconds

class AddSingleDownloadActivity : ABDMActivity() {
    private val json: Json by inject()
    private val downloadSystem: DownloadSystem by inject()
    private val appManager: ABDMAppManager by inject()
    private val downloadItemOpener: AndroidDownloadItemOpener by inject()
    private val downloaderInUiRegistry: DownloaderInUiRegistry by inject()
    private val lastSavedLocationsStorage: ILastSavedLocationsStorage by inject()
    private val selectQueueStorage: ISelectQueueStorage by inject()
    private val queueManager: QueueManager by inject()
    private val categoryManager: CategoryManager by inject()
    private val iconProvider: FileIconProvider by inject()
    private val appContext: Context by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fromExternal = intent.getBooleanExtra(EXTRA_FROM_EXTERNAL, false)
        val myRetainedComponent = myRetainedComponent {
            // TODO consider use a factory to create AndroidAddSingleDownloadComponent
            // we may create memory leaks if we accidentally pass Activity::this into the component lambdas
            val config = getComponentConfig(intent)
            val appManager = appManager
            val appContext = this@AddSingleDownloadActivity.appContext
            val scope = applicationScope
            val downloadItemOpener = downloadItemOpener
            val appSettingsStorage = appSettingsStorage
            val downloadSystem = downloadSystem
            val downloadErrorDialogManager = AndroidDownloadErrorPageManager(
                openIntent = {
                    sendEffect(RetainedComponentContainer.Effects.StartActivity(it))
                },
                json = json,
                context = applicationContext,
            )
            val closeAddDownloadDialog = {
                // finish directly instead of via the effect channel: that effect is not delivered
                // when the composable is already gone, which left the dialog frozen on screen.
                // Opened from outside the app this dialog lives in a task of its own
                // (AddDownloadActivity uses taskAffinity=""), so a plain finish() left the
                // now-empty task sitting on screen; finishAndRemoveTask() tears it down.
                this@AddSingleDownloadActivity.runOnUiThread {
                    if (fromExternal) {
                        this@AddSingleDownloadActivity.finishAndRemoveTask()
                    } else {
                        this@AddSingleDownloadActivity.finish()
                    }
                }
            }
            AndroidAddSingleDownloadComponent(
                ctx = it,
                onRequestClose = closeAddDownloadDialog,
                onRequestDownload = { item, categoryId ->
                    scope.launch {
                        val id = appManager.startNewDownload(item, categoryId).await()
                        if (!fromExternal && appSettingsStorage.showDownloadProgressDialog.value) {
                            runCatching {
                                appContext.startActivity(
                                    SingleDownloadPageActivity.createIntent(
                                        appContext,
                                        id,
                                        true,
                                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }.onFailure {
                                it.printStackTrace()
                            }
                        }
                    }
                    if (fromExternal) {
                        // external request: the live notification takes over.
                        // finish() directly instead of going through the effect channel:
                        // that effect is only consumed while the composable is alive, so
                        // when the download took a while this activity was left in the
                        // back stack and later popped up as a stale "downloading" page.
                        // finishAndRemoveTask, not plain finish(): the external-add task has to
                        // leave the recents list the moment the download is queued
                        runOnUiThread { finishAndRemoveTask() }
                    }
                },
                onRequestAddToQueue = { item, queue, category ->
                    appManager.addDownload(item, queue, category)
                },
                openExistingDownload = {
                    scope.launch {
                        downloadItemOpener.openDownloadItem(it)
                    }
                },
                updateExistingDownloadCredentials = { id, newCredentials, downloadJobExtraConfig ->
                    scope.launch {
                        downloadSystem.downloadManager.updateDownloadItem(
                            id = id,
                            downloadJobExtraConfig = downloadJobExtraConfig,
                            updater = {
                                it.withCredentials(newCredentials)
                            }
                        )
//                        openDownloadDialog(id)
                    }
                },
                downloadItemOpener = downloadItemOpener,
                downloadErrorDialogManager = downloadErrorDialogManager,
                lastSavedLocationsStorage = lastSavedLocationsStorage,
                selectQueueStorage = selectQueueStorage,
                importOptions = config.importOptions,
                id = config.id,
                downloaderInUi = downloaderInUiRegistry.getDownloaderOf(config.newDownload.credentials)!!,
                initialCredentials = config.newDownload,
                queueManager = queueManager,
                categoryManager = categoryManager,
                downloadSystem = downloadSystem,
                appSettings = appSettingsStorage,
                iconProvider = iconProvider,
                appScope = applicationScope,
                appRepository = appRepository,
                perHostSettingsManager = perHostSettingsManager,
            )
        }
        val addDownloadComponent = myRetainedComponent.component
        setABDMContent {
            myRetainedComponent.HandleActivityEffects()
            HandleEffects(addDownloadComponent) {
                if (it is AndroidAddSingleDownloadComponent.Effects.OpenInBrowser) {
                    startActivity(
                        BrowserActivity.createIntent(this, it.link)
                    )
                    finish()
                }
            }
            val dialogState = rememberResponsiveDialogState(false)
            dialogState.OnFullyDismissed {
                addDownloadComponent.onRequestClose()
            }
            LaunchedEffect(Unit) {
                // animate open after activity becomes fully open
                // is there a better way?
                delay(10.milliseconds)
                dialogState.show()
            }
            val onDismiss = { dialogState.hide() }
            ResponsiveDialog(
                dialogState,
                onDismiss
            ) {
                AddSingleDownloadPage(addDownloadComponent, onDismiss)
            }
            CategorySheet(
                categoryComponent = addDownloadComponent.categorySlot.rememberChild(),
                onDismiss = addDownloadComponent::closeCategoryDialog
            )
            NewQueueSheet(
                onQueueCreate = addDownloadComponent::createQueueWithName,
                isOpened = addDownloadComponent.showAddQueue.collectAsState().value,
                onCloseRequest = { addDownloadComponent.setShowAddQueue(false) },
            )
        }
    }

    private fun getComponentConfig(intent: Intent): AddDownloadConfig.SingleAddConfig {
        runCatching {
            with(json) {
                intent.getSerializedExtra<AddDownloadConfig.SingleAddConfig>(COMPONENT_CONFIG_KEY)
            }
        }.onFailure {
            it.printStackTrace()
        }.getOrNull()?.let {
            return it
        }
        val link = intent.data?.toString().orEmpty()
        return AddDownloadConfig.SingleAddConfig(
            newDownload = AddDownloadCredentialsInUiProps(
                credentials = HttpDownloadCredentials(
                    link = link,
                )
            )
        )
    }

    companion object {
        const val COMPONENT_CONFIG_KEY = "ComponentConfig"
        const val LINK_KEY = "link"
        const val EXTRA_FROM_EXTERNAL = "fromExternal"
        fun createIntent(
            context: Context,
            singleAddConfig: AddDownloadConfig.SingleAddConfig,
            json: Json,
        ): Intent {
            val intent = Intent(
                context,
                AddSingleDownloadActivity::class.java,
            )
            with(json) {
                intent.putSerializedExtra(COMPONENT_CONFIG_KEY, singleAddConfig)
            }
            return intent
        }
    }
}
