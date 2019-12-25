package be.zvz.kotlininside.api.article

import be.zvz.kotlininside.KotlinInside
import be.zvz.kotlininside.api.type.Article
import be.zvz.kotlininside.api.type.HeadText
import be.zvz.kotlininside.api.type.content.Content
import be.zvz.kotlininside.api.type.content.HtmlContent
import be.zvz.kotlininside.api.type.content.ImageContent
import be.zvz.kotlininside.http.Request
import be.zvz.kotlininside.session.Session
import be.zvz.kotlininside.session.user.Anonymous
import be.zvz.kotlininside.value.ApiUrl
import org.apache.commons.text.StringEscapeUtils
import java.io.BufferedInputStream
import java.net.URL


class ArticleModify(
        private val gallId: String,
        private val articleId: Int,
        private val session: Session
) {
    data class FileData(
            val block: Int,
            val fileSize: Int
    )

    data class ModifyResult(
            val result: Boolean,
            val gallId: String? = null,
            val articleId: Int = 0,
            val fileCount: Int = 0,
            val fileSize: Int = 0,
            val subject: String? = null,
            val content: List<Content> = mutableListOf(),
            val file: List<FileData> = mutableListOf(),
            val headText: List<HeadText> = mutableListOf(),
            val currentHeadText: String? = null,
            val cause: String? = null
    )

    fun modifyInfo(): ModifyResult {
        val option = Request.getDefaultOption()

        if (session.user is Anonymous) {
            option.addMultipartParameter("password", session.user.password)
        } else {
            option.addMultipartParameter("user_id", session.detail!!.userId)
        }

        option
                .addMultipartParameter("id", gallId)
                .addMultipartParameter("no", articleId.toString())
                .addMultipartParameter("app_id", KotlinInside.getInstance().auth.getAppId())

        val json = KotlinInside.getInstance().httpInterface.upload(
                ApiUrl.Article.MODIFY,
                option
        )!!.index(0)

        val result = json.get("result").`as`(Boolean::class.java)

        if (result) {
            return ModifyResult(
                    result = result,
                    gallId = json.get("gall_id").text(),
                    articleId = json.get("gall_no").`as`(Int::class.java),
                    fileCount = json.get("file_cnt").`as`(Int::class.java),
                    fileSize = json.get("file_size").`as`(Int::class.java),
                    subject = json.get("subject").text(),
                    content = mutableListOf<Content>().apply {
                        json.get("memo").values().forEach {
                            it.safeGet("tag_value").let { tagValue ->
                                when {
                                    tagValue.isNull -> {
                                        it.toMap<String, String>().forEach { (_, value) ->
                                            add(
                                                    HtmlContent(
                                                            htmlString = StringEscapeUtils.unescapeHtml4(value)
                                                    )
                                            )
                                        }
                                    }
                                    else -> {
                                        it.toMap<String, String>().forEach { (key, value) ->
                                            if (key != "tag_value") {
                                                val inputStream = BufferedInputStream(URL(StringEscapeUtils.unescapeHtml4(value)).openStream())
                                                add(
                                                        ImageContent(
                                                                stream = inputStream
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    file = mutableListOf<FileData>().apply {
                        json.get("file").values().forEach {
                            var block = 0
                            var fileSize = 0

                            var count = 0
                            it.toMap<String, String>().forEach { (_, value) ->
                                if (count == 0) {
                                    block = value.toInt()
                                } else {
                                    fileSize = value.toInt()
                                }

                                count++
                            }

                            add(
                                    FileData(
                                            block = block,
                                            fileSize = fileSize
                                    )
                            )
                        }
                    },
                    headText = mutableListOf<HeadText>().apply {
                        json.safeGet("head_text").let { jsonHeadText ->
                            when {
                                !jsonHeadText.isNull -> jsonHeadText.values().forEach {
                                    add(
                                            HeadText(
                                                    identifier = it.get("no").`as`(Int::class.java),
                                                    name = it.get("name").text(),
                                                    level = it.get("level").`as`(Int::class.java),
                                                    selected = it.get("selected").`as`(Boolean::class.java)
                                            )
                                    )
                                }
                            }
                        }
                    },
                    currentHeadText = json.safeGet("headtext").run {
                        when {
                            isNull -> null
                            else -> text()
                        }
                    }
            )
        } else {
            return ModifyResult(
                    result = result,
                    cause = json.get("cause").text()
            )
        }
    }

    @JvmOverloads
    fun modify(article: Article, fcmToken: String = "N"): ArticleWrite.WriteResult {
        val articleWrite = ArticleWrite(
                gallId = gallId,
                article = article,
                session = session,
                mode = "modify",
                fcmToken = fcmToken
        )

        return articleWrite.write()
    }
}