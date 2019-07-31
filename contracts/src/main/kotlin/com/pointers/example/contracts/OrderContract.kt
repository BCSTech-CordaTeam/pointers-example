package com.pointers.example.contracts

import com.pointers.example.states.Order
import com.pointers.example.states.Product
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

// ************
// * Contract *
// ************
class OrderContract : Contract {
    companion object {
        @JvmStatic
        // Used to identify our contract when building a transaction.
        val ID = "com.pointers.example.contracts.OrderContract"
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands.Create>()
        requireThat {
            val output = tx.outputsOfType<Order>().single()
            
            "No input should be consumed when creating an order." using (tx.inputs.isEmpty())
            "Only one output state should be created." using (tx.outputs.size == 1)
            "All of the participants must be signers." using (command.signers.containsAll(output.participants.map { it.owningKey }))
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Create : Commands
    }
}