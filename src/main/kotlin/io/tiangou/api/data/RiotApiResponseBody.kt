@file:JvmName("RiotApiResponseBodyKt")

package io.tiangou.api.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    val type: String,
    var multifactor: MultiFactor? = null,
    var response: Response? = null,
    val country: String? = null,
    var securityProfile: String? = null,
) {

    companion object {
        const val MULTI_FACTOR = "multifactor"

        const val RESPONSE = "response"

        const val ERROR = "error"

        const val AUTH = "auth"
    }

    /**
     * 关于Type字段
     * 很遗憾 我查了半天的bug
     * 发现 mirai的类加载器导致 kotlin serialization 没法序列化 enum类
     * 遂改成字符串
     */
    @Serializable
    data class MultiFactor(
        val email: String,
        val method: String,
        val methods: List<String>,
        val multiFactorCodeLength: Int,
        val mfaVersion: String,
    )

    @Serializable
    data class Response(
        val mode: String,
        val parameters: Parameters
    ) {

        @Serializable
        data class Parameters(
            val uri: String
        )

    }

}

@Serializable
data class EntitlementAuthResponse(
    @SerialName("entitlements_token") val entitlementsToken: String
)

@Serializable
data class PlayerInfoResponse(
    val country: String? = null,
    val sub: String,
    @SerialName("email_verified") val emailVerified: Boolean? = null,
    @SerialName("country_at") val countryAt: Long? = null,
    val pw: Pw? = null,
    @SerialName("phone_number_verified") val phoneNumberVerified: Boolean? = null,
    @SerialName("account_verified") val accountVerified: Boolean? = null,
    @SerialName("player_locale") val playerLocale: String? = null,
    val acct: Acct? = null,
    val age: Int? = null,
    val jti: String,
    val affinity: Affinity? = null
) {

    @Serializable
    data class Pw(
        @SerialName("cng_at") val cngAt: Long,
        val reset: Boolean,
        @SerialName("must_reset") val mustReset: Boolean
    )

    @Serializable
    data class Acct(
        val type: Int,
        val state: String,
        val adm: Boolean,
        @SerialName("game_name") val gameName: String,
        @SerialName("tag_line") val tagLine: String,
        @SerialName("created_at") val createdAt: Long
    )

    @Serializable
    data class Affinity(
        val pp: String
    )

}

@Serializable
data class StoreFrontResponse(
    @SerialName("FeaturedBundle") val featuredBundle: FeaturedBundle,
    @SerialName("SkinsPanelLayout") val skinsPanelLayout: SkinsPanelLayout,
    @SerialName("UpgradeCurrencyStore") val upgradeCurrencyStore: UpgradeCurrencyStore,
    @SerialName("AccessoryStore") val accessoryStore: AccessoryStore,
    @SerialName("BonusStore") val bonusStore: BonusStore? = null,
) {

    @Serializable
    data class FeaturedBundle(
        @SerialName("Bundle") val bundle: Bundle,
        @SerialName("BundleRemainingDurationInSeconds") val bundleRemainingDurationInSeconds: Int,
        @SerialName("Bundles") val bundles: List<Bundle>
    ) {

        @Serializable
        data class Bundle(
            @SerialName("CurrencyID") val currencyID: String,
            @SerialName("DataAssetID") val dataAssetID: String,
            @SerialName("ID") val id: String,
            @SerialName("Items") val items: List<Item>
        ) {

            @Serializable
            data class Item(
                @SerialName("BasePrice") val basePrice: Int,
                @SerialName("CurrencyID") val currencyID: String,
                @SerialName("DiscountPercent") val discountPercent: Double,
                @SerialName("DiscountedPrice") val discountedPrice: Int,
                @SerialName("IsPromoItem") val isPromoItem: Boolean,
                @SerialName("Item") val item: ItemDetail
            ) {

                @Serializable
                data class ItemDetail(
                    @SerialName("ItemTypeID") val itemTypeID: String,
                    @SerialName("ItemID") val itemID: String,
                    @SerialName("Amount") val amount: Long
                )

            }

        }

    }

    @Serializable
    data class SkinsPanelLayout(
        @SerialName("SingleItemOffers") val singleItemOffers: List<String>,
        @SerialName("SingleItemOffersRemainingDurationInSeconds") val singleItemOffersRemainingDurationInSeconds: Int,
        @SerialName("SingleItemStoreOffers") val singleItemStoreOffers: List<SingleItemStoreOffer>
    ) {

        @Serializable
        data class SingleItemStoreOffer(
            @SerialName("Cost") val cost: Map<String, Long>,
            @SerialName("IsDirectPurchase") val isDirectPurchase: Boolean,
            @SerialName("OfferID") val offerID: String,
            @SerialName("Rewards") val rewards: List<Reward>,
            @SerialName("StartDate") val startDate: String
        )

    }

    @Serializable
    data class UpgradeCurrencyStore(
        @SerialName("UpgradeCurrencyOffers") val upgradeCurrencyOffers: List<UpgradeCurrencyOffer>
    ) {

        @Serializable
        data class UpgradeCurrencyOffer(
            @SerialName("Offer") val offer: Offer,
            @SerialName("OfferID") val offerID: String,
            @SerialName("StorefrontItemID") val storefrontItemID: String
        )

    }

    @Serializable
    data class BonusStore(
        @SerialName("BonusStoreOffers") val bonusStoreOffers: List<BonusStoreOffer>,
        @SerialName("BonusStoreRemainingDurationInSeconds") val bonusStoreRemainingDurationInSeconds: Int
    ) {

        @Serializable
        data class BonusStoreOffer(
            @SerialName("BonusOfferID") val bonusOfferID: String,
            @SerialName("DiscountCosts") val discountCosts: Map<String, Long>,
            @SerialName("DiscountPercent") val discountPercent: Double,
            @SerialName("IsSeen") val isSeen: Boolean,
            @SerialName("Offer") val offer: Offer
        )

    }

    @Serializable
    data class AccessoryStore(
        @SerialName("AccessoryStoreOffers") val accessoryStoreOffers: List<AccessoryStoreOffer>,
        @SerialName("AccessoryStoreRemainingDurationInSeconds") val accessoryStoreRemainingDurationInSeconds: Int,
        @SerialName("StorefrontID") val storefrontID: String
    ) {
        @Serializable
        data class AccessoryStoreOffer(
            @SerialName("ContractID") val contractID: String,
            @SerialName("Offer") val offer: Offer
        )

    }

    @Serializable
    data class Offer(
        @SerialName("Cost") val cost: Map<String, Long>,
        @SerialName("IsDirectPurchase") val isDirectPurchase: Boolean,
        @SerialName("OfferID") val offerID: String,
        @SerialName("Rewards") val rewards: List<Reward>,
        @SerialName("StartDate") val startDate: String
    )

    @Serializable
    data class Reward(
        @SerialName("ItemID") val itemID: String,
        @SerialName("ItemTypeID") val itemTypeID: String,
        @SerialName("Quantity") val quantity: Int
    )

}






