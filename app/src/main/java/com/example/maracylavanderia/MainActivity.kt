package com.example.maracylavanderia

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.File
import java.net.URL

class MainActivity : AppCompatActivity() {
    private val currentVersion = BuildConfig.VERSION_NAME // Obtém a versão dinamicamente
    private lateinit var apkFile: File

    private lateinit var webview: WebView
    private lateinit var loadingText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializa componentes
        webview = findViewById(R.id.webview)
        loadingText = findViewById(R.id.loadingText)

        webview.settings.javaScriptEnabled = true

        // Configura o WebViewClient
        webview.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Esconde a mensagem de carregamento e mostra a WebView
                loadingText.visibility = View.GONE
                webview.visibility = View.VISIBLE
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)

                // Mostra um Toast com erro
                Toast.makeText(
                    this@MainActivity,
                    "Erro ao carregar a página. Verifique sua conexão!",
                    Toast.LENGTH_LONG
                ).show()

                // Atualiza a mensagem de erro
                loadingText.text = "Erro ao carregar o sistema. Verifique a conexão."
            }
        }

        // Carrega a página apenas se não houver estado salvo
        if (savedInstanceState == null) {
            webview.loadUrl("http://10.0.0.40:8080")
        } else {
            webview.restoreState(savedInstanceState)
        }

        // Verifica atualização assim que o app inicia
        CheckUpdateTask().execute("https://raw.githubusercontent.com/seu-usuario/seu-repo/main/update.json")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webview.saveState(outState)
    }

    inner class CheckUpdateTask : AsyncTask<String, Void, JSONObject?>() {
        override fun doInBackground(vararg params: String?): JSONObject? {
            return try {
                val jsonStr = URL(params[0]).readText()
                JSONObject(jsonStr)
            } catch (e: Exception) {
                null
            }
        }

        override fun onPostExecute(result: JSONObject?) {
            if (result != null) {
                val latestVersion = result.getString("version")
                val apkUrl = result.getString("apk_url")
                val changelog = result.getString("changelog")

                if (latestVersion != currentVersion) {
                    showUpdateDialog(apkUrl, changelog)
                }
            }
        }
    }

    private fun showUpdateDialog(apkUrl: String, changelog: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Nova Atualização Disponível")
        builder.setMessage("Alterações:\n$changelog\n\nDeseja atualizar agora?")
        builder.setPositiveButton("Atualizar") { _, _ ->
            downloadAndInstall(apkUrl)
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun downloadAndInstall(apkUrl: String) {
        val fileName = "app-update.apk"
        apkFile = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)

        val request = DownloadManager.Request(Uri.parse(apkUrl))
        request.setTitle("Baixando atualização...")
        request.setDescription("Aguarde o download da nova versão.")
        request.setDestinationUri(Uri.fromFile(apkFile))
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)

        // Espera um tempo e inicia a instalação automática
        Handler(Looper.getMainLooper()).postDelayed({
            installApk()
        }, 5000)
    }

    private fun installApk() {
        if (!apkFile.exists()) {
            Toast.makeText(this, "Arquivo de atualização não encontrado!", Toast.LENGTH_LONG).show()
            return
        }

        val intent = Intent(Intent.ACTION_VIEW)
        val uri = Uri.fromFile(apkFile)
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }
}
