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

/** Writes DocumentContent as semantic HTML5. */
class HtmlWriter : DocumentWriter {

    override val targetFormat: DocumentFormat = DocumentFormat.HTML

    override suspend fun write(
        content: DocumentContent,
        outputFile: File,
        scope: CoroutineScope
    ): Result<Unit, ConversionError> = withContext(Dispatchers.IO) {
        try {
            val sb = StringBuilder()
            sb.appendLine("<!DOCTYPE html>")
            sb.appendLine("<html lang=\"en\">")
            sb.appendLine("<head>")
            sb.appendLine("  <meta charset=\"UTF-8\">")
            sb.appendLine("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
            content.title?.let { sb.appendLine("  <title>${escape(it)}</title>") }
            sb.appendLine("  <style>")
            sb.appendLine("    body{font-family:system-ui,sans-serif;max-width:800px;margin:2rem auto;padding:0 1rem;line-height:1.6}")
            sb.appendLine("    h1,h2,h3,h4{margin-top:1.5em}")
            sb.appendLine("    table{border-collapse:collapse;width:100%;margin:1rem 0}")
            sb.appendLine("    th,td{border:1px solid #ddd;padding:0.5rem}")
            sb.appendLine("    th{background:#f5f5f5}")
            sb.appendLine("    ul{padding-left:1.5rem}")
            sb.appendLine("    .page-break{border-top:1px dashed #999;margin:2rem 0}")
            sb.appendLine("  </style>")
            sb.appendLine("</head>")
            sb.appendLine("<body>")
            content.title?.let { sb.appendLine("<h1>${escape(it)}</h1>") }
            for (block in content.blocks) {
                when (block) {
                    is ContentBlock.Heading -> sb.appendLine("<h${block.level}>${escape(block.text)}</h${block.level}>")
                    is ContentBlock.Paragraph -> {
                        val style = buildString {
                            if (block.bold) append("font-weight:bold;")
                            if (block.italic) append("font-style:italic;")
                        }
                        if (style.isNotEmpty()) sb.appendLine("<p style=\"$style\">${escape(block.text)}</p>")
                        else sb.appendLine("<p>${escape(block.text)}</p>")
                    }
                    is ContentBlock.ListItem -> sb.appendLine("<li>${escape(block.text)}</li>")
                    is ContentBlock.Table -> {
                        sb.appendLine("<table>")
                        block.rows.forEachIndexed { ri, row ->
                            sb.appendLine("<tr>")
                            row.forEach { cell ->
                                val tag = if (ri == 0 && block.headerRow) "th" else "td"
                                sb.appendLine("  <$tag>${escape(cell)}</$tag>")
                            }
                            sb.appendLine("</tr>")
                        }
                        sb.appendLine("</table>")
                    }
                    is ContentBlock.ImagePlaceholder -> sb.appendLine("<div class=\"image-placeholder\">[Image: ${escape(block.altText)}]</div>")
                    is ContentBlock.PageBreak -> sb.appendLine("<hr class=\"page-break\">")
                }
            }
            sb.appendLine("</body>")
            sb.appendLine("</html>")
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
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&#39;")
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }
}