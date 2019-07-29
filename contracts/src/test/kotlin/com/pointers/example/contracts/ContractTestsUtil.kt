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
			initialIdentity = TestIdentity(CordaX500Name("TestIdentity", "", "AU"))
	)
	protected val conan = TestIdentity(CordaX500Name("Conan", "", "AU"))
	protected val junko = TestIdentity(CordaX500Name("Junko", "", "AU"))
	
	protected val devilFruit = Product(name = "Devil Fruit", company = "WW", price = 1.0, owner = conan.party)
	protected val devilFruitUpdated = Product(name = "Devil Fruit", company = "WW", price = 2.0, owner = conan.party)
	protected val productWithNoName = Product(name = "", company = "WW", price = 2.0, owner = conan.party)
	protected val productWithNoCompany = Product(name = "Devil Fruit", company = "", price = 3.0, owner = conan.party)
	protected val productWithNegValue = Product(name = "Devil Fruit", company = "WW", price = -1.0, owner = conan.party)
	protected val devilFruitUpdatedWithDiffName = Product(name = "", company = "WW", price = 2.0, owner = conan.party)
	protected val devilFruitUpdatedWithDiffCompany = Product(name = "Devil Fruit", company = "", price = 3.0, owner = conan.party)
	protected val devilFruitUpdatedWithNegValue = Product(name = "Devil Fruit", company = "WW", price = -1.0, owner = conan.party)
	protected val devilFruitUpdatedWithSamePrice = Product(name = "Devil Fruit", company = "WW", price = 1.0, owner = conan.party)
}