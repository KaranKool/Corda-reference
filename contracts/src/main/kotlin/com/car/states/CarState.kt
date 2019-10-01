package com.car.states

import com.car.contracts.CarContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

// *********
// * State *
// *********

/*This annotation is required by any [ContractState] which needs to ensure that it is only ever processed as part of a
* [TransactionState] referencing the specified [Contract].*/

@BelongsToContract(CarContract::class)
data class CarState(val owningBank: Party,
                    val holdingDealer: Party,
                    val manufacturer: Party,
                    val vin: String,
                    val licensePlateNumber: String,
                    val make: String,
                    val model: String,
                    val dealershipLocation: String,
                    val linearId: UniqueIdentifier = UniqueIdentifier()): ContractState {
    override val participants: List<AbstractParty> = listOf (owningBank, holdingDealer, manufacturer)
}