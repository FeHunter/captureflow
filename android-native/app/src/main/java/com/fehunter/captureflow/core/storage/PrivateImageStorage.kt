package com.fehunter.captureflow.core.storage

/** Public core boundary for deleting app-owned private image files. */
interface PrivateImageStorage {
    fun deletePrivateImage(uri: String?)
}
