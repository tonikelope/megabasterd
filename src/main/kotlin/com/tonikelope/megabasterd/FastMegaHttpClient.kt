package com.tonikelope.megabasterd

import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.entity.DeflateInputStreamFactory
import org.apache.hc.client5.http.entity.GZIPInputStreamFactory
import org.apache.hc.client5.http.entity.InputStreamFactory
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.io.SocketConfig
import org.apache.hc.core5.io.CloseMode
import org.apache.hc.core5.io.ModalCloseable
import org.apache.hc.core5.util.Timeout
import java.net.InetSocketAddress
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean

class FastMegaHttpClient<T : HttpUriRequestBase>(
    targetUrl: URI,
    requestFactory: (URI) -> T,
    private val configBuilder: RequestConfig.Builder = RequestConfig.custom(),
    private val megaProxyConfig: MegaHttpProxyConfiguration = MegaHttpProxyConfiguration(),
    private val eventListeners: Map<FMEventType, () -> Unit> = mutableMapOf(),
) : HttpClientBuilder(), ModalCloseable {

    private lateinit var httpClient: CloseableHttpClient
    private val request: T = requestFactory(targetUrl)

    override fun close(closeMode: CloseMode) = if (::httpClient.isInitialized) httpClient.close(closeMode) else {}
    override fun close() = if (::httpClient.isInitialized) httpClient.close() else {}

    companion object {
        @JvmStatic
        private val contentDecoderMap: LinkedHashMap<String, InputStreamFactory> = linkedMapOf(
            "gzip" to GZIPInputStreamFactory.getInstance(),
            "deflate" to DeflateInputStreamFactory.getInstance(),
        )

        private fun getDefaultManagerBuilder() = PoolingHttpClientConnectionManagerBuilder.create().apply {
            setMaxConnTotal(200)
            setMaxConnPerRoute(20)
        }

        @JvmStatic
        private val manager = getDefaultManagerBuilder().build()

        @JvmStatic
        private fun getSocksProxyManagerBuilder(proxyAddress: InetSocketAddress): PoolingHttpClientConnectionManager =
            getDefaultManagerBuilder().setDefaultSocketConfig(
                SocketConfig.custom().setSocksProxyAddress(proxyAddress).build()
            ).build()
    }

    private var useDefaultManager: Boolean = true
    private var timeoutIfNotSmartProxy = true

    enum class FMProperty(private val block: FastMegaHttpClient<*>.() -> Unit) {
        NO_CACHE({
            request.setHeader("Cache-Control", "no-cache")
            request.setHeader("Pragma", "no-cache")
        })
        ;

        fun invokeProperty(clientBuilder: FastMegaHttpClient<*>) = clientBuilder.block()
    }

    fun withProperty(property: FMProperty): FastMegaHttpClient<T> = with (property) {
        invokeProperty(this@FastMegaHttpClient)
        return@with this@FastMegaHttpClient
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

    fun execute(): CloseableHttpResponse {
        if (!::httpClient.isInitialized) {
            httpClient = build()
        }
        return httpClient.execute(request)
    }

    override fun build(): CloseableHttpClient {
        request.setHeader("Content-Type", "application/x-www-form-urlencoded")
        setUserAgent(MainPanel.DEFAULT_USER_AGENT)
        disableDefaultUserAgent()
        setContentDecoderRegistry(contentDecoderMap)
        if (useDefaultManager) {
            setConnectionManager(manager)
            setConnectionManagerShared(true)
        }
        disableAuthCaching()
        configBuilder.setContentCompressionEnabled(false)
        setDefaultRequestConfig(configBuilder.build())
        setupProxy()
        return super.build()
    }

    private var smartProxySocks: AtomicBoolean = AtomicBoolean(false)
    private var currentSmartProxy: String? = null
        set(value) {
            field = value
            megaProxyConfig.smartProxyCallback(value)
        }

    private fun setupProxy() = with(megaProxyConfig) {
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

    private fun setupSmartProxy(): Boolean = with(megaProxyConfig) {
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
        val proxyHost = proxySplits[0]
        val proxyPort: Int = proxySplits[1].let { portString ->
            if (portString.contains("@")) {
                val secondarySplits = portString.split("@")
                request.setHeader("Proxy-Authorization", "Basic ${secondarySplits[1]}")
                if (smartProxySocks.get()) 1080 else secondarySplits[0].toInt()
            } else if (smartProxySocks.get()) 1080 else portString.toInt()
        }

        if (smartProxySocks.get()) {
            // 1080 is the default SOCKS port
            val proxyHostAddress = InetSocketAddress(proxyHost, proxyPort)
            val newManager = getSocksProxyManagerBuilder(proxyHostAddress)
            useDefaultManager = false
            setConnectionManager(newManager)
            setConnectionManagerShared(false)
        } else setProxy(HttpHost("http", proxyHost, proxyPort))

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
        (forcedSmartProxy || currentSmartProxy != null || megaProxyConfig.smartProxyExtraConditions()) &&
        megaProxyConfig.mostAggressiveProxyType == FMProxyType.SMART

    private fun shouldUseBasicProxy() = isUseProxy && !isUseSmartProxy &&
        megaProxyConfig.mostAggressiveProxyType >= FMProxyType.BASIC

    private fun getAndDigestSmartProxy(discardCurrentOnFail: Boolean = true) {
        val smartProxy = proxyManager?.getProxy(ArrayList<String>(megaProxyConfig.excludedProxies.invoke()))
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
    ) = HttpHost("http", hostname, port)

    // <editor-fold desc="Panel Wrappers">
    private val proxyManager: SmartMegaProxyManager? get() = MainPanel.getProxy_manager()
    private val forcedSmartProxy: Boolean get() = proxyManager?.isForce_smart_proxy ?: false
    private val isUseProxy: Boolean get() = MainPanel.isUse_proxy()
    private val isUseSmartProxy: Boolean get() = MainPanel.isUse_smart_proxy()
    private val proxyUser: String get() = MainPanel.getProxy_user()
    private val proxyPass: String get() = MainPanel.getProxy_pass()
    // </editor-fold>
}