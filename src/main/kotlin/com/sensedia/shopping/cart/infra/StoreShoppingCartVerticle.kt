package com.sensedia.shopping.cart.infra

import com.sensedia.shopping.cart.domain.ShoppingCart
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.json.Json
import io.vertx.kotlin.redis.RedisOptions
import io.vertx.redis.RedisClient

/**
 * @author claudioed on 06/08/18.
 * Project shopping-cart
 */
class StoreShoppingCartVerticle : AbstractVerticle() {

    override fun start(startFuture: Future<Void>) {
        val config = RedisOptions(host = System.getenv("REDIS_HOST") ?: "127.0.0.1")
        val redis = RedisClient.create(vertx, config)
        val consumer  = vertx.eventBus().consumer<String>("shopping.cart.new")
        consumer.handler { it ->
            val message  = it
            val cart = Json.decodeValue(it.body(),ShoppingCart::class.java)
            redis.set("cart:${cart.id}",Json.encode(cart)) {
                message.reply(Json.encode(cart))
                vertx.eventBus().send("shopping.cart.new.analytics",Json.encode(cart))
            }
        }
    }

}