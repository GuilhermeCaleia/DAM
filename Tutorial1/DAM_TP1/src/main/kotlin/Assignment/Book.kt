package org.example.Assignment

abstract class Book(
    val title: String,
    val author: String,
    private var _publicationYear: Int,
    availableCopies: Int
) {

    var availableCopies: Int = availableCopies
        set(value) {
            if (value < 0) {
                println("Error: Copies cannot be negative.")
                return
            }
            if (value == 0) {
                println("Warning: Book is now out of stock!")
            }
            field = value
        }

    val publicationYear: String
        get() {
            return when {
                _publicationYear < 1980 -> "Classic"
                _publicationYear in 1980..2010 -> "Modern"
                else -> "Contemporary"
            }
        }

    init {
        println("Book '$title' by $author has been added to the library.")
    }

    abstract fun getStorageInfo(): String

    override fun toString(): String {
        return "Title: $title, Author: $author, Era: $publicationYear, Available: $availableCopies copies"
    }
}