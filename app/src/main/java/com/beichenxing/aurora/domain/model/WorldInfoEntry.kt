package com.beichenxing.aurora.domain.model

/**
 * SillyTavern world info insertion position.
 */
enum class WorldInfoPosition(val value: Int) {
    BEFORE(0),        // worldInfoBefore（角色/主提示前）
    AFTER(1),         // worldInfoAfter（角色/主提示后）
    AN_TOP(2),        // Author's Note 上方
    AN_BOTTOM(3),     // Author's Note 下方
    AT_DEPTH(4),      // 按 depth 插入聊天历史中
    EM_TOP(5),        // Example Messages 上方
    EM_BOTTOM(6),     // Example Messages 下方
    OUTLET(7);        // 命名 outlet，由 prompt manager 决定位置

    companion object {
        fun fromValue(value: Int): WorldInfoPosition =
            entries.find { it.value == value } ?: BEFORE
    }
}

/**
 * SillyTavern selective logic for secondary keys.
 */
enum class SelectiveLogic(val value: Int) {
    AND_ANY(0),   // 任意一个 secondary 命中
    NOT_ALL(1),   // 至少一个 secondary 未命中
    NOT_ANY(2),   // 没有任何 secondary 命中
    AND_ALL(3);   // 所有 secondary 都命中

    companion object {
        fun fromValue(value: Int): SelectiveLogic =
            entries.find { it.value == value } ?: AND_ANY
    }
}

/**
 * A single world info / lorebook entry.
 *
 * Phase 1 fields: core activation logic.
 * Phase 2 fields (commented below): probability, recursion, inclusion groups, timed effects.
 *
 * Field names follow SillyTavern's character_book.entries[].* convention.
 */
data class WorldInfoEntry(
    val uid: Int = 0,                   // ST 唯一 ID，用于 character filter
    val key: List<String> = emptyList(), // 主关键词列表
    val keysecondary: List<String> = emptyList(), // 次要关键词
    val comment: String = "",            // 条目标题/注释
    val content: String = "",            // 注入的内容
    val order: Int = 100,                // 排序优先级，越大越前
    val position: WorldInfoPosition = WorldInfoPosition.BEFORE,
    val disable: Boolean = false,        // 是否禁用
    val selectiveLogic: SelectiveLogic = SelectiveLogic.AND_ANY,
    val constant: Boolean = false,       // 无条件激活
    val depth: Int = 4,                  // atDepth 的深度值
    val scanDepth: Int? = null,          // 扫描深度，null 继承全局
    val caseSensitive: Boolean? = null,  // null 继承全局
    val matchWholeWords: Boolean? = null, // null 继承全局
    val useRegex: Boolean? = null,       // null 自动检测（key 是 /regex/ 形式）
    val automationId: String = "",       // 字符过滤器 ID（通常是角色 ID）
    val role: Int? = null,               // 角色注入（0=system, 1=user, 2=assistant）
    val sticky: Int? = null,             // Phase 2：粘性激活次数
    val cooldown: Int? = null,           // Phase 2：冷却回合数
    val delay: Int? = null,              // Phase 2：延迟回合数
    // val probability: Int = 100,       // Phase 2：触发概率 0-100
    // val useProbability: Boolean = false, // Phase 2：是否使用概率
    // val group: String = "",           // Phase 2：inclusion group 名称
    // val groupOverride: Boolean = false, // Phase 2
    // val groupWeight: Int = 100,       // Phase 2
    // val preventRecursion: Boolean = false, // Phase 2
    // val delayUntilRecursion: Boolean = false, // Phase 2
    val vectorized: Boolean = false,     // 向量化标记（Phase 3+）
    val extensions: Map<String, Any> = emptyMap() // 保留未知扩展字段
)

/**
 * Character book / lorebook root structure.
 */
data class CharacterBook(
    val name: String = "",
    val description: String = "",
    val scanDepth: Int? = null,          // 全局默认扫描深度
    val tokenBudget: Int? = null,        // Phase 2
    val recursiveScanning: Boolean = false, // Phase 2
    val extensions: Map<String, Any> = emptyMap(),
    val entries: List<WorldInfoEntry> = emptyList()
)