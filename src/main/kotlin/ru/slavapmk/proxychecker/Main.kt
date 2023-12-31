package ru.slavapmk.proxychecker

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt
import kotlin.system.exitProcess

typealias Sec = Long
typealias Ms = Long

private const val timeout: Sec = 10
private const val attempts = 100000
private const val sleep: Ms = 100
private const val checkUrl = "https://ya.ru/"


fun main() {
    val data = File("storage/data.csv").bufferedReader().use { it.readText() }

    val proxies = mutableListOf<Triple<String, Proxy.Type, String>>()
    for (s in data.splitToSequence('\n')) {
        val url = s.splitToSequence(',').first()
        val splitToSequence = url.split("://")
        val typeId = splitToSequence[0]
        val host = splitToSequence[1]

        val type = if (typeId.startsWith("socks"))
            Proxy.Type.SOCKS
        else if (typeId.startsWith("http"))
            Proxy.Type.HTTP
        else
            continue

        proxies.add(Triple(host, type, url))
    }


    val threads = mutableListOf<Thread>()
    val result = TreeMap<Long, ScanResult>()

    val state = AtomicInteger()
    val maxCount = attempts * proxies.size
    var lastPercent: Double = -1.0

    for (proxy in proxies) {
        val proxyHost = proxy.first
        val proxyType = proxy.second
        val proxyUrl = proxy.third
        val thread = Thread {
            val split = proxyHost.split(':')

            val cl = OkHttpClient
                .Builder()
                .addInterceptor(
                    HttpLoggingInterceptor {}.apply {
                        level = HttpLoggingInterceptor.Level.NONE
                    }
                ).apply {
                    proxy(
                        Proxy(
                            proxyType,
                            InetSocketAddress(
                                split[0],
                                split[1].toInt()
                            )
                        )
                    )
                    connectTimeout(timeout, TimeUnit.SECONDS)
                }.build()


            val request = Request.Builder().url(checkUrl).build()
            var sumTimes = 0L
            var lenTimes = 0
            for (i in 1..attempts) {
                try {
                    val startTime = System.currentTimeMillis()
                    cl.newCall(request).execute().close()
                    val endTime = System.currentTimeMillis()
                    sumTimes += (endTime - startTime)
                    lenTimes++
                    Thread.sleep(sleep)
                } catch (_: Exception) {

                } finally {
                    val incrementAndGet = state.incrementAndGet()
                    val percent = (incrementAndGet.toDouble() / maxCount * 100000).roundToInt().toDouble() / 1000
                    if (lastPercent != percent) {
                        lastPercent = percent
                        println("$incrementAndGet\t/ $maxCount\t$percent%")
                    }
                }
            }
            if (lenTimes != 0) {
                result[sumTimes / lenTimes] = ScanResult(
                    proxyHost,
                    proxyType,
                    proxyUrl,
                    (lenTimes.toDouble() / attempts * 100).roundToInt()
                )
            }
        }

        threads.add(thread)
        thread.start()
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        println()
        val buffer = arrayOfNulls<String>(result.size)

        var i = 0
        for (s in result) {
            println("${s.key}ms\t${s.value.proxyEfficiency}\t${s.value.proxyHost}\t${s.value.proxyType}\t${s.value.proxyUrl}")
            buffer[i] =
                "${s.key}ms;${s.value.proxyEfficiency};${s.value.proxyHost};${s.value.proxyType};${s.value.proxyUrl}"
            i++
        }
        File("storage/out.csv").writeText(buffer.joinToString("\n"))
    })

    for (thread in threads) {
        thread.join()
    }

    exitProcess(0)
}


data class ScanResult(
    val proxyHost: String,
    val proxyType: Proxy.Type,
    val proxyUrl: String,
    val proxyEfficiency: Int
)