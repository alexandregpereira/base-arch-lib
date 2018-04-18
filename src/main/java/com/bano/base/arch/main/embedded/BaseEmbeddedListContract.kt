package com.bano.base.arch.main.embedded

import com.bano.base.arch.main.BaseRepository
import io.realm.Realm
import io.realm.RealmModel

/**
 * Created by bk_alexandre.pereira on 15/09/2017.
 *
 */
interface BaseEmbeddedListContract<in X : Any> {

    var embeddedRepositoryList: List<BaseRepository<*, *, *>>?

    fun createEmbeddedRepositoryList(realm: Realm, idParent: Long?): List<BaseRepository<*, *, *>>
    fun getId(apiData: X): Long?
    fun insertOrUpdateListAtEmbeddedRepository(embeddedRepository: BaseRepository<*, *, *>, offset: Int, realm: Realm, apiObj: X)

    fun getEmbeddedRepositoryList(realm: Realm, idParent: Long?): List<BaseRepository<*, *, *>> {
        if(embeddedRepositoryList?.any { it.idParent != idParent } == true) {
            embeddedRepositoryList = null
        }
        val baseRepositoryList = embeddedRepositoryList ?: createEmbeddedRepositoryList(realm, idParent)
        embeddedRepositoryList = baseRepositoryList
        return baseRepositoryList
    }

    fun onBeforeInsertData(realm: Realm, apiObj: X) {
        onBeforeInsertData(true, realm, getId(apiObj), apiObj)
    }

    fun onBeforeInsertData(realm: Realm, id: Long, apiObj: X) {
        onBeforeInsertData(false, realm, id, apiObj)
    }

    private fun onBeforeInsertData(resumeMode: Boolean, realm: Realm, idParent: Long?, apiObj: X) {
        val embeddedRepositoryList = getEmbeddedRepositoryList(realm, idParent)
        embeddedRepositoryList.forEach { embeddedRepository ->
            embeddedRepository.resumeMode = resumeMode
            insertOrUpdateListAtEmbeddedRepository(embeddedRepository, embeddedRepository.offset, realm, apiObj)
        }
    }
}