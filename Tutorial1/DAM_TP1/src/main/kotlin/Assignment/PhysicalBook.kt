package org.example.Assignment

// Especializa Book para copias fisicas com peso e tipo de capa.
class PhysicalBook(
    title: String,
    author: String,
    publicationYear: Int,
    availableCopies: Int,
    val weight: Int,
    val hasHardcover: Boolean = true
) : Book(title, author, publicationYear, availableCopies) {

    // Indica como e' guardado fisicamente, incluindo peso e capa.
    override fun storageInfo(): String {
        val cover = if (hasHardcover) "Hardcover" else "Paperback"
        return "Physical copy: ${weight}g, $cover"
    }
}
