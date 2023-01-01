package com.kaaphi.ranking

const val RANK_RADIX = 36
const val DEFAULT_BASE_RANK_LENGTH = 5
val RANK_MIDDLE = (RANK_RADIX/2).toString(RANK_RADIX)


infix fun String.rankBetween(other: String) : String =
    this.rankBetween(other, DEFAULT_BASE_RANK_LENGTH)


fun String.rankBetween(that: String, baseRankLength: Int) : String {
    require(this < that) { "Argument must be lexicographically later!" }

    if(that.endsWith("0") && this == that.dropLast(1)) {
        throw NoSuchRankException(this, that)
    }

    val paddedThis = this.padToMatch(that, '0')
    val paddedThat = that.padToMatch(this, 'z')
    val iterator = paddedThis.chunkedSequence(1).zip(paddedThat.chunkedSequence(1))
        .map { RankDiff(it.first, it.second) }
        .iterator()


    val sb = StringBuilder()
    var diffed = false
    var idx = 0
    //iterate until we find an index where this is at least one symbol between the pair
    while(iterator.hasNext() && !diffed) {
        val rankDiff = iterator.next()

        if(rankDiff.diff < 2) {
            sb.append(rankDiff.first)
            idx++
        } else {
            diffed = true
        }
    }

    if(!diffed) {
        //this means that no pair had a large enough diff
        sb.append(RANK_MIDDLE)
    } else {
        val len = maxOf(baseRankLength - sb.length, 1)
        sb.append(RankDiff(paddedThis.substring(idx, idx+len), paddedThat.substring(idx, idx+len)).simpleRankBetween)
    }

    return sb.toString()
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