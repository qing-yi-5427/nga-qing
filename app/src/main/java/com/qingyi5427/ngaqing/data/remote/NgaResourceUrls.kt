package com.qingyi5427.ngaqing.data.remote

/**
 * Canonical NGA-hosted resource URLs used by nga-qing.
 *
 * These rules describe behavior observed from NGA's public responses and are
 * covered by local contract tests so rendering code does not duplicate host
 * migration details.
 */
internal object NgaResourceUrls {
    private const val BOARD_ICON_BASE = "https://img4.nga.cn/ngabbs/nga_classic/f/app"
    private const val COLLECTION_ICON_BASE = "https://img4.nga.cn/proxy/cache_attach/ficon"
    private const val ATTACHMENT_BASE = "https://img.nga.cn/attachments"

    private val legacyAttachmentHost = Regex(
        """https?://img\d*\.(?:nga\.178\.com|ngacn\.cc)/attachments(?=/|[?#]|$)""",
        RegexOption.IGNORE_CASE
    )
    private val legacyImageHost = Regex(
        """https?://img(\d*)\.(?:nga\.178\.com|ngacn\.cc)""",
        RegexOption.IGNORE_CASE
    )

    fun boardIcon(fid: String, stid: String?): String? {
        val collectionId = stid?.trim()?.toLongOrNull()?.takeIf { it > 0 }
        if (collectionId != null) return "$COLLECTION_ICON_BASE/${collectionId}v.png"

        val forumId = fid.trim().toLongOrNull() ?: return null
        return "$BOARD_ICON_BASE/$forumId.png"
    }

    fun normalizeLegacyImageHosts(text: String): String {
        if (text.isEmpty()) return text
        val attachmentsUpdated = legacyAttachmentHost.replace(text, ATTACHMENT_BASE)
        return legacyImageHost.replace(attachmentsUpdated, "https://img\$1.nga.cn")
    }
}
