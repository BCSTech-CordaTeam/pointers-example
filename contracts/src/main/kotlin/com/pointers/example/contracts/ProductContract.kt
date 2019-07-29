package com.pointers.example.contracts

import com.pointers.example.states.Product
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey

// ************
// * Contract *
// ************
class ProductContract : Contract {
    companion object {
        @JvmStatic
        // Used to identify our contract when building a transaction.
        val ID = "com.pointers.example.contracts.ProductContract"
        
    }
    
    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Create : TypeOnlyCommandData(), Commands
        class UpdatePrice : TypeOnlyCommandData(), Commands
    }
    
    override fun verify(tx: LedgerTransaction) {
         tx.commandsOfType<Commands>().map {
            val setOfSigners = it.signers.toSet()
            when(it.value) {
                is Commands.Create -> verifyCreate(tx, setOfSigners)
                is Commands.UpdatePrice -> verifyUpdatePrice(tx, setOfSigners)
                else -> throw IllegalArgumentException("Unrecognised command.")
            }
        }
    }
    private fun verifyCreate(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        "No inputs should be consumed when creating a product." using (tx.inputs.isEmpty())
        "Only one output state should be created." using (tx.outputs.size == 1)
        val output = tx.outputsOfType<Product>().single()
        "All of the participants must be signers." using (signers.containsAll(output.participants.map { it.owningKey }))
    
        // Product Specific constraints.
        "Product's value must be non-negative." using (output.price > 0)
        "Product's name must be non-empty." using (output.name.isNotEmpty())
        "Product's company must be non-empty." using (output.company.isNotEmpty())
    }
    
    private fun verifyUpdatePrice(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        "Only one input should be consumed when updating a product." using (tx.inputs.size == 1)
        "Only one output state should be created." using (tx.outputs.size == 1)
        val input = tx.inputsOfType<Product>().single()
        val output = tx.outputsOfType<Product>().single()
        "All of the participants must be signers." using (signers.containsAll(output.participants.map { it.owningKey }))
        // Product Specific constraints.
        "Product's value must be non-negative." using (output.price > 0)
        "Product's price in the input and output shouldn't be the same." using (input.price != output.price)
        "Product's name must be the same in input and output." using (input.name == output.name)
        "Product's company must be the same in input and output." using (input.company == output.company)
    }
}