package dk.cachet.kodegen.example.infrastructure

import dk.cachet.kodegen.example.application.dice.kodegen.DiceServiceModule
import dk.cachet.kodegen.example.application.oracle.kodegen.OracleServiceModule
import dk.cachet.kodegen.example.application.shapes.kodegen.ShapesServiceModule
import dk.cachet.kodegen.example.DiceServiceImpl
import dk.cachet.kodegen.example.domain.oracle.AnswerRepositoryImpl
import dk.cachet.kodegen.example.application.oracle.OracleServiceImpl
import dk.cachet.kodegen.example.application.shapes.ShapesServiceImpl
import dk.cachet.kodegen.example.infrastructure.shapes.json
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.authenticate
import io.ktor.auth.basic
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.serialization.SerializationConverter
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.jetty.Jetty
import org.eclipse.jetty.server.ServerConnector
import java.lang.IllegalArgumentException

// Sets the main function to the startup of a Jetty engine (i.e. boots up a HTTP server)
// fun main(args: Array<String>): Unit = EngineMain.main(args)

// Sets the main function to the rad main function (configuration and engine startup)
// fun main(args: Array<String>): Unit = radMain(args)

fun main() {
	val environment = applicationEngineEnvironment {
		module {
			mainModule()
			DiceServiceModule(DiceServiceImpl(), "basic")
			OracleServiceModule(
				OracleServiceImpl(
					AnswerRepositoryImpl()
				), "basic")
			ShapesServiceModule(ShapesServiceImpl(), "basic")
		}
	}
	val server = embeddedServer(Jetty, environment) {
		configureServer = {
			this.addConnector(ServerConnector(this).apply { port = 8080 })
		}
	}
	server.start(wait = true)
}
fun Application.mainModule(): Unit {
	install(ContentNegotiation) {
		register(ContentType.Application.Json, SerializationConverter(json))
	}

	install(Authentication)
	{
		basic(name = "basic") {
			realm = "SampleServer"
			validate { credentials ->
				if(credentials.name == "admin" && credentials.password == "adminP") {
					UserIdPrincipal(credentials.name)
				}
				else {
					null
				}
			}
		}
	}

	install(StatusPages) {
		exception<IllegalArgumentException> {
			call.respond(HttpStatusCode.BadRequest)
		}
		exception<NoSuchElementException> {
			call.respond(HttpStatusCode.NotFound)
		}
	}

	routing {
		authenticate("basic") {
			get("/") {
				call.respondText("In root", contentType = ContentType.Text.Plain)
			}
		}
	}
}