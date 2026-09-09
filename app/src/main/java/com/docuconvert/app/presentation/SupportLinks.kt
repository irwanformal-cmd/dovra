package com.docuconvert.app.presentation

/**
 * External support/donation links for Dovra.
 *
 * These are fixed HTTPS constants only — never constructed dynamically and
 * never from user input. The app hands them to the external browser via
 * ACTION_VIEW; nothing is fetched inside the app and document processing
 * stays fully offline.
 */
object SupportLinks {
    const val BUY_ME_A_COFFEE = "https://buymeacoffee.com/riaksupport"
    const val SAWERIA = "https://saweria.co/riaksupport"
}
