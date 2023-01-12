package com.kaaphi.ranking

private const val RANK_RADIX = 36
private val RANK_MIDDLE = (RANK_RADIX/2).toString(RANK_RADIX)
const val DEFAULT_BASE_RANK_LENGTH = 5


infix fun String.rankBetween(other: String) : String =
    this.rankBetween(other, DEFAULT_BASE_RANK_LENGTH)

fun String.rankBetween(that: String, baseRankLength: Int) : String {
    require(this < that) { "Argument must be lexicographically later!" }

    if(that.endsWith("0") && this == that.dropLast(1)) {
        throw NoSuchRankException(this, that)
    }

    val paddedThis = this.padEnd(that.length, '0')
    val paddedThat = that.padEnd(this.length, '0')

    val rankDiff = RankDiff(paddedThis, paddedThat)

    return if(rankDiff.diff < 2) {
        paddedThis + RANK_MIDDLE
    } else {
        rankDiff.simpleRankBetween.padStart(paddedThat.length, '0')
    }
}

private fun String.padToMatch(other: String, padChar: Char = '0') : String =
    if(this.length < other.length) {
        "$this${String(CharArray(other.length - this.length) {padChar})}"
    } else {
        this
    }

class NoSuchRankException(first: String, second: String) :
    Exception("There is no rank between $first and $second!")

internal data class RankDiff(
    val first: String,
    val second: String
) {
    val diff : Long
    val simpleRankBetween: String

    init {
        val firstLong = first.toLong(RANK_RADIX)
        val secondLong = second.toLong(RANK_RADIX)
        diff = secondLong - firstLong
        simpleRankBetween = (firstLong + (diff / 2)).toString(36)
    }
}

fun initialRank(baseRankLength: Int = DEFAULT_BASE_RANK_LENGTH) =
    (String(CharArray(baseRankLength) {'z'}).toLong(RANK_RADIX) / 2).toString(
        RANK_RADIX)

fun generateRanks(itemCount: Int, baseRankLength: Int = DEFAULT_BASE_RANK_LENGTH) : Sequence<String> =
    sequence {
        val start = String(CharArray(baseRankLength) {'0'}).toLong(RANK_RADIX)
        val end = String(CharArray(baseRankLength) {'z'}).toLong(RANK_RADIX)

        val endPadding = itemCount/10
        val delta = (end - start) / (itemCount + (endPadding*2))

        var next = start + (delta*endPadding)
        repeat(itemCount) {
            yield(next.toString(RANK_RADIX))
            next += delta
        }
    }