package tech.figure.asset.frameworks.web.roles

/**
 * Role definitions for Role-based access control (RBAC)
 */
enum class Roles(val description: String) {
    ROLE_SUPER_USER("Super User"),
    ROLE_ADMIN("Administrator"),
    ROLE_CSR("Customer Service Representative")
}
