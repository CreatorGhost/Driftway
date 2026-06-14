package com.kododake.aabrowser.web

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Message
import android.view.View
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.net.toUri
import androidx.webkit.UserAgentMetadata
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.kododake.aabrowser.R
import com.kododake.aabrowser.adblock.AdBlockManager
import com.kododake.aabrowser.adblock.SponsorBlock
import com.kododake.aabrowser.model.UserAgentProfile
import java.util.concurrent.atomic.AtomicBoolean

data class BrowserCallbacks(
    val onUrlChange: (String) -> Unit = {},
    val onTitleChange: (String?) -> Unit = {},
    val onFaviconReceived: (String, Bitmap?) -> Unit = { _, _ -> },
    val onProgressChange: (Int) -> Unit = {},
    val onShowDownloadPrompt: (Uri) -> Unit = {},
    val onError: (Int, String?) -> Unit = { _, _ -> },
    val onCleartextNavigationRequested: (
        Uri,
        allowOnce: () -> Unit,
        allowHostPermanently: () -> Unit,
        cancel: () -> Unit
    ) -> Unit = { _, _, _, cancel -> cancel() },
    val onEnterFullscreen: (View, WebChromeClient.CustomViewCallback) -> Unit = { _, _ -> },
    val onExitFullscreen: () -> Unit = {},
    val onPermissionRequest: (PermissionRequest) -> Unit = { it.deny() },
    val onCreateWindowRequest: (Message) -> Boolean = { false },
    val onCloseWindowRequest: (WebView) -> Unit = {},
    /** The WebView's renderer process died (crash or OOM-kill). Host must recover the tab. */
    val onRendererGone: (WebView, didCrash: Boolean) -> Unit = { _, _ -> },
    /** A pop-up / pop-under / dialog storm was blocked — host may surface subtle feedback. */
    val onPopupBlocked: () -> Unit = {}
)

