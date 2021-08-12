package tech.figure.asset.fakes

import tech.figure.asset.domain.IdentityProvider
import java.util.UUID

class FakeIdentityProvider(private val fakeIdentity: UUID) : IdentityProvider {
    override suspend fun loggedInUser(): UUID = fakeIdentity
}
