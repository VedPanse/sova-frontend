package org.sova

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform