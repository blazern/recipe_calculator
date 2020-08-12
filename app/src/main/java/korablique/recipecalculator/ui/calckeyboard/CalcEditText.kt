package korablique.recipecalculator.ui.calckeyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.text.InputFilter
import android.text.SpannableString
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatEditText
import com.udojava.evalex.Expression
import korablique.recipecalculator.R
import korablique.recipecalculator.ui.inputfilters.DecimalNumberInputFilter
import korablique.recipecalculator.ui.DecimalUtils.toDecimalString
import korablique.recipecalculator.ui.inputfilters.FunctionalInputFilter
import java.math.BigDecimal
import java.util.regex.Pattern

open class CalcEditText : AppCompatEditText {
    // NOTE: В начале regex'а ".*", в конце ".+" - это не случайно, ".+"
    // там стоит, чтобы не рисовать серенький результат вычисления для
    // строк вида "2+", но рисовать для строк вида "2+2", т.к. "2+ =2" выглядит глупо.
    private val textAcceptableForCalcPreviewRegex = Pattern.compile(""".*[\-+×÷()].+""")
    private val extraOperatorsRegex = Pattern.compile(""".*[\-+*/][\-+*/].*""")
    private val operatorsRegex = Pattern.compile("""[\-+×÷()]""")
    private val operatorsWithoutBracketsRegex = Pattern.compile("""[\-+×÷]""")
    private val numbersFilters = mutableListOf<InputFilter>()
    private var minValue: BigDecimal
    private var maxValue: BigDecimal

    private var currentCalculatedValue: Float? = 0f
    private var currentTextAcceptableForCalcPreview = false

    private val textBounds = Rect()
    private val textWithoutBracketsBounds = Rect()
    private val calculatedValuePaint: Paint

    private var isFullyConstructed = false

