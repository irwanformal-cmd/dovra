# DocuConvert ProGuard / R8 rules.
# Keep model + Room + document-engine entry points; everything else may shrink.

# --- Room ---
-keep class androidx.room.RoomDatabase { *; }
-keep class com.docuconvert.app.data.** { *; }
-dontwarn androidx.room.paging.**

# --- Apache POI (reflection over schemas) ---
-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class schemaorg_apache_xmlbeans.** { *; }
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn schemaorg_apache_xmlbeans.**

# --- PDFBox-Android ---
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**

# --- ODFDOM ---
-keep class org.odftoolkit.** { *; }
-dontwarn org.odftoolkit.**

# --- EPUB (epublib uses reflection-free parsing, keep API) ---
-keep class nl.siegmann.epublib.** { *; }
-dontwarn nl.siegmann.epublib.**

# --- Kotlin / coroutines / serialization ---
-keep class kotlin.Metadata { *; }
-dontwarn kotlinx.**
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# --- Compose ---
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }

# --- SLF4J (epublib logging facade; no binding shipped, logs are dropped) ---
-dontwarn org.slf4j.**

# --- Transitive compile-only annotations / optional codecs referenced by
# POI / Jena / Commons (generated from missing_rules.txt; none are needed
# at runtime because those code paths are never exercised on device). ---
-dontwarn aQute.bnd.annotation.**
-dontwarn com.github.luben.zstd.**
-dontwarn edu.umd.cs.findbugs.annotations.**
-dontwarn java.awt.Shape
-dontwarn org.apache.jena.ext.**
-dontwarn org.osgi.framework.**
-dontwarn org.tukaani.xz.**

# --- kxml2 bundles org.xmlpull classes that collide with the Android
# framework's own XmlPullParser interface (android implements it too).
# Strip the duplicate out of the program classes so R8 no longer sees a
# library class implementing a program class. ---
-dontwarn org.xmlpull.v1.**
-dontwarn org.kxml2.**
-dontnote org.xmlpull.v1.**
-dontnote org.kxml2.**
