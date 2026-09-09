package com.docuconvert.app.conversion.writers

import com.docuconvert.app.conversion.DocumentWriter
import com.docuconvert.app.domain.ContentBlock
import com.docuconvert.app.domain.ConversionError
import com.docuconvert.app.domain.DocumentContent
import com.docuconvert.app.domain.DocumentFormat
import com.docuconvert.app.domain.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

/** Writes DocumentContent as CommonMark / GitHub-Flavored Markdown. */
class MarkdownWriter : DocumentWriter {

    override val targetFormat: DocumentFormat = DocumentFormat.MD

    override suspend fun write(
        content: DocumentContent,
        outputFile: File,
        scope: CoroutineScope
    ): Result<Unit, ConversionError> = withContext(Dispatchers.IO) {
        try {
            val sb = StringBuilder()
            content.title?.let { sb.appendLine("# $it").appendLine() }
            for (block in content.blocks) {
                when (block) {
                    is ContentBlock.Heading -> sb.appendLine("${"#".repeat(block.level.coerceIn(1, 6))} ${block.text}").appendLine()
                    is ContentBlock.Paragraph -> sb.appendLine(block.text).appendLine()
                    is ContentBlock.ListItem -> sb.appendLine("- ${block.text}")
                    is ContentBlock.Table -> {
                        sb.appendLine()
                        // header row
                        block.rows.forEachIndexed { ri, row ->
                            sb.append("| ")
                            sb.append(row.joinToString(" | "))
                            sb.appendLine(" |")
                            if (ri == 0 && block.headerRow) {
                                sb.append("| ")
                                sb.append(row.map { "---" }.joinToString(" | "))
                                sb.appendLine(" |")
                            }
                        }
                        sb.appendLine()
                    }
                    is ContentBlock.ImagePlaceholder -> sb.appendLine("![${block.altText}]()").appendLine()
                    is ContentBlock.PageBreak -> sb.appendLine("---\n")
                }
            }
            FileOutputStream(outputFile).use { out ->
                out.write(sb.toString().toByteArray(StandardCharsets.UTF_8))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ConversionError.EngineFailure(e.message.orEmpty()))
        }
    }
}