    constructor(context: Context): this(context, null)
    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, android.R.attr.editTextStyle)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        // Вытащим значения из XML-разметки
        val typedArray = context.obtainStyledAttributes(
                attrs,
                R.styleable.CalcEditText,
                0, 0)
        val digitsAfterDecimalDot: Int
        try {
            minValue = BigDecimal(typedArray.getFloat(R.styleable.CalcEditText_min_value, Float.MIN_VALUE).toDouble())
            maxValue = BigDecimal(typedArray.getFloat(R.styleable.CalcEditText_max_value, Float.MAX_VALUE).toDouble())
            digitsAfterDecimalDot = typedArray.getInt(R.styleable.CalcEditText_digits_after_decimal_dot, Int.MAX_VALUE)
        } finally {
            typedArray.recycle()
        }

        // Зададим фильтр, не допускающий строки, которые невозможно вычислить
        val inputFilter = FunctionalInputFilter.ofFunction(this::isAcceptableUserInput)
        setFilters(getFilters() + arrayOf(inputFilter))

        if (digitsAfterDecimalDot != Int.MAX_VALUE) {
            numbersFilters += DecimalNumberInputFilter.ofNDigitsAfterPoint(digitsAfterDecimalDot)
        }

        calculatedValuePaint = Paint(paint)
        calculatedValuePaint.color = resources.getColor(R.color.calc_edit_text_calculated_value_color)
        isFullyConstructed = true
    }

    private fun isAcceptableUserInput(str: String): Boolean {
        var str = str

        // Закроем незакрытые скобки и удалим лишние, чтобы пользователь
        // мог эти скобки впоследствии сам дозакрыть, а calcValueAsBigDecimal не сломался
        // от недозакрытых скобочек.
        str = normalizeBrackets(str) ?: return false

        if (str.isEmpty()) {
            // Позволяем очистить вводимый текст
            return true
        }

        val value = calcValueAsBigDecimal(str) ?: return false
        if (!isAllowedByNumbersFilters(str)) {
            return false
        }
        return isAcceptableBigDecimal(value)
    }

    private fun normalizeBrackets(str: String): String? {
        var str = str

        // Вырежем открывающиеся скобочки на конце строки
        var lastExtraLeftBracket = str.length
        for (index in (str.length-1) downTo 0) {
            if (str[index] == '(') {
                lastExtraLeftBracket = index
            } else {
                break
            }
        }
        str = str.substring(0, lastExtraLeftBracket)
        if (str.isEmpty()) {
            return str
        }

        val leftBracketsCount = str.count { it == '(' }
        val rightBracketsCount = str.count { it == ')' }
        if (rightBracketsCount < leftBracketsCount) {
            // Закроем недозакрытые скобочки
            val neededExtraRightBrackets = leftBracketsCount - rightBracketsCount
            if (operatorsWithoutBracketsRegex.matcher(str.last().toString()).matches()) {
                str = str.substring(0, str.length - 1) + ")".repeat(neededExtraRightBrackets) + str.last()
            } else {
                str += ")".repeat(neededExtraRightBrackets)
            }
        } else if (leftBracketsCount < rightBracketsCount) {
            // Откроем недооткрытые скобочки
            str = "(".repeat(rightBracketsCount - leftBracketsCount) + str
        }

        return str
    }

    private fun calcValueAsBigDecimal(inputStr: String): BigDecimal? {
        // Будем считать строку вида "-" нулём (если отрицательные числе приемлимы),
        // т.к. либа "-" не распарсит, но пользователь может находиться в процессе
        // ввода отрицательного числа.
        if (inputStr == "-" && minValue < BigDecimal.ZERO) {
            return BigDecimal.ZERO
        }

        // Заменим операторы на те, о которых знают либы
        val str = inputStr.replace('×', '*').replace('÷', '/')

        // Строки вроде "2++3", "4**" не допускаются (но с единичными операторами
        // допускаются, потому что единичный оператор может обозначать, что пользователь
        // только-только его ввёл и сейчас введет число).
        if (extraOperatorsRegex.matcher(str).matches()) {
            return null
        }

        val lastDigitOrBracket = str.indexOfLast { it in '0'..'9' || it == '(' || it == ')' }
        if (lastDigitOrBracket == -1) {
            return null
        }
        // Обрежем символы справа, если они операторы
        val croppedStr = str.substring(0..lastDigitOrBracket)

        try {
            return Expression(croppedStr).eval()
        } catch (e: Expression.ExpressionException) {
            return null
        } catch (e: ArithmeticException) {
            return null
        }
    }

    private fun isAllowedByNumbersFilters(inputStr: String): Boolean {
        // Раздробим входную строку на подстроки, содержащие только числа,
        // прогоним каждое из этих строк-чисел через numbersFilters.
        // Мотивация - InputFilter'ы в EditText'ах фильтруют всю вводимую пользователем строку целиком,
        // не пытаясь её дробить на части, но нам нужно уметь фильтровать не всю строку целиком,
        // а числа в ней.
        numbersFilters.forEach { numberFilter ->
            // Разделим строку вида "2+(11-4)" на строки ["2", "11", "4"]
            val numbersStrs = inputStr.split(operatorsRegex).filter { it.isNotEmpty() }
            numbersStrs.forEach { numberStr ->
                // mockDest - фальшивое destination для вставки отфильтрованной строки в него,
                // нужно для того, чтобы filter думал, что numberStr куда-то вставляется и
                // отфильтровал бы его хорошенько.
                val mockDest = SpannableString("")
                val filterResult = numberFilter.filter(
                        numberStr, 0, numberStr.length,
                        mockDest, 0, 0)
                // Если numberFilter не вернул null, то ему чем-то не нравится входящая строка.
                // Если входящая строка фильтру чем-то не нравится,
                // то будем считать inputStr неприемлимым.
                if (filterResult != null) {
                    return false
                }
            }
        }
        return true
    }

    private fun isAcceptableBigDecimal(value: BigDecimal): Boolean {
        return value in minValue..maxValue
    }

    /**
     * Отдаёт текущий вычисленный текст.
     */
    fun getCurrentCalculatedValue(): Float? {
        return calcCurrentValue()
    }

    /**
     * Устанавливает числовые границы возможного вычисляемого значения.
     */
    fun setBounds(min: Float, max: Float) {
        minValue = BigDecimal.valueOf(min.toDouble())
        maxValue = BigDecimal.valueOf(max.toDouble())
    }

    override fun onTextChanged(text: CharSequence, start: Int, lengthBefore: Int, lengthAfter: Int) {
        if (!isFullyConstructed) {
            // onTextChanged может вызываться (и вызывается) до окончания работы конструктора,
            // что ломает наш код, который должен выполняться только после завершения конструирования
            // объекта.
            // Сделаем ранний возврат, если объект сконструирован не до конца.
            return
        }
        currentCalculatedValue = calcCurrentValue()
        currentTextAcceptableForCalcPreview =
                textAcceptableForCalcPreviewRegex.matcher(text).matches()
    }

    private fun calcCurrentValue(): Float? {
        val value = calcValueAsBigDecimal(getText().toString()) ?: return null
        if (isAcceptableBigDecimal(value)) {
            return value.toFloat()
        } else {
            return null
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val calculatedValPreview = calculatedValuePreviewText() ?: return
        calculateTextBounds(textBounds)
        val x = textBounds.right.toFloat()
        val y = textBounds.bottom.toFloat()
        val xPadding = drawnCalculatedValueXPadding()

        canvas.drawText(calculatedValPreview, x + xPadding, y, calculatedValuePaint)
    }

    private fun calculatedValuePreviewText(): String? {
        // Нарисуем после введенного пользователем текста серенькое вычисленное значение
        // введённого пользователем текста.
        val currentCalculatedValue = this.currentCalculatedValue
        if (currentCalculatedValue == null || !currentTextAcceptableForCalcPreview) {
            // Если пользовательский текст не вычисляем или не содержит операторов - рисовать не будем.
            return null
        }
        return "=" + toDecimalString(currentCalculatedValue)
    }

    /**
     * Небольшой сдвиг, чтобы рисуемый нами текст не сливался с текстом пользователя.
     */
    private fun drawnCalculatedValueXPadding(): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, resources.displayMetrics)
    }

    // Вычислять границы рисуемого EditText'ом текста дело непростое.
    // Поэтому код вычисления нагло сворован из https://stackoverflow.com/a/52545927
    private fun calculateTextBounds(out: Rect) {
        // NOTE: скобки очень длинные,
        // поэтому не учитываем их в рассчитывании Y позиции отрисовки текста
        val text = getText().toString()
        val textWithoutBrackets = text.replace("(", "").replace(")", "")
        return calculateTextBounds(out, text, textWithoutBrackets)
    }

    // Вычислять границы рисуемого EditText'ом текста дело непростое.
    // Поэтому код вычисления нагло сворован из https://stackoverflow.com/a/52545927
    private fun calculateTextBounds(out: Rect, text: String, textWithoutTallChars: String = text) {
        val textPaint = getPaint()
        textPaint.getTextBounds(text, 0, text.length, out)
        textPaint.getTextBounds(textWithoutTallChars, 0, textWithoutTallChars.length, textWithoutBracketsBounds)

        val baseline = getBaseline()
        out.top = baseline + out.top
        out.bottom = baseline + textWithoutBracketsBounds.bottom
        val startPadding = getPaddingStart()
        out.left += startPadding
        out.right = textPaint.measureText(text).toInt() + startPadding
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val textBounds = Rect()
        val fullTextBounds = Rect()

        val calculatedTextPreview = calculatedValuePreviewText() ?: return
        val fullText = getText().toString() + calculatedTextPreview
        calculateTextBounds(fullTextBounds, fullText)
        fullTextBounds.right += drawnCalculatedValueXPadding().toInt()

        calculateTextBounds(textBounds)
        if (textBounds.width() != fullTextBounds.width()) {
            val textBoundsDiff = fullTextBounds.width() - textBounds.width()
            setMeasuredDimension(measuredWidth + textBoundsDiff, measuredHeight)
        }
    }
}