fun configureWebView(
    webView: WebView,
    callbacks: BrowserCallbacks = BrowserCallbacks(),
    useDesktopMode: Boolean = false,
    userAgentProfile: UserAgentProfile = UserAgentProfile.ANDROID_CHROME,
    allowDarkPages: Boolean = false
) {
    with(webView) {
        setBackgroundColor(Color.TRANSPARENT)

        isHorizontalScrollBarEnabled = false
        isVerticalScrollBarEnabled = true

        WebView.setWebContentsDebuggingEnabled(false)

        val originalUserAgent = settings.userAgentString
        setTag(R.id.webview_original_user_agent_tag, originalUserAgent)

        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            javaScriptCanOpenWindowsAutomatically = true

            setSupportMultipleWindows(true)

            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
            allowContentAccess = true
            allowFileAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = true
            }
            // offscreenPreRaster is toggled per-tab on the ACTIVE WebView only (see
            // MainActivity.switchToTab). Forcing it on every tab raises GPU memory pressure on
            // the large head-unit display and gives no benefit to a fullscreen video surface.
            offscreenPreRaster = false
        }

        applyPageDarkening(allowDarkPages)
        applyBrowserIdentity(userAgentProfile, useDesktopMode)

        CookieManager.getInstance().also {
            it.setAcceptCookie(true)
            it.setAcceptThirdPartyCookies(this, true)
        }

        // Cosmetic ad-hiding (collapse leftover ad gaps), the Facebook in-feed hider, and
        // SponsorBlock are injected per page load in onPageStarted (NOT registered once at
        // construction) so the live ad-block / SponsorBlock toggles and the per-site allowlist
        // take effect on the next page load instead of being frozen at WebView creation.
        //
        // adBlockDisabledForPage is set on the UI thread (onPageStarted) when the current page's
        // host is on the ad-block allowlist; read on the intercept thread to skip blocking for
        // that page. AtomicBoolean because shouldInterceptRequest runs off the UI thread.
        val adBlockDisabledForPage = AtomicBoolean(false)

        // Per-tab annoyance-guard state (this closure is created once per tab). Declared above
        // BOTH clients so onPageStarted (WebViewClient) can reset it on navigation while the
        // WebChromeClient dialog/popup handlers read and update it.
        var jsDialogWindowStart = 0L
        var jsDialogCount = 0
        var suppressDialogsForPage = false
        var lastPopupAt = 0L

        fun popupBlockOn(ctx: android.content.Context?): Boolean =
            ctx != null && com.kododake.aabrowser.data.BrowserPreferences.isPopupBlockEnabled(ctx)

        // Reset on navigation so dialog suppression is per-page, not just per 4s window: a fresh
        // page must start un-suppressed even if the user navigated quickly off a spam page.
        fun resetJsDialogGuardForNewPage() {
            jsDialogWindowStart = 0L
            jsDialogCount = 0
            suppressDialogsForPage = false
        }

        //setLayerType(View.LAYER_TYPE_HARDWARE, null)

        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                // Block ad/tracker SUBRESOURCES only — never the main-frame navigation. Runs off
                // the UI thread; AdBlockManager lookups are O(1) and thread-safe.
                if (request.isForMainFrame || adBlockDisabledForPage.get()) return null
                return if (AdBlockManager.shouldBlock(request.url?.host)) {
                    AdBlockManager.blockedResponse()
                } else {
                    null
                }
            }

            override fun onRenderProcessGone(
                view: WebView,
                detail: android.webkit.RenderProcessGoneDetail?
            ): Boolean {
                // Returning true tells the framework NOT to kill the whole app process when a
                // renderer dies (crash or low-memory kill). We then hand off to the host to
                // tear down and rebuild the affected tab — a dead WebView can't be reused.
                val didCrash = detail?.didCrash() ?: false
                callbacks.onRendererGone(view, didCrash)
                return true
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                if (handleCleartextIfNeeded(view, uri, callbacks, onPageStart = false)) return true
                return handleUri(view, uri)
            }

            private fun handleUri(view: WebView, uri: Uri?): Boolean {
                uri ?: return false
                val scheme = uri.scheme?.lowercase()
                if (scheme == null || scheme in setOf("http", "https", "about", "file", "data", "javascript")) {
                    return false
                }
                return true
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                resetJsDialogGuardForNewPage()
                val stringUrl = url ?: return
                val uri = Uri.parse(stringUrl)
                val scheme = uri.scheme?.lowercase()

                // Refresh the per-page ad-block allowlist flag on the UI thread for the
                // intercept thread to read.
                val allowlisted = AdBlockManager.isHostAllowlisted(view.context, uri.host)
                adBlockDisabledForPage.set(allowlisted)

                // Inject cosmetic hiding (and the Facebook in-feed hider) when ad-blocking is on
                // for this page; gated live so the toggle/allowlist take effect on next load.
                val host = uri.host?.lowercase().orEmpty()
                if (AdBlockManager.enabled && !allowlisted) {
                    view.evaluateJavascript(AdBlockManager.cosmeticCssJs(), null)
                    if (isHostOrSubdomainOf(host, "facebook.com")) {
                        view.evaluateJavascript(AdBlockManager.FACEBOOK_COSMETIC_JS, null)
                    }
                }
                // SponsorBlock is independently opt-in (it contacts a third-party server).
                if (isHostOrSubdomainOf(host, "youtube.com") &&
                    com.kododake.aabrowser.data.BrowserPreferences.isSponsorBlockEnabled(view.context)
                ) {
                    view.evaluateJavascript(SponsorBlock.JS, null)
                }

                if (scheme == "http") {
                    val allowedOnce = getTag(R.id.webview_allow_once_uri_tag) as? String
                    if (allowedOnce == stringUrl) {
                        setTag(R.id.webview_allow_once_uri_tag, null)
                    } else if (handleCleartextIfNeeded(view, uri, callbacks, onPageStart = true)) {
                        return
                    }
                }
            }

            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                view.evaluateJavascript(SpeechRecognitionBridge.POLYFILL_JS, null)
                view.evaluateJavascript(MediaPlaybackBridge.INJECTION_JS, null)
                url?.let(callbacks.onUrlChange)
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    val code = error.errorCode
                    val shouldShowErrorPage = when (code) {
                        WebViewClient.ERROR_HOST_LOOKUP,
                        WebViewClient.ERROR_CONNECT,
                        WebViewClient.ERROR_TIMEOUT,
                        WebViewClient.ERROR_UNKNOWN,
                        WebViewClient.ERROR_PROXY_AUTHENTICATION -> true
                        else -> false
                    }

                    if (shouldShowErrorPage) {
                        val failed = request.url?.toString().orEmpty()
                        val message = error.description?.toString().orEmpty()
                        val assetUrl = "file:///android_asset/error.html?failedUrl=${Uri.encode(failed)}&code=$code&message=${Uri.encode(message)}"
                        try {
                            view.loadUrl(assetUrl)
                        } catch (_: Exception) {
                            callbacks.onError(code, error.description?.toString())
                        }
                        return
                    }
                }
                callbacks.onError(error.errorCode, error.description?.toString())
            }

            override fun onReceivedHttpError(
                view: WebView,
                request: WebResourceRequest,
                errorResponse: WebResourceResponse
            ) {
                if (request.isForMainFrame) {
                    val code = errorResponse.statusCode
                    if (code in 400..599 && code != 429) {
                        val failed = request.url?.toString().orEmpty()
                        val message = errorResponse.reasonPhrase.orEmpty()
                        val assetUrl = "file:///android_asset/error.html?failedUrl=${Uri.encode(failed)}&code=$code&message=${Uri.encode(message)}"
                        try {
                            view.loadUrl(assetUrl)
                        } catch (_: Exception) {
                            callbacks.onError(code, message)
                        }
                        return
                    }
                }
            }

            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                val primary = try { error.primaryError } catch (_: Exception) { -1 }
                val url = error.url ?: ""
                val message = "SSL error: $primary"
                val assetUrl = "file:///android_asset/error.html?failedUrl=${Uri.encode(url)}&sslError=$primary&message=${Uri.encode(message)}"
                try {
                    view.loadUrl(assetUrl)
                    handler.cancel()
                    return
                } catch (_: Exception) {}

                handler.cancel()
                callbacks.onError(primary, message)
            }
        }

        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                callbacks.onProgressChange(newProgress)
            }

            // Suppress the "Leave site? Changes may not be saved" hostage prompt that sketchy
            // sites abuse to trap the user — auto-allow leaving.
            override fun onJsBeforeUnload(
                view: WebView?, url: String?, message: String?,
                result: android.webkit.JsResult?
            ): Boolean {
                if (!popupBlockOn(view?.context)) return super.onJsBeforeUnload(view, url, message, result)
                result?.confirm()
                return true
            }

            // Rate-limit alert/confirm/prompt so a JS loop can't spam dialogs; after a burst,
            // silently dismiss further dialogs from this page until it navigates/reloads.
            private fun rateLimitDialog(view: WebView?, result: android.webkit.JsResult?): Boolean {
                if (!popupBlockOn(view?.context)) return false
                val now = android.os.SystemClock.elapsedRealtime()
                if (now - jsDialogWindowStart > 4000L) { jsDialogWindowStart = now; jsDialogCount = 0; suppressDialogsForPage = false }
                jsDialogCount++
                if (suppressDialogsForPage || jsDialogCount > 2) {
                    suppressDialogsForPage = true
                    result?.cancel()
                    callbacks.onPopupBlocked()
                    return true
                }
                return false
            }

            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: android.webkit.JsResult?): Boolean {
                if (rateLimitDialog(view, result)) return true
                return super.onJsAlert(view, url, message, result)
            }

            override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: android.webkit.JsResult?): Boolean {
                if (rateLimitDialog(view, result)) return true
                return super.onJsConfirm(view, url, message, result)
            }

            override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: android.webkit.JsPromptResult?): Boolean {
                if (rateLimitDialog(view, result)) return true
                return super.onJsPrompt(view, url, message, defaultValue, result)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                callbacks.onTitleChange(title)
            }

            override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                super.onReceivedIcon(view, icon)
                val pageUrl = view?.url?.takeIf { it.isNotBlank() } ?: return
                callbacks.onFaviconReceived(pageUrl, icon)
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (view != null && callback != null) {
                    callbacks.onEnterFullscreen(view, callback)
                } else {
                    super.onShowCustomView(view, callback)
                }
            }

            override fun onHideCustomView() {
                callbacks.onExitFullscreen()
                super.onHideCustomView()
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                if (request == null) return

                val allowed = setOf(
                    PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID,
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE
                )

                val grantable = request.resources.filter { it in allowed }.toTypedArray()

                if (grantable.isEmpty()) {
                    request.deny()
                    return
                }

                if (PermissionRequest.RESOURCE_AUDIO_CAPTURE in grantable) {
                    callbacks.onPermissionRequest(request)
                } else {
                    this@with.post { request.grant(grantable) }
                }
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                resultMsg ?: return false
                // Only honor popups triggered by a real user gesture (e.g. tapping "Sign in with
                // Google/Apple"). Refusing programmatic window.open() blocks popup-spam / focus-
                // steal / lookalike-tab phishing while preserving genuine OAuth flows.
                // Gate popup blocking by the user's setting so turning it off restores the site
                // "escape hatch" even for programmatic window.open().
                val popupBlockEnabled = popupBlockOn(view?.context)
                if (popupBlockEnabled && !isUserGesture) { callbacks.onPopupBlocked(); return false }
                // Burst cap: a single tap that tries to open several windows in quick succession
                // is a pop-under storm — allow the first, block the rest.
                if (popupBlockEnabled) {
                    val now = android.os.SystemClock.elapsedRealtime()
                    if (now - lastPopupAt < 1200L) { callbacks.onPopupBlocked(); return false }
                    lastPopupAt = now
                }
                // Route the popup to the host so it opens as a real in-app tab via WebViewTransport.
                return callbacks.onCreateWindowRequest(resultMsg)
            }

            override fun onCloseWindow(window: WebView?) {
                window?.let(callbacks.onCloseWindowRequest)
                super.onCloseWindow(window)
            }
        }

        setDownloadListener(DownloadListener { url, _, _, _, _ ->
            val uri = url?.takeIf { it.isNotBlank() }?.toUri() ?: return@DownloadListener
            callbacks.onShowDownloadPrompt(uri)
        })
    }
}

