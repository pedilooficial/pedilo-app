package com.pedilo.app.core.runtime

import com.pedilo.app.core.firebase.FirebaseDriverOrdersAdapter
import com.pedilo.app.core.usecase.GetDriverOrdersUseCase

fun driverOrdersUseCase(): GetDriverOrdersUseCase =
    GetDriverOrdersUseCase(FirebaseDriverOrdersAdapter())
