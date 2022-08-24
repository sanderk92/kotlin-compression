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

class ZipCompressor : ArchiverCompressor {

    override fun supportedExtension() = EXTENSION

    /**
     * Do not output to the same folder as the input is read from. When a folder is compressed, the direct children of
     * the selected folder are added to the archive. When a single file is compressed, it will simply be the only file
     * in the archive. In case compression fails, all resulted files will be deleted. Any failures during this cleanup
     * will be ignored.
     */
    override fun compress(sourcePath: Path, outputFilePath: Path): CompressionResult {
        val outputZipPath = outputFilePath.resolveSibling(outputFilePath.name + EXTENSION)

        if (Files.notExists(sourcePath)) return InputError("sourcePath does not exist")
        if (Files.exists(outputZipPath)) return InputError("outputFilePath already exists")

        val result = runCatching {
            FileOutputStream(outputZipPath.toFile()).use { fileOutputStream ->
                BufferedOutputStream(fileOutputStream).use { bufferedOutputStream ->
                    ZipOutputStream(bufferedOutputStream).use { zipOutputStream ->
                        createZipEntries(sourcePath, zipOutputStream)
                    }
                }
            }
        }
        return result.process(outputZipPath)
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

    /**
     * All intermediately required directories are created.
     */
    override fun decompress(filePath: Path, outputFolder: Path): CompressionResult {
        if (Files.notExists(filePath)) return InputError("filePath does not exist")

        val result = runCatching {
            FileInputStream(filePath.toFile()).use { fileInputStream ->
                BufferedInputStream(fileInputStream).use { bufferedInputStream ->
                    ZipInputStream(bufferedInputStream).use { zipInputStream ->
                        createFiles(zipInputStream, outputFolder)
                    }
                }
            }
        }
        return result.process(outputFolder)
    }

    @Suppress("DuplicatedCode")
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
