package com.kododake.aabrowser.adblock

import android.content.Context
import android.webkit.WebResourceResponse
import com.kododake.aabrowser.data.BrowserPreferences
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Stable-tier ad/tracker blocker for the WebView.
 *
 * Network layer: domain blocking via [shouldBlock] (called from WebViewClient.shouldInterceptRequest),
 * matching a request host against a bundled curated hosts list, extended at runtime by a background
 * fetch of the StevenBlack unified hosts list. Lookup is O(1) against an immutable Set behind a
 * volatile reference, so it is thread-safe and fast enough for the per-subresource intercept thread.
 *
 * Cosmetic layer: [GENERIC_COSMETIC_CSS] collapses the blank gaps generic network blocking leaves;
 * [FACEBOOK_COSMETIC_JS] hides in-feed "Sponsored" posts (heuristic, best-effort).
 *
 * Deliberately does NOT attempt YouTube video-ad blocking (first-party + server-side-inserted,
 * an endless maintenance treadmill) — see SponsorBlock for the stable YouTube value-add instead.
 */
object AdBlockManager {

    @Volatile
    private var blockedHosts: Set<String> = emptySet()

    @Volatile
    var enabled: Boolean = true
        private set

    @Volatile
    private var loaded = false

    private const val UPDATE_INTERVAL_MS = 3L * 24 * 60 * 60 * 1000 // 3 days
    private const val CACHE_FILE = "adblock_hosts_cache.txt"
    private const val REMOTE_LIST_URL =
        "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"
    private const val MAX_REMOTE_BYTES = 12L * 1024 * 1024 // safety cap

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /** Loads lists off the main thread and kicks off a throttled background update. Idempotent. */
    fun initialize(context: Context) {
        enabled = BrowserPreferences.isAdBlockEnabled(context)
        if (loaded) return
        loaded = true
        val appContext = context.applicationContext
        Thread {
            val hosts = HashSet<String>(120_000)
            runCatching {
                appContext.assets.open("adblock/hosts.txt").bufferedReader().useLines { lines ->
                    lines.forEach { parseHostLine(it)?.let(hosts::add) }
                }
            }
            val cache = File(appContext.filesDir, CACHE_FILE)
            if (cache.exists()) {
                runCatching {
                    cache.bufferedReader().useLines { lines ->
                        lines.forEach { parseHostLine(it)?.let(hosts::add) }
                    }
                }
            }
            blockedHosts = hosts
            maybeUpdate(appContext, cache)
        }.apply { isDaemon = true; name = "adblock-load" }.start()
    }

    fun setEnabled(context: Context, value: Boolean) {
        BrowserPreferences.setAdBlockEnabled(context, value)
        enabled = value
    }

    /** True if [host] (or a parent domain) is on the block list and blocking is on for it. */
    fun shouldBlock(host: String?): Boolean {
        if (!enabled || host.isNullOrBlank()) return false
        val set = blockedHosts
        if (set.isEmpty()) return false
        // Check the host and each parent domain that still has a dot (so we match
        // "ads.example.com" against a listed "example.com") but never a bare TLD ("com").
        var candidate = host.lowercase().removePrefix("www.")
        while (candidate.contains('.')) {
            if (candidate in set) return true
            candidate = candidate.substring(candidate.indexOf('.') + 1)
        }
        return false
    }

    /** An empty 200 response used to swallow a blocked request. */
    fun blockedResponse(): WebResourceResponse =
        WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))

    /** Per-host allowlist check so users can disable blocking on sites that break. */
    fun isHostAllowlisted(context: Context, host: String?): Boolean =
        BrowserPreferences.isHostAdBlockDisabled(context, host)

    private fun parseHostLine(raw: String): String? {
        val line = raw.trim()
        if (line.isEmpty() || line.startsWith("#")) return null
        // Accept "domain.com" or hosts-file "0.0.0.0 domain.com" / "127.0.0.1 domain.com".
        val token = line.split(Regex("\\s+")).let { parts ->
            when {
                parts.size >= 2 && (parts[0] == "0.0.0.0" || parts[0] == "127.0.0.1") -> parts[1]
                parts.size == 1 -> parts[0]
                else -> return null
            }
        }.lowercase()
        if (token.isEmpty() || token == "localhost" || token.startsWith("#")) return null
        if (!token.contains('.')) return null
        return token.removePrefix("www.")
    }

    private fun maybeUpdate(context: Context, cache: File) {
        val last = BrowserPreferences.getAdBlockListUpdatedAt(context)
        val now = System.currentTimeMillis()
        if (last != 0L && now - last < UPDATE_INTERVAL_MS) return
        runCatching {
            val request = Request.Builder().url(REMOTE_LIST_URL).build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return
                val body = resp.body ?: return
                if (body.contentLength() in 1..MAX_REMOTE_BYTES || body.contentLength() == -1L) {
                    val text = body.string()
                    if (text.length < 1024) return // sanity: not a real list
                    cache.writeText(text)
                    val hosts = HashSet<String>(blockedHosts)
                    text.lineSequence().forEach { parseHostLine(it)?.let(hosts::add) }
                    blockedHosts = hosts
                    BrowserPreferences.setAdBlockListUpdatedAt(context, now)
                }
            }
        }
    }

    /** Generic, conservative element-hiding for the most common ad containers (low false-positive). */
    const val GENERIC_COSMETIC_CSS =
        "ins.adsbygoogle,.adsbygoogle,[id^=\"div-gpt-ad\"],[id*=\"google_ads\"]," +
        "iframe[src*=\"doubleclick\"],iframe[src*=\"googlesyndication\"]," +
        "[class*=\"ad-banner\"],[class*=\"advertisement\"],[aria-label=\"Advertisement\"]," +
        "[data-ad-slot],[id^=\"ad_\"],[class^=\"ad_\"]{display:none!important;}"

    /** Wraps the cosmetic CSS in an injectable document-start <style> snippet. */
    fun cosmeticCssJs(): String {
        val css = GENERIC_COSMETIC_CSS.replace("'", "\\'")
        return """
            (function(){
              try{
                if (window.__aaCosmeticInit) return; window.__aaCosmeticInit = true;
                var s = document.createElement('style');
                s.textContent = '$css';
                (document.head || document.documentElement).appendChild(s);
              }catch(e){}
            })();
        """.trimIndent()
    }

    /**
     * Best-effort Facebook in-feed "Sponsored" hider. Heuristic and fragile by nature (Facebook
     * obfuscates the label); cosmetic only, so a miss/false-positive never breaks the page.
     */
    val FACEBOOK_COSMETIC_JS = """
        (function(){
          if (window.__aaFbInit) return; window.__aaFbInit = true;
          function isSponsored(node){
            try{
              var t = (node.innerText||'').slice(0,4000);
              return /(^|\s)Sponsored(\s|$)/.test(t) || /(^|\s)Paid partnership(\s|$)/.test(t);
            }catch(e){ return false; }
          }
          function sweep(){
            try{
              var arts = document.querySelectorAll('div[role="article"]');
              for (var i=0;i<arts.length;i++){
                var a = arts[i];
                if (a.getAttribute('data-aahidden')) continue;
                var span = a.querySelector('a[aria-label="Sponsored"], a[href*="ads/about"]');
                if (span || isSponsored(a)){
                  a.setAttribute('data-aahidden','1');
                  a.style.display='none';
                }
              }
            }catch(e){}
          }
          var mo = new MutationObserver(function(){ sweep(); });
          try{ mo.observe(document.documentElement,{childList:true,subtree:true}); }catch(e){}
          sweep();
        })();
    """.trimIndent()
}
