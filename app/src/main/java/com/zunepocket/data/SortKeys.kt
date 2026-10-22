package com.zunepocket.data

/** Sort key that ignores a leading "The ", as the Zune did. */
fun sortKey(name: String): String {
    val lower = name.trim().lowercase()
    return if (lower.startsWith("the ") && lower.length > 4) lower.substring(4) else lower
}

/** The jump-list letter a name files under: 'A'..'Z', or '#' for everything else. */
fun jumpLetter(name: String): Char {
    val c = sortKey(name).firstOrNull()?.uppercaseChar() ?: return '#'
    return if (c in 'A'..'Z') c else '#'
}

val JUMP_LETTERS: List<Char> = listOf('#') + ('A'..'Z')
