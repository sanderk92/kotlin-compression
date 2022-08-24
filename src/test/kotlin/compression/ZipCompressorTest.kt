package compression

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

// TODO Test slip attack
// TODO Test missing files
// TODO Test state assertions
class ZipCompressorTest {

    private var instance: ZipCompressor = ZipCompressor()

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `a single file is properly zipped`() {
        // Arrange
        val content = "content"
        val inputFilePath = tempDir.resolve("inputFile.txt")
        val outputFilePath = tempDir.resolve("outputFile")
        Files.createFile(inputFilePath)
        Files.write(inputFilePath, content.toByteArray())

        // Act
        val result = instance.compress(inputFilePath, outputFilePath).getOrFail()

        // Assert
        val zipFile = ZipFile(result.toFile())
        val entry = zipFile.entries().nextElement()
        assertThat(Path.of(entry.name)).isEqualTo(tempDir.relativize(inputFilePath))
        assertThat(content.toByteArray()).isEqualTo(zipFile.getInputStream(entry).readAllBytes())

        // Cleanup
        zipFile.close()
    }

    @Test
    fun `a file in a folder is properly zipped`() {
        // Arrange
        val content = "content"
        val fileName = "inputFile.txt"
        val inputFolderPath = tempDir.resolve("inputFolder")
        val inputFilePath = inputFolderPath.resolve(fileName)
        val outputFilePath = tempDir.resolve("outputFile")
        Files.createDirectories(inputFolderPath)
        Files.createFile(inputFilePath)
        Files.write(inputFilePath, content.toByteArray())

        // Act
        val result = instance.compress(inputFolderPath, outputFilePath).getOrFail()

        // Assert
        val zipFile = ZipFile(result.toFile())
        val entry = zipFile.entries().nextElement()
        assertThat(entry.name).isEqualTo(fileName)
        assertThat(content.toByteArray()).isEqualTo(zipFile.getInputStream(entry).readAllBytes())

        // Cleanup
        zipFile.close()
    }

    @Test
    fun `a zip with a single file can be decompressed` () {
        // Arrange
        val content = "content"
        val inputFilePath = tempDir.resolve("inputFile.txt")
        val zipFilePath = tempDir.resolve("inputFile.zip")
        val outputFolderPath = tempDir.resolve("outputFolder")

        // Create the file
        Files.createFile(inputFilePath)
        Files.write(inputFilePath, content.toByteArray())

        // Create the zip including the file
        val fileOutputStream = FileOutputStream(zipFilePath.toFile())
        val zipOutputStream = ZipOutputStream(fileOutputStream)
        writeZipEntry(zipOutputStream, inputFilePath)

        // Cleanup creation
        fileOutputStream.close()
        zipOutputStream.closeEntry()

        // Act
        val result = instance.decompress(zipFilePath, outputFolderPath).getOrFail()

        // Assert
        val uncompressedFilePath = outputFolderPath.resolve(tempDir.relativize(inputFilePath))
        assertThat(result).isEqualTo(outputFolderPath)
        assertThat(uncompressedFilePath).exists()
        assertThat(content.toByteArray()).isEqualTo(Files.newInputStream(uncompressedFilePath).readAllBytes())
    }

    @Test
    fun `a zip with a file in a folder can be decompressed`() {
        // Arrange
        val content = "content"
        val inputFolderPath = tempDir.resolve("inputFolder")
        val inputFilePath = inputFolderPath.resolve("inputFile.txt")
        val zipFilePath = tempDir.resolve("zipFile.zip")
        val outputFolderPath = tempDir.resolve("outputFolder")

        // Create the file in a folder
        Files.createDirectories(inputFolderPath)
        Files.createFile(inputFilePath)
        Files.write(inputFilePath, content.toByteArray())

        // Create the zip including the file in a folder
        val fileOutputStream = FileOutputStream(zipFilePath.toFile())
        val zipOutputStream = ZipOutputStream(fileOutputStream)
        writeZipEntry(zipOutputStream, inputFilePath)

        // Cleanup creation
        fileOutputStream.close()
        zipOutputStream.closeEntry()

        // Act
        val result = instance.decompress(zipFilePath, outputFolderPath).getOrFail()

        // Assert
        val uncompressedFilePath = outputFolderPath.resolve(tempDir.relativize(inputFilePath))
        assertThat(result).isEqualTo(outputFolderPath)
        assertThat(uncompressedFilePath).exists()
        assertThat(content.toByteArray()).isEqualTo(Files.newInputStream(uncompressedFilePath).readAllBytes())
    }

    private fun writeZipEntry(zipOutputStream: ZipOutputStream, filePath: Path) {
        val zipEntry = ZipEntry(tempDir.relativize(filePath).toString())
        zipOutputStream.putNextEntry(zipEntry)
        Files.copy(filePath, zipOutputStream)
        zipOutputStream.closeEntry()
    }
}
