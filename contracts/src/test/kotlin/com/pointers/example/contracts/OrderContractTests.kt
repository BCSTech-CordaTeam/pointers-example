package com.pointers.example.contracts

import com.pointers.example.states.Order
import com.pointers.example.states.Product
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StaticPointer
import net.corda.core.contracts.TransactionResolutionException
import net.corda.core.node.NotaryInfo
import net.corda.core.node.ServiceHub
import net.corda.core.serialization.internal.SerializationEnvironment
import net.corda.core.serialization.internal.nodeSerializationEnv
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.node.serialization.amqp.AMQPServerSerializationScheme
import net.corda.node.serialization.kryo.KRYO_CHECKPOINT_CONTEXT
import net.corda.node.serialization.kryo.KryoCheckpointSerializer
import net.corda.nodeapi.internal.persistence.CordaPersistence
import net.corda.serialization.internal.AMQP_P2P_CONTEXT
import net.corda.serialization.internal.AMQP_STORAGE_CONTEXT
import net.corda.serialization.internal.SerializationFactoryImpl
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.DUMMY_NOTARY_NAME
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import net.corda.testing.node.makeTestIdentityService
import org.junit.Test
import kotlin.test.assertEquals

class OrderContractTests : ContractTestsUtil() {
	private val dummyNotary: TestIdentity = TestIdentity(DUMMY_NOTARY_NAME, 20)
	private val networkParameters = testNetworkParameters(notaries = listOf(NotaryInfo(dummyNotary.party, true)))
	private val allIdentities = arrayOf(conan.identity, junko.identity, dummyNotary.identity)
	
	private lateinit var conanDatabase: CordaPersistence
	lateinit var conanLedgerServices: ServiceHub
	
	private lateinit var junkoDatabase: CordaPersistence
	private lateinit var junkoLedgerServices: ServiceHub
	
	private val notaryServices = MockServices(
			listOf("com.pointers.example.contracts"),
			dummyNotary
	)
	
	init {
		initSerialization(this)
		makeDatabaseAndLedgerServices()
	}
	
	private fun makeDatabaseAndLedgerServices() {
		val conanDatabaseAndLedgerServices = MockServices.Companion.makeTestDatabaseAndMockServices(
				listOf("com.pointers.example.contracts"),
				identityService = makeTestIdentityService(*allIdentities),
				initialIdentity = conan,
				networkParameters = networkParameters
		)
		conanDatabase = conanDatabaseAndLedgerServices.first
		conanLedgerServices = conanDatabaseAndLedgerServices.second
		
		val junkoDatabaseAndLedgerServices = MockServices.Companion.makeTestDatabaseAndMockServices(
				listOf("com.pointers.example.contracts"),
				makeTestIdentityService(*allIdentities),
				junko,
				networkParameters
		)
		junkoDatabase = junkoDatabaseAndLedgerServices.first
		junkoLedgerServices = junkoDatabaseAndLedgerServices.second
		
	}
	
	fun initSerialization(lockObj: Any) {
		// This sync block is from
		// https://github.com/corda/corda/blob/release/4.0/tools/network-bootstrapper/src/main/kotlin/net/corda/bootstrapper/serialization/SerializationHelper.kt
		// Corda is under Apache 2.0
		// The block is necessary or de/serialization doesn't work
		synchronized(lockObj) {
			if (nodeSerializationEnv == null) {
				val classloader = lockObj::class.java.classLoader
				nodeSerializationEnv = SerializationEnvironment.with(
						SerializationFactoryImpl().apply {
							registerScheme(AMQPServerSerializationScheme(emptyList()))
						},
						p2pContext = AMQP_P2P_CONTEXT.withClassLoader(classloader),
						rpcServerContext = AMQP_P2P_CONTEXT.withClassLoader(classloader),
						storageContext = AMQP_STORAGE_CONTEXT.withClassLoader(classloader),
						
						checkpointContext = KRYO_CHECKPOINT_CONTEXT.withClassLoader(classloader),
						checkpointSerializer = KryoCheckpointSerializer
				)
			}
		}
	}
	
	private fun resolveTransactionFromCanonVault(productPointer: StaticPointer<Product>): SignedTransaction? {
		val txId = productPointer.resolve(conanLedgerServices).referenced().stateAndRef.ref.txhash
		return conanLedgerServices.validatedTransactions.getTransaction(txId)
	}
	
