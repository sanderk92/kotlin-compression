package compression

import java.nio.file.Files
import java.nio.file.Path

sealed interface CompressionResult
class Success(val path: Path) : CompressionResult
class InputError(val message: String): CompressionResult
class FileSystemError(val message: String) : CompressionResult

fun compressionResult(path: Path, compressionFn: () -> Unit): CompressionResult =
    try {
        compressionFn()
        Success(path)
    } catch (e: Exception) {
        deleteAll(path)
        FileSystemError(e.message ?: "Unexpected failure")
    }

private fun deleteAll(path: Path) = runCatching {
    Files.walk(path)
        .sorted(Comparator.reverseOrder())
        .map(Files::delete)
}