import Foundation
#if canImport(UIKit)
import UIKit
#endif
#if canImport(shared)
import shared
#endif

#if canImport(UIKit) && canImport(shared)
public class InspectorViewController: UIViewController {
    public override func viewDidLoad() {
        super.viewDidLoad()
        let composeViewController = InspectorViewControllerKt.InspectorViewController()
        addChild(composeViewController)
        composeViewController.view.frame = view.bounds
        view.addSubview(composeViewController.view)
        composeViewController.didMove(toParent: self)
    }
}
#endif
