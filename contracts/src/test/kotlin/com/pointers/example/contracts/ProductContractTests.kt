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
				output(ProductContract.ID, bike)
				this `fails with` "No inputs should be consumed when creating a product."
			}
			transaction {
				output(ProductContract.ID, bike)
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
				output(ProductContract.ID, bike) // Two outputs fails.
				output(ProductContract.ID, bike)
				this `fails with` "Only one output state should be created."
			}
			transaction {
				command(listOf(conan.publicKey), ProductContract.Commands.Create())
				output(ProductContract.ID, bike) // One output passes.
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
				output(ProductContract.ID, bike) // One output passes.
				verifies()
			}
		}
	}
	
	@Test
	fun `Update product price transaction must have one input`() {
		ledgerServices.ledger {
			transaction {
				input(ProductContract.ID, bike)
				input(ProductContract.ID, bike)
				output(ProductContract.ID, bikeUpdated)
				command(listOf(conan.publicKey), ProductContract.Commands.UpdatePrice())
				this `fails with` "Only one input should be consumed when updating product price."
			}
		}
		ledgerServices.ledger {
			transaction {
				input(ProductContract.ID, bike)
				output(ProductContract.ID, bikeUpdated)
				command(listOf(conan.publicKey), ProductContract.Commands.UpdatePrice())
				verifies()
			}
		}
	}
	
	@Test
	fun `Update product price transaction with different name, company, negative and same price`() {
		ledgerServices.ledger {
			transaction {
				input(ProductContract.ID, bike)
				output(ProductContract.ID, bikeUpdatedWithDiffName)
				command(listOf(conan.publicKey), ProductContract.Commands.UpdatePrice())
				this `fails with` "Product's name must be the same in input and output."
			}
			transaction {
				input(ProductContract.ID, bike)
				output(ProductContract.ID, bikeUpdatedWithDiffCompany)
				command(listOf(conan.publicKey), ProductContract.Commands.UpdatePrice())
				this `fails with` "Product's company must be the same in input and output."
			}
			transaction {
				input(ProductContract.ID, bike)
				output(ProductContract.ID, bikeUpdatedWithNegValue)
				command(listOf(conan.publicKey), ProductContract.Commands.UpdatePrice())
				this `fails with` "Product's value must be non-negative."
			}
			transaction {
				input(ProductContract.ID, bike)
				output(ProductContract.ID, bikeUpdatedWithSamePrice)
				command(listOf(conan.publicKey), ProductContract.Commands.UpdatePrice())
				this `fails with` "Product's price in the input and output shouldn't be the same."
			}
			transaction {
				input(ProductContract.ID, bike)
				output(ProductContract.ID, bikeUpdated)
				command(listOf(conan.publicKey), ProductContract.Commands.UpdatePrice())
				verifies()
			}
		}
	}
}