// LATEST_LV_DIFFERENCE
// WITH_STDLIB
// FULL_JDK

fun <T> Collection<T>?.concat(collection: Collection<T>): Collection<T>? {
    if (this is LinkedHashSet) {
        addAll(<!ARGUMENT_TYPE_MISMATCH!>collection<!>)
        return this
    }
    return this
}
