package compression

import java.nio.file.Path

interface ArchiverCompressor {

    /**
     * Returns the extension this archiver compressor supports
     */
    fun supportedExtension(): String

    /**
     * Archives and compresses the file or folder at the specified [Path].
     *
     * - When a folder is compressed, the direct children of the selected folder are added to the archive.
     * - When a single file is compressed, it will simply be the only file in the archive.
     * - In case compression fails for any reason, the resulting file will be deleted (if any).
     *
     * @property sourcePath to the file or folder to archive and compress
     * @property targetFilePath to store the archived and compressed file at (including filename, excluding extension)
     * @return the [Path] to the archived and compressed file
     */
    fun compress(sourcePath: Path, targetFilePath: Path): CompressionResult

    /**
     * Unarchives and decompresses the file at the specified [Path]
     *
     * - All intermediately required directories for the outputFolder will be created in the process.
     * - In case decompression fails for any reason, the resulting files will be deleted (if any).
     *
     * @property sourcePath to the file to unarchive and decompress
     * @property outputDirectory to store the unarchived and decompressed files at
     * @return the [Path] to the unarchive/decompress file/folder
     */
    fun decompress(sourcePath: Path, outputDirectory: Path): CompressionResult
}
