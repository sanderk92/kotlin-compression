package compression

import java.nio.file.Path

fun CompressionResult.getOrFail(): Path = when (this) {
    is Failure -> throw AssertionError("Expected a success result, but was a failure. Cause: $message")
    is Success -> path
}