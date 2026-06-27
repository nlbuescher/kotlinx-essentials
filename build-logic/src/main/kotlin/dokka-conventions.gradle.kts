plugins {
    id("org.jetbrains.dokka")
}

dokka {
    dokkaSourceSets.configureEach {
        sourceLink {
            localDirectory = rootDir
            remoteUrl = uri("https://github.com/nlbuescher/kotlinx-essentials/tree/main")
            remoteLineSuffix = "#L"
        }
    }
}
