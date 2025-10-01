/**
 * Type aliases for backward compatibility
 * Maps io.kreekt.light.* to io.kreekt.lighting.* implementations
 */
package io.kreekt.light

import io.kreekt.lighting.DirectionalLightImpl
import io.kreekt.lighting.SpotLightImpl
import io.kreekt.lighting.PointLightImpl
import io.kreekt.lighting.HemisphereLightImpl
import io.kreekt.lighting.AmbientLightImpl
import io.kreekt.lighting.AreaLightImpl
import io.kreekt.lighting.RectAreaLightImpl

// Type aliases for light implementations
typealias DirectionalLight = DirectionalLightImpl
typealias SpotLight = SpotLightImpl
typealias PointLight = PointLightImpl
typealias HemisphereLight = HemisphereLightImpl
typealias AmbientLight = AmbientLightImpl
typealias AreaLight = AreaLightImpl
typealias RectAreaLight = RectAreaLightImpl
