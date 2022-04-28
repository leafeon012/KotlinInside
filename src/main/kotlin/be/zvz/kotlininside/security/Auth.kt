package be.zvz.kotlininside.security

import be.zvz.kotlininside.KotlinInside
import be.zvz.kotlininside.http.HttpException
import be.zvz.kotlininside.http.HttpInterface
import be.zvz.kotlininside.http.Request
import be.zvz.kotlininside.json.JsonBrowser
import be.zvz.kotlininside.proto.checkin.CheckinProto
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
    private val refreshDateFormat = SimpleDateFormat("yyyyMMddHH", Locale.US).apply {
        timeZone = seoulTimeZone
    }
    private lateinit var time: String
    private lateinit var formattedTime: String

    lateinit var fcmToken: String
        private set
    lateinit var fid: String
    lateinit var refreshToken: String
    lateinit var androidCheckin: CheckinProto.CheckinResponse

    data class AppCheck(
        val result: Boolean,
        val version: String? = null,
        val notice: Boolean? = null,
        val noticeUpdate: Boolean? = null,
        val date: String? = null
    )

    fun fetchAndroidCheckin(): CheckinProto.CheckinResponse {
        val checkin = CheckinProto.CheckinRequest.newBuilder()
            .setAndroidId(0)
            .setCheckin(
                CheckinProto.CheckinRequest.Checkin.newBuilder()
                    .setBuild(
                        CheckinProto.CheckinRequest.Checkin.Build.newBuilder()
                            .setFingerprint("google/razor/flo:7.1.1/NMF26Q/1602158:user/release-keys")
                            .setHardware("flo")
                            .setBrand("google")
                            .setRadio("FLO-04.04")
                            .setClientId("android-google")
                            .setSdkVersion(25)
                            .build()
                    )
                    .setLastCheckinMs(0)
                    .build()
            )
            .setLocale("ko")
            .addMacAddress(RandomStringUtils.random(12, "ABCDEF0123456789"))
            .setMeid(RandomStringUtils.randomNumeric(15))
            .setTimeZone("KST")
            .setVersion(3)
            .addOtaCert("--no-output--")
            .addMacAddressType("wifi")
            .setFragment(0)
            .setUserSerialNumber(0)
            .build()

        val request = URL(ApiUrl.PlayService.CHECKIN).openConnection() as HttpURLConnection
        request.requestMethod = "POST"
        request.doOutput = true
        request.setRequestProperty("Content-Type", "application/x-protobuf")
        request.setRequestProperty("User-Agent", "Android-Checkin/3.0")
        request.outputStream.use {
            BufferedOutputStream(it).use(checkin::writeTo)
        }
        return request.inputStream.use {
            BufferedInputStream(it).use(CheckinProto.CheckinResponse::parseFrom)
        }
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
        androidCheckin = fetchAndroidCheckin()
        val register3 = KotlinInside.getInstance().httpInterface.post(
            ApiUrl.PlayService.REGISTER3,
            HttpInterface.Option()
                .addHeader("Authorization", "AidLogin ${androidCheckin.androidId}:${androidCheckin.securityToken}")
                .setUserAgent(Const.Register3.USER_AGENT)
                .addBodyParameter("sender", Const.Register3.SENDER)
                .addBodyParameter("X-appid", fid)
                .addBodyParameter("X-scope", Const.Register3.X_SCOPE)
                .addBodyParameter("X-app_ver_name", Const.DC_APP_VERSION_NAME)
                .addBodyParameter("X-Goog-Firebase-Installations-Auth", token)
                .addBodyParameter("X-gmp_app_id", Const.Firebase.APP_ID)
                .addBodyParameter("X-firebase-app-name-hash", Const.Register3.X_FIREBASE_APP_NAME_HASH)
                .addBodyParameter("app", Const.Register3.APP)
                .addBodyParameter("device", androidCheckin.androidId.toString())
                .addBodyParameter("app_ver", Const.DC_APP_VERSION_CODE)
                .addBodyParameter("gcm_ver", Const.Register3.GCM_VERSION)
                .addBodyParameter("cert", Const.Register3.CERT)
        )!!
        return register3.split('=')[1]
    }

    /**
     * app_check에서 정보를 얻어오는 메소드입니다.
     * @return [AppCheck] AppCheck 또는 null을 반환합니다.
     * @exception [HttpException] app_check에 접근 할 수 없는 경우, HttpException이 발생합니다.
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

    private fun getDayOfWeekMonday(day: Int): Int {
        return when (day) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }

    /**
     *
     * @return [java.lang.String] Fri332295548112911 형식의 날짜 문자열을 반환합니다.
     */
    private fun dateToString(date: Date): String {
        val calendar = Calendar.getInstance(seoulTimeZone, Locale.US)
        calendar.minimalDaysInFirstWeek = 4
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.time = date

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
        }.format(date)
    }

    /**
     * SHA256 단방향 암호화된 value_token을 서버로부터 얻어오거나, 생성하는 메소드입니다.
     * @return [java.lang.String] value_token을 반환합니다.
     */
    fun generateHashedAppKey(): String {
        val now = Date()
        val tempFormattedTime = refreshDateFormat.format(now)

        if (!::time.isInitialized || !::formattedTime.isInitialized || formattedTime != tempFormattedTime) {
            try {
                getAppCheck().run {
                    date?.let {
                        formattedTime = tempFormattedTime
                        time = it
                        return String(Hex.encodeHex(DigestUtils.sha256("dcArdchk_$time")))
                    }
                }
            } catch (_: Exception) {
            }
        } else {
            return String(Hex.encodeHex(DigestUtils.sha256("dcArdchk_$time")))
        }

        // 디시인사이드 2019/10/31 변경점 - Fri332295548112911 형식으로 변경됨
        // 예외가 발생했거나, 값이 null이어서 time을 제대로 설정하지 못한 경우
        formattedTime = tempFormattedTime
        time = dateToString(now)
        return String(Hex.encodeHex(DigestUtils.sha256("dcArdchk_$time")))
    }

    /**
     * 캐시된 app_id를 얻어오는 메소드입니다.
     * @return [java.lang.String] app_id를 반환합니다.
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
     * app_id를 서버로부터 얻어오는 메소드입니다.
     * @exception [java.lang.NullPointerException] app_id를 얻어올 수 없는 경우, NullPointerException이 발생합니다.
     * @param hashedAppKey SHA256 단방향 암호화된 value_token 값입니다.
     * @return [java.lang.String] app_id를 반환합니다.
     */
    @Throws(HttpException::class)
    fun fetchAppId(hashedAppKey: String): String {
        fcmToken = fetchFcmToken()

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
     * 로그인하기 위해 필요한 메소드
     *
     * @exception HttpException 계정에 로그인 할 수 없는 경우 HttpException 발생
     * @param user [be.zvz.kotlininside.session.user.Anonymous]와 [be.zvz.kotlininside.session.user.LoginUser] 클래스를 매개변수로 받음
     * @return [be.zvz.kotlininside.session.Session] 로그인에 성공했거나, 유동닉([be.zvz.kotlininside.session.user.Anonymous]) 객체를 담고있는 세션을 반환함
     */
    @Throws(HttpException::class)
    fun login(user: User): Session {
        if (user !is Anonymous) {
            val option = Request.getDefaultOption()
                .addMultipartParameter("user_id", user.id)
                .addMultipartParameter("user_pw", user.password)
                .addMultipartParameter("mode", "login_normal")
                .addMultipartParameter("client_token", fcmToken)

            val json = JsonBrowser.parse(
                KotlinInside.getInstance().httpInterface.upload(
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
                    throw HttpException(401, "계정의 타입을 알 수 없습니다.")
                }
            }

            return Session(loginUser, detail)
        } else {
            return Session(user, null)
        }
    }
}
