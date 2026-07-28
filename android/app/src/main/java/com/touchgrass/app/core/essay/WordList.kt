package com.touchgrass.app.core.essay

/**
 * The pool of essay prompts.
 *
 * WHY A RANDOM WORD AT ALL (app_plan.md §2.4): if you pick your own topic
 * you'll pick the same easy one every time and have a canned paragraph
 * memorised by week two. Randomness is what stops the toll decaying into a
 * ritual.
 *
 * The mix is deliberate — concrete nouns are easy to start writing about,
 * abstract concepts are more interesting to write about. Both are needed:
 * all-concrete gets boring, all-abstract gets intimidating at 11pm.
 *
 * Bundled in code rather than fetched, because the essay screen must work
 * with no network — it's the one screen that absolutely cannot fail.
 */
object WordList {

    val WORDS: List<String> = listOf(
        // ---- Concrete: places & nature ----
        "lighthouse", "harbour", "glacier", "orchard", "canyon", "meadow",
        "estuary", "quarry", "tundra", "reef", "delta", "hillside",
        "waterfall", "cavern", "marsh", "dune", "prairie", "fjord",
        "island", "volcano", "riverbank", "forest", "lagoon", "cliff",

        // ---- Concrete: objects ----
        "compass", "typewriter", "lantern", "anchor", "telescope", "kettle",
        "bicycle", "mirror", "suitcase", "envelope", "piano", "hammock",
        "wristwatch", "camera", "umbrella", "candle", "bookshelf", "ladder",
        "postcard", "keyring", "thermos", "sketchbook", "radio", "compass",
        "coin", "matchbox", "doorway", "staircase", "windowsill", "rooftop",
        "bridge", "tunnel", "fence", "gate", "wheel", "engine",

        // ---- Concrete: living things ----
        "heron", "otter", "moth", "fox", "swallow", "beetle",
        "whale", "sparrow", "cedar", "fern", "moss", "thistle",
        "lichen", "kelp", "bramble", "willow", "magpie", "hedgehog",
        "jellyfish", "dragonfly", "owl", "salmon", "ivy", "sunflower",

        // ---- Concrete: weather & light ----
        "thunderstorm", "frost", "dusk", "monsoon", "eclipse", "drizzle",
        "haze", "aurora", "hailstone", "shadow", "sunrise", "fog",
        "moonlight", "heatwave", "snowdrift", "rainbow",

        // ---- Sensory ----
        "petrichor", "echo", "static", "warmth", "salt", "smoke",
        "velvet", "rust", "hum", "chill", "glare", "silence",

        // ---- Abstract: feeling ----
        "nostalgia", "restlessness", "relief", "dread", "curiosity", "envy",
        "contentment", "impatience", "awe", "loneliness", "delight", "regret",
        "anticipation", "boredom", "gratitude", "embarrassment", "hope",
        "frustration", "tenderness", "surprise", "calm", "longing",

        // ---- Abstract: time ----
        "waiting", "beginnings", "endings", "routine", "interruption",
        "anniversary", "deadline", "afternoon", "midnight", "seasons",
        "childhood", "tomorrow", "yesterday", "delay", "pause", "rhythm",

        // ---- Abstract: people ----
        "strangers", "neighbours", "friendship", "apology", "advice",
        "argument", "reunion", "distance", "crowds", "family", "teachers",
        "kindness", "rivalry", "trust", "misunderstanding", "forgiveness",

        // ---- Abstract: ideas ----
        "attention", "habit", "boredom", "distraction", "focus", "memory",
        "choice", "chance", "consequence", "repetition", "escape", "control",
        "curiosity", "doubt", "certainty", "compromise", "ambition", "failure",
        "progress", "simplicity", "excess", "balance", "risk", "patience",

        // ---- Everyday life ----
        "commute", "breakfast", "laundry", "queue", "grocery", "chores",
        "packing", "moving", "cleaning", "cooking", "walking", "sleeping",
        "borrowing", "collecting", "repairing", "gardening", "shopping",

        // ---- Places you go ----
        "library", "kitchen", "hospital", "station", "market", "classroom",
        "stairwell", "balcony", "corridor", "basement", "attic", "garden",
        "playground", "workshop", "temple", "museum", "bakery", "laundromat",

        // ---- Slightly strange, to break the pattern ----
        "leftovers", "instructions", "signatures", "receipts", "passwords",
        "spare keys", "wrong turns", "old photographs", "unread messages",
        "the last slice", "unfinished books", "borrowed jumpers"
    ).distinct()

    val size: Int get() = WORDS.size
}
