package com.sensedia.shopping.cart.infra

import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.sensedia.shopping.cart.domain.ShoppingCart
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.healthchecks.HealthCheckHandler
import io.vertx.ext.healthchecks.HealthChecks
import io.vertx.ext.healthchecks.Status
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerHandler
import io.vertx.kotlin.core.eventbus.DeliveryOptions
import io.vertx.kotlin.redis.RedisOptions
import io.vertx.redis.RedisClient
import java.util.*


/**
 * @author claudioed on 06/08/18.
 * Project shopping-cart
 */
class MainVerticle : AbstractVerticle() {

    private val LOGGER = LoggerFactory.getLogger(MainVerticle::class.java)

    override fun start(startFuture: Future<Void>) {
        Json.mapper.registerModule(KotlinModule())
        vertx.deployVerticle(StoreShoppingCartVerticle())
        vertx.deployVerticle(FindShoppingCartVerticle())
        vertx.deployVerticle(RegisterShoppingCartAnalyticsVerticle())
        val router = router()
        vertx.createHttpServer()
                .requestHandler { router.accept(it) }
                .listen(config().getInteger("http.port", 8080)) { result ->
                    if (result.succeeded()) {
                        startFuture.complete()
                    } else {
                        startFuture.fail(result.cause())
                    }
                }
    }

    private fun router() = Router.router(vertx).apply {
        val healthCheckHandler = HealthCheckHandler.createWithHealthChecks(HealthChecks.create(vertx))
        LOGGER.info("Redis Host : ${System.getenv("REDIS_HOST")} ")
        val config = RedisOptions(host = System.getenv("REDIS_HOST") ?: "127.0.0.1")
        val redis = RedisClient.create(vertx, config)

        healthCheckHandler.register("redis"
        ) { future ->
            redis.get("cart:1") {
                if (it.failed()) {
                    LOGGER.error(it.cause().message)
                    future.fail("database connection failed")
                } else {
                    future.complete(Status.OK())
                }
            }
        }
        get("/carts/healthcheck").handler(healthCheckHandler)
        route().handler(BodyHandler.create())
        route().handler(LoggerHandler.create())
        post("/carts").handler(newShoppingCart)
        get("/carts/:id").handler(shoppingCartById)
    }

    private val newShoppingCart = Handler<RoutingContext> { req ->
        val cart = Json.decodeValue(req.bodyAsString, ShoppingCart::class.java)
        val shoppingCart = cart.copy(id = UUID.randomUUID().toString())
        vertx.eventBus().send("shopping.cart.new", Json.encode(shoppingCart),DeliveryOptions(headers = traceHeaders(req.request())))
        req.response().created(shoppingCart)
    }

    private val shoppingCartById = Handler<RoutingContext> { req ->
        val id = req.pathParam("id")
        vertx.eventBus().send<String>("shopping.cart.find.id", JsonObject().put("id", id)) {
            if (it.failed()) {
                val error = JsonObject().put("error", it.cause().message)
                req.response().setStatusCode(404).endWithJson(error)
            } else {
                val cart = Json.decodeValue(it.result().body(), ShoppingCart::class.java)
                req.response().endWithJson(cart)
            }
        }
    }

    private fun traceHeaders(req: HttpServerRequest): Map<String, String> {
        if(!req.headers().contains("x-request-id")){
            return mapOf()
        }
        return listOf("x-request-id", "x-b3-traceid", "x-b3-spanid", "x-b3-parentspanid", "x-b3-parentspanid", "x-b3-flags", "x-ot-span-context")
                .map { it to req.getHeader(it) }.toMap()
    }

    private fun HttpServerResponse.endWithJson(obj: Any) {
        this.putHeader("Content-Type", "application/json; charset=utf-8").end(Json.encodePrettily(obj))
    }

    private fun HttpServerResponse.created(cart: ShoppingCart) {
        this.statusCode = 201
        this.putHeader("Location", "/carts/${cart.id}").endWithJson(cart)
    }

}