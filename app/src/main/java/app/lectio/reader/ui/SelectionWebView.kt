package app.lectio.reader.ui

import android.content.Context
import android.annotation.SuppressLint
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.webkit.WebView
import app.lectio.reader.data.Selection
import org.json.JSONObject
import org.json.JSONTokener

/** Android's selection handles can take over the gesture without a DOM touchend. */
// Programmatic Compose view. All gestures and clicks still go through WebView's implementation.
@SuppressLint("ViewConstructor","ClickableViewAccessibility")
internal class SelectionWebView(context: Context, private val selected: (Selection) -> Unit) : WebView(context) {
    private var pointerDown=false
    private var nativeSelection: ActionMode?=null
    private val readSelection=Runnable {if(!pointerDown) submitSelection()}
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {pointerDown=true;removeCallbacks(readSelection)}
            MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL -> {pointerDown=false;postDelayed(readSelection,900)}
        }
        return super.onTouchEvent(event)
    }
    // WebView retains its own click and accessibility handling; touch is only observed above.
    override fun performClick(): Boolean=super.performClick()
    private fun submitSelection() {
        evaluateJavascript("window.lectioPayload ? JSON.stringify(window.lectioPayload()) : null") {raw ->
            val value=runCatching {JSONTokener(raw).nextValue() as? String}.getOrNull() ?: return@evaluateJavascript
            val json=runCatching {JSONObject(value)}.getOrNull() ?: return@evaluateJavascript
            val text=json.optString("text").trim()
            if(text.isNotBlank()&&text.length<=6000) {
                selected(Selection(text,json.optString("context").take(1200)))
                nativeSelection?.finish()
            }
        }
    }
    override fun startActionMode(callback: ActionMode.Callback): ActionMode?=startActionMode(callback,ActionMode.TYPE_FLOATING)
    override fun startActionMode(callback: ActionMode.Callback, type: Int): ActionMode? {
        val wrapped=object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                val created=callback.onCreateActionMode(mode,menu)
                if(created) {
                    menu.add(Menu.NONE,TRANSLATE_ID,0,"Traducir").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                    nativeSelection=mode
                    postDelayed(readSelection,900)
                }
                return created
            }
            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                val changed=callback.onPrepareActionMode(mode,menu)
                if(menu.findItem(TRANSLATE_ID)==null) menu.add(Menu.NONE,TRANSLATE_ID,0,"Traducir").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                removeCallbacks(readSelection);postDelayed(readSelection,900)
                return changed
            }
            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                if(item.itemId==TRANSLATE_ID) {submitSelection();return true}
                return callback.onActionItemClicked(mode,item)
            }
            override fun onDestroyActionMode(mode: ActionMode) {nativeSelection=null;callback.onDestroyActionMode(mode)}
        }
        return super.startActionMode(wrapped,type)
    }
    override fun destroy() {removeCallbacks(readSelection);nativeSelection=null;super.destroy()}
    companion object {private const val TRANSLATE_ID=0x4c4543}
}
