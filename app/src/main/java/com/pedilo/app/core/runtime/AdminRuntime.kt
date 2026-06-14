package com.pedilo.app.core.runtime

import com.pedilo.app.core.firebase.FirebaseAdminOrdersAdapter
import com.pedilo.app.core.usecase.GetAdminOperationOrdersUseCase

fun adminOrdersUseCase(): GetAdminOperationOrdersUseCase =
    GetAdminOperationOrdersUseCase(FirebaseAdminOrdersAdapter())
