package com.yago.grabadora

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

data class AudioItem(val uri: Uri, val name: String, val size: Long, val modified: Long)

/** Lista los audios de la carpeta SAF con UNA sola consulta (rápido con muchos ficheros). */
object AudioStore {

    fun list(ctx: Context): List<AudioItem> {
        val tree = Prefs.tree(ctx) ?: return emptyList()
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            tree, DocumentsContract.getTreeDocumentId(tree)
        )
        val proj = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )
        val out = ArrayList<AudioItem>()
        try {
            ctx.contentResolver.query(childrenUri, proj, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val name = c.getString(1) ?: continue
                    if (!name.endsWith(".wav") && !name.endsWith(".m4a")) continue
                    val docId = c.getString(0)
                    val uri = DocumentsContract.buildDocumentUriUsingTree(tree, docId)
                    out.add(AudioItem(uri, name, c.getLong(2), c.getLong(3)))
                }
            }
        } catch (_: Exception) {
        }
        out.sortByDescending { it.modified }
        return out
    }

    fun delete(ctx: Context, item: AudioItem): Boolean = try {
        DocumentsContract.deleteDocument(ctx.contentResolver, item.uri)
    } catch (_: Exception) {
        false
    }
}
