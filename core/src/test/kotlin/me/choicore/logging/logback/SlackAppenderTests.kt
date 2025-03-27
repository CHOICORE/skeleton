package me.choicore.logging.logback

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class SlackAppenderTests {
    @Test
    fun t1() {
        val loggerContext: LoggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        val slackAppender: SlackAppender =
            SlackAppender().apply {
                this.context = loggerContext
                this.webhookUrl = "https://hooks.slack.com/services/XXX/YYY/ZZZ"
                this.level = Level.ERROR
                this.username = "에러봇"
                this.channel = "#channel"
                this.start()
            }

        val log: Logger = loggerContext.getLogger("ROOT").apply { addAppender(slackAppender) }

        log.error("error message")
    }
}