/**
 * True if [host] equals [domain] or is a true subdomain of it (dot-boundary match), so that
 * "evil-facebook.com" / "notyoutube.com" do NOT match — only "facebook.com" / "m.youtube.com" do.
 */
private fun isHostOrSubdomainOf(host: String, domain: String): Boolean =
    host == domain || host.endsWith(".$domain")

private fun handleCleartextIfNeeded(view: WebView, uri: Uri?, callbacks: BrowserCallbacks, onPageStart: Boolean = false): Boolean {
    uri ?: return false
    val scheme = uri.scheme?.lowercase() ?: return false
    if (scheme != "http") return false

    val allowedOnce = view.getTag(R.id.webview_allow_once_uri_tag) as? String
    if (allowedOnce == uri.toString()) {
        view.setTag(R.id.webview_allow_once_uri_tag, null)
        return false
    }

    val host = uri.host?.lowercase()
    if (com.kododake.aabrowser.data.BrowserPreferences.isHostAllowedCleartext(view.context, host)) return false
    if (onPageStart) view.stopLoading()
    val allowOnce = {
        view.setTag(R.id.webview_allow_once_uri_tag, uri.toString())
        view.post { view.loadUrl(uri.toString()) }
        kotlin.Unit
    }
    val allowHost = {
        view.context?.let { ctx ->
            val hostToStore = uri.host?.lowercase()
            if (hostToStore != null) com.kododake.aabrowser.data.BrowserPreferences.addAllowedCleartextHost(ctx, hostToStore)
        }
        view.setTag(R.id.webview_allow_once_uri_tag, uri.toString())
        view.post { view.loadUrl(uri.toString()) }
        kotlin.Unit
    }
    val cancel = {
        if (onPageStart) view.stopLoading()
        kotlin.Unit
    }
    callbacks.onCleartextNavigationRequested(uri, allowOnce, allowHost, cancel)
    return true
}

