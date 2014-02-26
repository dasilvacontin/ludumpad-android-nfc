package com.dasilvacontin.ludumpad;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;


@SuppressLint("SetJavaScriptEnabled")
public class GamepadActivity extends Activity {
	
	@Override

	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		Intent intent = getIntent();
		String gamepadURL = intent.getStringExtra(Main.GAMEPAD_URL);
		
		WebView webview = new WebView(this);
		webview.getSettings().setJavaScriptEnabled(true);
		setContentView(webview);
		webview.setWebViewClient(new WebViewClient() {
		   public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
		     Log.d("GmaepadWebView", description);
		   }
		 });
		webview.loadUrl(gamepadURL);
		Log.d("GAMEPAD_URL", gamepadURL);
	
	}
	

	public GamepadActivity() {
		// TODO Auto-generated constructor stub
		
		
	}

}
