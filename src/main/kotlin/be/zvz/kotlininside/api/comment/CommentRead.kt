package be.zvz.kotlininside.api.comment

import be.zvz.kotlininside.KotlinInside
import be.zvz.kotlininside.api.type.DCCon
import be.zvz.kotlininside.api.type.comment.Comment
import be.zvz.kotlininside.api.type.comment.DCConComment
import be.zvz.kotlininside.api.type.comment.GenericComment
import be.zvz.kotlininside.http.HttpException
import be.zvz.kotlininside.http.Request
import be.zvz.kotlininside.value.ApiUrl

class CommentRead(
        private val gallId: String,
        private val articleId: Int,
        private val rePage: Int
) {
    data class ReadResult(
            val totalComment: Int,
            val totalPage: Int,
            val rePage: Int,
            val commentList: List<CommentData>
    )

    data class CommentData(
            val memberIcon: Int,
            val ipData: String,
            val gallerCon: String?,
            val name: String,
            val userId: String,
            val content: Comment,
            val identifier: Int,
            val dateTime: String
    )

    /**
     * 댓글 데이터를 읽어옵니다.
     * @exception [be.zvz.kotlininside.http.HttpException] 댓글을 읽어오지 못할 경우, HttpException 발생
     */
    @Throws(HttpException::class)
    fun get(): ReadResult {
        val url = "${ApiUrl.Comment.READ}?id=$gallId&no=$articleId&re_page=$rePage&app_id=${KotlinInside.getInstance().auth.getAppId()}"

        val json = KotlinInside.getInstance().httpInterface.get(Request.redirectUrl(url), Request.getDefaultOption())!!.index(0)

        return ReadResult(
                totalComment = json.get("total_comment").`as`(Int::class.java),
                totalPage = json.get("total_page").`as`(Int::class.java),
                rePage = json.get("re_page").`as`(Int::class.java),
                commentList = mutableListOf<CommentData>().apply {
                    json.get("comment_list").values().forEach {
                        add(
                                CommentData(
                                        memberIcon = it.get("member_icon").`as`(Int::class.java),
                                        ipData = it.get("ipData").text(),
                                        gallerCon = it.safeGet("gallercon").run {
                                            when {
                                                isNull -> null
                                                else -> text()
                                            }
                                        },
                                        name = it.get("name").text(),
                                        userId = it.get("user_id").text(),
                                        content = it.safeGet("dccon").run {
                                            when {
                                                isNull -> {
                                                    GenericComment(
                                                            memo = it.get("comment_memo").text()
                                                    )
                                                }
                                                else -> {
                                                    DCConComment(
                                                            dcCon = DCCon(
                                                                    imgLink = text(),
                                                                    memo = it.get("comment_memo").text(),
                                                                    detailIndex = it.get("dccon_detail_idx").`as`(Int::class.java)
                                                            )
                                                    )
                                                }
                                            }
                                        },
                                        identifier = it.get("comment_no").`as`(Int::class.java),
                                        dateTime = it.get("date_time").text()
                                )
                        )
                    }
                }
        )

    }
}
