package korablique.recipecalculator.ui.calckeyboard

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.LinearLayout

import korablique.recipecalculator.R
import korablique.recipecalculator.base.logging.Log

const val INTERVAL_BETWEEN_BACKSPACE_HOLD_DELETIONS = 70L

/**
 * Вьюшка клавиатуры-калькулятора, код скопирован https://stackoverflow.com/a/45005691
 */
class CalcKeyboard : LinearLayout {
    // Создадим маппинг кнопок в разметке к их значениям
    private var buttonsValues = mapOf(
            R.id.button_1 to "1",
            R.id.button_2 to "2",
            R.id.button_3 to "3",
            R.id.button_4 to "4",
            R.id.button_5 to "5",
            R.id.button_6 to "6",
            R.id.button_7 to "7",
            R.id.button_8 to "8",
            R.id.button_9 to "9",
            R.id.button_0 to "0",
            R.id.button_point to ".",
            R.id.button_plus to "+",
            R.id.button_minus to "-",
            R.id.button_multiply to "×",
            R.id.button_divide to "÷",
            R.id.button_bracket_left to "(",
            R.id.button_bracket_right to ")")

    data class Connection(
            val input: InputConnection,
            val editText: CalcEditText,
            val hideRequestFun: (CalcEditText)->Unit)

    // Подключение к EditText
    private var connection: Connection? = null

    private var isBackspaseBeingHeld = false

    constructor(context: Context, attrs: AttributeSet?) : this (context, attrs, 0)
    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        // Создаём иерархию с кнопочками из разметки и подключаем её к себе
        LayoutInflater.from(context).inflate(R.layout.calc_keyboard, this, true)
        
        // Слушаем клики на каждую кнопочку
        for (id in buttonsValues.keys) {
            findViewById<View>(id).setOnClickListener(this::onButtonClick)
        }

        findViewById<View>(R.id.button_delete).setOnClickListener(this::onButtonClick)
        findViewById<View>(R.id.button_delete).setOnLongClickListener(this::onBackspaceLongClick)
        findViewById<View>(R.id.button_delete).setOnTouchListener(this::onBackspaceKeyEvent)

        findViewById<View>(R.id.button_enter).setOnClickListener(this::onEnterClick)
        findViewById<View>(R.id.button_next).setOnClickListener(this::onNextFocusClick)
    }

    /**
     * Подключаем EditText к клавиатуре.
     */
    fun connectWith(editText: CalcEditText, hideRequestFun: (CalcEditText)->Unit) {
        this.connection = Connection(
                editText.onCreateInputConnection(EditorInfo()),
                editText,
                hideRequestFun)
        if (findNextFocus() != null) {
            findViewById<View>(R.id.button_enter).visibility = View.GONE
            findViewById<View>(R.id.button_next).visibility = View.VISIBLE
        } else {
            findViewById<View>(R.id.button_enter).visibility = View.VISIBLE
            findViewById<View>(R.id.button_next).visibility = View.GONE
        }
    }

    private fun onButtonClick(v: View) {
        val inputConnection = this.connection?.input
        if (inputConnection == null) {
            // Пока нет подключения ни с каким EditText'ом
            return
        }

        // На последних Андроидах текст при вводе постоянно начинает находится в "composing"
        // состоянии, и если закоммитить новый текст, пока предыдущий "composing", то предыдущий
        // текст сотрётся. Это приведёт к багу - новый текст будет стирать старый/часть старого.
        // Чтобы бага не было, перед коммитом нового текста остановим composing старого.
        inputConnection.finishComposingText()

        if (v.id == R.id.button_delete) {
            performTextDeletion(inputConnection)
        } else {
            val value = buttonsValues[v.id]
            inputConnection.commitText(value, 1/*курсор вправо на 1*/)
        }
    }

    private fun performTextDeletion(inputConnection: InputConnection) {
        // Удалим символы при клике на бекспейс
        val selectedText = inputConnection.getSelectedText(0/*flags*/)
        if (TextUtils.isEmpty(selectedText)) {
            // Никакой текст не выделен, удалим 1 символ перед курсором и 0 после
            inputConnection.deleteSurroundingText(1, 0)
        } else {
            // Текст выделен - удалим его, заменив пустым
            inputConnection.commitText("", 1/*курсор вправо на 1*/)
        }
    }

    private fun onBackspaceLongClick(v: View): Boolean {
        isBackspaseBeingHeld = true
        // Начинаем удалять символы!
        handler.postDelayed(this::onBackspaceHoldingTick, INTERVAL_BETWEEN_BACKSPACE_HOLD_DELETIONS)
        return true
    }

    private fun onBackspaceHoldingTick() {
        val inputConnection = this.connection?.input
        if (inputConnection == null) {
            return
        }
        // Бэкспейс держат в течение INTERVAL_BETWEEN_BACKSPACE_HOLD_DELETIONS, удалим символ
        onButtonClick(findViewById<View>(R.id.button_delete))

        // Если бекспейс ещё нажат, через несколько мс снова удалим символ!
        if (isBackspaseBeingHeld) {
            handler.postDelayed(this::onBackspaceHoldingTick, INTERVAL_BETWEEN_BACKSPACE_HOLD_DELETIONS)
        }
    }

    private fun onBackspaceKeyEvent(v: View, event: MotionEvent): Boolean {
        if (event.action == KeyEvent.ACTION_UP) {
            // С бэкспейса подняли палец (ACTION_UP) - бекспейс больше не нажат
            isBackspaseBeingHeld = false
        }
        return false
    }

    private fun onEnterClick(v: View) {
        val connection = this.connection
        if (connection == null) {
            return
        }
        connection.hideRequestFun.invoke(connection.editText)
    }

    private fun onNextFocusClick(v: View) {
        val connection = this.connection
        if (connection == null) {
            return
        }

        val nextFocus = findNextFocus()
        if (nextFocus == null) {
            Log.e("onNextFocusClick invoked but nextFocus == null")
            connection.hideRequestFun.invoke(connection.editText)
            return
        }
        nextFocus.requestFocus()
    }

    private fun findNextFocus(): View? {
        val connection = this.connection
        if (connection == null) {
            return null
        }

        val nextFocusId = connection.editText.nextFocusDownId
        if (nextFocusId == View.NO_ID) {
            return null
        }

        var root: View = connection.editText
        while (root.parent is View) {
            root = root.parent as View
        }
        return root.findViewById(nextFocusId)
    }
}