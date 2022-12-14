package be.zvz.kotlininside.api.management

import be.zvz.kotlininside.KotlinInside
import be.zvz.kotlininside.exception.InsufficientPermissionException
import be.zvz.kotlininside.session.Session
import be.zvz.kotlininside.session.user.Anonymous
import be.zvz.kotlininside.value.ApiUrl
import be.zvz.kotlininside.value.Const

class GallerySetting(
    private val gallId: String,
    private val session: Session
) {
    /**
     * 갤러리 관리 링크를 반환합니다.
     *
     * @throws InsufficientPermissionException 유저 세션이 [Anonymous]일 경우, 예외를 반환합니다.
     * @return 갤러리 관리 페이지 URL
     */
    @Throws(InsufficientPermissionException::class)
    fun getLink(): String {
        val url = "${ApiUrl.Gallery.MINOR_MANAGEMENT}?id=$gallId&app_id=${KotlinInside.getInstance().auth.getAppId()}"

        if (session.user is Anonymous) {
            throw InsufficientPermissionException(GallerySetting::class)
        } else {
            url.plus("&confirm_id=${session.detail!!.userId}")
        }

        return url
    }

    /**
     * User-Agent를 반환합니다.
     *
     * @return 갤러리 관리에 접근할 때 필요한 User-Agent를 반환합니다.
     */
    fun getUserAgent(): String {
        return Const.USER_AGENT
    }
}
