package com.example.treemap.data.model

data class EntryStats(
    val total: Int = 0,
    val thrivingCount: Int = 0,
    val fairCount: Int = 0,
    val atRiskCount: Int = 0
) {
    val thrivingPercent: Int get() = if (total > 0) (thrivingCount * 100) / total else 65
    val fairPercent: Int get() = if (total > 0) (fairCount * 100) / total else 20
    val atRiskPercent: Int get() = if (total > 0) (atRiskCount * 100) / total else 15
}

