package com.bano.base.model

/**
 * Created by henrique.oliveira on 11/10/2017.
 *
 */

abstract class ApiRequestModel<out T>(url: String) : BaseApiRequestModel(url) {
    protected abstract fun getAccessGrantFromDatabase(): T?
}

