package be.zvz.kotlininside.api.management

import be.zvz.kotlininside.KotlinInside
import be.zvz.kotlininside.exception.InsufficientPermissionException
import be.zvz.kotlininside.http.Request
import be.zvz.kotlininside.json.JsonBrowser
import be.zvz.kotlininside.session.Session
import be.zvz.kotlininside.session.user.Anonymous
import be.zvz.kotlininside.value.ApiUrl
import be.zvz.kotlininside.value.Const
import java.io.IOException

class UserBlock @JvmOverloads constructor(
    private val gallId: String,
    private val articleId: Int,
    private val session: Session,
    private val option: BlockOption = BlockOption()
) {
    enum class BlockCategory(val code: Int) {
        OBSCENE(1),
        ADVERTISEMENT(2),
        CUSS_WORDS(3),
        SPAMMING(4),
        PIRACY(5),
        DEFAMATION(6),
        CUSTOM(7)
    }

    class BlockOption {
        var commentId = 0
        var blockHour = 1
        var blockCategory = BlockCategory.CUSTOM
        var blockReason: String = "ėŽė  ėė"
    }

    data class BlockResult(
        val result: Boolean,
        val cause: String
    )

    @Throws(InsufficientPermissionException::class)
    fun block(): BlockResult {
        if (session.user is Anonymous) {
            throw InsufficientPermissionException(UserBlock::class)
        }

        val requestOption = Request.getDefaultOption()
            .addBodyParameter("_token", "")
            .addBodyParameter("avoid_hour", option.blockHour.toString())
            .addBodyParameter("avoid_category", option.blockCategory.code.toString())

        if (option.blockCategory === BlockCategory.CUSTOM) {
            requestOption.addBodyParameter("avoid_memo", option.blockReason)
        }

        requestOption
            .addBodyParameter("id", gallId)
            .addBodyParameter("no", articleId.toString())

        if (option.commentId > 0) {
            requestOption.addBodyParameter("comment_no", option.commentId.toString())
        }

        requestOption
            .addBodyParameter("app_id", KotlinInside.getInstance().auth.getAppId())
            .addBodyParameter("confirm_id", session.detail!!.userId)

        val json: JsonBrowser = try {
            JsonBrowser.parse(
                KotlinInside.getInstance().httpInterface.post(
                    ApiUrl.Gallery.MINOR_BLOCK_ADD,
                    requestOption
                )
            )
        } catch (e: IOException) {
            return BlockResult(
                result = false,
                cause = "ęķíėī ėėĩëëĪ."
            )
        }

        return BlockResult(
            result = json.get("result").asBoolean(),
            cause = json.get("cause").safeText()
        )
    }

    /**
     * ę°ĪëŽëĶŽ ė ė  ė°ĻëĻ ë§íŽëĨž ë°ííĐëëĪ.
     *
     * @throws RuntimeException ė ė  ėļėėī [Anonymous]ėž ęē―ė°, ėėļëĨž ë°ííĐëëĪ.
     * @return ę°ĪëŽëĶŽ ė ė  ė°ĻëĻ URLė ë°ííĐëëĪ.
     */
    fun getLink(): String {
        val url =
            "${ApiUrl.Gallery.MINOR_BLOCK_WEB}?id=$gallId&no=$articleId&app_id=${KotlinInside.getInstance().auth.getAppId()}"

        if (option.commentId > 0) {
            url.plus("&comment_no=${option.commentId}")
        }

        if (session.user is Anonymous) {
            throw RuntimeException("Anonymousë ę°ĪëŽëĶŽ ė ė  ė°ĻëĻė ėŽėĐí  ė ėėĩëëĪ.")
        } else {
            url.plus("&confirm_id=${session.detail!!.userId}")
        }

        return url
    }

    /**
     * User-AgentëĨž ë°ííĐëëĪ.
     *
     * @return ę°ĪëŽëĶŽ ė ė  ė°ĻëĻė ė ę·ží  ë íėí User-AgentëĨž ë°ííĐëëĪ.
     */
    fun getUserAgent(): String {
        return Const.USER_AGENT
    }
}
