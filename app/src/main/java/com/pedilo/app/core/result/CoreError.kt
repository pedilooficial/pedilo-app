package com.pedilo.app.core.result

sealed interface CoreError {
    val code: String

    data class Validation(
        val errors: List<ValidationError>,
    ) : CoreError {
        override val code: String = "validation"
    }

    data object IncompleteData : CoreError {
        override val code: String = "incomplete_data"
    }

    data object NotAvailable : CoreError {
        override val code: String = "not_available"
    }

    data class Operational(
        val humanMessage: String,
    ) : CoreError {
        override val code: String = "operational"
    }

    data object Unknown : CoreError {
        override val code: String = "unknown"
    }
}
