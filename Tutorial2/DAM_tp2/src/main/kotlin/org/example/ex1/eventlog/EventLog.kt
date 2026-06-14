package org.example.ex1.eventlog

/**
 * Ex 1.1 - Modelação de eventos com sealed class, extensões e função de ordem superior.
 */
sealed class Event {
    // evento de login
    data class Login(val username: String, val timestamp: Long) : Event()
    // compra com valor gasto
    data class Purchase(val username: String, val amount: Double, val timestamp: Long) : Event()
    // evento de logout
    data class Logout(val username: String, val timestamp: Long) : Event()
}

/**
 * Filtra apenas os eventos do utilizador indicado.
 */
fun List<Event>.filterByUser(username: String): List<Event> =
    this.filter { event ->
        when (event) {
            is Event.Login -> event.username == username
            is Event.Purchase -> event.username == username
            is Event.Logout -> event.username == username
        }
    }

/**
 * Soma o valor gasto pelo utilizador em todos os Purchase.
 */
fun List<Event>.totalSpent(username: String): Double =
    run {
        var total = 0.0
        for (event in this) {
            if (event is Event.Purchase && event.username == username) {
                total += event.amount
            }
        }
        total
    }

/**
 * Aplica o handler a cada evento da lista.
 */
fun processEvents(events: List<Event>, handler: (Event) -> Unit) {
    events.forEach(handler)
}

/**
 * Demonstração conforme enunciado (outputs alinhados com o exemplo).
 */
fun main() {
    // dados de exemplo fornecidos no enunciado
    val events = listOf(
        Event.Login("alice", 1_000),
        Event.Purchase("alice", 49.99, 1_100),
        Event.Purchase("bob", 19.99, 1_200),
        Event.Login("bob", 1_050),
        Event.Purchase("alice", 15.00, 1_300),
        Event.Logout("alice", 1_400),
        Event.Logout("bob", 1_500)
    )

    // imprime cada evento com texto legível
    processEvents(events) { event ->
        when (event) {
            is Event.Login -> println("[ LOGIN ] ${event.username} loggedin at t =${event.timestamp}")
            is Event.Purchase -> println("[ PURCHASE ] ${event.username} spent $${"%.2f".format(event.amount)} at t =${event.timestamp}")
            is Event.Logout -> println("[ LOGOUT ] ${event.username} logged out at t =${event.timestamp}")
        }
    }

    // recolhe lista de utilizadores distintos
    val users = mutableSetOf<String>()
    for (event in events) {
        when (event) {
            is Event.Login -> users.add(event.username)
            is Event.Purchase -> users.add(event.username)
            is Event.Logout -> users.add(event.username)
        }
    }

    // mostra totais e eventos filtrados
    users.forEach { user ->
        println("Total spent by $user :$${events.totalSpent(user)}")
        println("Events for $user :")
        events.filterByUser(user).forEach { println(it) }
    }
}
