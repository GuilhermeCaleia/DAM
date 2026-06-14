package org.example.Assignment

// Modelo base de livro; subclasses acrescentam detalhes especificos de armazenamento.
open class Book(
    val title: String,
    val author: String,
    val publicationYear: Int,
    var availableCopies: Int
) {
    // Permite que cada tipo de livro descreva como e' guardado.
    open fun storageInfo(): String = "Standard storage"

    // Formata os dados principais para listagens no catalogo.
    override fun toString(): String {
        return "Title: $title, Author: $author, Year: $publicationYear, Copies: $availableCopies\nStorage: ${storageInfo()}"
    }
}
