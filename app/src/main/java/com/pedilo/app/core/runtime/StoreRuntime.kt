package com.pedilo.app.core.runtime

import com.pedilo.app.core.firebase.FirebaseStoreOrdersAdapter
import com.pedilo.app.core.usecase.GetStoreOrdersUseCase

fun storeOrdersUseCase(): GetStoreOrdersUseCase =
    GetStoreOrdersUseCase(FirebaseStoreOrdersAdapter())
