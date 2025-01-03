// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "Nfc",
    platforms: [.iOS(.v13)],
    products: [
        .library(
            name: "Nfc",
            targets: ["NfcPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", branch: "main")
    ],
    targets: [
        .target(
            name: "NfcPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/NfcPlugin"),
        .testTarget(
            name: "NfcPluginTests",
            dependencies: ["NfcPlugin"],
            path: "ios/Tests/NfcPluginTests")
    ]
)