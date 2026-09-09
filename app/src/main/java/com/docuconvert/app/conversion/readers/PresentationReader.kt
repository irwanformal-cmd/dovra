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
import org.apache.poi.hslf.usermodel.HSLFSlideShow
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.odftoolkit.odfdom.doc.OdfPresentationDocument
import org.odftoolkit.odfdom.pkg.OdfPackage

/**
 * Reads PPTX / PPT text via POI. Title = first non-blank text shape,
 * remaining shapes become bullets. Slide thumbnails are NOT rendered
 * (XSLF needs AWT) — honesty note in CapabilityMatrix.
 */
abstract class PresentationReader(
    override val supportedFormat: DocumentFormat
) : DocumentReader {

    /**
     * Extracts (title, bullets) per slide from the input stream.
     * Subclasses open the correct POI show type.
     */
    protected abstract fun extractSlides(inputStream: java.io.InputStream): List<Pair<String, List<String>>>

    override suspend fun read(
        uri: Uri,
        contentResolver: ContentResolver,
        scope: CoroutineScope
    ): Result<DocumentContent, ConversionError> = withContext(Dispatchers.IO) {
        try {
            contentResolver.openInputStream(uri).use { input ->
                if (input == null) return@withContext Result.failure(ConversionError.CorruptedFile)
                val slides = extractSlides(input)
                val blocks = mutableListOf<ContentBlock>()
                slides.forEachIndexed { idx, (title, bullets) ->
                    if (title.isNotBlank()) blocks.add(ContentBlock.Heading(title, 1))
                    else blocks.add(ContentBlock.Heading("Slide ${idx + 1}", 2))
                    bullets.forEach { blocks.add(ContentBlock.ListItem(it, false)) }
                    if (idx < slides.size - 1) blocks.add(ContentBlock.PageBreak)
                }
                Result.success(DocumentContent(null, blocks))
            }
        } catch (e: OutOfMemoryError) {
            Result.failure(ConversionError.InsufficientStorage)
        } catch (e: Exception) {
            Result.failure(ConversionError.CorruptedFile)
        }
    }
}

class PptxReader : PresentationReader(DocumentFormat.PPTX) {
    override fun extractSlides(inputStream: java.io.InputStream): List<Pair<String, List<String>>> {
        XMLSlideShow(inputStream).use { show ->
            return show.slides.map { slide ->
                val texts = slide.shapes
                    .filterIsInstance<org.apache.poi.xslf.usermodel.XSLFTextShape>()
                    .mapNotNull { it.text?.trim()?.takeIf { t -> t.isNotBlank() } }
                val title = texts.firstOrNull().orEmpty()
                val bullets = texts.drop(1).flatMap { t ->
                    t.split("\n").map { it.trim() }.filter { it.isNotEmpty() && it != title }
                }
                title to bullets
            }
        }
    }
}

class PptReader : PresentationReader(DocumentFormat.PPT) {
    override fun extractSlides(inputStream: java.io.InputStream): List<Pair<String, List<String>>> {
        HSLFSlideShow(inputStream).use { show ->
            return show.slides.map { slide ->
                val texts = slide.shapes
                    .filterIsInstance<org.apache.poi.hslf.usermodel.HSLFTextShape>()
                    .mapNotNull { it.text?.trim()?.takeIf { t -> t.isNotBlank() } }
                val title = texts.firstOrNull().orEmpty()
                val bullets = texts.drop(1).flatMap { t ->
                    t.split("\n").map { it.trim() }.filter { it.isNotEmpty() && it != title }
                }
                title to bullets
            }
        }
    }
}

/** ODP reader using ODFDOM. */
class OdpReader : DocumentReader {

    override val supportedFormat: DocumentFormat = DocumentFormat.ODP

    override suspend fun read(
        uri: Uri,
        contentResolver: ContentResolver,
        scope: CoroutineScope
    ): Result<DocumentContent, ConversionError> = withContext(Dispatchers.IO) {
        try {
            contentResolver.openInputStream(uri).use { input ->
                if (input == null) return@withContext Result.failure(ConversionError.CorruptedFile)
                val pkg = OdfPackage.loadPackage(input)
                pkg.use { p ->
                    val doc = OdfPresentationDocument.loadDocument(p)
                    val blocks = mutableListOf<ContentBlock>()
                    val root = doc.getContentRoot()
                    walkOdpPage(root, blocks)
                    Result.success(DocumentContent(null, blocks))
                }
            }
        } catch (e: OutOfMemoryError) {
            Result.failure(ConversionError.InsufficientStorage)
        } catch (e: Exception) {
            Result.failure(ConversionError.CorruptedFile)
        }
    }

    private fun walkOdpPage(node: org.w3c.dom.Node, out: MutableList<ContentBlock>) {
        val drawNs = "urn:oasis:names:tc:opendocument:xmlns:drawing:1.0"
        val textNs = "urn:oasis:names:tc:opendocument:xmlns:text:1.0"
        if (drawNs == node.namespaceURI && "page" == node.localName) {
            // new slide
            if (out.isNotEmpty()) out.add(ContentBlock.PageBreak)
            val slideIdx = out.count { it is ContentBlock.PageBreak } + 1
            out.add(ContentBlock.Heading("Slide $slideIdx", 2))
            // walk children for text
            var child = node.firstChild
            while (child != null) {
                collectOdpText(child, out, textNs)
                child = child.nextSibling
            }
        } else {
            var child = node.firstChild
            while (child != null) {
                walkOdpPage(child, out)
                child = child.nextSibling
            }
        }
    }

    private fun collectOdpText(node: org.w3c.dom.Node, out: MutableList<ContentBlock>, textNs: String) {
        if (textNs == node.namespaceURI && ("p" == node.localName || "h" == node.localName)) {
            val t = node.textContent?.trim().orEmpty()
            if (t.isNotBlank()) out.add(ContentBlock.ListItem(t, false))
        } else {
            var child = node.firstChild
            while (child != null) {
                collectOdpText(child, out, textNs)
                child = child.nextSibling
            }
        }
    }
}