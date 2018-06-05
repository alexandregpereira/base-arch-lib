package com.bano.base.arch.main.sync

/**
 * Created by bk_alexandre.pereira on 06/10/2017.
 *
 */
interface Syncable {

    var syncStatus: Int

    fun isPendentSyncStatus(): Boolean = this.syncStatus == SYNC_PENDENT_STATUS
    fun isIncompleteSyncStatus(): Boolean = this.syncStatus == SYNC_INCOMPLETE_STATUS
    fun isRealizeSyncStatus(): Boolean = this.syncStatus == SYNC_REALIZED_STATUS

    companion object {
        val SYNC_STATUS_FIELD_NAME = "syncStatus"

        val SYNC_REALIZED_STATUS = 0
        val SYNC_PENDENT_STATUS = 1
        val SYNC_INCOMPLETE_STATUS = 2
        val SYNC_ERROR_STATUS = 3
        val NOT_SYNC_STATUS = 4
    }
}