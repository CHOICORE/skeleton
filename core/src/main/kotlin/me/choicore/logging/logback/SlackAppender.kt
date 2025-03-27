package me.choicore.logging.logback

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Layout
import ch.qos.logback.core.UnsynchronizedAppenderBase
import java.time.format.DateTimeFormatter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SlackAppender : UnsynchronizedAppenderBase<ILoggingEvent>() {
    private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    private val executorService: ExecutorService =
        Executors.newSingleThreadExecutor {
            Thread(it, "slack").apply { isDaemon = true }
        }
    var webhookUrl: String? = null
    var layout: Layout<ILoggingEvent>? = null
    var channel: String? = null
    var iconEmoji: String = ":memo:"
    var level: Level = Level.ERROR
    var username: String? = null

    override fun start() {
        if (webhookUrl == null) {
            addError("No webhookUrl provided for SlackAppender")
            return
        }
        if (super.started.not()) {
            super.start()
        }
    }

    override fun stop() {
        if (super.started) {
            executorService.shutdown()
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
        val formattedMessage: String = layout?.doLayout(event) ?: event.formattedMessage
        val message: String =
            buildString {
                append("*${event.level}*: $formattedMessage")
                if (event.throwableProxy != null) {
                    append("\n```")
                    append(
                        event.throwableProxy.stackTraceElementProxyArray.joinToString("\n") {
                            it.stackTraceElement.toString()
                        },
                    )
                    append("```")
                }
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
                append("\"title\":\"${escapeJson(event.loggerName)}\",")
                append("\"text\":\"${escapeJson(message)}\",")
                append("\"ts\":\"${event.timeStamp}\",")
                append("\"footer\":\"${event.threadName}\"")
                append("}]")
                append("}")
            }
        this.send(payload)
        executorService.execute {
        }
    }

    fun send(payload: String) {
        println("[SlackAppender] send called: $payload")
    }

    private fun escapeJson(text: String): String =
        text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
}
