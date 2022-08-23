package compression

import java.io.InputStream
import java.nio.file.Path

sealed interface CompressionResult
class Success(val path: Path) : CompressionResult
class Failure(val message: String) : CompressionResult

interface ArchiverCompressor {

    /**
     * Returns the extension this archiver compressor supports
     */
    fun supportedExtension(): String

    /**
     * Archives and compresses the file or folder at the specified [Path].
     *
     * @param sourcePath to the file or folder to archive and compress
     * @param outputFilePath to store the archived and compressed file at (including filename, excluding extension)
     * @return the [Path] to the archived and compressed file
     */
    fun compress(sourcePath: Path, outputFilePath: Path): CompressionResult

    /**
     * Unarchives and decompresses the file at the specified [Path]
     *
     * @param filePath to the file to unarchive and decompress
     * @param outputFolder to store the unarchived and decompressed files at
     * @return the [Path] to the unarchive/decompress file/folder
     */
    fun decompress(filePath: Path, outputFolder: Path): CompressionResult
}
