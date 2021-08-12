package tech.figure.asset.frameworks.web.config

import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import tech.figure.asset.domain.IdentityProvider
import java.util.UUID

@Component
class PrincipalIdentityProvider : IdentityProvider {
    override suspend fun loggedInUser(): UUID = awaitSecurityContext().authentication.principal as UUID

    private suspend fun awaitSecurityContext() = ReactiveSecurityContextHolder.getContext().awaitFirst()
}
