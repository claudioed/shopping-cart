package com.sensedia.shopping.cart.infra

import com.sensedia.shopping.cart.domain.ShoppingCart
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import java.util.*

/**
 * @author claudioed on 06/08/18.
 * Project shopping-cart
 */
class MainVerticle : AbstractVerticle() {

    override fun start(startFuture: Future<Void>) {
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
        post("/cart").handler(newShoppingCart)
        get("/cart/:id").handler(shoppingCartById)
    }

    private val newShoppingCart = Handler<RoutingContext> { req ->
        val cart = Json.decodeValue(req.bodyAsString, ShoppingCart::class.java)
        val shoppingCart = cart.copy(id = UUID.randomUUID().toString())
        vertx.eventBus().publish("shopping.cart.new", shoppingCart)
        req.response().endWithJson(shoppingCart)
    }

    private val shoppingCartById = Handler<RoutingContext> { req ->
        val id = req.pathParam("id")
        vertx.eventBus().send<ShoppingCart>("shopping.cart.find.id", JsonObject().put("id", id)) {
            val cart = it.result().body()
            req.response().endWithJson(cart)
        }
    }

    private fun HttpServerResponse.endWithJson(obj: Any) {
        this.putHeader("Content-Type", "application/json; charset=utf-8").end(Json.encodePrettily(obj))
    }

}