package eu.pkgsoftware.babybuddywidgets

object Constants {
    enum class FeedingTypeEnum(@JvmField var value: Int, @JvmField var post_name: String) {
        BREAST_MILK(0, "breast milk"),
        FORMULA(1, "formula"),
        FORTIFIED_BREAST_MILK(2,"fortified breast milk"),
        SOLID_FOOD(3, "solid food"),
    }

    enum class FeedingMethodEnum(@JvmField var value: Int, @JvmField var post_name: String) {
        BOTTLE(0, "bottle"),
        LEFT_BREAST(1, "left breast"),
        RIGHT_BREAST(2, "right breast"),
        BOTH_BREASTS(3, "both breasts"),
        PARENT_FED(4, "parent fed"),
        SELF_FED(5, "self fed"),
    }

    @JvmField
    var FeedingTypeEnumValues: Map<Int, FeedingTypeEnum>
    init {
        val m = mutableMapOf<Int, FeedingTypeEnum>()
        for (e in FeedingTypeEnum.values()) {
            m.put(e.value, e)
        }
        FeedingTypeEnumValues = m.toMap()
    }

    @JvmField
    var FeedingMethodEnumValues: Map<Int, FeedingMethodEnum>
    init {
        val m = mutableMapOf<Int, FeedingMethodEnum>()
        for (e in FeedingMethodEnum.values()) {
            m.put(e.value, e)
        }
        FeedingMethodEnumValues = m.toMap()
    }
}