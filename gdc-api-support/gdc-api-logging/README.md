# GoodData API Logging Library

Goal of this library is to support transfer metadata information from application to the target
logging/monitoring utility (like CloudWatch or Loki). In the first place
we make it possible to log structurally. To keep the solution easy and lightweight
we build on KLogger class (from [KotlinLogging](https://github.com/MicroUtils/kotlin-logging)).
So creating and typical usage of the logger stays the same.

## Usage

### Logging
If there is no need to add extra information to log message,
it can use all methods from KLogger API.
For other cases there is a group of extension methods for building log message.

Example of usage:
```kotlin
private val logger = KotlinLogging.logger {}

logger.logWarn {
    withMessage { "conditional text" }
    withAction("test action")
    withKey(LogKey("TEST"), "test message")
    withException(IllegalArgumentException("test exception"))
}

val timedResult = measureCatching { somethingCanThrowException() }

timedResult.onSuccess {
    logger.logInfo {
        withMessage { "OK" }
        withDuration(it.duration)
    }
}.onFailure {
    logger.logError(it.value) {
        withMessage { "ERROR" }
        withDuration(it.duration)
    }
}

fun somethingCanThrowException(): Int = throw ArithmeticException()
```

### Underlying logging system configuration
To enable key-value logging the appropriate layout for logs must be configured, which is able to read data stored by
[LogKey](src/main/kotlin/LogKey.kt) in the log event.

## How it works
We use internal mechanism of `org.slf4j.Logger` for transferring log events. Trick is
composition of log event. For our log events we use special `org.slf4j.Marker` (see `GD_API_MARKER`). All
log details, except the message text, are transferred as pairs of `LogKey` and value added to list of parameters.
According to (un)set Marker the underlying logging system layout can recognize type of log event
and use a proper formatter.
