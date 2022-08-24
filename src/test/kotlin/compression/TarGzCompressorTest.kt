package compression

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path

// TODO Test slip attack
// TODO Test missing files
// TODO Test InputErrors
class TarGzCompressorTest {

    private var instance: TarGzCompressor = TarGzCompressor()

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `a single file is properly tarred`() {

        // Arrange
        val content = "content"
        val inputFilePath = tempDir.resolve("inputFile.txt")
        val outputFilePath = tempDir.resolve("outputFile")
        Files.createFile(inputFilePath)
        Files.write(inputFilePath, content.toByteArray())

        // Act
        val result = instance.compress(inputFilePath, outputFilePath).getOrFail()

        // Assert
        val gzipInputStream = GzipCompressorInputStream(Files.newInputStream(result))
        val tarInputStream = TarArchiveInputStream(gzipInputStream)
        val entry = tarInputStream.nextEntry
        assertThat(Path.of(entry.name)).isEqualTo(tempDir.relativize(inputFilePath))
        assertThat(content.toByteArray()).isEqualTo(tarInputStream.readAllBytes())

        // Cleanup
        gzipInputStream.close()
        tarInputStream.close()
    }

    @Test
    fun `a file in a folder is properly tarred`() {
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
        val gzipInputStream = GzipCompressorInputStream(Files.newInputStream(result))
        val tarInputStream = TarArchiveInputStream(gzipInputStream)
        val entry = tarInputStream.nextEntry
        assertThat(entry.name).isEqualTo(fileName)
        assertThat(content.toByteArray()).isEqualTo(tarInputStream.readAllBytes())

        // Cleanup
        gzipInputStream.close()
        tarInputStream.close()
    }

    @Test
    fun `a tar with a single file can be decompressed`() {
        // Arrange
        val content = "content"
        val inputFilePath = tempDir.resolve("inputFile.txt")
        val tarGzFilePath = tempDir.resolve("file.tar.gz")
        val outputFolderPath = tempDir.resolve("outputFolder")

        // Create the file
        Files.createFile(inputFilePath)
        Files.write(inputFilePath, content.toByteArray())

        // Create the tar.gz including the file
        val fileOutputStream = FileOutputStream(tarGzFilePath.toFile())
        val gzipOutputStream = GzipCompressorOutputStream(fileOutputStream)
        val tarOutputStream = TarArchiveOutputStream(gzipOutputStream)
        writeTarEntry(tarOutputStream, inputFilePath)

        // Cleanup creation
        tarOutputStream.close()
        gzipOutputStream.close()
        fileOutputStream.close()

        // Act
        val result = instance.decompress(tarGzFilePath, outputFolderPath).getOrFail()

        // Assert
        val uncompressedFilePath = outputFolderPath.resolve(tempDir.relativize(inputFilePath))
        assertThat(result).isEqualTo(outputFolderPath)
        assertThat(uncompressedFilePath).exists()
        assertThat(content.toByteArray()).isEqualTo(Files.newInputStream(uncompressedFilePath).readAllBytes())
    }

    @Test
    fun `a tar with a file in a folder can be decompressed`() {
        // Arrange
        val content = "content"
        val inputFolderPath = tempDir.resolve("inputFolder")
        val inputFilePath = inputFolderPath.resolve("inputFile.txt")
        val tarGzFilePath = tempDir.resolve("file.tar.gz")
        val outputFolderPath = tempDir.resolve("outputFolder")

        // Create the file in a folder
        Files.createDirectories(inputFolderPath)
        Files.createFile(inputFilePath)
        Files.write(inputFilePath, content.toByteArray())

        // Create the tar.gz including the file
        val fileOutputStream = FileOutputStream(tarGzFilePath.toFile())
        val gzipOutputStream = GzipCompressorOutputStream(fileOutputStream)
        val tarOutputStream = TarArchiveOutputStream(gzipOutputStream)
        writeTarEntry(tarOutputStream, inputFilePath)

        // Cleanup creation
        tarOutputStream.close()
        gzipOutputStream.close()
        fileOutputStream.close()

        // Act
        val result = instance.decompress(tarGzFilePath, outputFolderPath).getOrFail()

        // Assert
        val uncompressedFilePath = outputFolderPath.resolve(tempDir.relativize(inputFilePath))
        assertThat(result).isEqualTo(outputFolderPath)
        assertThat(uncompressedFilePath.resolve(inputFilePath)).exists()
        assertThat(content.toByteArray()).isEqualTo(Files.newInputStream(uncompressedFilePath).readAllBytes())
    }

    private fun writeTarEntry(tarOutputStream: TarArchiveOutputStream, filePath: Path) {
        val tarArchiveEntry = TarArchiveEntry(tempDir.relativize(filePath).toFile())
        tarArchiveEntry.size = Files.size(filePath)
        tarOutputStream.putArchiveEntry(tarArchiveEntry)
        Files.copy(filePath, tarOutputStream)
        tarOutputStream.closeArchiveEntry()
    }
}
