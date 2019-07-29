package com.pointers.example.states

import com.pointers.example.contracts.OrderContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StaticPointer
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

@BelongsToContract(OrderContract::class)
data class Order(
		val consumer: Party,
		val productOwner: Party,
    val productPointer: StaticPointer<Product>,
		override val participants: List<AbstractParty> = listOf(consumer, productOwner)
	) : ContractState