rootProject.name = "CraftingChest"

// Two independent builds: Fabric Loom and paperweight don't share a build well, so each keeps its own setup.
includeBuild("client-fabric")
includeBuild("server-paper")
