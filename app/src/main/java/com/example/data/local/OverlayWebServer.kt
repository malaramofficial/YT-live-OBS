package com.example.data.local

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.nio.charset.StandardCharsets

class OverlayWebServer(
    private val port: Int = 8080,
    private val stateJson: () -> String
) {
    @Volatile private var running = false
    private var serverSocket: ServerSocket? = null
    private var thread: Thread? = null

    fun start(): Boolean {
        if (running) return true
        return try {
            serverSocket = ServerSocket(port)
            serverSocket?.reuseAddress = true
            running = true
            thread = Thread {
                while (running) {
                    try {
                        val socket = serverSocket?.accept() ?: break
                        handle(socket)
                    } catch (_: SocketException) {
                        if (running) break
                    } catch (_: Exception) {
                    }
                }
            }.apply {
                name = "malaram-overlay-server"
                isDaemon = true
                start()
            }
            true
        } catch (_: Exception) {
            running = false
            serverSocket?.close()
            serverSocket = null
            false
        }
    }

    fun stop() {
        running = false
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
        thread = null
    }

    fun isRunning(): Boolean = running

    fun localUrl(): String? {
        if (!running) return null
        val host = findLocalIpv4() ?: return null
        return "http://" + host + ":" + port + "/overlay"
    }

    private fun handle(socket: Socket) {
        socket.use { s ->
            s.soTimeout = 3000
            val reader = BufferedReader(InputStreamReader(s.getInputStream(), StandardCharsets.US_ASCII))
            val requestLine = reader.readLine() ?: return
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
            }

            val path = requestLine.split(" ").getOrNull(1)?.substringBefore("?") ?: "/"
            val ok = path == "/overlay" || path == "/" || path == "/state"
            val body: String
            val contentType: String
            when (path) {
                "/state" -> {
                    body = stateJson()
                    contentType = "application/json; charset=utf-8"
                }
                "/overlay", "/" -> {
                    body = overlayHtml()
                    contentType = "text/html; charset=utf-8"
                }
                else -> {
                    body = "Not Found"
                    contentType = "text/plain; charset=utf-8"
                }
            }

            val bytes = body.toByteArray(StandardCharsets.UTF_8)
            val writer = PrintWriter(OutputStreamWriter(s.getOutputStream(), StandardCharsets.US_ASCII))
            writer.print("HTTP/1.1 " + (if (ok) "200 OK" else "404 Not Found") + "\r\n")
            writer.print("Content-Type: " + contentType + "\r\n")
            writer.print("Content-Length: " + bytes.size + "\r\n")
            writer.print("Cache-Control: no-store\r\n")
            writer.print("Connection: close\r\n\r\n")
            writer.flush()
            s.getOutputStream().write(bytes)
            s.getOutputStream().flush()
        }
    }

    private fun overlayHtml(): String = """
<!doctype html>
<html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<style>
html,body{margin:0;width:100%;height:100%;overflow:hidden;background:transparent;font-family:Arial,sans-serif;color:#fff}
#root{width:100%;height:100%;position:relative}
.top{position:absolute;left:3%;right:3%;top:5%;display:flex;justify-content:space-between;align-items:center}
.live{background:#ff1744;border-radius:8px;padding:8px 14px;font-weight:800}.channel{font-size:30px;font-weight:800;text-shadow:0 2px 8px #000}
.viewers{background:rgba(0,0,0,.65);border-radius:8px;padding:8px 14px}.card{position:absolute;left:5%;right:5%;bottom:10%;background:rgba(10,12,18,.86);border:1px solid rgba(255,255,255,.18);border-radius:18px;padding:22px}
.question{font-size:30px;font-weight:700;margin-bottom:16px}.option{margin:8px 0}.bar{height:22px;background:rgba(255,255,255,.12);border-radius:11px;overflow:hidden}.fill{height:100%;background:#ff1744}
.row{display:flex;justify-content:space-between;font-size:18px;margin-bottom:5px}.announcement{position:absolute;left:5%;right:5%;bottom:2%;text-align:center;font-size:20px;font-weight:700}
</style></head><body><div id="root"><div id="content"></div></div>
<script>
const esc=s=>String(s??'').replace(/[&<>"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c]));
async function refresh(){try{
const d=await fetch('/state',{cache:'no-store'}).then(r=>r.json());let h='<div class="top">';
h+='<div class="live">🔴 LIVE</div><div class="channel">'+esc(d.channel)+'</div><div class="viewers">👥 '+Number(d.viewers||0).toLocaleString('en-IN')+'</div></div>';
h+='<div class="card">';if(d.question)h+='<div class="question">❓ '+esc(d.question)+'</div>';
if(d.poll&&d.poll.length){const total=d.poll.reduce((a,o)=>a+Number(o.votes||0),0);d.poll.forEach(o=>{const pct=total?Math.round(Number(o.votes||0)*100/total):0;
h+='<div class="option"><div class="row"><span>'+esc(o.text)+'</span><span>'+pct+'% ('+Number(o.votes||0)+')</span></div><div class="bar"><div class="fill" style="width:'+pct+'%"></div></div></div>';});}
h+='</div>';if(d.announcement)h+='<div class="announcement">📢 '+esc(d.announcement)+'</div>';
document.getElementById('content').innerHTML=h;}catch(e){}}refresh();setInterval(refresh,1000);
</script></body></html>
""".trimIndent()

    private fun findLocalIpv4(): String? {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val ni = interfaces.nextElement()
            if (!ni.isUp || ni.isLoopback) continue
            val addresses = ni.inetAddresses
            while (addresses.hasMoreElements()) {
                val address = addresses.nextElement()
                if (address is Inet4Address && address.isSiteLocalAddress && !address.isLoopbackAddress) {
                    return address.hostAddress
                }
            }
        }
        return null
    }
}
