package com.car.flows.databaseServices

import co.paralleluniverse.fibers.Suspendable
import com.car.base.CarModel
import com.car.schema.CarSchemaV1
import com.car.states.CarState
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.*
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class CarQuery (private val vinCon: String) : FlowLogic<List<CarModel>>() {

    companion object {
        object AWAITING_REQUEST : ProgressTracker.Step("Awaiting DB Request")
        object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")

        fun tracker() = ProgressTracker(
                AWAITING_REQUEST
        )
    }

    override val progressTracker = tracker()
    @Suspendable
    override fun call(): List<CarModel> {
        progressTracker.currentStep = AWAITING_REQUEST
        val generalCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL)
        val index = builder{ CarSchemaV1.PersistentCar::vin.equal(vinCon) }
        val customQueryCriteria = QueryCriteria.VaultCustomQueryCriteria(index)
        val criteria = generalCriteria.and(customQueryCriteria)
        val result = serviceHub.vaultService.queryBy<CarState>(criteria)
        return result.states.map {
            val car = it.state.data
            CarModel(car.owningBank.toString(),car.holdingDealer.toString(),car.manufacturer.toString(),car.vin,
                    car.licensePlateNumber,car.make,car.model,car.dealershipLocation,car.linearId.toString())
        }
    }
}
