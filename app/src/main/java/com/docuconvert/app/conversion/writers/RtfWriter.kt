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

/**
 * Minimal RTF writer — outputs paragraphs, headings, bold, italic, lists, tables.
 * Does NOT emit embedded objects or macros.
 */
class RtfWriter : DocumentWriter {

    override val targetFormat: DocumentFormat = DocumentFormat.RTF

    override suspend fun write(
        content: DocumentContent,
        outputFile: File,
        scope: CoroutineScope
    ): Result<Unit, ConversionError> = withContext(Dispatchers.IO) {
        try {
            val sb = StringBuilder()
            sb.append("{\\rtf1\\ansi\\deff0 {\\fonttbl {\\f0 Times New Roman;}}")
            sb.append("{\\colortbl ;\\red0\\green0\\blue0;}")
            content.title?.let {
                sb.append("\\b ${escapeRtf(it)}\\b0\\par\n")
            }
            for (block in content.blocks) {
                when (block) {
                    is ContentBlock.Heading -> sb.append("${"\\b".repeat(block.level.coerceIn(1, 6))} ${escapeRtf(block.text)}\\b0\\par\n")
                    is ContentBlock.Paragraph -> {
                        val style = buildString {
                            if (block.bold) append("\\b ")
                            if (block.italic) append("\\i ")
                        }
                        if (style.isNotEmpty()) sb.append("$style${escapeRtf(block.text)}\\b0\\i0\\par\n")
                        else sb.append("${escapeRtf(block.text)}\\par\n")
                    }
                    is ContentBlock.ListItem -> sb.append("\\bullet ${escapeRtf(block.text)}\\par\n")
                    is ContentBlock.Table -> {
                        for (row in block.rows) {
                            sb.append("{\\trowd ")
                            row.forEach { cell ->
                                sb.append("\\cellx1000 ${escapeRtf(cell)}\\cell ")
                            }
                            sb.append("\\row }")
                        }
                    }
                    is ContentBlock.ImagePlaceholder -> sb.append("{\\field{\\*\\fldinst {SYMBOL 158}}{\\fldrslt [Image: ${escapeRtf(block.altText)}]}}\\par\n")
                    is ContentBlock.PageBreak -> sb.append("\\page\n")
                }
            }
            sb.append("}")
            FileOutputStream(outputFile).use { out ->
                out.write(sb.toString().toByteArray(StandardCharsets.UTF_8))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ConversionError.EngineFailure(e.message.orEmpty()))
        }
    }

    private fun escapeRtf(s: String): String {
        val sb = StringBuilder()
        for (c in s) {
            when {
                c == '\\' -> sb.append("\\\\")
                c == '{' -> sb.append("\\{")
                c == '}' -> sb.append("\\}")
                c < ' ' || c > '~' -> sb.append("\\u${c.code}?")
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }
}