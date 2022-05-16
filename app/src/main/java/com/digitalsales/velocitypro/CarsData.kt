package com.digitalsales.velocitypro
fun carsList(): List<Cars> {
    val carsList = ArrayList<Cars>()
    listOf(
        "BMW", "Ferrari", "Hyundai", "Mahindra",
    ).forEach {
        carsList.add(Cars(it))
    }
    return carsList
}
