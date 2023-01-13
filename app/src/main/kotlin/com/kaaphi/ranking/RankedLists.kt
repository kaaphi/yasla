package com.kaaphi.ranking

fun <T> List<T>.calculateInsertRank(index: Int, rankSelector: (T) -> String,
                              baseRankLength: Int = DEFAULT_BASE_RANK_LENGTH) : String {
    require(index in 0 .. size)
    return when {
        isEmpty() -> initialRank(baseRankLength)
        else -> calculateRankBetween(index-1, index, rankSelector, baseRankLength)
    }
}

fun <T> List<T>.getRank(index: Int, rankSelector: (T) -> String,
                        baseRankLength: Int = DEFAULT_BASE_RANK_LENGTH) : String {
    require(index in -1..size)

    return when(index) {
        -1 -> String(CharArray(baseRankLength) {'0'})
        size -> String(CharArray(baseRankLength) {'z'})
        else -> rankSelector(get(index))
    }
}

fun <T> List<T>.calculateRankAt(index: Int, rankSelector: (T) -> String,
                                    baseRankLength: Int = DEFAULT_BASE_RANK_LENGTH) : String {
    require(index in indices)
    return calculateRankBetween(index-1, index+1, rankSelector, baseRankLength)
}

fun <T> List<T>.calculateRankBetween(firstIdx: Int, secondIdx: Int, rankSelector: (T) -> String,
                                     baseRankLength: Int = DEFAULT_BASE_RANK_LENGTH): String {
    require(firstIdx < secondIdx)
    require(firstIdx in -1 until size)
    require(secondIdx in 0 .. size)

    return getRank(firstIdx, rankSelector, baseRankLength) rankBetween getRank(secondIdx, rankSelector, baseRankLength)
}

fun <T> MutableList<T>.addByRank(item: T, rankSelector: (T) -> String) {
    val idx = binarySearchBy(rankSelector(item), selector = rankSelector)
    check(idx < 0)
    add(-(idx + 1), item)
}

fun <T> MutableList<T>.addRanked(index: Int, element: T,
                                 rankSelector: (T) -> String,
                                 changeRank: (T, String) -> T,
                                 baseRankLength: Int = DEFAULT_BASE_RANK_LENGTH) {
    require(index in 0 .. size)

    add(index, changeRank(element, calculateInsertRank(index, rankSelector, baseRankLength)))
}