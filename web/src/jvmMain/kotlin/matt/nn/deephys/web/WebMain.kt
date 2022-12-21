package matt.nn.deephys.web

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.html.respondHtml
import io.ktor.server.http.content.file
import io.ktor.server.http.content.static
import io.ktor.server.netty.Netty
import io.ktor.server.request.acceptEncoding
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.head
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.style
import kotlinx.html.unsafe
import matt.file.construct.mFile
import matt.file.thismachine.thisMachine
import matt.model.code.sys.NEW_MAC
import matt.model.code.valjson.ValJson
import matt.mstruct.rstruct.appName
import org.slf4j.LoggerFactory
import kotlin.concurrent.thread

private object Context: DeephysWebMain

fun main() {
  println("hello deephys website!")

  val context: LoggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
  context.loggerList.forEach {
	it.level = Level.INFO
  }
  val server = embeddedServer(Netty, port = System.getenv("PORT")?.toInt() ?: run {
	require(thisMachine is NEW_MAC)
	ValJson.Port.localKtorTest
  }) {
	routing {
	  val mainJsFile = "$appName.js"
	  static {
		/*this.staticRootFolder*/
		file(mainJsFile)
		/*file("web.js.map")*/ /*helps with debugging! but I was using the wrong task, so I no longer have it*/
	  }
	  get("/") {
		call.respondHtml {
		  head {
			script(src = mainJsFile) {

			}
			style {

			}
		  }
		  body {
			style {
			  unsafe {
				+"body {background-color: black;}"
			  }
			}
			p {
			  +"here is some text"
			}
			a {
			  this.href = "/text"
			  +"see some text"
			}
		  }
		}
	  }
	  get("text") {
		call.respond("hello Deephys, now I have :k:file")
	  }
	  get("file") {
		call.respondFile(baseDir = mFile("baseDir"), fileName = "fileName")
	  }
	  get("params") {
		@Suppress("UNUSED_VARIABLE") val aParam = call.request.queryParameters["A_PARAM"]
	  }
	  get("decode") {
		call.request.acceptEncoding()
		@Suppress("UNUSED_VARIABLE") val r = call.receive<String>()
	  }
	  get("can be client if I want") {
		"""
		  val client = HttpClient(CIO)
		  		val reqURL = "someURL"
		  		client.request(reqURL)
		""".trimIndent()
	  }

	}
  }
  thread(isDaemon = true) {
	do {
	  val line = readln()
	  println("stdin: $line")
	} while (line.trim() != "kill")
	server.stop()
  }
  server.start(wait = true)
  println("goodbye deephy website...")
}