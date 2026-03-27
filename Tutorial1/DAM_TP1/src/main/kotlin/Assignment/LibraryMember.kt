package org.example.Assignment

// Representa um socio da biblioteca e os titulos que tem emprestados.
data class LibraryMember(
    val name: String,
    val membershipId: String,
    val borrowedBooks: MutableList<String> = mutableListOf()
)