	@Test
	fun `create order`() {
		ledgerServices.ledger {
			val tx = transaction {
				output(ProductContract.ID, bike)
				command(listOf(conan.publicKey), ProductContract.Commands.Create())
				verifies() // As there are no input states.
			}
			
			//Canon creates the order with junko
			val productStateAndRef: StateAndRef<Product> = tx.outRef(0)
			val productStaticPointer = StaticPointer(productStateAndRef.ref, bike.javaClass)
			val order = Order(junko.party, conan.party, productStaticPointer)
			transaction {
				output(OrderContract.ID, order)
				command(listOf(junko.publicKey, conan.publicKey), OrderContract.Commands.Create())
				verifies() // As there are no input states.
			}
		}
	}
	
	@Test
	fun `create order and resolve product pointer in canon vault`() {
		// Conan creates the devil fruit product.
		val createProductTx = conanDatabase.transaction {
			val builder = TransactionBuilder(dummyNotary.party)
			builder.addOutputState(bike)
			builder.addCommand(ProductContract.Commands.Create(), conan.publicKey)
			val ptx = conanLedgerServices.signInitialTransaction(builder)
			val stx = notaryServices.addSignature(ptx)
			conanLedgerServices.recordTransactions(listOf(stx))
			stx
		}
		
		//Canon creates the order with junko
		val productStateAndRef: StateAndRef<Product> = createProductTx.tx.outRef(0)
		val productStaticPointer = StaticPointer(productStateAndRef.ref, bike.javaClass)
		val order = Order(junko.party, conan.party, productStaticPointer)
		val createOrderTx = conanDatabase.transaction {
			val builder = TransactionBuilder(dummyNotary.party)
			builder.addOutputState(order)
			builder.addCommand(OrderContract.Commands.Create(), listOf(conan.publicKey, junko.publicKey))
			val ptx = conanLedgerServices.signInitialTransaction(builder)
			val ptx2 = junkoLedgerServices.addSignature(ptx)
			val stx = notaryServices.addSignature(ptx2)
			conanLedgerServices.recordTransactions(listOf(stx))
			stx
		}
		
		junkoDatabase.transaction {
			createOrderTx.toLedgerTransaction(junkoLedgerServices).verify()
			junkoLedgerServices.recordTransactions(listOf(createOrderTx))
		}
		
		//Check the product state at Canon
		val createdOrder: Order = createOrderTx.tx.outputsOfType(Order::class.java).single()
		val productInOrderAtConanVault = createdOrder.productPointer.resolve(conanLedgerServices).state.data
		assertEquals(productInOrderAtConanVault.price, 1.0)
	}
	
	@Test(expected = TransactionResolutionException::class)
	//This will fail as the product state is not transferred to Junko's vault when the order state is created
	fun `create order and resolve product pointer in junko vault fails`() {
		// Conan creates the devil fruit product.
		val createProductTx = conanDatabase.transaction {
			val builder = TransactionBuilder(dummyNotary.party)
			builder.addOutputState(bike)
			builder.addCommand(ProductContract.Commands.Create(), conan.publicKey)
			val ptx = conanLedgerServices.signInitialTransaction(builder)
			val stx = notaryServices.addSignature(ptx)
			conanLedgerServices.recordTransactions(listOf(stx))
			stx
		}
		
		//Canon creates the order with junko
		val productStateAndRef: StateAndRef<Product> = createProductTx.tx.outRef(0)
		val productStaticPointer = StaticPointer(productStateAndRef.ref, bike.javaClass)
		val order = Order(junko.party, conan.party, productStaticPointer)
		val createOrderTx = conanDatabase.transaction {
			val builder = TransactionBuilder(dummyNotary.party)
			builder.addOutputState(order)
			builder.addCommand(OrderContract.Commands.Create(), listOf(conan.publicKey, junko.publicKey))
			val ptx = conanLedgerServices.signInitialTransaction(builder)
			val ptx2 = junkoLedgerServices.addSignature(ptx)
			val stx = notaryServices.addSignature(ptx2)
			conanLedgerServices.recordTransactions(listOf(stx))
			stx
		}
		
		junkoDatabase.transaction {
			createOrderTx.toLedgerTransaction(junkoLedgerServices).verify()
			junkoLedgerServices.recordTransactions(listOf(createOrderTx))
		}
		
		//Check the product state at Canon
		val createdOrder: Order = createOrderTx.tx.outputsOfType(Order::class.java).single()
		createdOrder.productPointer.resolve(junkoLedgerServices).state.data
	}
	
