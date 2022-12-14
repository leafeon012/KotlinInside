---
title: AsyncJoinMiniGallery
---
//[KotlinInside](../../../index.html)/[be.zvz.kotlininside.api.async.generic.minigallery](../index.html)/[AsyncJoinMiniGallery](index.html)



# AsyncJoinMiniGallery



[jvm]\
class [AsyncJoinMiniGallery](index.html)(gallId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), session: [Session](../../be.zvz.kotlininside.session/-session/index.html))



## Constructors


| | |
|---|---|
| [AsyncJoinMiniGallery](-async-join-mini-gallery.html) | [jvm]<br>fun [AsyncJoinMiniGallery](-async-join-mini-gallery.html)(gallId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), session: [Session](../../be.zvz.kotlininside.session/-session/index.html)) |


## Functions


| Name | Summary |
|---|---|
| [joinAsync](join-async.html) | [jvm]<br>suspend fun [joinAsync](join-async.html)(): Deferred&lt;[Pair](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-pair/index.html)&lt;[JoinMiniGallery.MemberJoinResult](../../be.zvz.kotlininside.api.generic.minigallery/-join-mini-gallery/-member-join-result/index.html), [JoinMiniGallery.MemberJoinOkResult](../../be.zvz.kotlininside.api.generic.minigallery/-join-mini-gallery/-member-join-ok-result/index.html)&gt;&gt; |
| [requestMemberJoinAsync](request-member-join-async.html) | [jvm]<br>suspend fun [requestMemberJoinAsync](request-member-join-async.html)(): Deferred&lt;[JoinMiniGallery.MemberJoinResult](../../be.zvz.kotlininside.api.generic.minigallery/-join-mini-gallery/-member-join-result/index.html)&gt; |
| [requestMemberJoinOkAsync](request-member-join-ok-async.html) | [jvm]<br>suspend fun [requestMemberJoinOkAsync](request-member-join-ok-async.html)(): Deferred&lt;[JoinMiniGallery.MemberJoinOkResult](../../be.zvz.kotlininside.api.generic.minigallery/-join-mini-gallery/-member-join-ok-result/index.html)&gt; |

