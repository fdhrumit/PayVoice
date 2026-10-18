package com.payvoice.app.utils

import kotlin.math.roundToLong

object NumberToWordsConverter {

    // English arrays
    private val EN_UNITS = arrayOf(
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
        "Seventeen", "Eighteen", "Nineteen"
    )
    private val EN_TENS = arrayOf(
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    )

    // Hindi arrays (0 to 99 in Hindi words)
    private val HI_WORDS = arrayOf(
        "शून्य", "एक", "दो", "तीन", "चार", "पाँच", "छह", "सात", "आठ", "नौ",
        "दस", "ग्यारह", "बारह", "तेरह", "चौदह", "पंद्रह", "सोलह", "सत्रह", "अठारह", "उन्नीस",
        "बीस", "इक्कीस", "बाईस", "तेईस", "चौबीस", "पच्चीस", "छब्बीस", "सत्ताईस", "अट्ठाइस", "उनतीस",
        "तीस", "इकतीस", "बत्तीस", "तैंतीस", "चौंतीस", "पैंतीस", "छत्तीस", "सैंतीस", "अड़तीस", "उनतालीस",
        "चालीस", "इकतालीस", "बयालीस", "तैंतालीस", "चवालीस", "पैंतालीस", "छियालीस", "सैंतालीस", "अड़तालीस", "उनचास",
        "पचास", "इक्यावन", "बावन", "तिरेपन", "चौवन", "पचपन", "छप्पन", "सत्तावन", "अट्ठावन", "उनसठ",
        "साठ", "इकसठ", "बासठ", "तिरेसठ", "चौंसठ", "पैंसठ", "छियासठ", "सरसठ", "अड़सठ", "उनहत्तर",
        "सत्तर", "इकहत्तर", "बहत्तर", "तिहत्तर", "चौहत्तर", "पचहत्तर", "छिहत्तर", "सतहत्तर", "अठहत्तर", "उन्नासी",
        "अस्सी", "इक्यासी", "बयासी", "तिरासी", "चौरासी", "पचासी", "छियासी", "सत्तासी", "अट्ठासी", "नवासी",
        "नब्बे", "इक्यानवे", "बानवे", "तिरानवे", "चौरानवे", "पंचानवे", "छियानवे", "सत्तानवे", "अट्ठानवे", "निन्यानवे"
    )

    // Gujarati arrays (0 to 99 in Gujarati words)
    private val GU_WORDS = arrayOf(
        "શૂન્ય", "એક", "બે", "ત્રણ", "ચાર", "પાંચ", "છ", "સાત", "આઠ", "નવ",
        "દસ", "અગિયાર", "બાર", "તેર", "ચૌદ", "પંદર", "સોળ", "સત્તર", "અઢાર", "ઓગણીસ",
        "વીસ", "એકવીસ", "બાવીસ", "ત્રેવીસ", "ચોવીસ", "પચ્ચીસ", "છવ્વીસ", "સત્તાવીસ", "અઠ્ઠાવીસ", "ઓગણત્રીસ",
        "ત્રીસ", "એકત્રીસ", "બત્રીસ", "તેત્રીસ", "ચોત્રીસ", "પાંત્રીસ", "છત્રીસ", "સાડત્રીસ", "ઓગણચાલીસ", "ચાલીસ",
        "એકતાલીસ", "બેતાલીસ", "તેતાલીસ", "ચુંમાલીસ", "પિસ્તાલીસ", "છેતાલીસ", "સુડતાલીસ", "અડતાલીસ", "ઓગણપચાસ", "પચાસ",
        "એકાવન", "બાવન", "ત્રેપન", "ચોપન", "પંચાવન", "છપ્પન", "સત્તાવન", "અઠાવન", "ઓગણસાઠ", "સાઠ",
        "એકસઠ", "બાસઠ", "ત્રેસઠ", "ચોસઠ", "પાંસઠ", "છાસઠ", "સડસઠ", "અડસઠ", "અગણોસિત્તેર", "સિત્તેર",
        "એકોતેર", "બોતેર", "તોતેર", "ચોતેર", "પંચોતેર", "છોતેર", "સંતોતેર", "ઇઠોતેર", "ઓગણાએંસી", "એંસી",
        "એક્યાસી", "બ્યાસી", "ત્યાસી", "ચોર્યાસી", "પંચાસી", "છ્યાસી", "સિત્યાસી", "ઈઠ્યાસી", "નેવ્યાંસી", "નેવું",
        "એકાણું", "બાણું", "ત્રાણું", "ચોરાણું", "પંચાણું", "છન્નું", "સત્તાણું", "અઠ્ઠાણું", "નવ્વાણું"
    )

    fun convertAmountToSpeech(
        amount: Double,
        language: String,
        senderName: String = "",
        announceSender: Boolean = true
    ): String {
        val totalPaise = (amount * 100).roundToLong()
        val rupees = totalPaise / 100
        val paise = (totalPaise % 100).toInt()

        return when (language.lowercase()) {
            "hi" -> formatHindi(rupees, paise, senderName, announceSender)
            "gu" -> formatGujarati(rupees, paise, senderName, announceSender)
            else -> formatEnglish(rupees, paise, senderName, announceSender)
        }
    }

