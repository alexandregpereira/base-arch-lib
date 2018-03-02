package com.bano.base.arch.main.embedded

import com.bano.base.BaseResponse
import com.bano.base.contract.BaseContract

/**
 * Created by bk_alexandre.pereira on 06/12/2017.
 *
 */
class HolderMapResponse<out E : BaseContract, F: BaseContract>(
        val objValue: E?, value: List<F>, isRemoteCallback: Boolean)
    : BaseResponse<List<F>>(value, isRemoteCallback)