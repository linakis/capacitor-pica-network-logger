import Foundation
#if canImport(UIKit)
import UIKit
#endif
#if canImport(PicaNetworkLoggerShared)
import PicaNetworkLoggerShared
#endif

#if canImport(UIKit) && canImport(PicaNetworkLoggerShared)
public class InspectorViewController: UIViewController {
    public override func viewDidLoad() {
        super.viewDidLoad()
        let composeViewController = InspectorViewControllerKt.InspectorViewController(
            onClose: { [weak self] in
                self?.dismiss(animated: true)
            }
        )
        addChild(composeViewController)
        composeViewController.view.frame = view.bounds
        composeViewController.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(composeViewController.view)
        composeViewController.didMove(toParent: self)
    }
}
#endif
