package org.example.Assignment

class Library(val name: String) {

    private val books = mutableListOf<Book>()

    companion object {
        private var totalBooksCreated = 0

        fun getTotalBooksCreated(): Int {
            return totalBooksCreated
        }
    }

    fun addBook(book: Book) {
        books.add(book)
        totalBooksCreated++
    }

    fun borrowBook(title: String) {
        val book = books.find { it.title.equals(title, ignoreCase = true) }

        if (book != null) {
            if (book.availableCopies > 0) {
                book.availableCopies -= 1
                println("Successfully borrowed '${book.title}'. Copies remaining: ${book.availableCopies}")
            } else {
                println("Sorry, '${book.title}' is not available.")
            }
        } else {
            println("Book not found.")
        }
    }

    fun returnBook(title: String) {
        val book = books.find { it.title.equals(title, ignoreCase = true) }

        if (book != null) {
            book.availableCopies += 1
            println("Book '${book.title}' returned successfully. Copies available: ${book.availableCopies}")
        } else {
            println("Book not found.")
        }
    }

    fun showBooks() {
        println("\n--- Library Catalog ---")
        for (book in books) {
            println(book)
        }
    }

    fun searchByAuthor(author: String) {
        println("\nBooks by $author:")

        val results = books.filter { it.author.equals(author, ignoreCase = true) }

        if (results.isEmpty()) {
            println("No books found.")
            return
        }

        for (book in results) {
            val copyText = if (book.availableCopies == 1) "copy" else "copies"
            println("- ${book.title} (${book.publicationYear}, ${book.availableCopies} $copyText available)")
        }
    }
}