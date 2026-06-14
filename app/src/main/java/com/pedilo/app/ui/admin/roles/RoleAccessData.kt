package com.pedilo.app.ui.admin

internal val roleEntries = listOf(
    "Usuarios del equipo",
    "Administradores",
    "Locales store",
    "Repartidores driver",
    "Altas pendientes",
    "Usuarios inactivos",
    "Vinculaciones pendientes",
).map { AdminEntry(it, "Organización de accesos") }
