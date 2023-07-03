package io.tiangou.repository.persistnce

import io.tiangou.api.ThirdPartyValorantApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.utils.MiraiLogger

object PersistenceDataInitiator {

    private val log: MiraiLogger = MiraiLogger.Factory.create(PersistenceDataInitiator::class)


    init {
        PersistenceData.createTable(WeaponSkin)
        PersistenceData.createTable(WeaponSkinLevel)
        PersistenceData.createTable(ContentTier)
        PersistenceData.createTable(Theme)
        PersistenceData.createTable(Contract)
        PersistenceData.createTable(ContractLevel)
        PersistenceData.createTable(Buddie)
        PersistenceData.createTable(BuddieLevel)
        PersistenceData.createTable(Currency)
        PersistenceData.createTable(PlayCard)
        PersistenceData.createTable(PlayTitle)
        PersistenceData.createTable(Spray)
        PersistenceData.createTable(SprayLevel)
    }


    suspend fun init() {
        withContext(Dispatchers.IO) {
            log.info("starting initial Valorant DataBase")
            initAllSkinsAndSkinLevels()
            initAllContentTiers()
            initAllThemes()
            initAllContracts()
            initAllBuddies()
            initAllCurrencies()
            initAllPlayCards()
            initAllPlayTitles()
            initAllSprays()
            log.info("Valorant DataBase initialled successful")
        }
    }

    private suspend fun initAllSkinsAndSkinLevels() {
        ThirdPartyValorantApi.AllWeaponSkins.execute().data.forEach { skin ->
            WeaponSkin(
                skin.uuid,
                skin.contentTierUuid,
                skin.themeUuid
            ).save()
            skin.levels.forEach { level ->
                WeaponSkinLevel(
                    level.uuid,
                    level.displayIcon,
                    level.displayName,
                    skin.uuid
                ).save()
            }
        }
        log.info("skins and levels data initialled")
    }

    private suspend fun initAllContentTiers() {
        ThirdPartyValorantApi.AllContentTiers.execute().data.forEach { contentTier ->
            ContentTier(
                contentTier.uuid,
                contentTier.displayIcon,
                contentTier.displayName,
                contentTier.highlightColor
            ).save()
        }
        log.info("contentTiers data initialled")
    }

    private suspend fun initAllThemes() {
        ThirdPartyValorantApi.AllThemes.execute().data.forEach { theme ->
            Theme(theme.uuid, theme.displayIcon, theme.displayName).save()
        }
        log.info("themes data initialled")
    }

    private suspend fun initAllContracts() {
        ThirdPartyValorantApi.AllContracts.execute().data.forEach {
            var levelInt = 1
            var freeRewardLevelInt = 1
            it.content.apply {
                Contract(it.uuid, it.displayIcon, it.displayName).save()
            }.chapters.sortedBy { chapter -> chapter.levels.sumOf { level -> level.xp } }
                .forEach { chapter ->
                    chapter.levels.sortedBy { level -> level.xp }.forEach { level ->
                        ContractLevel(
                            level.reward.uuid,
                            it.uuid,
                            level.reward.type,
                            level.xp,
                            level.reward.amount,
                            levelInt++,
                            false
                        ).save()
                    }
                    chapter.freeRewards?.forEach { freeReward ->
                        ContractLevel
                        ContractLevel(
                            freeReward.uuid,
                            it.uuid,
                            freeReward.type,
                            0,
                            freeReward.amount,
                            freeRewardLevelInt++,
                            true
                        ).save()
                    }
                }
        }
        log.info("contracts and levels data initialled")
    }

    private suspend fun initAllBuddies() {
        ThirdPartyValorantApi.AllBuddies.execute().data.forEach { buddie ->
            Buddie(buddie.uuid, buddie.displayIcon, buddie.displayName).save()
            buddie.levels.forEach { level ->
                BuddieLevel(level.uuid, level.displayIcon, level.displayName, buddie.uuid).save()
            }
        }
        log.info("buddies and level data initialled")
    }

    private suspend fun initAllCurrencies() {
        ThirdPartyValorantApi.AllCurrencies.execute().data.forEach { currency ->
            Currency(currency.uuid, currency.displayIcon, currency.displayName, currency.largeIcon).save()
        }
        log.info("currencies data initialled")
    }

    private suspend fun initAllPlayCards() {
        ThirdPartyValorantApi.AllPlayerCards.execute().data.forEach { playCard ->
            PlayCard(
                playCard.uuid,
                playCard.displayIcon,
                playCard.displayName,
                playCard.largeArt,
                playCard.smallArt,
                playCard.wideArt
            ).save()
        }
        log.info("playCards data initialled")
    }

    private suspend fun initAllPlayTitles() {
        ThirdPartyValorantApi.AllPlayerTitles.execute().data.forEach { playTitle ->
            PlayTitle(playTitle.uuid, playTitle.displayName, playTitle.titleText).save()
        }
        log.info("playTitles data initialled")
    }

    private suspend fun initAllSprays() {
        ThirdPartyValorantApi.AllSprays.execute().data.forEach { spray ->
            Spray(
                spray.uuid, spray.animationGif, spray.animationPng,
                spray.displayIcon, spray.displayName,
                spray.fullIcon, spray.fullTransparentIcon,
                spray.isNullSpray
            ).save()
            spray.levels.forEach {
                SprayLevel(it.uuid, spray.uuid, it.displayIcon, it.displayName, it.sprayLevel)
            }
        }
        log.info("sprays and level data initialled")
    }

}