package compression

import java.nio.file.Path

fun CompressionResult.getOrFail(): Path = when (this) {
    is Success -> path
    is InputError -> throw AssertionError("Expected a success result, but was a InputError. Cause: $message")
    is FileSystemError -> throw AssertionError("Expected a success result, but was a FileSystemError. Cause: $message")
}