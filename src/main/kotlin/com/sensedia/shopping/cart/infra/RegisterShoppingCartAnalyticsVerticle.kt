package com.sensedia.shopping.cart.infra

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.MultiMap
import io.vertx.core.http.HttpClientRequest
import io.vertx.core.logging.LoggerFactory

/**
 * @author claudioed on 06/08/18.
 * Project shopping-cart
 */
class RegisterShoppingCartAnalyticsVerticle : AbstractVerticle() {

    private val LOGGER = LoggerFactory.getLogger(RegisterShoppingCartAnalyticsVerticle::class.java)

    override fun start(startFuture: Future<Void>) {
        val analyticsHost = System.getenv("ANALYTICS_HOST") ?: "127.0.0.1"
        val httpClient = vertx.createHttpClient()
        val consumer = vertx.eventBus().consumer<String>("shopping.cart.new.analytics")
        consumer.handler { it ->
            LOGGER.info(" New shopping cart analytics data...")
            LOGGER.info(" Message Headers ${it.headers()}")
            httpClient.postAbs(analyticsHost) {
                LOGGER.info("Status Code ${it.statusCode()}")
            }.putHeader("Content-Length",it.body().length.toString()).openTracingHeaders(it.headers())
              .end(it.body(),"utf8")
        }
    }

    private fun HttpClientRequest.openTracingHeaders(tracingHeaders: MultiMap): HttpClientRequest{
        tracingHeaders.forEach {
            LOGGER.info( "key ${it.key} value ${it.value}")
            this.putHeader(it.key,it.value)
        }
        return this
    }

}