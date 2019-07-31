package com.pointers.example.contracts

import com.pointers.example.states.Product
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.makeTestIdentityService

abstract class ContractTestsUtil {
	protected val ledgerServices = MockServices(
			listOf("com.pointers.example.contracts"),
			identityService = makeTestIdentityService(),
			initialIdentity = TestIdentity(CordaX500Name("TestIdentity", "Sydney", "AU"))
	)
	protected val conan = TestIdentity(CordaX500Name("Conan", "Sydney", "AU"))
	protected val junko = TestIdentity(CordaX500Name("Junko", "Sydney", "AU"))
	
	protected val bike = Product(name ="Hummer", company = "GM", price = 1.0, owner = conan.party)
	protected val bikeUpdated = Product(name ="Hummer", company = "GM", price = 2.0, owner = conan.party)
	protected val productWithNoName = Product(name = "", company = "GM", price = 2.0, owner = conan.party)
	protected val productWithNoCompany = Product(name ="Hummer", company = "", price = 3.0, owner = conan.party)
	protected val productWithNegValue = Product(name ="Hummer", company = "GM", price = -1.0, owner = conan.party)
	protected val bikeUpdatedWithDiffName = Product(name = "", company = "GM", price = 2.0, owner = conan.party)
	protected val bikeUpdatedWithDiffCompany = Product(name ="Hummer", company = "", price = 3.0, owner = conan.party)
	protected val bikeUpdatedWithNegValue = Product(name ="Hummer", company = "GM", price = -1.0, owner = conan.party)
	protected val bikeUpdatedWithSamePrice = Product(name ="Hummer", company = "GM", price = 1.0, owner = conan.party)
}