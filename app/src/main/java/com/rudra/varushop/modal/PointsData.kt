package com.rudra.varushop.modal

data class PointsData(
    val totalActivePoints: Int,
    val totalPendingPoints: Int,
    val transactions: List<PointsTransaction>
)