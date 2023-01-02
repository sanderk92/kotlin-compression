package compression

import java.nio.file.Files
import java.nio.file.Path

sealed interface CompressionResult
class Success(val path: Path) : CompressionResult
class InputError(val message: String): CompressionResult
class FileSystemError(val exception: Throwable) : CompressionResult

fun compressionResult(path: Path, compressionFn: () -> Unit): CompressionResult =
    runCatching { compressionFn() }
        .map { Success(path) }
        .onFailure { deleteAll(path) }
        .getOrElse(::FileSystemError)

private fun deleteAll(path: Path) = runCatching {
    Files.walk(path)
        .sorted(Comparator.reverseOrder())
        .map(Files::delete)
}