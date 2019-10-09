package com.car.webserver

import com.car.base.CarModel
import com.car.flows.CarIssueInitiator
import com.car.flows.databaseServices.CarQuery
import com.car.states.CarState
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.FlowHandle
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Define your API endpoints here.
 */
val SERVICE_NAMES = listOf("Notary", "Network Map Service")

@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class Controller(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val myLegalName = rpc.proxy.nodeInfo().legalIdentities.first().name
    private val proxy = rpc.proxy

    /**
     * Returns the node's name.
     */
    @GetMapping(value = [ "me" ], produces = ["application/json"])
    fun whoami() = mapOf("me" to myLegalName)

    /**
     * Returns all parties registered with the network map service. These names can be used to look up identities using
     * the identity service.
     */
    @GetMapping(value = [ "peers" ], produces = ["application/json"])
    fun getPeers(): Map<String, List<CordaX500Name>> {
        logger.info("Accessing peer api")
        val nodeInfo = proxy.networkMapSnapshot()
        return mapOf("peers" to nodeInfo
                .map { it.legalIdentities.first().name }
                //filter out myself, notary and eventual network map started by driver
                .filter { it.organisation !in (SERVICE_NAMES + myLegalName.organisation) })
    }

    @GetMapping(value = [ "cars" ], produces = ["application/json"])
    fun getCars() : ResponseEntity<List<CarModel>> {
        logger.info("Accessing car query")
        val queryCriteria = QueryCriteria.VaultQueryCriteria()
        val carState = proxy.vaultQueryBy<CarState>(queryCriteria).states.map { it.state.data }
        if(carState.isEmpty()){
            return ResponseEntity.status(HttpStatus.OK).body(emptyList())
        }
        val result = carState.map { CarModel(it.owningBank.name.toString(), it.holdingDealer.name.toString(),
                it.manufacturer.name.toString(), it.vin, it.licensePlateNumber, it.make, it.model, it.dealershipLocation
                , it.linearId.toString())}
        return ResponseEntity.status(HttpStatus.OK)
                .body(result)

    }

    @PostMapping(value = ["/issue"], produces = ["application/json"])
    private fun carIssue(req: RequestEntity<CarModel>): ResponseEntity<String> {
        logger.info("Accessing create car issue")
        if(myLegalName.organisationUnit != "Manufacturer"){
            return ResponseEntity.badRequest().body("new car can only be issued by Manufacturer\n")
        }
        val vin = req.body?.vin ?: return ResponseEntity.badRequest().body("Query parameter 'vin' must not be null.\n")
        val licensePlateNumber = req.body?.licensePlateNumber ?: return ResponseEntity.badRequest()
                .body("Query parameter 'licensePlateNumber' must not be null.\n")
        val make = req.body?.make ?: return ResponseEntity.badRequest().body("Query parameter 'make' must not be null.\n")
        val model = req.body?.model ?: return ResponseEntity.badRequest().body("Query parameter 'model' must not be null.\n")
        val dealershipLocation = req.body?.dealershipLocation ?: return ResponseEntity.badRequest()
                .body("Query parameter 'dealershipLocation' must not be null.\n")
        return try {
            val signedTx = proxy.startTrackedFlow(::CarIssueInitiator, vin, licensePlateNumber, make, model,
                    dealershipLocation).returnValue.getOrThrow()
            logger.info("Transaction id ${signedTx.id} committed to ledger.\n")
            ResponseEntity.status(HttpStatus.CREATED).body("Transaction id ${signedTx.id} committed to ledger.\n")

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }

    @GetMapping(value = ["/query"], produces = ["application/json"])
    private fun carQuery(@RequestParam vinCon:String?): ResponseEntity<List<CarModel>> {
        logger.info("Accessing create car issue")
        val flowHandle: FlowHandle<List<CarModel>> = proxy.startFlowDynamic(
                CarQuery::class.java,
                vinCon)
        val result = flowHandle.use { flowHandle.returnValue.getOrThrow() }
        return ResponseEntity.status(HttpStatus.OK)
                    .body(result)
    }
}