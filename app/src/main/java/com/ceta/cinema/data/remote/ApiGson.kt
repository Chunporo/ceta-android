package com.ceta.cinema.data.remote

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

/**
 * Gson ignores Kotlin default values, so a list the server leaves out would arrive as null in a non-null field and crash a
 * mapper much later. After reading any of our DTOs, every null collection field becomes empty.
 */
private object EmptyCollectionsFactory : TypeAdapterFactory {
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        val raw = type.rawType
        if (!raw.name.startsWith("com.ceta.cinema.data.remote.dto.")) return null
        val delegate = gson.getDelegateAdapter(this, type)
        val listFields = generateSequence(raw as Class<*>?) { it.superclass }
            .flatMap { it.declaredFields.asSequence() }
            .filter { List::class.java.isAssignableFrom(it.type) || Set::class.java.isAssignableFrom(it.type) }
            .onEach { it.isAccessible = true }
            .toList()
        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T?) = delegate.write(out, value)
            override fun read(reader: JsonReader): T? {
                val value = delegate.read(reader) ?: return null
                for (field in listFields) {
                    if (field.get(value) == null) {
                        field.set(value, if (Set::class.java.isAssignableFrom(field.type)) emptySet<Any>() else emptyList<Any>())
                    }
                }
                return value
            }
        }
    }
}

object ApiGson {
    fun create(): Gson = GsonBuilder().registerTypeAdapterFactory(EmptyCollectionsFactory).create()
}
