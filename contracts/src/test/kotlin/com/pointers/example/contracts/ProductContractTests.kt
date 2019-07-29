package com.pointers.example.contracts

import net.corda.testing.contracts.DummyState
import net.corda.testing.node.ledger
import org.junit.Test

class ProductContractTests: ContractTestsUtil() {
	@Test
	fun `create product transaction must have no inputs`() {
		ledgerServices.ledger {
			transaction {
				input(ProductContract.ID, DummyState())
				command(
						listOf(conan.publicKey),
						ProductContract.Commands.Create()
				)
				output(ProductContract.ID, devilFruit)
				this `fails with` "No inputs should be consumed when creating a product."
			}
			transaction {
				output(ProductContract.ID, devilFruit)
				command(listOf(conan.publicKey), ProductContract.Commands.Create())
				verifies() // As there are no input states.
			}
		}
	}
	
	@Test
	fun `create transaction must have only one output product`() {
		ledgerServices.ledger {
			transaction {
				command(
						listOf(conan.publicKey),
						ProductContract.Commands.Create()
				)
				output(ProductContract.ID, devilFruit) // Two outputs fails.
				output(ProductContract.ID, devilFruit)
				this `fails with` "Only one output state should be created."
			}
			transaction {
				command(listOf(conan.publicKey), ProductContract.Commands.Create())
				output(ProductContract.ID, devilFruit) // One output passes.
				verifies()
			}
		}
	}
	
	@Test
	fun `create transaction must have non empty product name, company name and non-negative price value`() {
		ledgerServices.ledger {
			transaction {
				command(
						listOf(conan.publicKey),
						ProductContract.Commands.Create()
				)
				output(ProductContract.ID, productWithNoName)
				this `fails with` "Product's name must be non-empty"
			}
			transaction {
				command(
						listOf(conan.publicKey),
						ProductContract.Commands.Create()
				)
				output(ProductContract.ID, productWithNoCompany)
				this `fails with` "Product's company must be non-empty"
			}
			transaction {
				command(
						listOf(conan.publicKey),
						ProductContract.Commands.Create()
				)
				output(ProductContract.ID, productWithNegValue)
				this `fails with` "Product's value must be non-negative."
			}
			transaction {
				command(listOf(conan.publicKey), ProductContract.Commands.Create())
				output(ProductContract.ID, devilFruit) // One output passes.
				verifies()
			}
		}
	}
	
	@Test
	fun `Update product transaction must have one input`() {
		ledgerServices.ledger {
			transaction {
				input(ProductContract.ID, devilFruit)
				output(ProductContract.ID, devilFruitUpdated)
				command(listOf(conan.publicKey), ProductContract.Commands.UpdatePrice())
				verifies()
			}
		}
	}
	
	@Test
	fun `Update product price transaction with different name, company and negative price`() {
		ledgerServices.ledger {
			transaction {
				input(ProductContract.ID, devilFruit)
				output(ProductContract.ID, devilFruitUpdatedWithDiffName)
				command(listOf(conan.publicKey), ProductContract.Commands.UpdatePrice())
				this `fails with` "Product's name must be the same in input and output."
			}
			transaction {
				input(ProductContract.ID, devilFruit)
				output(ProductContract.ID, devilFruitUpdatedWithDiffCompany)
				command(listOf(conan.publicKey), ProductContract.Commands.UpdatePrice())
				this `fails with` "Product's company must be the same in input and output."
			}
			transaction {
				input(ProductContract.ID, devilFruit)
				output(ProductContract.ID, devilFruitUpdatedWithNegValue)
				command(listOf(conan.publicKey), ProductContract.Commands.UpdatePrice())
				this `fails with` "Product's value must be non-negative."
			}
			transaction {
				input(ProductContract.ID, devilFruit)
				output(ProductContract.ID, devilFruitUpdatedWithSamePrice)
				command(listOf(conan.publicKey), ProductContract.Commands.UpdatePrice())
				this `fails with` "Product's price in the input and output shouldn't be the same."
			}
			transaction {
				input(ProductContract.ID, devilFruit)
				output(ProductContract.ID, devilFruitUpdated)
				command(listOf(conan.publicKey), ProductContract.Commands.UpdatePrice())
				verifies()
			}
		}
	}
}