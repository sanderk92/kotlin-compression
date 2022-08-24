# Kotlin Compression Tools

A collection of archiver + compressor tools for Kotlin. 

```kotlin
fun myFun() {
    
    // All available compressors
    val zipCompressor = ZipCompressor()
    val tarGzCompressor = TarGzCompressor()

    // Some random file and folder
    val myFile = Files.createFile(Path.of("/etc/file.txt"))
    val myFolder = Files.createDirectories(Path.of("/etc/folder"))
    
    compress(myFile, myFolder, zipCompressor)
    compress(myFile, myFolder, tarGzCompressor)

    decompress(myFile, myFolder, zipCompressor)
    decompress(myFile, myFolder, tarGzCompressor)
}

private fun compress(sourcePath: Path, targetPath: Path, compressor: ArchiverCompressor) {
    compressor.compress(sourcePath, targetPath)
}

private fun decompress(sourcePath: Path, targetPath: Path, compressor: ArchiverCompressor) {
    compressor.compress(sourcePath, targetPath)
}
```