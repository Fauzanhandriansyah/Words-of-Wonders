package com.example.game

import java.util.Random
import kotlin.math.max
import kotlin.math.min

data class LevelData(
    val levelNumber: Int,
    val difficulty: String, // "Mudah", "Sedang", "Sulit"
    val baseLetters: String, // Scrambled display letters, e.g., "K, O, T, A"
    val targetWords: List<String>, // Words displayed in the crossword grid
    val extraWords: List<String>, // Words that are valid but not in crossword (bonus)
    val wordPlacements: List<WordPlacement>, // Coordinates of crossword words
    val themeName: String, // "Mesir", "Jepang", "Paris", "Venesia"
    val themeBackgroundResId: Int // Resource ID for background
)

data class WordPlacement(
    val word: String,
    val startX: Int,
    val startY: Int,
    val isHorizontal: Boolean
)

data class BaseWordFamily(
    val root: String,
    val validWords: List<String>
)

object LevelGenerator {
    
    // Easy Pool: 3 to 4 letter root words
    private val easyPool = listOf(
        BaseWordFamily("KOTA", listOf("KOTA", "OTAK", "TOK", "TAK")),
        BaseWordFamily("BATU", listOf("BATU", "BUAT", "TUA", "BAU", "ABU", "BUTA", "TAU")),
        BaseWordFamily("NADI", listOf("NADI", "DAN", "DIA", "IDA", "ANI")),
        BaseWordFamily("ALAM", listOf("ALAM", "LAMA", "AMAL", "MAL")),
        BaseWordFamily("TAPI", listOf("TAPI", "PITA", "TIAP", "API", "PATI", "IPA", "TIP")),
        BaseWordFamily("DAUN", listOf("DAUN", "DAN", "DUA", "ADU", "UNA", "NAU")),
        BaseWordFamily("AJAR", listOf("AJAR", "RAJA", "ARA", "JARA")),
        BaseWordFamily("BAGI", listOf("BAGI", "IGA", "IBA")),
        BaseWordFamily("KAMU", listOf("KAMU", "MUKA", "KAU", "MAU", "AKU", "UMAK")),
        BaseWordFamily("SATU", listOf("SATU", "TUA", "TAU", "USA", "UTAS", "SAUT")),
        BaseWordFamily("LIAR", listOf("LIAR", "ALIR", "LARI", "IRA", "ARI", "LIRA")),
        BaseWordFamily("BUKU", listOf("BUKU", "KUBU", "BUK", "KUB")),
        BaseWordFamily("ROTI", listOf("ROTI", "TRIO", "TORI", "ROI")),
        BaseWordFamily("SAPI", listOf("SAPI", "SIAP", "PAS", "ISA", "API", "PIA")),
        BaseWordFamily("PITA", listOf("PITA", "PATI", "API", "TIP", "IPA")),
        BaseWordFamily("RELA", listOf("RELA", "ERA", "LAR")),
        BaseWordFamily("SANA", listOf("SANA", "NASA", "ASA", "ANA")),
        BaseWordFamily("RASA", listOf("RASA", "ASAR", "SARA", "ASA", "ARA")),
        BaseWordFamily("ALAS", listOf("ALAS", "ASAL", "SALA", "LAS", "ALA")),
        BaseWordFamily("SINI", listOf("SINI", "INI", "ISI", "SIN")),
        BaseWordFamily("BUKA", listOf("BUKA", "BAKU", "KAU", "BAU", "ABU", "BAK", "BUA")),
        BaseWordFamily("BUMI", listOf("BUMI", "IBU", "UBI", "BIM")),
        BaseWordFamily("BISA", listOf("BISA", "ABIS", "BIAS", "IBA", "ISA", "BAS")),
        BaseWordFamily("MAIN", listOf("MAIN", "IMAN", "AMIN", "MANI", "ANI", "MIN")),
        BaseWordFamily("PADI", listOf("PADI", "TIAP", "DIA", "IPA", "API")),
        BaseWordFamily("LIMA", listOf("LIMA", "ALIM", "AMIL", "MAL", "MIL", "ALI")),
        BaseWordFamily("TAHU", listOf("TAHU", "TUA", "TAU", "BAU", "UHA")),
        BaseWordFamily("MADU", listOf("MADU", "MUDA", "DUA", "ADU", "MAU", "DAM")),
        BaseWordFamily("DARI", listOf("DARI", "AIR", "DIA", "ARI", "RAD", "ADI")),
        BaseWordFamily("PALU", listOf("PALU", "LUPA", "PULA", "ALU", "LAP", "UAP")),
        BaseWordFamily("MASA", listOf("MASA", "ASAM", "SAMA", "ASA", "AMA", "MAS")),
        BaseWordFamily("PENA", listOf("PENA", "NEPA", "PAN", "ENA", "PEN"))
    )

