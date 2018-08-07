package com.sensedia.shopping.cart.infra

import com.sensedia.shopping.cart.domain.ShoppingCart
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.get
import io.vertx.kotlin.redis.RedisOptions
import io.vertx.redis.RedisClient

/**
 * @author claudioed on 06/08/18.
 * Project shopping-cart
 */
class FindShoppingCartVerticle : AbstractVerticle() {

    override fun start(startFuture: Future<Void>) {
        val config = RedisOptions(host = "127.0.0.1")
        val redis = RedisClient.create(vertx, config)
        val consumer  = vertx.eventBus().consumer<JsonObject>("shopping.cart.find.id")
        consumer.handler {
            val message  = it
            val id = it.body().get<String>("id")
            redis.get("cart:$id") {
                val cart = Json.decodeValue(it.result(),ShoppingCart::class.java)
                message.reply(cart)
            }
        }
    }

}