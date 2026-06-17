package com.pedilo.app.core.runtime

import com.pedilo.app.core.firebase.FirebasePublicCatalogAdapter
import com.pedilo.app.core.firebase.FirebasePublicClaimAdapter
import com.pedilo.app.core.firebase.FirebasePublicConfigurationAdapter
import com.pedilo.app.core.firebase.FirebasePublicOrderAdapter
import com.pedilo.app.core.firebase.FirebasePublicPlusOrderAdapter
import com.pedilo.app.core.firebase.FirebasePublicTrackingAdapter
import com.pedilo.app.core.port.PublicCatalogPort
import com.pedilo.app.core.port.PublicConfigurationPort
import com.pedilo.app.core.usecase.CreatePublicOrderUseCase
import com.pedilo.app.core.usecase.CreatePublicPlusOrderUseCase
import com.pedilo.app.core.usecase.GetPublicConfigurationUseCase
import com.pedilo.app.core.usecase.GetPublicTrackingUseCase
import com.pedilo.app.core.usecase.SubmitPublicClaimUseCase

fun publicCatalogPort(): PublicCatalogPort =
    FirebasePublicCatalogAdapter()

fun publicConfigurationPort(): PublicConfigurationPort =
    FirebasePublicConfigurationAdapter()

fun publicConfigurationUseCase(): GetPublicConfigurationUseCase =
    GetPublicConfigurationUseCase(FirebasePublicConfigurationAdapter())

fun publicLocalOrderUseCase(): CreatePublicOrderUseCase =
    CreatePublicOrderUseCase(FirebasePublicOrderAdapter())

fun publicPlusOrderUseCase(): CreatePublicPlusOrderUseCase =
    CreatePublicPlusOrderUseCase(FirebasePublicPlusOrderAdapter())

fun publicTrackingUseCase(): GetPublicTrackingUseCase =
    GetPublicTrackingUseCase(FirebasePublicTrackingAdapter())

fun publicClaimUseCase(): SubmitPublicClaimUseCase =
    SubmitPublicClaimUseCase(FirebasePublicClaimAdapter())
