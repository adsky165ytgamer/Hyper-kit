rootProject.name = "HyperKit"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

// HyperKit application (Frontend / Integration + API / Backend Shelves live in here).
include(":app")

// Whiteboard UPS = DrawBox. Included from source, unmodified, so the build is
// reproducible offline from this repository. Project names are kept exactly as
// upstream ("DrawBox", "drawbox-ui") because drawbox-ui depends on ":DrawBox".
include(":DrawBox")
project(":DrawBox").projectDir = file("ups/drawbox/DrawBox")
include(":drawbox-ui")
project(":drawbox-ui").projectDir = file("ups/drawbox/drawbox-ui")
