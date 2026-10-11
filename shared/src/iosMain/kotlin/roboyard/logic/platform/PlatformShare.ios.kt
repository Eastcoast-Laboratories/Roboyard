package roboyard.logic.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController

/**
 * iOS implementation: presents the system share sheet (Mail, Messages,
 * AirDrop, Files, copy) from the top-most view controller.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun shareText(subject: String, text: String): Boolean {
    var top: UIViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        ?: run {
            println("[SHARE] No root view controller — cannot present share sheet")
            return false
        }
    while (top.presentedViewController != null) top = top.presentedViewController!!

    val sheet = UIActivityViewController(activityItems = listOf("$subject\n\n$text"), applicationActivities = null)
    // iPad presents the sheet as a popover, which needs an anchor
    sheet.popoverPresentationController?.let { popover ->
        val view = top.view
        popover.sourceView = view
        view.bounds.useContents {
            popover.sourceRect = CGRectMake(size.width / 2, size.height / 2, 1.0, 1.0)
        }
    }
    top.presentViewController(sheet, animated = true, completion = null)
    return true
}
