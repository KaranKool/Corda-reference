package com.car.base

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class CarModel(val owningBank: String?,
                    val holdingDealer: String?,
                    val manufacturer: String?,
                    val vin: String?,
                    val licensePlateNumber: String?,
                    val make: String?,
                    val model: String?,
                    val dealershipLocation: String?,
                    val linearId: String?)