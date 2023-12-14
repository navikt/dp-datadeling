package no.nav.dagpenger.datadeling

import no.nav.security.mock.oauth2.MockOAuth2Server

class TestApiServer {
    companion object {
        private lateinit var mockOAuth2Server: MockOAuth2Server
    }

    fun start() {
        mockOAuth2Server = MockOAuth2Server()
        mockOAuth2Server.start(8091)
    }

    fun shutdown() {
        mockOAuth2Server.shutdown()
    }
}
