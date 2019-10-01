package com.car.flows

import co.paralleluniverse.fibers.Suspendable
import com.car.contracts.CarContract
import com.car.states.CarState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class CarIssueInitiator(private val vin: String,
                        private val licensePlateNumber: String,
                        private val make: String,
                        private val model: String,
                        private val dealershipLocation: String) : FlowLogic<SignedTransaction>() {
    companion object {
        object GENERATING_TRANSACTION : Step("Generating transaction based on new Car.")
        object VERIFYING_TRANSACTION : Step("Verifying contract constraints.")
        object SIGNING_TRANSACTION : Step("Signing transaction with our private key.")
        object GATHERING_SIGS : Step("Gathering the counterparty's signature.") {
            override fun childProgressTracker() = CollectSignaturesFlow.tracker()
        }

        object FINALISING_TRANSACTION : Step("Obtaining notary signature and recording transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                GATHERING_SIGS,
                FINALISING_TRANSACTION
        )
    }

    override val progressTracker = tracker()

    @Suspendable
    override fun call(): SignedTransaction {
        if(ourIdentity.name.organisationUnit != "Manufacturer"){
            throw IllegalAccessError("Only Manufacturer can Issue New Car")
        }
        val everyone = serviceHub.networkMapCache.allNodes.flatMap { it.legalIdentities }
        val everyoneButMeAndNotary = everyone.filter { serviceHub.networkMapCache.isNotary(it).not() } - ourIdentity
        val everyoneButNotary = everyone.filter { serviceHub.networkMapCache.isNotary(it).not() }

        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        progressTracker.currentStep = GENERATING_TRANSACTION
        val command = Command(CarContract.Commands.Issue(), everyoneButNotary.map { it.owningKey })
        val carState = CarState(everyoneButMeAndNotary.single { it.name.organisationUnit == "Bank" },
                everyoneButMeAndNotary.single { it.name.organisationUnit == "Dealer" }, ourIdentity, vin,
                licensePlateNumber, make, model, dealershipLocation)
        val txBuilder = TransactionBuilder(notary).addOutputState(carState, CarContract.ID).addCommand(command)

        progressTracker.currentStep = VERIFYING_TRANSACTION
        txBuilder.verify(serviceHub)

        progressTracker.currentStep = SIGNING_TRANSACTION
        val tx = serviceHub.signInitialTransaction(txBuilder)

        progressTracker.currentStep = GATHERING_SIGS
        val sessions = everyoneButMeAndNotary.map {initiateFlow(it)}.toSet()
        val stx = subFlow(CollectSignaturesFlow(tx, sessions, GATHERING_SIGS.childProgressTracker()))

        progressTracker.currentStep = FINALISING_TRANSACTION
        return subFlow(FinalityFlow(stx, sessions,FINALISING_TRANSACTION.childProgressTracker()))
    }
}

    @InitiatedBy(CarIssueInitiator::class)
    class CarIssueResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signedTransactionFlow = object: SignTransactionFlow(counterpartySession)
            {
                override fun checkTransaction(stx: SignedTransaction) = requireThat{
                    val output = stx.tx.outputs.single().data
                    "The output must be a CarState" using (output is CarState)
                }
            }
            val txWeJustSignedId = subFlow(signedTransactionFlow)
            return subFlow(ReceiveFinalityFlow(counterpartySession, txWeJustSignedId.id))
        }
    }
