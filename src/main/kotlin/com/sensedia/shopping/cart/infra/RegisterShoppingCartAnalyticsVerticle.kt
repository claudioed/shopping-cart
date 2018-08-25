package com.sensedia.shopping.cart.infra

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.MultiMap
import io.vertx.core.http.HttpClientRequest

/**
 * @author claudioed on 06/08/18.
 * Project shopping-cart
 */
class RegisterShoppingCartAnalyticsVerticle : AbstractVerticle() {

    override fun start(startFuture: Future<Void>) {
        val analyticsHost = System.getenv("ANALYTICS_HOST") ?: "127.0.0.1"
        val httpClient = vertx.createHttpClient()
        val consumer = vertx.eventBus().consumer<String>("shopping.cart.new.analytics")
        consumer.handler { it ->
            httpClient.postAbs(analyticsHost) {
                it.statusCode()

            }.putHeader("Content-Length",it.body().length.toString())
              .write(it.body(),"utf8")
              .openTracingHeaders(it.headers())
        }
    }

    private fun HttpClientRequest.openTracingHeaders(tracingHeaders: MultiMap) {
        tracingHeaders.forEach {
            this.putHeader(it.key,it.value)
        }
    }

}