	@Test
	//This will fail as the product state is not transferred to Junko's vault when the order state is created
	fun `create order and resolve product pointer in junko vault`() {
		// Conan creates the devil fruit product.
		val createProductTx = conanDatabase.transaction {
			val builder = TransactionBuilder(dummyNotary.party)
			builder.addOutputState(bike)
			builder.addCommand(ProductContract.Commands.Create(), conan.publicKey)
			val ptx = conanLedgerServices.signInitialTransaction(builder)
			val stx = notaryServices.addSignature(ptx)
			conanLedgerServices.recordTransactions(listOf(stx))
			stx
		}
		
		//Canon creates the order with junko
		val productStateAndRef: StateAndRef<Product> = createProductTx.tx.outRef(0)
		val productStaticPointer = StaticPointer(productStateAndRef.ref, bike.javaClass)
		val order = Order(junko.party, conan.party, productStaticPointer)
		val createOrderTx = conanDatabase.transaction {
			val builder = TransactionBuilder(dummyNotary.party)
			builder.addOutputState(order)
			builder.addCommand(OrderContract.Commands.Create(), listOf(conan.publicKey, junko.publicKey))
			val ptx = conanLedgerServices.signInitialTransaction(builder)
			val ptx2 = junkoLedgerServices.addSignature(ptx)
			val stx = notaryServices.addSignature(ptx2)
			conanLedgerServices.recordTransactions(listOf(stx))
			stx
		}
		
		junkoDatabase.transaction {
			createOrderTx.toLedgerTransaction(junkoLedgerServices).verify()
			junkoLedgerServices.recordTransactions(listOf(createOrderTx))
		}
		
		//Request Conan for the product state by passing product pointer
		val orderStateInJunkoVault: Order = junkoLedgerServices.vaultService.queryBy(Order::class.java).states.single().state.data
		val productTransaction: SignedTransaction? = resolveTransactionFromCanonVault(orderStateInJunkoVault.productPointer)
		junkoLedgerServices.recordTransactions(listOf(productTransaction!!))
		
		val product = orderStateInJunkoVault.productPointer.resolve(junkoLedgerServices).state.data
		assertEquals(1.0, product.price)
	}
	
	
	@Test
	fun `create order, update product and verify the price order in conan vault`() {
		
		// Conan creates the devil fruit product.
		val createProductTx = conanDatabase.transaction {
			val builder = TransactionBuilder(dummyNotary.party)
			builder.addOutputState(bike)
			builder.addCommand(ProductContract.Commands.Create(), conan.publicKey)
			val ptx = conanLedgerServices.signInitialTransaction(builder)
			val stx = notaryServices.addSignature(ptx)
			conanLedgerServices.recordTransactions(listOf(stx))
			stx
		}
		
		//Canon creates the order with junko
		val productStateAndRef: StateAndRef<Product> = createProductTx.tx.outRef(0)
		val productStaticPointer = StaticPointer(productStateAndRef.ref, bike.javaClass)
		val order = Order(junko.party, conan.party, productStaticPointer)
		val createOrderTx = conanDatabase.transaction {
			val builder = TransactionBuilder(dummyNotary.party)
			builder.addOutputState(order)
			builder.addCommand(OrderContract.Commands.Create(), listOf(conan.publicKey, junko.publicKey))
			val ptx = conanLedgerServices.signInitialTransaction(builder)
			val ptx2 = junkoLedgerServices.addSignature(ptx)
			val stx = notaryServices.addSignature(ptx2)
			conanLedgerServices.recordTransactions(listOf(stx))
			stx
		}
		
		junkoDatabase.transaction {
			createOrderTx.toLedgerTransaction(junkoLedgerServices).verify()
			junkoLedgerServices.recordTransactions(listOf(createOrderTx))
		}
		
		val updateProductTx = conanDatabase.transaction {
			val builder = TransactionBuilder(dummyNotary.party)
			builder.addInputState(createProductTx.tx.outRef<Product>(0))
			builder.addOutputState(bikeUpdated)
			builder.addCommand(ProductContract.Commands.UpdatePrice(), conan.publicKey)
			val ptx = conanLedgerServices.signInitialTransaction(builder)
			val stx = notaryServices.addSignature(ptx)
			conanLedgerServices.recordTransactions(listOf(stx))
			stx
		}
		
		//Check the product state at Canon
		val updatedProduct: Product = updateProductTx.tx.outputsOfType(Product::class.java).single()
		val createdOrder: Order = createOrderTx.tx.outputsOfType(Order::class.java).single()
		val productInOrderAtConanVault = createdOrder.productPointer.resolve(conanLedgerServices).state.data
		assertEquals(productInOrderAtConanVault.price, 1.0)
		assertEquals(updatedProduct.price, 2.0)
		
	}
	
