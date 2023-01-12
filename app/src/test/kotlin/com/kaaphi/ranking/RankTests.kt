package com.kaaphi.ranking

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import kotlin.random.Random
import kotlin.random.nextInt

class RankTests : FunSpec({
    test("No Rank Between") {
        shouldThrow<NoSuchRankException> {
            "aaaaa" rankBetween "aaaaa0"
        }
    }

    context("between tests") {
        withData(
            Triple("aaaaa", "ccccc", "bbbbb"),
            Triple("i000e", "i000f", "i000ei"),
            Triple("i0000i", "i0001", "i0000${RankDiff("i", "10").simpleRankBetween}"),
            Triple("hzzzr", "hzzzri", "hzzzr${RankDiff("0", "i").simpleRankBetween}")
        ) { (first, second, expected) ->
            val between = first rankBetween second
            val list = listOf(first, between, second)
            list shouldBe list.sorted()
            between shouldBe expected
        }
    }

    context("between sort fuzz") {
        withData(sequence {
            val random = Random(1234)
            repeat(100) {
                val list = mutableListOf(randomRank(random), randomRank(random)).apply{sort()}
                yield(list[0] to list[1])
            }
        }) { (first, second) ->
            val between = first rankBetween second
            val list = listOf(first, between, second)
            list shouldBe list.sorted()
        }
    }

    context("Insertion Sort Fuzz") {
        withData(1..100) { seed ->
            val list = mutableListOf<Ranked<Long>>()
            val random = Random(seed)
            repeat(1000) {
                do {
                    val newValue = random.nextLong()
                    val idx = list.binarySearchBy(newValue, selector = Ranked<Long>::item)
                    if(idx < 0) {
                        val insertionPoint = -(idx + 1)
                        list.add(insertionPoint, Ranked(newValue, list.calculateInsertRank(insertionPoint,
                            Ranked<*>::rank
                        )))

                        withClue(lazy{
                            "Inserting $newValue at $insertionPoint\n" +
                                    list.mapIndexed {i,r -> "$i : $r"}.joinToString("\n")
                        }) {
                            list shouldBe list.toList().sortedBy(Ranked<*>::rank)
                            list.groupBy {it.rank}.map {it.value.size}.toSet() shouldBe setOf(1)
                        }
                    }
                } while(idx >= 0)
            }
        }
    }
})

fun randomRank(random: Random) =
    String(CharArray(random.nextInt(5..7)) { random.nextInt(37).toString(36)[0] })

data class Ranked<T>(val item: T, val rank: String)
