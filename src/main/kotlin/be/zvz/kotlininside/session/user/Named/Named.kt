package be.zvz.kotlininside.session.user.named

import be.zvz.kotlininside.session.user.LoginUser

data class Named(
        override val id: String,
        override val password: String
) : LoginUser(id, password)