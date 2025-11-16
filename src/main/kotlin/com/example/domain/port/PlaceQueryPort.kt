package com.example.domain.port

interface PlaceQueryPort {
    /**
     * memo: defect_flag=0 のレコードだけを返す。
     */
    fun findActiveCommentsByIds(ids: List<String>): Map<String, String?>
}
