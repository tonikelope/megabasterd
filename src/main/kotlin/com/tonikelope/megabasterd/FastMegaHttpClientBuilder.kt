package com.tonikelope.megabasterd

import org.apache.hc.client5.http.ConnectionKeepAliveStrategy
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.entity.DeflateInputStreamFactory
import org.apache.hc.client5.http.entity.GZIPInputStreamFactory
import org.apache.hc.client5.http.entity.InputStreamFactory
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.util.Timeout
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level
import java.util.logging.Logger

class FastMegaHttpClientBuilder<T : HttpUriRequestBase>(
    private val request: T,
    private val configBuilder: RequestConfig.Builder = RequestConfig.custom(),
    private val proxyConfig: MegaHttpProxyConfiguration = MegaHttpProxyConfiguration(),
    private val eventListeners: Map<FMEventType, () -> Unit> = mutableMapOf(),
) : HttpClientBuilder() {

    init {
        Logger.getLogger(FastMegaHttpClientBuilder::class.java.name).level = Level.SEVERE
    }

    companion object {
        @JvmStatic
        private val contentDecoderMap: LinkedHashMap<String, InputStreamFactory> = linkedMapOf(
            "gzip" to GZIPInputStreamFactory.getInstance(),
            "deflate" to DeflateInputStreamFactory.getInstance(),
        )
        @JvmStatic
        private val manager = PoolingHttpClientConnectionManagerBuilder.create().apply {
            useSystemProperties()
            setMaxConnTotal(200)
            setMaxConnPerRoute(20)
        }.build()
    }

    private var timeoutIfNotSmartProxy = false

    enum class FMProperty(private val block: FastMegaHttpClientBuilder<*>.() -> Unit) {
        NO_CACHE({
            request.setHeader("Cache-Control", "no-cache")
            request.setHeader("Pragma", "no-cache")
        })
        ;

        fun invokeProperty(clientBuilder: FastMegaHttpClientBuilder<*>) = clientBuilder.block()
    }

    fun withProperty(property: FMProperty): FastMegaHttpClientBuilder<T> = with (property) {
        invokeProperty(this@FastMegaHttpClientBuilder)
        return@with this@FastMegaHttpClientBuilder
    }

    enum class FMProxyType { NONE, BASIC, SMART }

    enum class FMEventType {
        CURRENT_SMART_PROXY_ERRORED,
        SMART_PROXY_NULL,
        WILL_USE_SMART_PROXY,
    }

    /**
     * Represents the configuration for the Mega HTTP proxy.
     * @param mostAggressiveProxyType Indicates the highest 'level' of proxy we will attempt to use for this client.
     *                                for example, if set to FMProxyType.SMART, it will use the smart proxy if available.
     *                                If NOT available, it will fallback to FMProxyType.BASIC, and finally to FMProxyType.NONE.
     * @param excludedProxies List of proxies to exclude from use for this client.
     * @param smartProxyCallback Callback function executed when smart proxy 'changes'.
     */
    class MegaHttpProxyConfiguration(
        val mostAggressiveProxyType: FMProxyType = FMProxyType.NONE,
        val excludedProxies: () -> List<String> = { emptyList() },
        val proxyFailedCondition: () -> Boolean = { false },
        val smartProxyExtraConditions: () -> Boolean = { true },
        val tryBasicAfterSmartFail: Boolean = true,
        val trySmartAfterNoUseBasic: Boolean = true,
        val smartProxyCallback: (String?) -> Unit = { },
    )

    override fun build(): CloseableHttpClient {
        request.setHeader("Content-Type", "application/x-www-form-urlencoded")
        request.setHeader("User-Agent", MainPanel.DEFAULT_USER_AGENT)
        disableDefaultUserAgent()
        setContentDecoderRegistry(linkedMapOf())
        setConnectionManager(manager)
        disableAuthCaching()
        setConnectionManagerShared(true)
        configBuilder.setContentCompressionEnabled(false)
        setDefaultRequestConfig(configBuilder.build())
        setupProxy()
        return super.build()
    }

    private var smartProxySocks: AtomicBoolean = AtomicBoolean(false)
    private var currentSmartProxy: String? = null
        set(value) {
            field = value
            proxyConfig.smartProxyCallback(value)
        }

    /**
     *
     *
     *
     *
     *
     * todo;
     * https://stackoverflow.com/questions/22937983/how-to-use-socks-5-proxy-with-apache-http-client-4
     *
     *
     *
     *
     *
     *
     *
     */

    private fun setupProxy() = with(proxyConfig) {
        if (shouldUseSmartProxy()) {
            val success = setupSmartProxy()
            if (!success && tryBasicAfterSmartFail) setupBasicProxy()
        }
        else if (shouldUseBasicProxy()) setupBasicProxy()
        else if (trySmartAfterNoUseBasic) setupSmartProxy()

        val proxyManager = proxyManager
        val currentSmartProxy = currentSmartProxy
        val (connectMillis, responseMillis) = when {
            proxyManager != null && currentSmartProxy != null -> {
                val timeoutMillis = proxyManager.proxy_timeout.toLong()
                timeoutMillis to timeoutMillis * 2L
            }
            timeoutIfNotSmartProxy -> Transference.HTTP_CONNECT_TIMEOUT.toLong() to Transference.HTTP_READ_TIMEOUT.toLong()
            else -> 0L to 0L
        }

        // Todo deal with this deprecation
        configBuilder.setConnectTimeout(Timeout.ofMilliseconds(connectMillis))
        configBuilder.setResponseTimeout(Timeout.ofMilliseconds(responseMillis))
    }

    private fun setupSmartProxy(): Boolean = with(proxyConfig) {
        if (currentSmartProxy != null && proxyFailedCondition()) {
            eventListeners[FMEventType.CURRENT_SMART_PROXY_ERRORED]?.invoke()
            getAndDigestSmartProxy()
        } else if (currentSmartProxy == null) getAndDigestSmartProxy()

        val currentSmartProxy = currentSmartProxy ?: run {
            eventListeners[FMEventType.SMART_PROXY_NULL]?.invoke()
            return@with false
        }

        eventListeners[FMEventType.WILL_USE_SMART_PROXY]?.invoke()
        val proxySplits = currentSmartProxy.split(':')
        setProxy(getProxyHttpHost(proxySplits[0], proxySplits[1].toInt()))
        return@with true
    }

    private fun setupBasicProxy() {
        currentSmartProxy = null
        setProxy(getProxyHttpHost())
        val proxyUser = proxyUser.takeIf { it.isNotEmpty() } ?: return

        val hashedProxyAuth = MiscTools.Bin2BASE64(("$proxyUser:$proxyPass").toByteArray(StandardCharsets.UTF_8))
        request.setHeader("Proxy-Authorization", "Basic $hashedProxyAuth")
    }

    private fun shouldUseSmartProxy() = isUseSmartProxy && !isUseProxy && proxyManager != null &&
        (forcedSmartProxy || currentSmartProxy != null || proxyConfig.smartProxyExtraConditions()) &&
        proxyConfig.mostAggressiveProxyType == FMProxyType.SMART

    private fun shouldUseBasicProxy() = isUseProxy && !isUseSmartProxy &&
        proxyConfig.mostAggressiveProxyType >= FMProxyType.BASIC

    private fun getAndDigestSmartProxy(discardCurrentOnFail: Boolean = true) {
        val smartProxy = proxyManager?.getProxy(ArrayList<String>(proxyConfig.excludedProxies.invoke()))
        if (smartProxy == null || smartProxy.size < 2) {
            if (discardCurrentOnFail) currentSmartProxy = null
            return
        }
        currentSmartProxy = smartProxy[0]
        smartProxySocks.set(smartProxy[1].equals("socks", ignoreCase = true))
    }

    private fun getProxyHttpHost(
        hostname: String = MainPanel.getProxy_host(),
        port: Int = MainPanel.getProxy_port(),
    ) = HttpHost(hostname, port, if (smartProxySocks.get()) "socks" else null)

    // <editor-fold desc="Panel Wrappers">
    private val proxyManager: SmartMegaProxyManager? get() = MainPanel.getProxy_manager()
    private val forcedSmartProxy: Boolean get() = proxyManager?.isForce_smart_proxy ?: false
    private val isUseProxy: Boolean get() = MainPanel.isUse_proxy()
    private val isUseSmartProxy: Boolean get() = MainPanel.isUse_smart_proxy()
    private val proxyUser: String get() = MainPanel.getProxy_user()
    private val proxyPass: String get() = MainPanel.getProxy_pass()
    // </editor-fold>
}