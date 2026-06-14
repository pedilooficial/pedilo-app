package com.pedilo.app.core.model

fun publicLocalOrderDraft(
    storeId: String,
    storeName: String,
    contactName: String,
    contactPhone: String,
    addressLine: String,
    addressNotes: String,
    items: List<PublicOrderItem>,
    paymentMethod: PaymentMethod,
    notes: String,
): PublicOrderDraft =
    PublicOrderDraft(
        source = "public_local",
        storeId = storeId,
        storeName = storeName,
        contact = CustomerContact(
            name = contactName,
            phone = contactPhone,
        ),
        deliveryLocation = DeliveryLocation(
            addressLine = addressLine,
            notes = addressNotes,
        ),
        items = items,
        paymentMethod = paymentMethod,
        notes = notes,
    )
