package be.zvz.kotlininside.security

import be.zvz.kotlininside.KotlinInside
import be.zvz.kotlininside.http.HttpException
import be.zvz.kotlininside.http.HttpInterface
import be.zvz.kotlininside.http.Request
import be.zvz.kotlininside.json.JsonBrowser
import be.zvz.kotlininside.proto.checkin.CheckinProto
import be.zvz.kotlininside.proto.checkin.CheckinRequestKt.CheckinKt.build
import be.zvz.kotlininside.proto.checkin.CheckinRequestKt.checkin
import be.zvz.kotlininside.proto.checkin.checkinRequest
import be.zvz.kotlininside.session.Session
import be.zvz.kotlininside.session.SessionDetail
import be.zvz.kotlininside.session.user.Anonymous
import be.zvz.kotlininside.session.user.User
import be.zvz.kotlininside.session.user.UserType
import be.zvz.kotlininside.session.user.named.DuplicateNamed
import be.zvz.kotlininside.session.user.named.Named
import be.zvz.kotlininside.value.ApiUrl
import be.zvz.kotlininside.value.Const
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.lang3.RandomStringUtils
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class Auth {
    private val seoulTimeZone = TimeZone.getTimeZone("Asia/Seoul")
    private lateinit var time: String
    private lateinit var lastRefreshTime: Calendar

    var fcmToken: String = ""
        private set
    var fid: String? = null
    var refreshToken: String? = null

    data class AppCheck(
        val result: Boolean,
        val version: String? = null,
        val notice: Boolean? = null,
        val noticeUpdate: Boolean? = null,
        val date: String? = null
    )

    fun generateAidLoginFromCheckin(checkinRes: CheckinProto.CheckinResponse): String =
        "AidLogin ${checkinRes.androidId}:${checkinRes.securityToken}"

    fun fetchAndroidCheckin(): CheckinProto.CheckinResponse {
        val checkinReq = checkinRequest {
            androidId = 0
            checkin = checkin {
                build = build {
                    fingerprint = "google/razor/flo:7.1.1/NMF26Q/1602158:user/release-keys"
                    hardware = "flo"
                    brand = "google"
                    radio = "FLO-04.04"
                    clientId = "android-google"
                    sdkVersion = 25
                }
                lastCheckinMs = 0
            }
            locale = "ko"
            macAddress.add(RandomStringUtils.random(12, "ABCDEF0123456789"))
            meid = RandomStringUtils.randomNumeric(15)
            timeZone = TimeZone.getDefault().getDisplayName(true, TimeZone.SHORT)
            version = 3
            otaCert.add("--no-output--")
            macAddressType.add("wifi")
            fragment = 0
            userSerialNumber = 0
        }

        val request = (URL(ApiUrl.PlayService.CHECKIN).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/x-protobuf")
            setRequestProperty("User-Agent", "Android-Checkin/3.0")
        }

        BufferedOutputStream(request.outputStream).use(checkinReq::writeTo)
        return BufferedInputStream(request.inputStream).use(CheckinProto.CheckinResponse::parseFrom)
    }

    private fun requestToGcmWithScope(androidCheckin: CheckinProto.CheckinResponse, clientToken: String, installationToken: String, scope: String) {
        KotlinInside.getInstance().httpInterface.post(
            ApiUrl.PlayService.REGISTER3,
            HttpInterface.Option()
                .addHeader("Authorization", generateAidLoginFromCheckin(androidCheckin))
                .setUserAgent(Const.Register3.USER_AGENT)
                .addBodyParameter("X-subtype", clientToken)
                .addBodyParameter("sender", clientToken)
                .addBodyParameter("X-gcm.topic", scope)
                .addBodyParameter("X-app_ver", Const.DC_APP_VERSION_CODE)
                .addBodyParameter("X-appid", fid ?: "")
                .addBodyParameter("X-scope", scope)
                .addBodyParameter("X-Goog-Firebase-Installations-Auth", installationToken)
                .addBodyParameter("X-gmp_app_id", Const.Firebase.APP_ID)
                .addBodyParameter("X-firebase-app-name-hash", Const.Register3.X_FIREBASE_APP_NAME_HASH)
                .addBodyParameter("X-app_ver_name", Const.DC_APP_VERSION_NAME)
                .addBodyParameter("app", Const.Register3.APP)
                .addBodyParameter("device", androidCheckin.androidId.toString())
                .addBodyParameter("app_ver", Const.DC_APP_VERSION_CODE)
                .addBodyParameter("cert", Const.Register3.CERT)
        )
    }

    @JvmOverloads
    fun fetchFcmToken(argFid: String? = null, argRefreshToken: String? = null): String {
        val firebaseInstallations = JsonBrowser.parse(
            KotlinInside.getInstance().httpInterface.post(
                ApiUrl.Firebase.INSTALLATIONS,
                HttpInterface.Option()
                    .addHeader("X-Android-Package", Const.Installations.X_ANDROID_PACKAGE)
                    .addHeader("X-Android-Cert", Const.Installations.X_ANDROID_CERT)
                    .addHeader("x-goog-api-key", Const.Installations.X_GOOG_API_KEY)
                    .setContentTypeAndBody(
                        "application/json",
                        JsonBrowser.getMapper().writeValueAsString(
                            JsonBrowser.getMapper().createObjectNode().apply {
                                argFid?.let {
                                    put("fid", it)
                                }
                                argRefreshToken?.let {
                                    put("refreshToken", it)
                                }
                                put("appId", Const.Firebase.APP_ID)
                                put("authVersion", Const.Firebase.AUTH_VERSION)
                                put("sdkVersion", Const.Firebase.SDK_VERSION)
                            }
                        )
                    )
            )
        )

        fid = firebaseInstallations.get("fid").safeText()
        refreshToken = firebaseInstallations.get("refreshToken").safeText()
        val token = firebaseInstallations.get("authToken").get("token").safeText()
        val androidCheckin = fetchAndroidCheckin()

        val register3 = KotlinInside.getInstance().httpInterface.post(
            ApiUrl.PlayService.REGISTER3,
            HttpInterface.Option()
                .addHeader("Authorization", generateAidLoginFromCheckin(androidCheckin))
                .setUserAgent(Const.Register3.USER_AGENT)
                .addBodyParameter("X-subtype", Const.Register3.SENDER)
                .addBodyParameter("sender", Const.Register3.SENDER)
                .addBodyParameter("X-app_ver", Const.DC_APP_VERSION_CODE)
                .addBodyParameter("X-appid", fid ?: "")
                .addBodyParameter("X-scope", Const.Register3.X_SCOPE_ALL)
                .addBodyParameter("X-Goog-Firebase-Installations-Auth", token)
                .addBodyParameter("X-gmp_app_id", Const.Firebase.APP_ID)
                .addBodyParameter("X-firebase-app-name-hash", Const.Register3.X_FIREBASE_APP_NAME_HASH)
                .addBodyParameter("X-app_ver_name", Const.DC_APP_VERSION_NAME)
                .addBodyParameter("app", Const.Register3.APP)
                .addBodyParameter("device", androidCheckin.androidId.toString())
                .addBodyParameter("app_ver", Const.DC_APP_VERSION_CODE)
                .addBodyParameter("gcm_ver", Const.Register3.GCM_VERSION)
                .addBodyParameter("cert", Const.Register3.CERT)
        ) ?: throw RuntimeException("Can't get client_token")

        val clientToken = register3.split('=')[1]

        requestToGcmWithScope(androidCheckin, clientToken, token, Const.Register3.X_SCOPE_REFRESH_REMOTE_CONFIG)
        requestToGcmWithScope(androidCheckin, clientToken, token, Const.Register3.X_SCOPE_SHOW_NOTICE_MESSAGE)

        return clientToken
    }

    /**
     * app_check?????? ????????? ???????????? ??????????????????.
     * @return [AppCheck] AppCheck ?????? null??? ???????????????.
     * @exception [HttpException] app_check??? ?????? ??? ??? ?????? ??????, HttpException??? ???????????????.
     */
    fun getAppCheck(): AppCheck {
        val appCheck = JsonBrowser.parse(
            KotlinInside.getInstance().httpInterface.get(
                ApiUrl.Auth.APP_CHECK,
                Request.getDefaultOption()
            )
        )

        if (!appCheck.get("result").isNull)
            return AppCheck(
                result = appCheck.get("result").asBoolean()
            )

        val json = appCheck.index(0)

        return AppCheck(
            result = json.get("result").asBoolean(),
            version = json.get("ver").text(),
            notice = json.get("notice").asBoolean(),
            noticeUpdate = json.get("notice_update").asBoolean(),
            date = json.get("date").text()
        )
    }

    private fun getDayOfWeekMonday(day: Int): Int = when (day) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        Calendar.SUNDAY -> 7
        else -> 1
    }

    /**
     *
     * @return [java.lang.String] Fri332295548112911 ????????? ?????? ???????????? ???????????????.
     */
    private fun dateToString(calendar: Calendar): String {
        val dayOfYear = calendar[Calendar.DAY_OF_YEAR]
        val dayOfWeek = calendar[Calendar.DAY_OF_WEEK]
        val weekOfYear = calendar[Calendar.WEEK_OF_YEAR]

        return SimpleDateFormat(
            "E${dayOfYear - 1}d${getDayOfWeekMonday(dayOfWeek)}${dayOfWeek - 1}${
            String.format(
                "%02d",
                weekOfYear
            )
            }MddMM",
            Locale.US
        ).apply {
            timeZone = seoulTimeZone
        }.format(calendar.time)
    }

    private fun needsRefresh(old: Calendar, new: Calendar): Boolean = old[Calendar.YEAR] != new[Calendar.YEAR] ||
        old[Calendar.MONTH] != new[Calendar.MONTH] ||
        old[Calendar.DAY_OF_MONTH] != new[Calendar.DAY_OF_MONTH] ||
        old[Calendar.HOUR_OF_DAY] != new[Calendar.HOUR_OF_DAY]

    /**
     * SHA256 ????????? ???????????? value_token??? ??????????????? ???????????????, ???????????? ??????????????????.
     * @return [java.lang.String] value_token??? ???????????????.
     */
    fun generateHashedAppKey(): String {
        val now = Calendar.getInstance(seoulTimeZone, Locale.US).apply {
            minimalDaysInFirstWeek = 4
            firstDayOfWeek = Calendar.MONDAY
        }

        if (!::time.isInitialized || !::lastRefreshTime.isInitialized || needsRefresh(lastRefreshTime, now)) {
            try {
                getAppCheck().run {
                    date?.let {
                        lastRefreshTime = now
                        time = it
                        return String(Hex.encodeHex(DigestUtils.sha256("dcArdchk_$time")))
                    }
                }
            } catch (_: Exception) {
            }
        } else {
            return String(Hex.encodeHex(DigestUtils.sha256("dcArdchk_$time")))
        }

        // ?????????????????? 2019/10/31 ????????? - Fri332295548112911 ???????????? ?????????
        // ????????? ???????????????, ?????? null????????? time??? ????????? ???????????? ?????? ??????
        lastRefreshTime = now
        time = dateToString(now)
        return String(Hex.encodeHex(DigestUtils.sha256("dcArdchk_$time")))
    }

    /**
     * ????????? app_id??? ???????????? ??????????????????.
     * @return [java.lang.String] app_id??? ???????????????.
     */
    fun getAppId(): String = when (val hashedAppKey = generateHashedAppKey()) {
        KotlinInside.getInstance().app.token -> KotlinInside.getInstance().app.id
        else -> {
            KotlinInside.getInstance().app = App(
                token = hashedAppKey,
                id = fetchAppId(hashedAppKey)
            )
            KotlinInside.getInstance().app.id
        }
    }

    /**
     * app_id??? ??????????????? ???????????? ??????????????????.
     * @exception [java.lang.NullPointerException] app_id??? ????????? ??? ?????? ??????, NullPointerException??? ???????????????.
     * @param hashedAppKey SHA256 ????????? ???????????? value_token ????????????.
     * @return [java.lang.String] app_id??? ???????????????.
     */
    @Throws(HttpException::class)
    fun fetchAppId(hashedAppKey: String): String {
        fcmToken = fetchFcmToken(fid, refreshToken)

        val option = Request.getDefaultOption()
            .addMultipartParameter("value_token", hashedAppKey)
            .addMultipartParameter("signature", Const.DC_APP_SIGNATURE)
            .addMultipartParameter("pkg", Const.DC_APP_PACKAGE)
            .addMultipartParameter("vCode", Const.DC_APP_VERSION_CODE)
            .addMultipartParameter("vName", Const.DC_APP_VERSION_NAME)
            .addMultipartParameter("client_token", fcmToken)

        val appId = JsonBrowser.parse(KotlinInside.getInstance().httpInterface.upload(ApiUrl.Auth.APP_ID, option))

        return appId.get("app_id").text() ?: throw HttpException(RuntimeException("Can't get app_id: ${appId.text()}"))
    }

    /**
     * ??????????????? ?????? ????????? ?????????
     *
     * @exception HttpException ????????? ????????? ??? ??? ?????? ?????? HttpException ??????
     * @param user [be.zvz.kotlininside.session.user.Anonymous]??? [be.zvz.kotlininside.session.user.LoginUser] ???????????? ??????????????? ??????
     * @return [be.zvz.kotlininside.session.Session] ???????????? ???????????????, ?????????([be.zvz.kotlininside.session.user.Anonymous]) ????????? ???????????? ????????? ?????????
     */
    @Throws(HttpException::class)
    fun login(user: User): Session {
        if (user !is Anonymous) {
            val option = HttpInterface.Option()
                .setUserAgent("com.dcinside.mobileapp")
                .addHeader("Referer", "http://www.dcinside.com")
                .addBodyParameter("client_token", fcmToken)
                .addBodyParameter("mode", "login_quick")
                .addBodyParameter("user_id", user.id)
                .addBodyParameter("user_pw", user.password)

            val json = JsonBrowser.parse(
                KotlinInside.getInstance().httpInterface.post(
                    ApiUrl.Auth.LOGIN,
                    option
                )
            )

            val detail = SessionDetail(
                result = json.get("result").asBoolean(),
                userId = json.get("user_id").safeText(),
                userNo = json.get("user_no").safeText(),
                name = json.get("name").safeText(),
                sessionType = json.get("stype").safeText(),
                isAdult = json.get("is_adult").asInteger(),
                isDormancy = json.get("is_dormancy").asInteger(),
                isOtp = json.get("is_otp").asInteger(),
                pwCampaign = json.get("pw_campaign").asInteger(),
                mailSend = json.get("mail_send").safeText(),
                isGonick = json.get("is_gonick").asInteger(),
                isSecurityCode = json.get("is_security_code").safeText(),
                authChange = json.get("auth_change").asInteger(),
                cause = json.get("cause").text(),
            )

            if (!detail.result) {
                throw HttpException(401, detail.cause)
            }

            val loginUser = when (detail.sessionType) {
                UserType.NAMED.sessionType -> {
                    Named(user.id, user.password)
                }
                UserType.DUPLICATE_NAMED.sessionType -> {
                    DuplicateNamed(user.id, user.password)
                }
                else -> {
                    throw HttpException(401, "????????? ????????? ??? ??? ????????????.")
                }
            }

            return Session(loginUser, detail)
        } else {
            return Session(user, null)
        }
    }
}
