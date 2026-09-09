package com.docuconvert.app.conversion.readers

import android.content.ContentResolver
import android.net.Uri
import com.docuconvert.app.conversion.DocumentReader
import com.docuconvert.app.domain.ContentBlock
import com.docuconvert.app.domain.ConversionError
import com.docuconvert.app.domain.DocumentContent
import com.docuconvert.app.domain.DocumentFormat
import com.docuconvert.app.domain.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Minimal RTF → text reader.
 *
 * Parses RTF control words for paragraphs/headings/bold/italic and decodes
 * \\uN, \\'hh and \\par. Embedded objects/pictures are replaced with
 * [ImagePlaceholder]. Macros are never executed (there is no execution
 * path — this parser only extracts display text).
 */
class RtfReader : DocumentReader {

    override val supportedFormat: DocumentFormat = DocumentFormat.RTF

    override suspend fun read(
        uri: Uri,
        contentResolver: ContentResolver,
        scope: CoroutineScope
    ): Result<DocumentContent, ConversionError> = withContext(Dispatchers.IO) {
        try {
            contentResolver.openInputStream(uri).use { input ->
                if (input == null) return@withContext Result.failure(ConversionError.CorruptedFile)
                val raw = input.bufferedReader(Charsets.UTF_8).readText()
                if (!raw.trimStart().startsWith("{\\rtf")) {
                    return@withContext Result.failure(ConversionError.CorruptedFile)
                }
                Result.success(parseRtf(raw))
            }
        } catch (e: OutOfMemoryError) {
            Result.failure(ConversionError.InsufficientStorage)
        } catch (e: Exception) {
            Result.failure(ConversionError.CorruptedFile)
        }
    }

    internal fun parseRtf(raw: String): DocumentContent {
        val blocks = mutableListOf<ContentBlock>()
        val current = StringBuilder()
        var bold = false
        var italic = false
        var boldAtStart = false
        var italicAtStart = false
        // Tracks whether any text in the current paragraph was emitted while
        // bold/italic was active. Needed because a run terminator (e.g. \b0)
        // arrives *before* the \par that flushes the paragraph — snapshotting
        // the flag at flush time would lose the run's formatting.
        var boldSeen = false
        var italicSeen = false
        var inPicture = 0
        var i = 0
        var sawObject = false

        fun emitText(s: String) {
            if (s.isEmpty()) return
            if (bold) boldSeen = true
            if (italic) italicSeen = true
            current.append(s)
        }

        fun emitText(c: Char) = emitText(c.toString())

        fun flushParagraph() {
            val text = current.toString().trim()
            if (text.isNotEmpty()) {
                blocks.add(
                    ContentBlock.Paragraph(
                        text,
                        bold = boldAtStart || boldSeen,
                        italic = italicAtStart || italicSeen
                    )
                )
            }
            current.clear()
            boldAtStart = bold
            italicAtStart = italic
            boldSeen = false
            italicSeen = false
        }

        while (i < raw.length) {
            val c = raw[i]
            when {
                c == '{' -> { i++ }
                c == '}' -> {
                    if (inPicture > 0) inPicture--
                    i++
                }
                c == '\\' && i + 1 < raw.length -> {
                    val n = raw[i + 1]
                    when {
                        n == '\\' || n == '{' || n == '}' -> { emitText(n); i += 2 }
                        n == '\'' && i + 3 < raw.length -> {
                            val hex = raw.substring(i + 2, i + 4)
                            val code = hex.toIntOrNull(16) ?: 0x3F
                            // RTF hex escapes are in Windows-1252 for most docs.
                            emitText(
                                java.nio.charset.Charset.forName("windows-1252")
                                    .decode(java.nio.ByteBuffer.wrap(byteArrayOf(code.toByte()))).toString()
                            )
                            i += 4
                        }
                        n == '~' -> { emitText('\u00A0'); i += 2 }
                        n == '-' -> { emitText('\u00AD'); i += 2 }
                        n == '_' -> { emitText('\u2011'); i += 2 }
                        n == '*' -> { i += 2 } // destination — skip control, keep skimming
                        n.isLetter() -> {
                            var j = i + 1
                            while (j < raw.length && raw[j].isLetter()) j++
                            val word = raw.substring(i + 1, j)
                            var k = j
                            var negative = false
                            if (k < raw.length && (raw[k] == '-' || raw[k].isDigit())) {
                                if (raw[k] == '-') { negative = true; k++ }
                                while (k < raw.length && raw[k].isDigit()) k++
                            }
                            val paramStr = raw.substring(j, k).trimStart('-')
                            val param = paramStr.toIntOrNull()
                            if (k < raw.length && raw[k] == ' ') k++
                            when (word) {
                                "par", "line", "row" -> flushParagraph()
                                "b" -> bold = (param ?: 1) != 0
                                "i" -> italic = (param ?: 1) != 0
                                "plain", "pard" -> { bold = false; italic = false }
                                "u" -> {
                                    var code = param ?: 63
                                    if (negative) code = -code
                                    val ch = (code and 0xFFFF).toChar()
                                    emitText(ch)
                                    // Skip the ANSI fallback char that follows \uN.
                                    if (k < raw.length) {
                                        if (raw[k] == '\\' && k + 3 < raw.length && raw[k + 1] == '\'') k += 4
                                        else if (raw[k] != '\\' && raw[k] != '{' && raw[k] != '}') k++
                                    }
                                }
                                "pict" -> inPicture++
                                "object", "objemb", "objclass" -> sawObject = true
                                "title" -> { /* container; text handled inline */ }
                                else -> { /* ignore unknown control words (fonts, colors, margins…) */ }
                            }
                            i = k
                        }
                        else -> i += 2
                    }
                }
                c == '\r' || c == '\n' -> i++ // RTF source line breaks are not content
                c == ';' && inPicture > 0 -> i++
                else -> {
                    if (inPicture == 0) emitText(c)
                    i++
                }
            }
        }
        flushParagraph()
        if (sawObject && blocks.none { it is ContentBlock.ImagePlaceholder }) {
            blocks.add(ContentBlock.ImagePlaceholder("Embedded object (not rendered)"))
        }
        return DocumentContent(title = null, blocks = blocks)
    }
}
