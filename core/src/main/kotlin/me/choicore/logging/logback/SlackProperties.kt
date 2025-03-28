package me.choicore.logging.logback

import ch.qos.logback.classic.Level
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "logging.slack")
data class SlackProperties(
    var webhook: Webhook,
) {
    data class Webhook(
        var url: String? = null,
        var channel: String? = null,
        var iconEmoji: String,
        var level: Level,
        var username: String? = null,
        var serviceName: String? = null,
        var includeStackTrace: Boolean,
        var maxStackTraceLines: Int = -1,
    )
}