	@Test
	fun `create order, update product and verify the price order in junko vault`() {
		
		// Conan creates the devil fruit product.
		val createProductTx = conanDatabase.transaction {
			val builder = TransactionBuilder(dummyNotary.party)
			builder.addOutputState(bike)
			builder.addCommand(ProductContract.Commands.Create(), conan.publicKey)
			val ptx = conanLedgerServices.signInitialTransaction(builder)
			val stx = notaryServices.addSignature(ptx)
			conanLedgerServices.recordTransactions(listOf(stx))
			stx
		}
		
		//Canon creates the order with junko
		val productStateAndRef: StateAndRef<Product> = createProductTx.tx.outRef(0)
		val productStaticPointer = StaticPointer(productStateAndRef.ref, bike.javaClass)
		val order = Order(junko.party, conan.party, productStaticPointer)
		val createOrderTx = conanDatabase.transaction {
			val builder = TransactionBuilder(dummyNotary.party)
			builder.addOutputState(order)
			builder.addCommand(OrderContract.Commands.Create(), listOf(conan.publicKey, junko.publicKey))
			val ptx = conanLedgerServices.signInitialTransaction(builder)
			val ptx2 = junkoLedgerServices.addSignature(ptx)
			val stx = notaryServices.addSignature(ptx2)
			conanLedgerServices.recordTransactions(listOf(stx))
			stx
		}
		
		junkoDatabase.transaction {
			createOrderTx.toLedgerTransaction(junkoLedgerServices).verify()
			junkoLedgerServices.recordTransactions(listOf(createOrderTx))
		}
		
		val updateProductTx = conanDatabase.transaction {
			val builder = TransactionBuilder(dummyNotary.party)
			builder.addInputState(createProductTx.tx.outRef<Product>(0))
			builder.addOutputState(bikeUpdated)
			builder.addCommand(ProductContract.Commands.UpdatePrice(), conan.publicKey)
			val ptx = conanLedgerServices.signInitialTransaction(builder)
			val stx = notaryServices.addSignature(ptx)
			conanLedgerServices.recordTransactions(listOf(stx))
			stx
		}
		
		//Check the product state at Canon
		val updatedProduct: Product = updateProductTx.tx.outputsOfType(Product::class.java).single()
		val createdOrder: Order = createOrderTx.tx.outputsOfType(Order::class.java).single()
		val productInOrderAtConanVault = createdOrder.productPointer.resolve(conanLedgerServices).state.data
		assertEquals(productInOrderAtConanVault.price, 1.0)
		assertEquals(updatedProduct.price, 2.0)
		
		//Check the product state at Junko
		val orderStateTxInJunko: SignedTransaction? = junkoLedgerServices.validatedTransactions.getTransaction(createOrderTx.id)
		val orderStateInJunko: Order = orderStateTxInJunko!!.coreTransaction.outputsOfType(Order::class.java).single()
		val productTransaction: SignedTransaction? = resolveTransactionFromCanonVault(orderStateInJunko.productPointer)
		junkoLedgerServices.recordTransactions(listOf(productTransaction!!))
		val productInOrderAtJunkoVault = orderStateInJunko.productPointer.resolve(junkoLedgerServices).state.data
		assertEquals(productInOrderAtJunkoVault.price, 1.0)
	}
}