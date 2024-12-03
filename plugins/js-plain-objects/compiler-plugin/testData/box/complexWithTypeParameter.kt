// KT-68943

package foo

import kotlinx.js.JsPlainObject

@JsPlainObject
external interface BaseUser<T> {
   val name: T
}

@JsPlainObject
external interface User<T> : BaseUser<T> {
    override val name: T
    val age: Int
}

fun box(): String {
    val user = User(name = "Name", age = 10, friends = friends)

    if (user.name != "Name") return "Fail: problem with `name` property"
    if (user.age != 10) return "Fail: problem with `age` property"

    val json = js("JSON.stringify(user)")
    if (json != "{\"age\":10,\"name\":\"Name\"}") return "Fail: got the next json: $json"

    return "OK"
}