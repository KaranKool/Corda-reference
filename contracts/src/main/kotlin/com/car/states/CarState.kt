package com.car.states

import com.car.contracts.CarContract
import com.car.schema.CarSchemaV1
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

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
                    override val linearId: UniqueIdentifier = UniqueIdentifier()): LinearState, QueryableState {

    override val participants: List<AbstractParty> = listOf (owningBank, holdingDealer, manufacturer)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is CarSchemaV1 -> CarSchemaV1.PersistentCar(
                    this.owningBank.name.toString(),
                    this.holdingDealer.name.toString(),
                    this.manufacturer.name.toString(),
                    this.vin,
                    this.licensePlateNumber,
                    this.make,
                    this.model,
                    this.dealershipLocation,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(CarSchemaV1)

}