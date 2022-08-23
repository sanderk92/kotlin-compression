package compression

import java.nio.file.Files
import java.nio.file.Path

sealed interface CompressionResult
class Success(val path: Path) : CompressionResult
class Failure(val message: String) : CompressionResult

fun <T> Result<T>.process(outputPath: Path): CompressionResult =
    if (this.isFailure) {
        deleteAll(outputPath)
        Failure(this.exceptionOrNull()?.message ?: "Unexpected failure")
    } else {
        Success(outputPath)
    }

private fun deleteAll(path: Path) = runCatching {
    Files.walk(path)
        .sorted(Comparator.reverseOrder())
        .map(Files::delete)
}