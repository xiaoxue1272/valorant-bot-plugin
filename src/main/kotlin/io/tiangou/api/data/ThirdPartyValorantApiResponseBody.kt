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

@Serializable
data class ContractResponse(
    val assetPath: String,
    val content: Content,
    val displayIcon: String?,
    val displayName: String,
    val uuid: String
) {

    @Serializable
    data class Content(
        val chapters: List<Chapter>,
        val premiumRewardScheduleUuid: String?,
        val premiumVPCost: Int,
        val relationType: String?,
        val relationUuid: String?
    ) {

        @Serializable
        data class Chapter(
            val freeRewards: List<FreeReward>?,
            val isEpilogue: Boolean,
            val levels: List<Level>
        ) {

            @Serializable
            data class FreeReward(
                val amount: Int,
                val isHighlighted: Boolean,
                val type: String,
                val uuid: String
            )

            @Serializable
            data class Level(
                val doughCost: Int,
                val isPurchasableWithDough: Boolean,
                val isPurchasableWithVP: Boolean,
                val reward: Reward,
                val vpCost: Int,
                val xp: Int
            )

            @Serializable
            data class Reward(
                val amount: Int,
                val isHighlighted: Boolean,
                val type: String,
                val uuid: String
            )

        }

    }

}

@Serializable
data class BuddieResponse(
    val assetPath: String,
    val displayIcon: String,
    val displayName: String,
    val isHiddenIfNotOwned: Boolean,
    val levels: List<Level>,
    val themeUuid: String?,
    val uuid: String
) {

    @Serializable
    data class Level(
        val assetPath: String,
        val charmLevel: Int,
        val displayIcon: String,
        val displayName: String,
        val uuid: String
    )

}

@Serializable
data class CurrencyResponse(
    val assetPath: String,
    val displayIcon: String,
    val displayName: String,
    val displayNameSingular: String,
    val largeIcon: String,
    val uuid: String
)

@Serializable
data class PlayCardResponse(
    val uuid: String,
    val assetPath: String,
    val displayIcon: String,
    val displayName: String,
    val isHiddenIfNotOwned: Boolean,
    val largeArt: String,
    val smallArt: String,
    val themeUuid: String?,
    val wideArt: String
)

@Serializable
data class PlayTitleResponse(
    val uuid: String,
    val assetPath: String,
    val displayName: String?,
    val isHiddenIfNotOwned: Boolean,
    val titleText: String?
)

@Serializable
data class SprayResponse(
    val animationGif: String?,
    val animationPng: String?,
    val assetPath: String,
    val category: String?,
    val displayIcon: String,
    val displayName: String,
    val fullIcon: String?,
    val fullTransparentIcon: String?,
    val isNullSpray: Boolean,
    val levels: List<Level>,
    val themeUuid: String?,
    val uuid: String
) {

    @Serializable
    data class Level(
        val assetPath: String,
        val displayIcon: String?,
        val displayName: String,
        val sprayLevel: Int,
        val uuid: String
    )

}