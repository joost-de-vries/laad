# load testing using kotlin coroutines
This is an experiment in load testing without a specialised load framework like gatling or jmeter.  
Starting from the idea that with Kotlin coroutines concurrency is easy enough that you don't really need a framework.
To see whether that's true let's see what's achievable in a few hours work.

The goal is to be able to increase the number of concurrent 'users' gradually, hold steady for a while, do a brief burst and then cool down.

To try it out first start [wiremock](src/main/kotlin/laad/RunWiremock.kt)  
Then start an [example scenario](src/main/kotlin/laad/Example.kt)

Each 'user's actions is represented by a scenario
```kotlin
interface Scenario {
    suspend fun runSession()
}
```
A kotlin actor is started that can be sent two messages: 'scale to $number concurrent users' and 'stop'.  
It launches scenario coroutines to get to the desired amount of concurrently running scenario's. Starting new ones when running ones finish. Or to scale up to a higher load. And stopping scenario's if a lower number is required.

```kotlin
private fun main() = runBlocking {

    val scenarioRunner = runScenario(ExampleScenario(loggingEventProcessor()), tick = 1.s)

    // every second increase by 1
    for (i in 1 .. 10) {
        scenarioRunner.goTo(i)
        delay(1.s)
    }
    // hold steady for 5 seconds
    delay(5.s)
    // steep increase for 3 seconds
    scenarioRunner.goTo(20)
    delay(3.s)

    // back to steady for 3 seconds
    scenarioRunner.goTo(10)
    delay(3.s)

    scenarioRunner.stop()
}
```

[WebClientScenario](src/main/kotlin/laad/WebClientScenario.kt) is an example implementation using Spring WebClient. But any other http client would be possible.  
The implementation will have to map the responses to [load test events](src/main/kotlin/laad/Event.kt): did the call succeed or not and how much time did it take.

Another kotlin actor receives these events. To aggregate the events and allow creation of reports. Here we only log the incoming events.

Concluding:  
- coroutines _do_ make concurrency easy enough to implement a load runner in a days work  
- coroutines are a very readable way to express the 'first this, then that' intention of load testing.  
- actors are good tool to update mutable state concurrently. It just worked.  
- kotlin actors are a bit clunky. Currently they don't support using classes. Which means that logic quickly becomes messy. There's a long standing [issue](https://github.com/Kotlin/kotlinx.coroutines/issues/87) to fix that.
- this is not a DSL. It's just kotlin coroutines. Which makes for 1. easier debugging 2. clear runtime semantics 3. easy extendability for new uses. None of which is true for a confining DSL like gatling f.i.  

Follow up:  
See [ReportExample](src/main/kotlin/laad/ReportExample.kt) for an extended example that uses Gatling to create a report. 