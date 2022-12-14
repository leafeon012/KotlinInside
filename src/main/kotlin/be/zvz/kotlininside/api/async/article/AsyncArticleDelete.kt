package be.zvz.kotlininside.api.async.article

import be.zvz.kotlininside.KotlinInside
import be.zvz.kotlininside.api.article.ArticleDelete
import be.zvz.kotlininside.session.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class AsyncArticleDelete @JvmOverloads constructor(
    gallId: String,
    articleId: Int,
    session: Session,
    fcmToken: String = KotlinInside.getInstance().auth.fcmToken
) {
    private val articleDelete = ArticleDelete(gallId, articleId, session, fcmToken)

    suspend fun deleteAsync() = coroutineScope {
        async(Dispatchers.IO) {
            articleDelete.delete()
        }
    }
}
