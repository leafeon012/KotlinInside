---
title: AsyncArticleDelete
---
//[KotlinInside](../../../index.html)/[be.zvz.kotlininside.api.async.article](../index.html)/[AsyncArticleDelete](index.html)



# AsyncArticleDelete



[jvm]\
class [AsyncArticleDelete](index.html)@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)constructor(gallId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), articleId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), session: [Session](../../be.zvz.kotlininside.session/-session/index.html), fcmToken: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = KotlinInside.getInstance().auth.fcmToken)



## Constructors


| | |
|---|---|
| [AsyncArticleDelete](-async-article-delete.html) | [jvm]<br>@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)<br>fun [AsyncArticleDelete](-async-article-delete.html)(gallId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), articleId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), session: [Session](../../be.zvz.kotlininside.session/-session/index.html), fcmToken: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = KotlinInside.getInstance().auth.fcmToken) |


## Functions


| Name | Summary |
|---|---|
| [deleteAsync](delete-async.html) | [jvm]<br>suspend fun [deleteAsync](delete-async.html)(): Deferred&lt;[ArticleDelete.DeleteResult](../../be.zvz.kotlininside.api.article/-article-delete/-delete-result/index.html)&gt; |

