package com.pointers.example.states

import com.pointers.example.contracts.ProductContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

@BelongsToContract(ProductContract::class)
data class Product(
		val name: String,
		val company: String,
		val price: Double,
		val owner: Party,
		override val linearId: UniqueIdentifier = UniqueIdentifier(),
		override val participants: List<AbstractParty> = listOf(owner)
) : LinearState