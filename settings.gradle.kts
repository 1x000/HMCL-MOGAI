rootProject.name = "CU3"
include(
    "CU",
    "CUCore",
    "CUTransformerDiscoveryService"
)

val minecraftLibraries = listOf("CUTransformerDiscoveryService")

for (library in minecraftLibraries) {
    project(":$library").projectDir = file("minecraft/libraries/$library")
}
