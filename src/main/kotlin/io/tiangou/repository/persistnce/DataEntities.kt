package io.tiangou.repository.persistnce

data class WeaponSkin(
    @Column("varchar(100)", true)
    @PrimaryKey
    val uuid: String? = null,
    @Column("varchar(100)")
    @PrimaryKey
    val contentTiersUuid: String? = null,
    @Column("varchar(100)")
    @PrimaryKey
    val themeUuid: String? = null
) : PersistenceData<WeaponSkin>() {

    override val relation: ObjectTableRelation<WeaponSkin> = WeaponSkin

    companion object : ObjectTableRelation<WeaponSkin>(WeaponSkin::class)

}

data class WeaponSkinLevel(
    @Column("varchar(100)", true) @PrimaryKey val uuid: String? = null,
    @Column("varchar(255)") val displayIcon: String? = null,
    @Column("varchar(100)", true) val displayName: String? = null,
    @Column("varchar(100)", true) @PrimaryKey val weaponSkinUuid: String? = null,
) : PersistenceData<WeaponSkinLevel>() {

    override val relation: ObjectTableRelation<WeaponSkinLevel> = WeaponSkinLevel

    companion object : ObjectTableRelation<WeaponSkinLevel>(WeaponSkinLevel::class)

}

data class Theme(
    @Column("varchar(100)", true) @PrimaryKey val uuid: String? = null,
    @Column("varchar(255)") val displayIcon: String? = null,
    @Column("varchar(100)") val displayName: String? = null,
) : PersistenceData<Theme>() {

    override val relation: ObjectTableRelation<Theme> = Theme

    companion object : ObjectTableRelation<Theme>(Theme::class)

}

data class ContentTier(
    @Column("varchar(100)", true) @PrimaryKey val uuid: String? = null,
    @Column("varchar(255)") val displayIcon: String? = null,
    @Column("varchar(100)") val displayName: String? = null,
    @Column("varchar(255)") val highlightColor: String? = null,
) : PersistenceData<ContentTier>() {

    override val relation: ObjectTableRelation<ContentTier> = ContentTier

    companion object : ObjectTableRelation<ContentTier>(ContentTier::class)

}

data class Contract(
    @Column("varchar(100)", true) @PrimaryKey val uuid: String? = null,
    @Column("varchar(255)") val displayIcon: String? = null,
    @Column("varchar(100)", true) val displayName: String? = null,
) : PersistenceData<Contract>() {

    override val relation: ObjectTableRelation<Contract> = Contract

    companion object : ObjectTableRelation<Contract>(Contract::class)

}

/**
 * 特别说明:
 * 通行证中的UUID不保证唯一性
 * 如:皮肤点数就可能会出现重复
 */
data class ContractLevel(
    @Column("varchar(100)", true) @Index @PrimaryKey val uuid: String? = null,
    @Column("varchar(100)", true) @Index @PrimaryKey val contractUuid: String? = null,
    @Column("varchar(50)", true) val type: String? = null,
    @Column("integer", true) val xp: Int? = null,
    @Column("integer", true) val amount: Int? = null,
    @Column("integer", true) @PrimaryKey val level: Int? = null,
    @Column("boolean", true) val isFreeReward: Boolean? = null,
) : PersistenceData<ContractLevel>() {

    override val relation: ObjectTableRelation<ContractLevel> = ContractLevel

    companion object : ObjectTableRelation<ContractLevel>(ContractLevel::class)

}

data class Buddie(
    @Column("varchar(100)", true) @PrimaryKey val uuid: String? = null,
    @Column("varchar(255)", true) val displayIcon: String? = null,
    @Column("varchar(100)", true) val displayName: String? = null,
) : PersistenceData<Buddie>() {

    override val relation: ObjectTableRelation<Buddie> = Buddie

    companion object : ObjectTableRelation<Buddie>(Buddie::class)

}

data class BuddieLevel(
    @Column("varchar(100)", true) @PrimaryKey val uuid: String? = null,
    @Column("varchar(255)", true) val displayIcon: String? = null,
    @Column("varchar(100)", true) val displayName: String? = null,
    @Column("varchar(100)", true) @PrimaryKey @Index val buddieUuid: String? = null
) : PersistenceData<BuddieLevel>() {

    override val relation: ObjectTableRelation<BuddieLevel> = BuddieLevel

    companion object : ObjectTableRelation<BuddieLevel>(BuddieLevel::class)

}

data class Currency(
    @Column("varchar(100)", true) @PrimaryKey val uuid: String? = null,
    @Column("varchar(255)", true) val displayIcon: String? = null,
    @Column("varchar(100)", true) val displayName: String? = null,
    @Column("varchar(255)", true) val largeIcon: String? = null
) : PersistenceData<Currency>() {

    override val relation: ObjectTableRelation<Currency> = Currency

    companion object : ObjectTableRelation<Currency>(Currency::class)

}

data class PlayCard(
    @Column("varchar(100)", true) @PrimaryKey val uuid: String? = null,
    @Column("varchar(255)", true) val displayIcon: String? = null,
    @Column("varchar(100)", true) val displayName: String? = null,
    @Column("varchar(255)", true) val largeArt: String? = null,
    @Column("varchar(255)", true) val smallArt: String? = null,
    @Column("varchar(255)", true) val wideArt: String? = null
) : PersistenceData<PlayCard>() {

    override val relation: ObjectTableRelation<PlayCard> = PlayCard

    companion object : ObjectTableRelation<PlayCard>(PlayCard::class)

}

data class PlayTitle(
    @Column("varchar(100)", true) @PrimaryKey val uuid: String? = null,
    @Column("varchar(100)") val displayName: String? = null,
    @Column("varchar(100)") val titleText: String? = null,
) : PersistenceData<PlayTitle>() {

    override val relation: ObjectTableRelation<PlayTitle> = PlayTitle

    companion object : ObjectTableRelation<PlayTitle>(PlayTitle::class)

}

data class Spray(
    @Column("varchar(100)", true) @PrimaryKey val uuid: String? = null,
    @Column("varchar(255)") val animationGif: String? = null,
    @Column("varchar(255)") val animationPng: String? = null,
    @Column("varchar(255)", true) val displayIcon: String? = null,
    @Column("varchar(100)", true) val displayName: String? = null,
    @Column("varchar(255)") val fullIcon: String? = null,
    @Column("varchar(255)", true) val fullTransparentIcon: String? = null,
    @Column("boolean", true) val isNullSpray: Boolean? = null
) : PersistenceData<Spray>() {

    override val relation: ObjectTableRelation<Spray> = Spray

    companion object : ObjectTableRelation<Spray>(Spray::class)

}

data class SprayLevel(
    @Column("varchar(100)", true) @PrimaryKey val uuid: String? = null,
    @Column("varchar(100)", true) @PrimaryKey @Index val sprayUuid: String? = null,
    @Column("varchar(255)") val displayIcon: String? = null,
    @Column("varchar(100)", true) val displayName: String? = null,
    @Column("integer", true) val sprayLevel: Int? = null
) : PersistenceData<SprayLevel>() {

    override val relation: ObjectTableRelation<SprayLevel> = SprayLevel

    companion object : ObjectTableRelation<SprayLevel>(SprayLevel::class)

}