    // Medium Pool: 5 letter root words
    private val mediumPool = listOf(
        BaseWordFamily("HARUM", listOf("HARUM", "MURAH", "RUMAH", "RUAM", "MAU", "AUM")),
        BaseWordFamily("DUNIA", listOf("DUNIA", "DAUN", "DUA", "ADU", "DAN", "UNI", "ADI", "DIA")),
        BaseWordFamily("BERAS", listOf("BERAS", "BESAR", "SERBA", "ERA", "SAR", "BEA", "SABER", "BAS")),
        BaseWordFamily("INTAN", listOf("INTAN", "TANI", "NIAT", "NANTI", "TIN", "NAN")),
        BaseWordFamily("LIANG", listOf("LIANG", "ILANG", "LAGI", "ALIN", "GILA", "IGA", "NILA")),
        BaseWordFamily("BAKAR", listOf("BAKAR", "KABAR", "AKAR", "BARA", "KARA", "RABA", "BAK")),
        BaseWordFamily("ANGIN", listOf("ANGIN", "NANI", "IGA", "NAGI", "NIAN")),
        BaseWordFamily("SITUS", listOf("SITUS", "TISU", "SUTI")),
        BaseWordFamily("KELAS", listOf("KELAS", "SELA", "LAS", "EKA", "KAS", "LES", "SEK", "ALKES")),
        BaseWordFamily("PAGAR", listOf("PAGAR", "RAGA", "AGAR", "PARA", "APAR", "GAP")),
        BaseWordFamily("PANTAI", listOf("PANTAI", "PINTA", "NAPI", "TIAP", "PITA", "TAPI", "IPA", "PATI")),
        BaseWordFamily("KASUR", listOf("KASUR", "RUSA", "KURS", "SAKU", "SUKA", "ARUS", "SURAK", "KAS")),
        BaseWordFamily("MINUM", listOf("MINUM", "IMUN", "MIN", "NIM")),
        BaseWordFamily("TANAH", listOf("TANAH", "TAHAN", "ANTA", "TAH", "ANA", "NAH")),
        BaseWordFamily("PASAR", listOf("PASAR", "SAPAR", "ASAP", "RASA", "SARA", "PARA", "SAP", "ASA")),
        BaseWordFamily("SIRAM", listOf("SIRAM", "AMIR", "RIAS", "SARI", "MARI", "ARI", "RIA", "MAS")),
        BaseWordFamily("KAPAL", listOf("KAPAL", "PALAK", "LAPAK", "ALAP", "PALA", "KALA", "APA", "LAP")),
        BaseWordFamily("SURAT", listOf("SURAT", "RUSA", "ARUS", "TUAS", "SATU", "TUA", "TAU")),
        BaseWordFamily("SUDUT", listOf("SUDUT", "UTUS", "DUS", "TUS")),
        BaseWordFamily("BUTUH", listOf("BUTUH", "TUBUH", "UTUH", "BUTU", "TUH")),
        BaseWordFamily("DARAT", listOf("DARAT", "RADAR", "DATA", "RADA", "ADA", "DAT", "RAD")),
        BaseWordFamily("PINTU", listOf("PINTU", "TIPU", "UNIT", "PIN", "TIP", "UNI")),
        BaseWordFamily("MERAH", listOf("MERAH", "REMAH", "REHAM", "ERA", "RAM")),
        BaseWordFamily("PUTIH", listOf("PUTIH", "TIPU", "TIUP", "HIT", "PIU")),
        BaseWordFamily("HIJAU", listOf("HIJAU", "UJI", "JAUH", "HAJI", "JUA")),
        BaseWordFamily("SAYUR", listOf("SAYUR", "SURYA", "RAYU", "AYU", "SUA", "ARUS", "RUSA")),
        BaseWordFamily("KIPAS", listOf("KIPAS", "SIKAP", "APIK", "PASI", "ISAK", "PAS", "SAP", "KAS")),
        BaseWordFamily("BUAYA", listOf("BUAYA", "BAU", "BAYA", "ABU", "ABA"))
    )

