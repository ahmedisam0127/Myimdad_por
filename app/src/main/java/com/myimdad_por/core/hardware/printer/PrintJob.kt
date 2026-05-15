package com.myimdad_por.core.hardware.printer

/**
 * Print alignment options for thermal output.
 */
enum class PrintAlignment {
    LEFT,
    CENTER,
    RIGHT
}

/**
 * Immutable thermal print job.
 */
data class PrintJob(
    val title: String? = null,
    val lines: List<String>,
    val copies: Int = 1,
    val alignment: PrintAlignment = PrintAlignment.LEFT,
    val feedPaperLines: Int = 2,
    val cutPaper: Boolean = false,
    val timestampMillis: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(lines.isNotEmpty()) { "lines must not be empty." }
        require(copies > 0) { "copies must be greater than 0." }
        require(feedPaperLines >= 0) { "feedPaperLines must be greater than or equal to 0." }
    }

    fun normalizedLines(): List<String> {
        return lines.map { it.trimEnd() }
    }
}