package com.bano.base.arch.main.embedded

import com.bano.base.BaseResponse

/**
 * Created by bk_alexandre.pereira on 06/12/2017.
 *
 */
class HolderMapResponse<out E, F>(
        val objValue: E?, value: List<F>, isRemoteCallback: Boolean)
    : BaseResponse<List<F>>(value, isRemoteCallback)