    // Hard Pool: 6 to 7 letter root words
    private val hardPool = listOf(
        BaseWordFamily("KERTAS", listOf("KERTAS", "RETAK", "KERAS", "SERTA", "KARET", "TERAS", "REKSA", "ERA", "KAS", "TEKAS")),
        BaseWordFamily("BINTANG", listOf("BINTANG", "BIANG", "TIANG", "BANG", "NANTI", "TIGA", "NAGI", "TANG", "BING")),
        BaseWordFamily("BELAJAR", listOf("BELAJAR", "BELA", "AJAR", "RABA", "JALA", "RELA", "BALA", "ERA")),
        BaseWordFamily("KELUAR", listOf("KELUAR", "KERA", "RELA", "LUAR", "ALUR", "LAKU", "KUAL", "ERA", "KAU", "ULA")),
        BaseWordFamily("PERTAMA", listOf("PERTAMA", "TAMPA", "RAMA", "EMAT", "TAPA", "PETA", "APA", "MAP")),
        BaseWordFamily("GUNUNG", listOf("GUNUNG", "NUN", "UNG")),
        BaseWordFamily("SELAMAT", listOf("SELAMAT", "SALAM", "SELAM", "LAMA", "ALAM", "AMAL", "ATAS", "MATA", "MALA", "ALAT")),
        BaseWordFamily("SEPEDA", listOf("SEPEDA", "SEDA", "PADA", "EDA", "PAS", "PEDAS")),
        BaseWordFamily("MERDEKA", listOf("MERDEKA", "REKAM", "KERA", "REKA", "ERA", "DEK", "REK")),
        BaseWordFamily("SABUN", listOf("SABUN", "BUSA", "UBAN", "SABU", "ABU", "BAU", "BUN")),
        BaseWordFamily("MAKANAN", listOf("MAKANAN", "MAKAN", "MAKA", "ANAK", "NAMA", "AMAN", "MANA")),
        BaseWordFamily("PANDUAN", listOf("PANDUAN", "PANDU", "DAUN", "DAN", "DUA", "ADA")),
        BaseWordFamily("MENTARI", listOf("MENTARI", "MERANTI", "MINAT", "TENAR", "IMAN", "ANTI", "TARI", "RAMI", "ERA", "AIR")),
        BaseWordFamily("BUDAYA", listOf("BUDAYA", "BADUY", "ABADI", "ADU", "BAYU", "BAU", "DUA", "ADA", "ABU", "DAYA")),
        BaseWordFamily("NEGARA", listOf("NEGARA", "ARENA", "RAGA", "AGAR", "AREA", "GAYA", "NAGA", "ANA", "ERA")),
        BaseWordFamily("MELATI", listOf("MELATI", "METAL", "TALI", "LIMA", "MATI", "LIAT", "AMIL", "MIL", "MEI")),
        BaseWordFamily("RAKYAT", listOf("RAKYAT", "KARYA", "KAYA", "RAKA", "AKAR", "TAK", "RAK")),
        BaseWordFamily("PANDAN", listOf("PANDAN", "NADA", "DANA", "PADA", "DAN", "NAN")),
        BaseWordFamily("SEJARAH", listOf("SEJARAH", "RAJA", "JARA", "JASA", "ARAH", "HARA", "SERA", "SARA", "ASA", "ERA")),
        BaseWordFamily("PEMUDA", listOf("PEMUDA", "MADU", "MUDA", "ADU", "MAU", "DUA")),
        BaseWordFamily("WARISAN", listOf("WARISAN", "WARIS", "SAWAN", "RAWA", "SARI", "RASA", "SARA", "SANA", "NAS", "WAN", "AIR")),
        BaseWordFamily("STASIUN", listOf("STASIUN", "SATU", "TISU", "UNIT", "SAUT", "SUSI", "NAS", "UNI")),
        BaseWordFamily("KOMPAS", listOf("KOMPAS", "SAMPO", "KAOS", "SOKA", "PAS", "KAS", "MAS", "SAP")),
        BaseWordFamily("PANCING", listOf("PANCING", "CINA", "NAPI", "IGA", "PIN", "PAN"))
    )

