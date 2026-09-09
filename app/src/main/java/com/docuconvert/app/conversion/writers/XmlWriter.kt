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

/** Writes DocumentContent as pretty-printed XML (document interchange). */
class XmlWriter : DocumentWriter {

    override val targetFormat: DocumentFormat = DocumentFormat.XML

    override suspend fun write(
        content: DocumentContent,
        outputFile: File,
        scope: CoroutineScope
    ): Result<Unit, ConversionError> = withContext(Dispatchers.IO) {
        try {
            val sb = StringBuilder()
            sb.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            sb.appendLine("<document>")
            content.title?.let { sb.appendLine("  <title>${escape(it)}</title>") }
            sb.appendLine("  <body>")
            for (block in content.blocks) {
                when (block) {
                    is ContentBlock.Heading -> sb.appendLine("    <heading level=\"${block.level}\">${escape(block.text)}</heading>")
                    is ContentBlock.Paragraph -> sb.appendLine("    <paragraph>${escape(block.text)}</paragraph>")
                    is ContentBlock.ListItem -> sb.appendLine("    <listItem ordered=\"${block.ordered}\">${escape(block.text)}</listItem>")
                    is ContentBlock.Table -> {
                        sb.appendLine("    <table>")
                        block.rows.forEach { row ->
                            sb.appendLine("      <row>")
                            row.forEach { cell -> sb.appendLine("        <cell>${escape(cell)}</cell>") }
                            sb.appendLine("      </row>")
                        }
                        sb.appendLine("    </table>")
                    }
                    is ContentBlock.ImagePlaceholder -> sb.appendLine("    <image alt=\"${escape(block.altText)}\"/>")
                    is ContentBlock.PageBreak -> sb.appendLine("    <pageBreak/>")
                }
            }
            sb.appendLine("  </body>")
            sb.appendLine("</document>")
            FileOutputStream(outputFile).use { out ->
                out.write(sb.toString().toByteArray(StandardCharsets.UTF_8))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ConversionError.EngineFailure(e.message.orEmpty()))
        }
    }

    private fun escape(s: String): String {
        val sb = StringBuilder()
        for (c in s) {
            when (c) {
                '&' -> sb.append(AMP)
                '<' -> sb.append(LT)
                '>' -> sb.append(GT)
                '"' -> sb.append(QUOT)
                '\'' -> sb.append(APOS)
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    companion object {
        private const val AMP = "&amp;"
        private const val LT = "&lt;"
        private const val GT = "&gt;"
        private const val QUOT = "&quot;"
        private const val APOS = "&#39;"
    }
}