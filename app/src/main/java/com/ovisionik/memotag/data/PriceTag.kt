package com.ovisionik.memotag.data

import java.math.BigDecimal
import java.time.LocalDate

data class PriceTag(
    var id: Int = -1,
    var note: String = "",
    var label: String = "",
    var price: BigDecimal = BigDecimal("0.0"),
    var createdOn: LocalDate = LocalDate.now(),
)