import Foundation

@objc public class Nfc: NSObject {
    @objc public func echo(_ value: String) -> String {
        print(value)
        return value
    }
}
