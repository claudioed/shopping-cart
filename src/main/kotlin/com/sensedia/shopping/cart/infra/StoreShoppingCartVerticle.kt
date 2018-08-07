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
        val config = RedisOptions(host = "127.0.0.1")
        val redis = RedisClient.create(vertx, config)
        val consumer  = vertx.eventBus().consumer<ShoppingCart>("shopping.cart.new")
        consumer.handler {
            val message  = it
            val cart = it.body()
            redis.set("cart:${cart.id}",Json.encode(cart)) {
                message.reply(Json.encode(cart))
            }
        }
    }

}