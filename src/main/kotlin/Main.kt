package kr.co.bogo

import java.math.BigInteger

fun main() {

    val memoryBefore = getMemoryUsage()

    val list = mutableListOf<BigInteger>()
    println("hash=" + System.identityHashCode(list))
    for(i in 1500000..3000000){
        list.add(BigInteger.valueOf(i.toLong()))
    }

    val memoryAfter1 = getMemoryUsage()
    println("Memory used by myList: ${(memoryAfter1 - memoryBefore)/1024/1024} MB")

    callBySomething(list)
    val memoryAfter2 = getMemoryUsage()
    println("Memory used by myList: ${(memoryAfter2 - memoryBefore)/1024/1024} MB")

    val newList = copy(list)
    val memoryAfter3 = getMemoryUsage()
    println("Memory used by myList: ${(memoryAfter3 - memoryBefore)/1024/1024} MB")

    referenceOfValue(list)
    println(newList.size)
}

val list: MutableList<BigInteger> = mutableListOf()


fun callBySomething(list: MutableList<BigInteger>): MutableList<BigInteger> {
    println("hash=" + System.identityHashCode(list))
    return list
}

fun copy(list: MutableList<BigInteger>): List<BigInteger> {
    return list.map { BigInteger(it.toByteArray()) }.also {
        println("hash=" + System.identityHashCode(it))
    }
}

fun referenceOfValue(list: MutableList<BigInteger>) {
    var list2 = list
    println("list2 hash=" + System.identityHashCode(list2))
    list2 = mutableListOf()
    println("list2 hash=" + System.identityHashCode(list2))
    println("list=${list.size}")
}

fun getMemoryUsage(): Long {
    val runtime = Runtime.getRuntime()
    return runtime.totalMemory() - runtime.freeMemory()
}
