package roboyard.logic.platform

actual fun requestGc() {
    // No explicit GC on iOS (ARC). Kept as no-op for API parity.
}
