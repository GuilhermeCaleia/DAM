package org.example.Assignment

class PhysicalBook(
    title: String,
    author: String,
    publicationYear: Int,
    availableCopies: Int,
    val weight: Int,
    val hasHardcover: Boolean = true
) : Book(title, author, publicationYear, availableCopies) {

    override fun getStorageInfo(): String {
        val cover = if (hasHardcover) "Yes" else "No"
        return "Physical book: ${weight}g, Hardcover: $cover"
    }

    override fun toString(): String {
        return super.toString() + "\nStorage: ${getStorageInfo()}"
    }
}
