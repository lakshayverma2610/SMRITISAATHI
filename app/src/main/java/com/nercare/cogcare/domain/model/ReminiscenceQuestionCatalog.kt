package com.nercare.cogcare.domain.model

/**
 * Clinical-grade reminiscence therapy questions tailored for older adults and dementia care.
 * Designed to evoke positive long-term episodic memories, sensory recollections, and emotional comfort.
 */
data class ReminiscenceQuestion(
    val id: String,
    val domain: String,
    val categoryEmoji: String,
    val categoryLabel: String,
    val prompt: String
)

object ReminiscenceQuestionCatalog {

    val QUESTIONS: List<ReminiscenceQuestion> = listOf(
        ReminiscenceQuestion(
            id = "childhood_hometown",
            domain = "CHILDHOOD",
            categoryEmoji = "🏡",
            categoryLabel = "Childhood & Roots",
            prompt = "Where did you grow up, and what is your favorite memory from your childhood home?"
        ),
        ReminiscenceQuestion(
            id = "favorite_food",
            domain = "FOOD_PREFERENCES",
            categoryEmoji = "🍲",
            categoryLabel = "Favorite Meals",
            prompt = "What was your all-time favorite home-cooked dish or sweet when you were growing up?"
        ),
        ReminiscenceQuestion(
            id = "closest_friend",
            domain = "RELATIONSHIPS",
            categoryEmoji = "🤝",
            categoryLabel = "Youth & Friends",
            prompt = "Who was your closest friend when you were young, and what fun things did you do together?"
        ),
        ReminiscenceQuestion(
            id = "career_passion",
            domain = "WORK_AND_PASSION",
            categoryEmoji = "💼",
            categoryLabel = "Life's Work",
            prompt = "What kind of work or profession did you do, and what did you enjoy most about it?"
        ),
        ReminiscenceQuestion(
            id = "music_prayers",
            domain = "MUSIC_AND_ARTS",
            categoryEmoji = "🎵",
            categoryLabel = "Music & Melodies",
            prompt = "Can you tell me about the songs, old tunes, or prayers that you love listening to?"
        ),
        ReminiscenceQuestion(
            id = "festivals_traditions",
            domain = "TRADITIONS",
            categoryEmoji = "✨",
            categoryLabel = "Festivals & Celebrations",
            prompt = "What was your favorite festival or family celebration to celebrate together?"
        ),
        ReminiscenceQuestion(
            id = "family_pride",
            domain = "FAMILY_ANCHORS",
            categoryEmoji = "💛",
            categoryLabel = "Family Moments",
            prompt = "Tell me about a moment with your family or children that made you feel proud and happy."
        ),
        ReminiscenceQuestion(
            id = "childhood_games",
            domain = "CHILDHOOD",
            categoryEmoji = "🪁",
            categoryLabel = "Play & Games",
            prompt = "What games did you love playing outside with your siblings or neighbors when you were little?"
        ),
        ReminiscenceQuestion(
            id = "tea_chai_routine",
            domain = "FOOD_PREFERENCES",
            categoryEmoji = "☕",
            categoryLabel = "Daily Comforts",
            prompt = "How do you like to have your morning tea or coffee, and what was your favorite morning routine?"
        ),
        ReminiscenceQuestion(
            id = "hobbies_crafts",
            domain = "HOBBIES",
            categoryEmoji = "🎨",
            categoryLabel = "Hobbies & Pastimes",
            prompt = "What was a favorite hobby, craft, or sport that you always loved spending time on?"
        ),
        ReminiscenceQuestion(
            id = "favorite_travel",
            domain = "PLACES",
            categoryEmoji = "🚂",
            categoryLabel = "Travel & Places",
            prompt = "Do you have a favorite town, hill station, or pilgrimage place you loved visiting?"
        ),
        ReminiscenceQuestion(
            id = "parents_grandparents",
            domain = "FAMILY_ANCHORS",
            categoryEmoji = "👵",
            categoryLabel = "Elders & Heritage",
            prompt = "Tell me about your parents or grandparents — what is something special you remember about them?"
        ),
        ReminiscenceQuestion(
            id = "life_wisdom",
            domain = "LIFE_WISDOM",
            categoryEmoji = "📖",
            categoryLabel = "Life Wisdom",
            prompt = "What is a piece of advice or life lesson that has always guided you through life?"
        ),
        ReminiscenceQuestion(
            id = "daily_joy",
            domain = "DAILY_JOY",
            categoryEmoji = "🌸",
            categoryLabel = "Daily Joy",
            prompt = "What is something simple that always brings a warm smile to your face these days?"
        )
    )

    private fun normalize(text: String): String {
        return text.lowercase()
            .replace(Regex("[^a-z0-9 ]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Finds the next unanswered question that has not been asked in this session
     * and has not been recorded in existing memories.
     */
    fun getNextUnansweredQuestion(
        knownMemories: List<LifeMemoryNode>,
        sessionAskedPrompts: Collection<String> = emptySet()
    ): ReminiscenceQuestion {
        val answeredNorms = knownMemories.map { normalize(it.questionPrompt) }.toSet()
        val sessionNorms = sessionAskedPrompts.map { normalize(it) }.toSet()

        // 1. Try to find a question never answered and not asked in this session
        val candidate = QUESTIONS.firstOrNull { q ->
            val norm = normalize(q.prompt)
            norm !in answeredNorms && norm !in sessionNorms
        }
        if (candidate != null) return candidate

        // 2. If all are in answeredNorms, pick from those not asked in this session
        val notInSession = QUESTIONS.firstOrNull { q ->
            val norm = normalize(q.prompt)
            norm !in sessionNorms
        }
        if (notInSession != null) return notInSession

        // 3. Fallback: least recently answered question
        val leastRecent = QUESTIONS.minByOrNull { q ->
            val norm = normalize(q.prompt)
            knownMemories.find { normalize(it.questionPrompt) == norm }?.dateVerified ?: 0L
        }
        return leastRecent ?: QUESTIONS.first()
    }

    fun isDuplicate(prompt: String, knownMemories: List<LifeMemoryNode>): Boolean {
        val norm = normalize(prompt)
        return knownMemories.any { normalize(it.questionPrompt) == norm }
    }
}