    // --- ENGLISH ---
    private fun formatEnglish(rupees: Long, paise: Int, senderName: String, announceSender: Boolean): String {
        val rupeeWords = numberToEnglishWords(rupees)
        val sb = StringBuilder()
        if (rupeeWords.isNotBlank()) {
            sb.append(rupeeWords).append(" rupees")
        }
        if (paise > 0) {
            val paiseWords = numberToEnglishWords(paise.toLong())
            if (sb.isNotEmpty()) sb.append(" ")
            sb.append(paiseWords).append(" paise")
        }
        if (sb.isEmpty()) {
            sb.append("Zero rupees")
        }
        sb.append(" received")
        if (announceSender && senderName.isNotBlank() && senderName != "Customer") {
            sb.append(" from ").append(senderName)
        }
        return sb.toString()
    }

    private fun numberToEnglishWords(num: Long): String {
        if (num == 0L) return ""
        val sb = StringBuilder()

        val crore = num / 10000000
        var rem = num % 10000000

        val lakh = rem / 100000
        rem %= 100000

        val thousand = rem / 1000
        rem %= 1000

        val hundred = rem / 100
        rem %= 100

        if (crore > 0) sb.append(numberToEnglishWords(crore)).append(" Crore ")
        if (lakh > 0) sb.append(numberToEnglishWords(lakh)).append(" Lakh ")
        if (thousand > 0) sb.append(numberToEnglishWords(thousand)).append(" Thousand ")
        if (hundred > 0) sb.append(numberToEnglishWords(hundred)).append(" Hundred ")

        if (rem > 0) {
            if (rem < 20) {
                sb.append(EN_UNITS[rem.toInt()]).append(" ")
            } else {
                sb.append(EN_TENS[(rem / 10).toInt()]).append(" ")
                val unit = (rem % 10).toInt()
                if (unit > 0) sb.append(EN_UNITS[unit]).append(" ")
            }
        }
        return sb.toString().trim()
    }

    // --- HINDI ---
    private fun formatHindi(rupees: Long, paise: Int, senderName: String, announceSender: Boolean): String {
        val rupeeWords = numberToHindiWords(rupees)
        val sb = StringBuilder()
        if (announceSender && senderName.isNotBlank() && senderName != "Customer") {
            sb.append(senderName).append(" से ")
        }
        if (rupeeWords.isNotBlank()) {
            sb.append(rupeeWords).append(" रुपये ")
        }
        if (paise > 0) {
            val paiseWords = if (paise < HI_WORDS.size) HI_WORDS[paise] else paise.toString()
            sb.append(paiseWords).append(" पैसे ")
        }
        if (rupeeWords.isBlank() && paise == 0) {
            sb.append("शून्य रुपये ")
        }
        sb.append("प्राप्त हुए")
        return sb.toString().trim()
    }

    private fun numberToHindiWords(num: Long): String {
        if (num == 0L) return ""
        val sb = StringBuilder()

        val crore = num / 10000000
        var rem = num % 10000000

        val lakh = rem / 100000
        rem %= 100000

        val thousand = rem / 1000
        rem %= 1000

        val hundred = rem / 100
        rem %= 100

        if (crore > 0) sb.append(numberToHindiWords(crore)).append(" करोड़ ")
        if (lakh > 0) sb.append(numberToHindiWords(lakh)).append(" लाख ")
        if (thousand > 0) sb.append(numberToHindiWords(thousand)).append(" हज़ार ")
        if (hundred > 0) {
            val hWord = if (hundred.toInt() < HI_WORDS.size) HI_WORDS[hundred.toInt()] else hundred.toString()
            sb.append(hWord).append(" सौ ")
        }

        if (rem > 0) {
            val remInt = rem.toInt()
            val w = if (remInt < HI_WORDS.size) HI_WORDS[remInt] else remInt.toString()
            sb.append(w).append(" ")
        }

        return sb.toString().trim()
    }

    // --- GUJARATI ---
    private fun formatGujarati(rupees: Long, paise: Int, senderName: String, announceSender: Boolean): String {
        val rupeeWords = numberToGujaratiWords(rupees)
        val sb = StringBuilder()
        if (announceSender && senderName.isNotBlank() && senderName != "Customer") {
            sb.append(senderName).append(" તરફથી ")
        }
        if (rupeeWords.isNotBlank()) {
            sb.append(rupeeWords).append(" રૂપિયા ")
        }
        if (paise > 0) {
            val paiseWords = if (paise < GU_WORDS.size) GU_WORDS[paise] else paise.toString()
            sb.append(paiseWords).append(" પૈસા ")
        }
        if (rupeeWords.isBlank() && paise == 0) {
            sb.append("શૂન્ય રૂપિયા ")
        }
        sb.append("મળ્યા")
        return sb.toString().trim()
    }

    private fun numberToGujaratiWords(num: Long): String {
        if (num == 0L) return ""
        val sb = StringBuilder()

        val crore = num / 10000000
        var rem = num % 10000000

        val lakh = rem / 100000
        rem %= 100000

        val thousand = rem / 1000
        rem %= 1000

        val hundred = rem / 100
        rem %= 100

        if (crore > 0) sb.append(numberToGujaratiWords(crore)).append(" કરોડ ")
        if (lakh > 0) sb.append(numberToGujaratiWords(lakh)).append(" લાખ ")
        if (thousand > 0) sb.append(numberToGujaratiWords(thousand)).append(" હજાર ")
        if (hundred > 0) {
            val hWord = if (hundred.toInt() < GU_WORDS.size) GU_WORDS[hundred.toInt()] else hundred.toString()
            sb.append(hWord).append(" સો ")
        }

        if (rem > 0) {
            val remInt = rem.toInt()
            val w = if (remInt < GU_WORDS.size) GU_WORDS[remInt] else remInt.toString()
            sb.append(w).append(" ")
        }

        return sb.toString().trim()
    }
}
