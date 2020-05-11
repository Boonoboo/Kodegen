package dk.cachet.rad.example.infrastructure

import dk.cachet.rad.example.infrastructure.dice.DiceServiceImpl
import dk.cachet.rad.example.infrastructure.dice.rad.DiceServiceImplModule
import dk.cachet.rad.example.infrastructure.oracle.AnswerRepository
import dk.cachet.rad.example.infrastructure.oracle.OracleServiceImpl
import dk.cachet.rad.example.infrastructure.oracle.rad.OracleServiceImplModule
import dk.cachet.rad.example.infrastructure.shapes.ShapesServiceImpl
import dk.cachet.rad.example.infrastructure.shapes.rad.ShapesServiceImplModule
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.*
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.serialization.DefaultJsonConfiguration
import io.ktor.serialization.SerializationConverter
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.jetty.Jetty
import kotlinx.serialization.json.Json
import org.eclipse.jetty.server.ServerConnector

// Sets the main function to the startup of a Jetty engine (i.e. boots up a HTTP server)
// fun main(args: Array<String>): Unit = EngineMain.main(args)

// Sets the main function to the rad main function (configuration and engine startup)
// fun main(args: Array<String>): Unit = radMain(args)

fun main() {
	val environment = applicationEngineEnvironment {
		module {
			mainModule()
			DiceServiceImplModule(DiceServiceImpl())
			OracleServiceImplModule(OracleServiceImpl(AnswerRepository()))
			ShapesServiceImplModule(ShapesServiceImpl())
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
		register(ContentType.Application.Json, SerializationConverter(Json(DefaultJsonConfiguration)))
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

	routing {
		authenticate("basic") {
			get("/") {
				call.respondText("In root", contentType = ContentType.Text.Plain)
			}
		}
	}
}