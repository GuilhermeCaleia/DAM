package org.example.Assignment

// Pequena demo do funcionamento da classe Library com livros digitais e fisicos.
fun main() {

    val library = Library("Central Library")

    // Cria livros de tipos distintos para testar polimorfismo de storageInfo/toString.
    val digitalBook = DigitalBook(
        "Kotlin in Action",
        "Dmitry Jemerov",
        2017,
        5,
        4.5,
        "PDF"
    )

    val physicalBook = PhysicalBook(
        "Clean Code",
        "Robert C. Martin",
        2008,
        3,
        650,
        true
    )

    val classicBook = PhysicalBook(
        "1984",
        "George Orwell",
        1949,
        2,
        400,
        false
    )

    // Insere os livros no catalogo.
    library.addBook(digitalBook)
    library.addBook(physicalBook)
    library.addBook(classicBook)

    library.showBooks()

    println("\n--- Borrowing Books ---")
    library.borrowBook("Clean Code")
    library.borrowBook("1984")
    library.borrowBook("1984")
    library.borrowBook("1984")

    println("\n--- Returning Books ---")
    library.returnBook("1984")

    println("\n--- Search by Author ---")
    library.searchByAuthor("George Orwell")
}