    private fun getBgResIdAndTheme(levelNumber: Int): Pair<String, Int> {
        val themeIndex = ((levelNumber - 1) / 50) % 4
        return when (themeIndex) {
            0 -> Pair("Mesir", com.example.R.drawable.img_bg_egypt_1784190855541)
            1 -> Pair("Jepang", com.example.R.drawable.img_bg_japan_1784190869659)
            2 -> Pair("Paris", com.example.R.drawable.img_bg_paris_1784190884704)
            else -> Pair("Venesia", com.example.R.drawable.img_bg_venice_1784190901212)
        }
    }

    fun generateLevel(levelNumber: Int, isDailyPuzzle: Boolean = false): LevelData {
        val rand = Random(levelNumber.toLong() * (if (isDailyPuzzle) 7 else 13))
        
        // 1. Determine Difficulty
        val difficulty = when {
            isDailyPuzzle -> "Spesial"
            levelNumber <= 300 -> "Mudah"
            levelNumber <= 700 -> "Sedang"
            else -> "Sulit"
        }

        // 2. Select Pool & Base Word Family
        val pool = when (difficulty) {
            "Mudah" -> easyPool
            "Sedang" -> mediumPool
            "Spesial" -> mediumPool + hardPool
            else -> hardPool
        }
        
        val familyIndex = rand.nextInt(pool.size)
        val family = pool[familyIndex]
        
        // 3. Select Target and Extra Words
        val allValid = family.validWords.shuffled(rand)
        
        // Easy levels have 3-4 crossword words, Medium 4-5, Hard 5-7
        val targetWordCount = when (difficulty) {
            "Mudah" -> clamp(3, allValid.size, 4)
            "Sedang" -> clamp(4, allValid.size, 5)
            "Spesial" -> clamp(4, allValid.size, 6)
            else -> clamp(5, allValid.size, 7)
        }
        
        val targetWords = allValid.take(targetWordCount)
        val extraWords = allValid.drop(targetWordCount)

        // 4. Generate Display letters (all letters of root, scrambled)
        val allChars = family.root.uppercase().toList()
        val baseLetters = allChars.shuffled(rand).joinToString(",")

        // 5. Generate Crossword Placements
        val placements = CrosswordLayoutGenerator.generate(targetWords)

        // 6. Theme and background
        val (themeName, bgResId) = if (isDailyPuzzle) {
            Pair("Tantangan Harian", com.example.R.drawable.img_bg_aurora_custom_1784191093948)
        } else {
            getBgResIdAndTheme(levelNumber)
        }

        return LevelData(
            levelNumber = levelNumber,
            difficulty = difficulty,
            baseLetters = baseLetters,
            targetWords = targetWords,
            extraWords = extraWords,
            wordPlacements = placements,
            themeName = themeName,
            themeBackgroundResId = bgResId
        )
    }

    private fun clamp(minVal: Int, maxVal: Int, defaultVal: Int): Int {
        val cap = min(maxVal, defaultVal)
        return max(minVal, cap)
    }
}

object CrosswordLayoutGenerator {

    fun generate(words: List<String>): List<WordPlacement> {
        if (words.isEmpty()) return emptyList()

        // Sort words by length descending to start with the longest word in the center
        val sortedWords = words.sortedByDescending { it.length }
        val placements = mutableListOf<WordPlacement>()

        // Place first word horizontally at (0, 0)
        placements.add(WordPlacement(sortedWords[0], 0, 0, isHorizontal = true))

        for (i in 1 until sortedWords.size) {
            val word = sortedWords[i]
            var bestPlacement: WordPlacement? = null
            var bestScore = -1

            // Search for intersections with already placed words
            for (placed in placements) {
                for (pIdx in placed.word.indices) {
                    val pChar = placed.word[pIdx]
                    val px = if (placed.isHorizontal) placed.startX + pIdx else placed.startX
                    val py = if (placed.isHorizontal) placed.startY else placed.startY + pIdx

                    for (wIdx in word.indices) {
                        val wChar = word[wIdx]
                        if (pChar == wChar) {
                            // Match! Calculate starting coordinate for new word
                            val newIsHorizontal = !placed.isHorizontal
                            val startX = if (newIsHorizontal) px - wIdx else px
                            val startY = if (newIsHorizontal) py else py - wIdx

                            val placement = WordPlacement(word, startX, startY, newIsHorizontal)
                            if (isValidPlacement(placement, placements)) {
                                val score = calculateScore(placement, placements)
                                if (score > bestScore) {
                                    bestScore = score
                                    bestPlacement = placement
                                }
                            }
                        }
                    }
                }
            }

            if (bestPlacement != null) {
                placements.add(bestPlacement)
            } else {
                // Fallback: Place word horizontally 2 rows below the lowest y-value of any placed word
                var lowestY = 0
                for (p in placements) {
                    val endY = if (p.isHorizontal) p.startY else p.startY + p.word.length - 1
                    if (endY > lowestY) {
                        lowestY = endY
                    }
                }
                placements.add(WordPlacement(word, 0, lowestY + 2, isHorizontal = true))
            }
        }

        // Shift coordinates to be positive and starting from (0,0) to make it easier for UI rendering
        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        for (p in placements) {
            minX = min(minX, p.startX)
            minY = min(minY, p.startY)
        }

        return placements.map { p ->
            WordPlacement(
                word = p.word,
                startX = p.startX - minX,
                startY = p.startY - minY,
                isHorizontal = p.isHorizontal
            )
        }
    }