fun WebView.updateDesktopMode(enable: Boolean, profile: UserAgentProfile) {
    applyBrowserIdentity(profile, enable)
    reload()
}

fun WebView.updateUserAgentProfile(profile: UserAgentProfile, desktop: Boolean) {
    applyBrowserIdentity(profile, desktop)
    reload()
}

fun WebView.updatePageDarkening(enabled: Boolean) {
    applyPageDarkening(enabled)
    reload()
}

fun WebView.releaseCompletely() {
    stopLoading()
    webChromeClient = WebChromeClient()
    webViewClient = WebViewClient()
    destroy()
}

private fun WebView.applyBrowserIdentity(profile: UserAgentProfile, desktop: Boolean) {
    setTag(R.id.webview_user_agent_profile_tag, profile.storageKey)
    settings.userAgentString = buildUserAgent(profile, desktop)
    settings.useWideViewPort = desktop
    settings.loadWithOverviewMode = desktop
    setInitialScale(if (desktop) DESKTOP_INITIAL_SCALE_PERCENT else mobileInitialScalePercent())
    applyUserAgentMetadata(profile, desktop)
}

private fun WebView.mobileInitialScalePercent(): Int {
    return (context.resources.displayMetrics.density * 100).toInt()
}

private fun WebView.applyUserAgentMetadata(profile: UserAgentProfile, desktop: Boolean) {
    if (!WebViewFeature.isFeatureSupported(WebViewFeature.USER_AGENT_METADATA)) return

    val metadata = when (profile) {
        UserAgentProfile.ANDROID_CHROME -> buildChromeUserAgentMetadata(desktop)
        UserAgentProfile.SAFARI -> buildSafariLikeUserAgentMetadata(desktop)
    }
    WebSettingsCompat.setUserAgentMetadata(settings, metadata)
}

