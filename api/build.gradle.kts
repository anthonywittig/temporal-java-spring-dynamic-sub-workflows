// Shared contract. Deliberately has no Temporal or Spring dependency so the consumer can use it
// without pulling in workflow or handler code.
dependencies {
    api(libs.jackson.databind)
}
