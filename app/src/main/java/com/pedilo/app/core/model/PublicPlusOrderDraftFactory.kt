package com.pedilo.app.core.model

fun publicPlusBuyOrderDraft(
    contactName: String,
    contactPhone: String,
    deliveryAddress: String,
    items: List<PublicPlusOrderItem>,
    whereToBuy: String,
    note: String,
    paymentMethod: String,
    amount: String,
    schedule: String,
): PublicPlusOrderDraft =
    PublicPlusOrderDraft(
        source = "public_plus_buy",
        requestType = PublicPlusOrderType.BUY,
        contact = CustomerContact(name = contactName, phone = contactPhone),
        items = items,
        sourceReference = whereToBuy,
        destination = deliveryAddress,
        note = note,
        paymentMethod = paymentMethod,
        amount = amount,
        schedule = schedule,
    )

fun publicPlusPickupShippingOrderDraft(
    contactName: String,
    contactPhone: String,
    pickupAddress: String,
    deliveryAddress: String,
    packageDescription: String,
    referenceName: String,
    note: String,
    paymentMethod: String,
    amount: String,
    schedule: String,
): PublicPlusOrderDraft =
    PublicPlusOrderDraft(
        source = "public_plus_pickup_shipping",
        requestType = PublicPlusOrderType.PICKUP_SHIPPING,
        contact = CustomerContact(name = contactName, phone = contactPhone),
        items = listOf(PublicPlusOrderItem(packageDescription, referenceName)),
        sourceReference = pickupAddress,
        destination = deliveryAddress,
        note = note,
        paymentMethod = paymentMethod,
        amount = amount,
        schedule = schedule,
    )
