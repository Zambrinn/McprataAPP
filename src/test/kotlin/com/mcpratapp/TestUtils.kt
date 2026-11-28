package com.mcpratapp

import org.mockito.Mockito

@Suppress("UNCHECKED_CAST")
fun <T> anyNonNull(): T {
    Mockito.any<T>()
    return null as T
}
