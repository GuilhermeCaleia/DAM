package org.example.Assignment

// Representa um livro digital, acrescentando tamanho do ficheiro e formato.
class DigitalBook(
    title: String,
    author: String,
    publicationYear: Int,
    availableCopies: Int,
    val fileSize: Double,
    val format: String
) : Book(title, author, publicationYear, availableCopies) {

    // Descreve armazenamento com dados especificos de ficheiro.
    override fun storageInfo(): String = "Digital file: $fileSize MB ($format)"
}
