/*
 * (C) 2021 GoodData Corporation
 */
package com.gooddata.api.exception

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import kotlin.reflect.KClass

class MyException(
    override val message: String,
    override val messageTemplate: String,
    val name: String,
    val age: Int,
    val address: String?,
    val `class`: KClass<MyException>,
    val listOfString: List<String>
) : RuntimeException(message), ClientException

internal object MyObfuscateExceptionMessageRenderer : ExceptionMessageRenderer {
    override fun render(template: String, values: Map<String, Any?>): String {
        return replacePlaceholder(template, "name", "Jan")
    }
}

internal object MyNameExceptionMessageRenderer : ExceptionMessageRenderer {
    override fun convertValue(name: String, value: Any?): String {
        return name
    }
}

internal class ExceptionMessageTest {

    @Test
    fun `test format`() {
        expectThat(
            MyException(
                "Ahoj Jim 42 exists Brno 1A \"one, two, three\"",
                "Ahoj \${name} \${age} \${notExist} \${address} \${class} \"\${listOfString}\"",
                "John",
                57,
                null,
                MyException::class,
                listOf("one", "two", "three")
            )
        ) {
            get { message }.isEqualTo("Ahoj Jim 42 exists Brno 1A \"one, two, three\"")
            get { messageTemplate }.isEqualTo(
                "Ahoj \${name} \${age} \${notExist} \${address} \${class} \"\${listOfString}\"")
            get { renderMessage(MyNameExceptionMessageRenderer) }
                .isEqualTo("Ahoj name age \${notExist} address class \"listOfString\"")
            get { renderMessage(MyObfuscateExceptionMessageRenderer) }
                .isEqualTo("Ahoj Jan \${age} \${notExist} \${address} \${class} \"\${listOfString}\"")
            get { renderMessage() }.isEqualTo("Ahoj John 57 \${notExist} null MyException \"one, two, three\"")
            get { name }.isEqualTo("John")
            get { age }.isEqualTo(57)
            get { address }.isNull()
            get { `class` }.isEqualTo(MyException::class)
            get { listOfString }.isEqualTo(listOf("one", "two", "three"))
        }
    }
}
