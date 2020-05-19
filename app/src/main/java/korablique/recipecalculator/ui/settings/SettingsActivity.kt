package korablique.recipecalculator.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import korablique.recipecalculator.R
import korablique.recipecalculator.base.BaseActivity

class SettingsActivity : BaseActivity() {
    override fun getLayoutId(): Int = R.layout.activity_settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<View>(R.id.button_close).setOnClickListener {
            finish()
        }
        findViewById<View>(R.id.button_delete).visibility = View.GONE
        findViewById<TextView>(R.id.title_text).text = getString(R.string.settings_activity_title)
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.activity_settings_fragment_container, SettingsFragment())
                .commit()
    }

    companion object {
        @JvmStatic
        fun start(activity: BaseActivity) {
            val intent = Intent(activity, SettingsActivity::class.java)
            activity.startActivity(intent)
        }
    }
}