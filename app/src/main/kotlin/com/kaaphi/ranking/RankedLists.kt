package com.kaaphi.ranking

fun <T> List<T>.calculateRank(index: Int, rankSelector: (T) -> String,
                              baseRankLength: Int = DEFAULT_BASE_RANK_LENGTH) : String {
    require(index in 0 .. size)

    return when {
        isEmpty() -> initialRank(baseRankLength)
        index == 0 ->  String(CharArray(baseRankLength) {'0'}) rankBetween rankSelector(get(0))
        index == size -> rankSelector(get(size - 1)) rankBetween String(CharArray(baseRankLength) {'z'})
        else -> rankSelector(get(index - 1)) rankBetween rankSelector(get(index))
    }
}

fun <T> MutableList<T>.addRanked(index: Int, element: T,
                                 rankSelector: (T) -> String,
                                 changeRank: (T, String) -> T,
                                 baseRankLength: Int = DEFAULT_BASE_RANK_LENGTH) {
    require(index in 0 .. size)

    add(index, changeRank(element, calculateRank(index, rankSelector, baseRankLength)))
}