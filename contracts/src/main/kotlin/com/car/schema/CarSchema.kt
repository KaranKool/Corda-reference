package com.car.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.serialization.CordaSerializable
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

/**
 * The family of schemas for IOUState.
 */
object CarSchema

/**
 * An IOUState schema.
 */
@CordaSerializable
object CarSchemaV1 : MappedSchema(
        schemaFamily = CarSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentCar::class.java)) {

    @Entity
    @Table(name = "car_states")
    class PersistentCar(
            @Column(name = "owningBank")
            var bankName: String,

            @Column(name = "holdingDealer")
            var dealerName: String,

            @Column(name = "manufacturer")
            var manufacturerName: String,

            @Column(name = "vin")
            var vin: String,

            @Column(name = "licensePlateNumber")
            var licensePlateNumber: String,

            @Column(name = "make")
            var make: String,

            @Column(name = "model")
            var model: String,

            @Column(name = "dealershipLocation")
            var dealershipLocation: String,

            @Column(name = "linear_id")
            var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", "", "", "","",""
                ,"","", UUID.randomUUID())
    }
}