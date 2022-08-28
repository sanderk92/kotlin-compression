package compression

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.io.path.name

private const val EXTENSION = ".zip"

@Suppress("DuplicatedCode")
class ZipCompressor : ArchiverCompressor {

    override fun supportedExtension() = EXTENSION

    override fun compress(sourcePath: Path, targetFilePath: Path): CompressionResult {
        val outputZipPath = targetFilePath.resolveSibling(targetFilePath.name + EXTENSION)

        if (Files.notExists(sourcePath)) return InputError("sourcePath does not exist")
        if (Files.exists(outputZipPath)) return InputError("targetFilePath already exists")
        if (sourcePath == targetFilePath.parent) return InputError("sourcePath and targetFilePath's parent are equal")

        return compressionResult(outputZipPath) {
            FileOutputStream(outputZipPath.toFile()).use { fileOutputStream ->
                BufferedOutputStream(fileOutputStream).use { bufferedOutputStream ->
                    ZipOutputStream(bufferedOutputStream).use { zipOutputStream ->
                        createZipEntries(sourcePath, zipOutputStream)
                    }
                }
            }
        }
    }

    private fun createZipEntries(sourcePath: Path, zipOutputStream: ZipOutputStream) =
        Files.walk(sourcePath).use { fileWalk ->
            fileWalk
                .filter { !Files.isDirectory(it) }
                .forEach { sourceFile -> createZipEntry(zipOutputStream, sourceFile, sourcePath) }
        }

    private fun createZipEntry(zipOutputStream: ZipOutputStream, sourceFile: Path, sourcePath: Path) {
        val filePathInArchive = determinePathInArchive(sourceFile, sourcePath)
        ZipEntry(filePathInArchive.toString()).also {
            zipOutputStream.putNextEntry(it)
            Files.copy(sourceFile, zipOutputStream)
            zipOutputStream.closeEntry()
        }
    }

    private fun determinePathInArchive(sourceFile: Path, sourcePath: Path): Path =
        if (sourceFile == sourcePath) {
            // A single file has been selected for compression
            sourceFile.fileName
        } else {
            // A folder has been selected for compression
            sourcePath.relativize(sourceFile)
        }

    override fun decompress(sourcePath: Path, outputDirectory: Path): CompressionResult {

        if (Files.notExists(sourcePath)) return InputError("sourcePath does not exist")

        return compressionResult(outputDirectory) {
            FileInputStream(sourcePath.toFile()).use { fileInputStream ->
                BufferedInputStream(fileInputStream).use { bufferedInputStream ->
                    ZipInputStream(bufferedInputStream).use { zipInputStream ->
                        createFiles(zipInputStream, outputDirectory)
                    }
                }
            }
        }
    }

    private fun createFiles(zipInputStream: ZipInputStream, outputFolder: Path) {
        var zipEntry = zipInputStream.nextEntry
        while (zipEntry != null) {
            val filePath = outputFolder.resolve(zipEntry.name)
            validatedFilePath(filePath)
            if (zipEntry.isDirectory) {
                Files.createDirectories(filePath)
            } else {
                Files.createDirectories(filePath.parent)
                Files.copy(zipInputStream, filePath)
            }
            zipEntry = zipInputStream.nextEntry
        }
    }

    private fun validatedFilePath(path: Path) {
        // Redundant path elements have been found
        if (!path.normalize().equals(path)) {
            throw SecurityException(String.format("Attempted zip slip attack through file \"%s\"", path));
        }
    }
}
