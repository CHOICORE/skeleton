package me.choicore.logging.logback

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.IThrowableProxy
import ch.qos.logback.classic.spi.StackTraceElementProxy
import ch.qos.logback.core.UnsynchronizedAppenderBase
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

class SlackAppender : UnsynchronizedAppenderBase<ILoggingEvent>() {
    var webhookUrl: String? = null
    var channel: String? = null
    var iconEmoji: String = ":rage:"
    var level: Level = Level.ERROR
    var username: String? = null
    var serviceName: String? = null
    var includeStackTrace: Boolean = true
    var maxStackTraceLines: Int = 20

    override fun start() {
        if (this.webhookUrl == null) {
            addError("No webhook URL set for the appender named [${super.name}].")
            return
        }
        if (super.started.not()) {
            super.start()
        }
    }

    override fun stop() {
        if (super.started) {
            super.stop()
        }
    }

    override fun append(event: ILoggingEvent) {
        if (!event.level.isGreaterOrEqual(this.level)) {
            return
        }

        val color: String =
            when (event.level) {
                Level.ERROR -> "danger"
                Level.WARN -> "warning"
                Level.INFO -> "good"
                else -> "#439FE0"
            }

        val mdcPropertyMap: MutableMap<String, String> = event.mdcPropertyMap
        val traceIdKey = "traceId"
        val spanIdKey = "spanId"
        val message: String =
            buildString {
                if (mdcPropertyMap.containsKey(traceIdKey)) {
                    append("*Trace*: ${mdcPropertyMap[traceIdKey]}\n")
                }
                if (mdcPropertyMap.containsKey(spanIdKey)) {
                    append("*Span*: ${mdcPropertyMap[spanIdKey]}\n")
                }
                append("```\n")
                append("${event.formattedMessage}\n")

                if (includeStackTrace) {
                    val throwableProxy: IThrowableProxy = event.throwableProxy
                    append("${throwableProxy.className}: ${throwableProxy.message}\n")
                    val stackTraceElements: Array<StackTraceElementProxy> = throwableProxy.stackTraceElementProxyArray

                    if (maxStackTraceLines == -1) {
                        for (element: StackTraceElementProxy in stackTraceElements) {
                            append("\tat ${element.stackTraceElement}\n")
                        }
                    } else {
                        val linesToShow: Int = minOf(stackTraceElements.size, maxStackTraceLines)

                        for (i in 0 until linesToShow) {
                            append("\tat ${stackTraceElements[i].stackTraceElement}\n")
                        }

                        if (stackTraceElements.size > maxStackTraceLines) {
                            append("\t... ${stackTraceElements.size - maxStackTraceLines} more\n")
                        }
                    }
                    if (throwableProxy.cause != null) {
                        val cause: IThrowableProxy = throwableProxy.cause
                        append("Caused by: ${cause.className}: ${cause.message}\n")
                    }
                }

                append("```\n")
            }

        val payload: String =
            buildString {
                append("{")
                if (channel != null) {
                    append("\"channel\":\"$channel\",")
                }
                append("\"username\":\"$username\",")
                append("\"icon_emoji\":\"$iconEmoji\",")
                append("\"attachments\":[{")
                append("\"color\":\"$color\",")
                append("\"title\":\"${event.level} - ${event.loggerName}\",")
                append("\"text\":\"${escapeJson(message)}\",")
                append("\"ts\":\"${event.timeStamp / 1000}\",")
                append("\"footer\":\"$serviceName - thread: ${event.threadName}\"")
                append("}]")
                append("}")
            }

        this.send(payload)
    }

    private fun send(payload: String) {
        val url: URL = URI(this.webhookUrl!!).toURL()
        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
        try {
            with(connection) {
                this.connectTimeout = 1000
                this.readTimeout = 1000
                this.requestMethod = "POST"
                this.setRequestProperty("Content-Type", "application/json")
                this.doOutput = true
                this.outputStream.use {
                    it.write(payload.toByteArray())
                    it.flush()
                }

                if (this.responseCode != 200) {
                    addError("Failed to send log to Slack: ${this.responseCode}")
                }
            }
        } catch (e: Exception) {
            addError("Failed to send log to Slack", e)
        } finally {
            connection.disconnect()
        }
    }

    private fun escapeJson(text: String): String =
        text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
}
