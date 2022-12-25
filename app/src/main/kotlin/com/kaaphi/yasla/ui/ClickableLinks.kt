package com.kaaphi.yasla.ui

import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

const val DEFAULT_LINK_TAG = "uriLink"

class ClickableLinks(
    private val uriHandler: UriHandler,
    private val linkStyle: SpanStyle = SpanStyle(textDecoration = TextDecoration.Underline),
    private val tag: String = DEFAULT_LINK_TAG,
    builder: AnnotatedString.Builder.(ClickableLinks) -> Unit
) {
    private lateinit var stringBuilder: AnnotatedString.Builder
    val annotatedString: AnnotatedString = androidx.compose.ui.text.buildAnnotatedString {
        stringBuilder = this
        builder.invoke(this, this@ClickableLinks)
    }

    fun appendLink(text: String, linkUri: String) {
        stringBuilder.apply {
            pushStringAnnotation(tag = tag, annotation = linkUri)
            withStyle(style = linkStyle) {
                append(text)
            }
            pop()
        }
    }

    fun onClick(offset: Int) {
        annotatedString.getStringAnnotations(tag = tag, start = offset, end = offset).firstOrNull()?.let {
            uriHandler.openUri(it.item)
        }
    }
}