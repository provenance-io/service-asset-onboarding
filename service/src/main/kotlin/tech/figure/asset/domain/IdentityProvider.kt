package tech.figure.asset.domain

import java.util.UUID

interface IdentityProvider {
    suspend fun loggedInUser(): UUID
}
