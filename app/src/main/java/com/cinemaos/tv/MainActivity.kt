package com.cinemaos.tv

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.webkit.*
import android.widget.ProgressBar
import androidx.fragment.app.FragmentActivity

class MainActivity : FragmentActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    // --- Virtual Pointer Variables ---
    private var isPointerMode = false
    private var cursorX = 500f
    private var cursorY = 500f
    private val cursorSpeed = 35f

    private val dpadJS = """
        (function() {
            if (window.__dpadInjected) return;
            window.__dpadInjected = true;
            var currentIndex = 0;
            function getFocusables() {
                return Array.from(document.querySelectorAll('a, button, input, select, [tabindex], [role="button"], [role="link"], [role="menuitem"]'))
                    .filter(el => {
                        var rect = el.getBoundingClientRect();
                        return rect.width > 0 && rect.height > 0 &&
                               window.getComputedStyle(el).visibility !== 'hidden' &&
                               window.getComputedStyle(el).display !== 'none';
                    });
            }
            function focusElement(el) {
                if (!el) return;
                el.focus();
                el.scrollIntoView({ block: 'center', behavior: 'smooth' });
                el.style.outline = '3px solid #e50914';
                el.style.outlineOffset = '2px';
                el.style.borderRadius = '4px';
            }
            function clearHighlights() {
                document.querySelectorAll('[data-dpad-focused]').forEach(el => {
                    el.style.outline = '';
                    el.style.outlineOffset = '';
                    el.removeAttribute('data-dpad-focused');
                });
            }
            window.dpadMove = function(direction) {
                var focusables = getFocusables();
                if (focusables.length === 0) return;
                var focused = document.activeElement;
                var idx = focusables.indexOf(focused);
                clearHighlights();
                if (direction === 'down' || direction === 'right') {
                    currentIndex = (idx < 0) ? 0 : Math.min(idx + 1, focusables.length - 1);
                } else {
                    currentIndex = (idx < 0) ? 0 : Math.max(idx - 1, 0);
                }
                var target = focusables[currentIndex];
                target.setAttribute('data-dpad-focused', 'true');
                focusElement(target);
            };
            window.dpadClick = function() {
                var el = document.activeElement;
                if (el && el !== document.body) { el.click(); }
                else {
                    var focusables = getFocusables();
                    if (focusables.length > 0) focusElement(focusables[0]);
                }
            };
            setTimeout(function() {
                var focusables = getFocusables();
                if (focusables.length > 0) focusElement(focusables[0]);
            }, 800);
        })();
    """.trimIndent()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        progressBar = findViewById(R.id.progressBar)
        webView = findViewById(R.id.webView)
        
        setupWebView()
        webView.loadUrl("https://cinemaos.live")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            userAgentString = "Mozilla/5.0 (Linux; Android 11; SHIELD Android TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36"
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                view.evaluateJavascript(dpadJS, null)
            }
            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                view.loadUrl(request.url.toString())
                return true
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progressBar.progress = newProgress
                if (newProgress == 100) progressBar.visibility = View.GONE
            }
        }
    }

    // --- Combined Input Handling ---
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val cursorView = findViewById<View>(R.id.virtual_cursor)

        // 1. Toggle Button: Press MENU to switch modes
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            isPointerMode = !isPointerMode
            cursorView?.visibility = if (isPointerMode) View.VISIBLE else View.GONE
            return true
        }

        // 2. If Pointer Mode is ON, move the red dot
        if (isPointerMode) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> { cursorY -= cursorSpeed; updateCursorPosition(cursorView); return true }
                KeyEvent.KEYCODE_DPAD_DOWN -> { cursorY += cursorSpeed; updateCursorPosition(cursorView); return true }
                KeyEvent.KEYCODE_DPAD_LEFT -> { cursorX -= cursorSpeed; updateCursorPosition(cursorView); return true }
                KeyEvent.KEYCODE_DPAD_RIGHT -> { cursorX += cursorSpeed; updateCursorPosition(cursorView); return true }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> { simulateClick(cursorX, cursorY); return true }
            }
        } 
        // 3. If Pointer Mode is OFF, use your original JS Web Navigation
        else {
            val direction = when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP    -> "up"
                KeyEvent.KEYCODE_DPAD_DOWN  -> "down"
                KeyEvent.KEYCODE_DPAD_LEFT  -> "left"
                KeyEvent.KEYCODE_DPAD_RIGHT -> "right"
                else -> null
            }
            if (direction != null) {
                webView.evaluateJavascript("window.dpadMove('$direction');", null)
                return true
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                webView.evaluateJavascript("window.dpadClick();", null)
                return true
            }
        }

        // 4. Handle the Back button normally
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView.canGoBack()) { 
                webView.goBack()
                return true 
            }
        }
        
        return super.onKeyDown(keyCode, event)
    }

    // --- Pointer Visual and Touch Logic ---
    private fun updateCursorPosition(cursorView: View?) {
        if (cursorView == null) return
        cursorView.x = cursorX
        cursorView.y = cursorY
    }

    private fun simulateClick(x: Float, y: Float) {
        val downTime = SystemClock.uptimeMillis()
        val eventTime = SystemClock.uptimeMillis()
        
        val downEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0)
        window.decorView.dispatchTouchEvent(downEvent)
        
        val upEvent = MotionEvent.obtain(downTime, eventTime + 50, MotionEvent.ACTION_UP, x, y, 0)
        window.decorView.dispatchTouchEvent(upEvent)
        
        downEvent.recycle()
        upEvent.recycle()
    }

    override fun onResume() { super.onResume(); webView.onResume() }
    override fun onPause() { super.onPause(); webView.onPause() }
}
