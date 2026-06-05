# Multi-Tenancy Configuration
multitenancy.enabled=true
multitenancy.jwt-secret=your-secret-key-min-256-bits
multitenancy.tenant-id-claim-name=tenantId
multitenancy.token-header=Authorization
multitenancy.token-prefix=Bearer
multitenancy.schema-separator=_
multitenancy.fail-on-missing-tenant=true

this we should set on each applicaiton