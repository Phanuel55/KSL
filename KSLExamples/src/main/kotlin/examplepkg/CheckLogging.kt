package examplepkg

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun main() {
    logger.trace { "This is trace log" }
    logger.debug { "This is debug log" }
    logger.info { "This is info log" }
    logger.warn { "This is warn log" }
    logger.error { "This is error log" }
}