package io.tiangou.api.data

import kotlinx.serialization.Serializable

@Serializable
data class ThirdPartyValorantApiResponse<T>(
    val status: Int,
    val data: T
)

@Serializable
data class WeaponSkinResponse(
    val uuid: String,
    val displayName: String,
    val themeUuid: String,
    val contentTierUuid: String?,
    val displayIcon: String?,
    val assetPath: String,
    val chromas: List<WeaponSkinChromaResponse>,
    val levels: List<WeaponSkinLevelResponse>,
)

@Serializable
data class WeaponSkinChromaResponse(
    val uuid: String,
    val displayName: String,
    val displayIcon: String?,
    val fullRender: String,
    val swatch: String?,
    val assetPath: String,
)

@Serializable
data class WeaponSkinLevelResponse(
    val uuid: String,
    val displayName: String,
    val displayIcon: String?,
    val streamedVideo: String?,
    val assetPath: String,
    val levelItem: String?
)

@Serializable
data class ContentTiersResponse(
    val assetPath: String,
    val devName: String?,
    val displayIcon: String?,
    val displayName: String?,
    val highlightColor: String?,
    val juiceCost: Int?,
    val juiceValue: Int?,
    val rank: Int?,
    val uuid: String
)

@Serializable
data class ThemeResponse(
    val assetPath: String,
    val displayIcon: String?,
    val displayName: String?,
    val uuid: String
)
