package org.rsmod.api.type.builders

public abstract class TypeBuilder<B, T> {
    @PublishedApi internal val cache: MutableList<T> = mutableListOf()

    /** Returns a stable snapshot of definitions declared by this builder. */
    public fun definitions(): List<T> = cache.toList()
}

public abstract class HashTypeBuilder<B, T> : TypeBuilder<B, T>() {
    public abstract fun build(internal: String, init: B.() -> Unit = {})
}

public abstract class NameTypeBuilder<B, T> : TypeBuilder<B, T>() {
    public abstract fun build(internal: String, init: B.() -> Unit = {}): T
}
