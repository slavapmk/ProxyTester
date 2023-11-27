package ru.slavapmk.proxychecker

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.TreeMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.exitProcess


private const val timeout = 10L
private const val attempts = 10
private const val check = "https://api.openai.com"

fun main() {
    val data = File("data.csv").bufferedReader().use { it.readText() }

    val proxies = mutableListOf<Pair<String, Proxy.Type>>()
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
            Proxy.Type.DIRECT

        proxies.add(Pair(host, type))
    }


    val threads = mutableListOf<Thread>()
    val result = TreeMap<Long, Pair<String, Proxy.Type>>()

    val state = AtomicInteger()

    for (proxy in proxies) {
        val proxyHost = proxy.first
        val proxyType = proxy.second
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

            val status = try {
                val request = Request.Builder().url(check).build()
                var sumTimes = 0L
                var lenTimes = 0
                for (i in 0..attempts) {
                    val startTime = System.currentTimeMillis()
                    cl.newCall(request).execute().close()
                    val endTime = System.currentTimeMillis()
                    sumTimes += (endTime - startTime)
                    lenTimes++
                }
                result[sumTimes / lenTimes] = Pair(proxyHost, proxyType)
                "SUC"
            } catch (_: Exception) {
                "ERR"
            }
            println("${state.incrementAndGet()}\t/ ${proxies.size}\t$status")
        }

        threads.add(thread)
        thread.start()
    }

    for (thread in threads) {
        thread.join()
    }
    println()
    for (s in result) {
        println("${s.key}ms\t${s.value.second}\t${s.value.first}")
    }
    exitProcess(0)
}