package compression

import java.nio.file.Files
import java.nio.file.Path

sealed interface CompressionResult
class Success(val path: Path) : CompressionResult
class InputError(val message: String): CompressionResult
class FileSystemError(val message: String) : CompressionResult

fun <T> Result<T>.process(outputPath: Path): CompressionResult =
    if (this.isFailure) {
        deleteAll(outputPath)
        FileSystemError(this.exceptionOrNull()?.message ?: "Unexpected failure")
    } else {
        Success(outputPath)
    }

private fun deleteAll(path: Path) = runCatching {
    Files.walk(path)
        .sorted(Comparator.reverseOrder())
        .map(Files::delete)
}