    private fun isValidPlacement(cand: WordPlacement, existing: List<WordPlacement>): Boolean {
        // Build a temporary board from existing placements
        val board = mutableMapOf<Pair<Int, Int>, Char>()
        for (p in existing) {
            for (idx in p.word.indices) {
                val cx = if (p.isHorizontal) p.startX + idx else p.startX
                val cy = if (p.isHorizontal) p.startY else p.startY + idx
                board[Pair(cx, cy)] = p.word[idx]
            }
        }

        var intersectCount = 0

        // Check every cell of the candidate word
        for (idx in cand.word.indices) {
            val cx = if (cand.isHorizontal) cand.startX + idx else cand.startX
            val cy = if (cand.isHorizontal) cand.startY else cand.startY + idx
            val cChar = cand.word[idx]

            val boardChar = board[Pair(cx, cy)]
            if (boardChar != null) {
                if (boardChar != cChar) {
                    return false // Character mismatch!
                }
                intersectCount++
            } else {
                // Adjacent cell checks (to prevent words from merging side-by-side or touching incorrectly)
                if (cand.isHorizontal) {
                    // Check top and bottom neighbors
                    if (board.containsKey(Pair(cx, cy - 1)) || board.containsKey(Pair(cx, cy + 1))) {
                        return false
                    }
                } else {
                    // Check left and right neighbors
                    if (board.containsKey(Pair(cx - 1, cy)) || board.containsKey(Pair(cx + 1, cy))) {
                        return false
                    }
                }
            }
        }

        // Check head and tail padding to make sure no letters touch head-to-head or tail-to-tail
        if (cand.isHorizontal) {
            if (board.containsKey(Pair(cand.startX - 1, cand.startY)) ||
                board.containsKey(Pair(cand.startX + cand.word.length, cand.startY))
            ) {
                return false
            }
        } else {
            if (board.containsKey(Pair(cand.startX, cand.startY - 1)) ||
                board.containsKey(Pair(cand.startX, cand.startY + cand.word.length))
            ) {
                return false
            }
        }

        // Must intersect with at least one existing word (to keep board connected)
        return intersectCount > 0
    }

    private fun calculateScore(cand: WordPlacement, existing: List<WordPlacement>): Int {
        // High score for more intersections and a tight layout centered around (0,0)
        var intersections = 0
        val board = mutableMapOf<Pair<Int, Int>, Char>()
        for (p in existing) {
            for (idx in p.word.indices) {
                val cx = if (p.isHorizontal) p.startX + idx else p.startX
                val cy = if (p.isHorizontal) p.startY else p.startY + idx
                board[Pair(cx, cy)] = p.word[idx]
            }
        }

        for (idx in cand.word.indices) {
            val cx = if (cand.isHorizontal) cand.startX + idx else cand.startX
            val cy = if (cand.isHorizontal) cand.startY else cand.startY + idx
            if (board.containsKey(Pair(cx, cy))) {
                intersections++
            }
        }

        // Keep layout centered to 0,0
        val distanceToCenter = max(0, max(Math.abs(cand.startX), Math.abs(cand.startY)))
        return intersections * 10 - distanceToCenter
    }
}