private fun WebView.applyPageDarkening(enabled: Boolean) {
    if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
        WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, enabled)
    }
}

private fun buildUserAgent(profile: UserAgentProfile, desktop: Boolean): String {
    return when (profile) {
        UserAgentProfile.ANDROID_CHROME -> if (desktop) WINDOWS_CHROME_UA else MOBILE_CHROME_UA
        UserAgentProfile.SAFARI -> if (desktop) SAFARI_MAC_UA else SAFARI_IOS_UA
    }
}

private fun buildChromeUserAgentMetadata(desktop: Boolean): UserAgentMetadata {
    return UserAgentMetadata.Builder()
        .setBrandVersionList(chromeBrandVersions())
        .setFullVersion(CHROME_VERSION)
        .setPlatform(if (desktop) "Windows" else "Android")
        .setPlatformVersion(if (desktop) WINDOWS_PLATFORM_VERSION else ANDROID_PLATFORM_VERSION)
        .setArchitecture(if (desktop) "x86" else "")
        .setModel("")
        .setMobile(!desktop)
        .setBitness(if (desktop) DESKTOP_BITNESS else UserAgentMetadata.BITNESS_DEFAULT)
        .setWow64(false)
        .build()
}

private fun buildSafariLikeUserAgentMetadata(desktop: Boolean): UserAgentMetadata {
    return UserAgentMetadata.Builder()
        .setPlatform(if (desktop) "macOS" else "iOS")
        .setPlatformVersion(if (desktop) MACOS_PLATFORM_VERSION else IOS_PLATFORM_VERSION)
        .setArchitecture(if (desktop) "arm" else "")
        .setModel("")
        .setMobile(!desktop)
        .setBitness(if (desktop) DESKTOP_BITNESS else UserAgentMetadata.BITNESS_DEFAULT)
        .setWow64(false)
        .build()
}

private fun chromeBrandVersions(): List<UserAgentMetadata.BrandVersion> {
    val majorVersion = CHROME_VERSION.substringBefore('.')
    return listOf(
        UserAgentMetadata.BrandVersion.Builder()
            .setBrand("Chromium")
            .setMajorVersion(majorVersion)
            .setFullVersion(CHROME_VERSION)
            .build(),
        UserAgentMetadata.BrandVersion.Builder()
            .setBrand("Google Chrome")
            .setMajorVersion(majorVersion)
            .setFullVersion(CHROME_VERSION)
            .build()
    )
}

private const val DESKTOP_INITIAL_SCALE_PERCENT = 100
private const val DESKTOP_BITNESS = 64
private const val CHROME_VERSION = "146.0.0.0"
private const val ANDROID_PLATFORM_VERSION = "10.0.0"
private const val WINDOWS_PLATFORM_VERSION = "10.0.0"
private const val MACOS_PLATFORM_VERSION = "14.0.0"
private const val IOS_PLATFORM_VERSION = "17.0.0"
private const val MOBILE_CHROME_UA = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/${CHROME_VERSION} Mobile Safari/537.36"
private const val WINDOWS_CHROME_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/${CHROME_VERSION} Safari/537.36"
private const val SAFARI_MAC_UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_0_0) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15"
private const val SAFARI_IOS_UA = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"
