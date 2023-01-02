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

@Suppress("DuplicatedCode")
class TarGzCompressor : ArchiverCompressor {

    override fun supportedExtension() = EXTENSION

    override fun compress(sourcePath: Path, targetFilePath: Path): CompressionResult {
        val outputZipPath = targetFilePath.resolveSibling(targetFilePath.name + EXTENSION)

        if (Files.notExists(sourcePath)) return InputError("sourcePath does not exist")
        if (Files.exists(outputZipPath)) return InputError("targetFilePath already exists")

        return compressionResult(outputZipPath) {
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

    override fun decompress(sourcePath: Path, outputDirectory: Path): CompressionResult {

        if (Files.notExists(sourcePath)) return InputError("sourcePath does not exist")

        return compressionResult(outputDirectory) {
            FileInputStream(sourcePath.toFile()).use { fileInputStream ->
                BufferedInputStream(fileInputStream).use { bufferedInputStream ->
                    GzipCompressorInputStream(bufferedInputStream).use { gzipInputStream ->
                        TarArchiveInputStream(gzipInputStream).use { tarInputStream ->
                            createFiles(tarInputStream, outputDirectory)
                        }
                    }
                }
            }
        }
    }

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