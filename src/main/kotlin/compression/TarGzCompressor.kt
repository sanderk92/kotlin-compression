package compression

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name

private const val EXTENSION = ".tar.gz"

class TarGzCompressor : ArchiverCompressor {

    override fun supportedExtension() = EXTENSION

    /**
     * Do not output to the same folder as the input is read from. When a folder is compressed, the direct children of
     * the selected folder are added to the archive. When a single file is compressed, it will simply be the only file
     * in the archive. In case compression fails, all resulted files will be deleted. Any failures during this cleanup
     * will be ignored.
     */
    override fun compress(sourcePath: Path, outputFilePath: Path): CompressionResult {
        val outputZipPath = outputFilePath.resolveSibling(outputFilePath.name + EXTENSION)

        if (Files.notExists(sourcePath)) return Failure("sourcePath does not exist")
        if (Files.exists(outputZipPath)) return Failure("outputFilePath already exists")

        val result = runCatching {
            FileOutputStream(outputZipPath.toFile()).use { fileOutputStream ->
                BufferedOutputStream(fileOutputStream).use { bufferedOutputStream ->
                    GzipCompressorOutputStream(bufferedOutputStream).use { gzipOutputStream ->
                        TarArchiveOutputStream(gzipOutputStream).use { tarOutputStream ->
                            createTarEntries(sourcePath, tarOutputStream)
                        }
                    }
                }
            }
        }
        return result.process(outputZipPath)
    }

    private fun createTarEntries(sourcePath: Path, tarOutputStream: TarArchiveOutputStream) {
        Files.walk(sourcePath).use { fileWalk ->
            fileWalk
                .filter { !Files.isDirectory(it) }
                .forEach { sourcePath -> createTarEntry(tarOutputStream, sourcePath, sourcePath) }
            tarOutputStream.finish()
        }
    }

    private fun createTarEntry(outputStream: TarArchiveOutputStream, file: Path, selectedPath: Path) {
        val filePathInArchive: Path = determinePathInArchive(file, selectedPath)
        val tarEntry = TarArchiveEntry(file, filePathInArchive.toString())
        outputStream.putArchiveEntry(tarEntry)
        Files.copy(file, outputStream)
        outputStream.closeArchiveEntry()
    }

    private fun determinePathInArchive(file: Path, selectedPath: Path): Path =
        if (file == selectedPath) {
            // A single file has been selected to be compressed
            file.fileName
        } else {
            // A folder has been selected to be compressed
            selectedPath.relativize(file)
        }

    /**
     * All intermediately required directories are created.
     */
    override fun decompress(filePath: Path, outputFolder: Path): CompressionResult {
        if (Files.notExists(filePath)) return Failure("filePath does not exist")

        val result = runCatching {
            FileInputStream(filePath.toFile()).use { fileInputStream ->
                BufferedInputStream(fileInputStream).use { bufferedInputStream ->
                    GzipCompressorInputStream(bufferedInputStream).use { gzipInputStream ->
                        TarArchiveInputStream(gzipInputStream).use { tarInputStream ->
                            createFiles(tarInputStream, outputFolder)
                        }
                    }
                }
            }
        }
        return result.process(outputFolder)
    }

    @Suppress("DuplicatedCode")
    private fun createFiles(tarInputStream: TarArchiveInputStream, outputFolder: Path) {
        var tarEntry = tarInputStream.nextTarEntry
        while (tarEntry != null) {
            val filePath = outputFolder.resolve(tarEntry.name)
            validatedFilePath(filePath)
            if (tarEntry.isDirectory) {
                Files.createDirectories(filePath)
            } else {
                Files.createDirectories(filePath.parent)
                Files.copy(tarInputStream, filePath)
            }
            tarEntry = tarInputStream.nextTarEntry
        }
    }

    private fun validatedFilePath(path: Path) {
        // Redundant path elements have been found
        if (!path.normalize().equals(path)) {
            throw SecurityException(java.lang.String.format("Attempted zip slip attack through file \"%s\"", path))
        }
    }
}