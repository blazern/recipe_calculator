package korablique.recipecalculator.util

import java.lang.StringBuilder

private val russianToEnglishMap = mapOf(
        'А' to "A",
        'Б' to "B",
        'В' to "V",
        'Г' to "G",
        'Д' to "D",
        'Е' to "E",
        'Ё' to "Yo",
        'Ж' to "Zh",
        'З' to "Z",
        'И' to "I",
        'Й' to "Y",
        'К' to "K",
        'Л' to "L",
        'М' to "M",
        'Н' to "N",
        'О' to "O",
        'П' to "P",
        'Р' to "R",
        'С' to "S",
        'Т' to "T",
        'У' to "U",
        'Ф' to "F",
        'Х' to "H",
        'Ц' to "C",
        'Ч' to "Ch",
        'Ш' to "Sh",
        'Щ' to "Shch",
        'Ъ' to "",
        'Ы' to "Y",
        'Ь' to "",
        'Э' to "E",
        'Ю' to "Yu",
        'Я' to "Ya",
        'а' to "a",
        'б' to "b",
        'в' to "v",
        'г' to "g",
        'д' to "d",
        'е' to "e",
        'ё' to "yo",
        'ж' to "zh",
        'з' to "z",
        'и' to "i",
        'й' to "y",
        'к' to "k",
        'л' to "l",
        'м' to "m",
        'н' to "n",
        'о' to "o",
        'п' to "p",
        'р' to "r",
        'с' to "s",
        'т' to "t",
        'у' to "u",
        'ф' to "f",
        'х' to "h",
        'ц' to "c",
        'ч' to "ch",
        'ш' to "sh",
        'щ' to "shch",
        'ъ' to "",
        'ы' to "y",
        'ь' to "",
        'э' to "e",
        'ю' to "yu",
        'я' to "ya")

fun transliterateRussian(str: String): String {
    val builder = StringBuilder(str.length)
    for (letter in str) {
        val transliteratedLetter = russianToEnglishMap.get(letter)
        if (transliteratedLetter != null) {
            builder.append(transliteratedLetter)
        } else {
            builder.append(letter)
        }
    }
    return builder.toString()
}

