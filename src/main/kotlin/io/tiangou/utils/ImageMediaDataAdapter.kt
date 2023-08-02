package io.tiangou.utils

import io.tiangou.api.data.StoreFrontResponse
import io.tiangou.repository.persistnce.*

internal interface ImageMediaDataAdapter<T : Any, R : Any> {

    fun convert(arg: T): R

}

data class SkinImageData(
    val skinUrl: String?,
    val contentTiersUrl: String?,
    val themeUrl: String?,
    val backgroundColor: String?,
    val skinName: String?
)

object SkinsPanelLayout : ImageMediaDataAdapter<StoreFrontResponse, List<SkinImageData>> {
    override fun convert(arg: StoreFrontResponse): List<SkinImageData> =
        arg.skinsPanelLayout.singleItemOffers.map {
            val skinLevel = WeaponSkinLevel(it).queryOne()
            val skin = WeaponSkin(skinLevel?.weaponSkinUuid).queryOne()
            val theme = skin?.themeUuid?.let { Theme(it).queryOne() }
            val contentTier = skin?.contentTiersUuid?.let { ContentTier(it).queryOne() }
            SkinImageData(
                skinLevel?.displayIcon,
                contentTier?.displayIcon,
                theme?.displayIcon,
                contentTier?.highlightColor,
                skinLevel?.displayName
            )
        }
}

object Accessory : ImageMediaDataAdapter<StoreFrontResponse, List<AccessoryImageData>> {

    enum class AccessoryItemType(val value: String) {
        CURRENCY("Currency"),
        PENDANT("EquippableCharmLevel"),
        SKIN("EquippableSkinLevel"),
        PLAYER_CARD("PlayerCard"),
        SPRAY("Spray"),
        TITLE("Title"),
        ;

        companion object {
            fun match(value: String): AccessoryItemType {
                values().forEach {
                    if (it.value == value) return it
                }
                throw IllegalArgumentException("value is not in AccessoryItemType")
            }
        }
    }

    override fun convert(arg: StoreFrontResponse): List<AccessoryImageData> =
        arg.accessoryStore.accessoryStoreOffers.map {
            val contract = Contract(it.contractID).queryOne()
            val itemID = it.offer.rewards.first().itemID
            val itemType = ContractLevel(itemID, it.contractID).queryOne()?.type?.let { AccessoryItemType.match(it) }
            var itemUrl: String? = null
            var itemName: String? = null
            when (itemType) {
                AccessoryItemType.CURRENCY -> Currency(itemID).queryOne()
                    ?.apply { itemUrl = largeIcon; itemName = displayName }

                AccessoryItemType.PENDANT -> {
                    val buddieLevel = BuddieLevel(itemID).queryOne()
                    Buddie(buddieLevel?.buddieUuid).queryOne()
                        ?.apply { itemUrl = displayIcon; itemName = displayName }
                }

                AccessoryItemType.SKIN -> WeaponSkinLevel(itemID).queryOne()
                    ?.apply { itemUrl = displayIcon; itemName = displayName }

                AccessoryItemType.PLAYER_CARD -> PlayCard(itemID).queryOne()
                    ?.apply { itemUrl = displayIcon; itemName = displayName }

                AccessoryItemType.SPRAY -> Spray(itemID).queryOne()
                    ?.apply { itemUrl = displayIcon; itemName = displayName }

                AccessoryItemType.TITLE -> PlayTitle(itemID).queryOne()?.apply { itemName = displayName }
                null -> {}
            }
            AccessoryImageData(itemUrl, itemName, contract?.displayName)
        }
}


data class AccessoryImageData(
    val itemUrl: String?,
    val itemName: String?,
    val contractName: String?,
)