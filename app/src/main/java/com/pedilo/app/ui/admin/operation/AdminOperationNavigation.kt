package com.pedilo.app.ui.admin

internal enum class AdminOperationListKind {
    AllAttention,
    AllPreparing,
    AllInDelivery,
    AllProblems,
    AllBlocked,
    AllClosed,
    TodayAll,
    TodayActive,
    TodayProblems,
    TodayClosed,
    TodayReview,
    Unclassified,
    ClosedFinished,
    ClosedCancelled,
    ActiveWaitingStore,
    ActivePreparing,
    ActiveWaitingDriver,
    ActiveInDelivery,
    ActiveReviewState,
    ProblemStoreNotResponding,
    ProblemUserClaim,
    ProblemDelayed,
    ProblemWithoutResponsible,
    ProblemOperationalReview,
}

internal data class AdminOperationList(
    val title: String,
    val summary: String,
    val emptyText: String,
    val kind: AdminOperationListKind,
)
