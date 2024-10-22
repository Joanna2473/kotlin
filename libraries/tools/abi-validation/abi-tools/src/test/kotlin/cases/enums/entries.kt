package cases.enums

@OptIn(ExperimentalStdlibApi::class)
fun test() {
    EnumClass.entries.forEach {
        println(it)
    }

    _root_ide_package_.cases.enums.JavaEnum.entries.forEach {
        println(it)
    }
}

