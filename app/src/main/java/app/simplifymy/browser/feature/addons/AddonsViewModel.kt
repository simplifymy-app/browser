package app.simplifymy.browser.feature.addons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.simplifymy.browser.Components
import app.simplifymy.browser.componentsFactory
import app.simplifymy.browser.core.addons.Amo
import app.simplifymy.browser.core.addons.AmoAddon
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mozilla.components.browser.state.action.WebExtensionAction
import mozilla.components.browser.state.state.WebExtensionState
import mozilla.components.browser.state.state.extension.WebExtensionPromptRequest
import mozilla.components.concept.engine.webextension.InstallationMethod
import mozilla.components.concept.engine.webextension.PermissionPromptResponse
import mozilla.components.feature.addons.Addon
import mozilla.components.lib.state.ext.flow

data class AddonsState(
    val query: String = "",
    val loading: Boolean = true,
    val failed: Boolean = false,
    val installed: List<Addon> = emptyList(),
    val results: List<AmoAddon> = emptyList(),
    val busy: Set<String> = emptySet(),
) {
    val installedIds get() = installed.map { it.id }.toSet()
}

@OptIn(FlowPreview::class)
class AddonsViewModel(private val components: Components) : ViewModel() {

    private val manager = components.addonManager
    private val store = components.store
    private val query = MutableStateFlow("")
    private val _state = MutableStateFlow(AddonsState())
    val state: StateFlow<AddonsState> = _state.asStateFlow()

    val prompt: StateFlow<WebExtensionPromptRequest?> = store.flow()
        .map { it.webExtensionPromptRequest }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val popup: StateFlow<WebExtensionState?> = store.flow()
        .map { state -> state.extensions.values.firstOrNull { it.popupSession != null } }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var searchJob: Job? = null

    fun open() {
        refresh()
        if (searchJob == null) {
            searchJob = viewModelScope.launch {
                query.debounce(SEARCH_DELAY_MS).distinctUntilChanged().collectLatest(::search)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { manager.getAddons() }.onSuccess { list ->
                _state.update { it.copy(installed = list.filter(Addon::isInstalled)) }
            }
        }
    }

    fun setQuery(value: String) {
        _state.update { it.copy(query = value) }
        query.value = value
    }

    fun submit() {
        val input = query.value.trim()
        when {
            input.isXpi() -> installFrom(input, input)
            Amo.slugOf(input) != null -> _state.value.results.firstOrNull()?.let(::install)
        }
    }

    fun install(addon: AmoAddon) = installFrom(addon.guid, addon.downloadUrl)

    fun setEnabled(addon: Addon, enabled: Boolean) {
        if (enabled) manager.enableAddon(addon, onSuccess = { refresh() }) else manager.disableAddon(addon, onSuccess = { refresh() })
    }

    fun uninstall(addon: Addon) = manager.uninstallAddon(addon, onSuccess = { refresh() })

    fun openOptions(addon: Addon) {
        addon.installedState?.optionsPageUrl?.let { components.tabsUseCases.addTab(it) }
    }

    fun answerPermissions(request: WebExtensionPromptRequest, granted: Boolean, privateBrowsing: Boolean) {
        when (request) {
            is WebExtensionPromptRequest.AfterInstallation.Permissions.Required ->
                request.onConfirm(PermissionPromptResponse(isPermissionsGranted = granted, isPrivateModeGranted = granted && privateBrowsing))
            is WebExtensionPromptRequest.AfterInstallation.Permissions.Optional -> request.onConfirm(granted)
            else -> Unit
        }
        consumePrompt()
    }

    fun consumePrompt() {
        store.dispatch(WebExtensionAction.ConsumePromptRequestWebExtensionAction)
        refresh()
    }

    fun closePopup(extension: WebExtensionState) {
        store.dispatch(WebExtensionAction.UpdatePopupSessionAction(extension.id, popupSession = null))
    }

    private suspend fun search(input: String) {
        _state.update { it.copy(loading = true, failed = false) }
        val result = runCatching {
            when {
                input.isXpi() -> emptyList()
                else -> Amo.slugOf(input)?.let { listOf(components.amo.bySlug(it)) } ?: components.amo.search(input)
            }
        }.onFailure { if (it is CancellationException) throw it }
        _state.update { it.copy(loading = false, failed = result.isFailure, results = result.getOrDefault(emptyList())) }
    }

    private fun installFrom(key: String, url: String) {
        _state.update { it.copy(busy = it.busy + key) }
        manager.installAddon(
            url = url,
            installationMethod = InstallationMethod.MANAGER,
            onSuccess = { done(key) },
            onError = { done(key) },
        )
    }

    private fun done(key: String) {
        _state.update { it.copy(busy = it.busy - key) }
        refresh()
    }

    private fun String.isXpi() = substringBefore('?').endsWith(".xpi")

    companion object {
        private const val SEARCH_DELAY_MS = 300L
        val Factory = componentsFactory { AddonsViewModel(it) }
    }
}
