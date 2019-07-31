<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Sample to demonstrate Static Pointers - Kotlin

The static pointer example is built using the [cordapp-template](https://github.com/corda/cordapp-template-kotlin)

# Pre-Requisites

See https://docs.corda.net/getting-set-up.html.

# Usage

## Running tests inside IntelliJ

We recommend editing your IntelliJ preferences so that you use the Gradle runner - this means that the quasar utils
plugin will make sure that some flags (like ``-javaagent`` - see below) are
set for you.

To switch to using the Gradle runner:

* Navigate to ``Build, Execution, Deployment -> Build Tools -> Gradle -> Runner`` (or search for `runner`)
  * Windows: this is in "Settings"
  * MacOS: this is in "Preferences"
* Set "Delegate IDE build/run actions to gradle" to true
* Set "Run test using:" to "Gradle Test Runner"

If you would prefer to use the built in IntelliJ JUnit test runner, you can run ``gradlew installQuasar`` which will
copy your quasar JAR file to the lib directory. You will then need to specify ``-javaagent:lib/quasar.jar``
and set the run directory to the project root directory for each test.

## State Pointers
A StatePointer contains a pointer to a ContractState. The StatePointer can be included in a ContractState as a property, or included in an off-ledger data structure. StatePointers can be resolved to a StateAndRef by performing a look-up. 
There are two types of pointers; linear and static.The focus of this example is to demonstrate static pointers. StaticPointer s are for use with any type of ContractState. The StaticPointer does as it suggests, it always points to the same ContractState.
As the pointers are associated with states, this example contains only states and unit test cases to try and understand the usage of the static pointer.

There are two states in this example: Product and Order. Product State is then added as a static pointer to the order state.