package com.bano.base.arch.second

import android.arch.lifecycle.LiveData
import android.arch.paging.PagedList
import io.reactivex.Flowable

abstract class RoomRepository<E> {

    private val tag = "BaseRepository"

    val idParent: Long?
    val limit: Int
    var offset: Int = 0
        private set
    var total: Int? = null
    private val mExcludeFieldName: String?
    private val mOrderFieldName: String?

    constructor() {
        idParent = null
        limit = 0
        mExcludeFieldName = null
        mOrderFieldName = null
    }

    constructor(builder: Builder<E>) {
        idParent = builder.idParent
        limit = builder.limit
        mExcludeFieldName = builder.excludeFieldName
        mOrderFieldName = builder.orderFieldName
        offset = 0
    }

    abstract fun getLocalList(): LiveData<PagedList<E>?>
    abstract fun getLocalObj(id: Any): Flowable<E?>

    protected open fun getTagLog(): String = "BaseRepository"

    /**
     * Call this function to set the next page only. Do the query after this method
     */
    open fun nextPage() {
        val total = total
        if(total != null && offset + limit > total) {
            return
        }
        offset += limit
    }

    fun resetPage() {
        offset = 0
    }

    class Builder<T> {
        internal var excludeFieldName: String? = null
        internal var orderFieldName: String? = null
        internal var resumeMode: Boolean = false
        internal var limit: Int = 0
        internal var total: Int? = null
        internal var idParent: Long? = null

        fun setExcludeDateFieldName(fieldName: String): Builder<T> {
            excludeFieldName = fieldName
            return this
        }

        fun setOrderFieldName(fieldName: String): Builder<T> {
            orderFieldName = fieldName
            return this
        }

        fun resumeMode(): Builder<T> {
            resumeMode = true
            return this
        }

        fun setLimitQuery(limit: Int): Builder<T> {
            this.limit = limit
            return this
        }

        fun setTotalPagination(total: Int): Builder<T> {
            this.total = total
            return this
        }

        fun setIdParent(idParent: Long?): Builder<T> {
            this.idParent = idParent
            return this
        }
    }
}