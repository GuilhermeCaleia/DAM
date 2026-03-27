package org.example.Assignment

// Gere um catalogo simples de livros e emprestimos.
class Library(val name: String) {

    private val books = mutableListOf<Book>()

    // Adiciona qualquer tipo de Book (digital ou fisico) ao catalogo.
    fun addBook(book: Book) {
        books.add(book)
    }

    // Tenta emprestar uma copia do titulo indicado, se existir disponibilidade.
    fun borrowBook(title: String) {
        val book = books.find { it.title.equals(title, ignoreCase = true) }
        if (book == null) {
            println("Book not found.")
            return
        }
        if (book.availableCopies > 0) {
            book.availableCopies -= 1
            println("Borrowed '${book.title}'. Copies remaining: ${book.availableCopies}")
        } else {
            println("No copies available for '${book.title}'.")
        }
    }

    // Devolve uma copia ao catalogo, se o livro existir na lista.
    fun returnBook(title: String) {
        val book = books.find { it.title.equals(title, ignoreCase = true) }
        if (book == null) {
            println("Book not found.")
            return
        }
        book.availableCopies += 1
        println("Returned '${book.title}'. Copies available: ${book.availableCopies}")
    }

    // Imprime todos os livros com a descricao formatada pelo toString de cada tipo.
    fun showBooks() {
        println("\n--- Library Catalog ---")
        books.forEach { println(it) }
    }

    // Filtra por autor e apresenta os resultados ao utilizador.
    fun searchByAuthor(author: String) {
        println("\nBooks by $author:")
        val results = books.filter { it.author.equals(author, ignoreCase = true) }
        if (results.isEmpty()) {
            println("No books found.")
        } else {
            results.forEach { println("- ${it.title} (${it.publicationYear}) copies: ${it.availableCopies}") }
        }
    }
}
