@file:OptIn(kotlin.ExperimentalMultiplatform::class)

package roboyard.logic.util

import kotlin.reflect.KClass

/**
 * Optional-expect JVM interop annotations for commonMain.
 *
 * kotlin.jvm.Synchronized is an error on non-JVM targets and kotlin.jvm.Throws
 * does not exist there at all. These optional expectations keep the JVM
 * behavior (actualized via typealias in androidMain/desktopMain) and compile
 * away on Native.
 *
 * Usage: import roboyard.logic.util.Throws / roboyard.logic.util.Synchronized
 * instead of the kotlin.jvm variants in commonMain.
 */
@OptionalExpectation
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.BINARY)
expect annotation class Throws(vararg val exceptionClasses: KClass<out Throwable>)

@OptionalExpectation
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.BINARY)
expect annotation class Synchronized()
