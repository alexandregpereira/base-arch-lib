package com.bano.base.contract

/**
 * Created by bk_alexandre.pereira on 30/11/2017.
 *
 * Interface that must be implemented by the models
 */
interface BaseContract {

    @Deprecated("Field not being used")
    var excludeDate: Long?
    var order: Int
}