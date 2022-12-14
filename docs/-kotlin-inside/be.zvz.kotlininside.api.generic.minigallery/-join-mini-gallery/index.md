---
title: JoinMiniGallery
---
//[KotlinInside](../../../index.html)/[be.zvz.kotlininside.api.generic.minigallery](../index.html)/[JoinMiniGallery](index.html)



# JoinMiniGallery



[jvm]\
class [JoinMiniGallery](index.html)(gallId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), session: [Session](../../be.zvz.kotlininside.session/-session/index.html))



## Constructors


| | |
|---|---|
| [JoinMiniGallery](-join-mini-gallery.html) | [jvm]<br>fun [JoinMiniGallery](-join-mini-gallery.html)(gallId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), session: [Session](../../be.zvz.kotlininside.session/-session/index.html)) |


## Types


| Name | Summary |
|---|---|
| [MemberJoinOkResult](-member-join-ok-result/index.html) | [jvm]<br>data class [MemberJoinOkResult](-member-join-ok-result/index.html)(val result: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), val cause: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val status: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |
| [MemberJoinResult](-member-join-result/index.html) | [jvm]<br>data class [MemberJoinResult](-member-join-result/index.html)(val result: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), val joinQuestion: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |


## Functions


| Name | Summary |
|---|---|
| [join](join.html) | [jvm]<br>fun [join](join.html)(): [Pair](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-pair/index.html)&lt;[JoinMiniGallery.MemberJoinResult](-member-join-result/index.html), [JoinMiniGallery.MemberJoinOkResult](-member-join-ok-result/index.html)&gt; |
| [requestMemberJoin](request-member-join.html) | [jvm]<br>fun [requestMemberJoin](request-member-join.html)(): [JoinMiniGallery.MemberJoinResult](-member-join-result/index.html) |
| [requestMemberJoinOk](request-member-join-ok.html) | [jvm]<br>fun [requestMemberJoinOk](request-member-join-ok.html)(): [JoinMiniGallery.MemberJoinOkResult](-member-join-ok-result/index.html) |

