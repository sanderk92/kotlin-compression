package compression

import java.nio.file.Path

fun CompressionResult.getOrFail(): Path = when (this) {
    is Failure -> throw AssertionError("Expected a success result, but was a failure")
    is Success -> path
}