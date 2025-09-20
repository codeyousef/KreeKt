package io.kreekt.animation

import kotlinx.serialization.Contextual
import io.kreekt.core.platform.currentTimeMillis
import kotlinx.serialization.Serializable
import io.kreekt.core.platform.currentTimeMillis
import kotlin.math.abs
import io.kreekt.core.platform.currentTimeMillis

/**
 * Advanced Animation State Machine for complex animation flow management.
 * Supports conditional transitions, parameter-driven blending, and hierarchical states.
 *
 * T039 - StateMachine for animation state management
 */
class StateMachine(
    val name: String = "AnimationStateMachine"
) {

    // Current state management
    private var currentState: AnimationState? = null
    private var previousState: AnimationState? = null
    private var transitionState: TransitionState? = null

    // States and transitions
    private val states = mutableMapOf<String, AnimationState>()
    private val transitions = mutableMapOf<String, MutableList<StateTransition>>()

    // Parameters for driving transitions
    private val parameters = mutableMapOf<String, Parameter>()

    // Listeners for state changes
    private val stateListeners = mutableListOf<StateChangeListener>()

    // Update timing
    private var lastUpdateTime = 0L
    private var deltaTime = 0f

    // Debug and visualization
    var debugMode = false
    private val debugHistory = mutableListOf<StateChangeEvent>()

    /**
     * Animation state definition
     */
    data class AnimationState(
        val name: String,
        val animations: List<AnimationClip> = emptyList(),
        val blendMode: BlendMode = BlendMode.REPLACE,
        val speed: Float = 1f,
        val loop: Boolean = true,
        val weight: Float = 1f,
        val onEnter: (() -> Unit)? = null,
        val onExit: (() -> Unit)? = null,
        val onUpdate: ((Float) -> Unit)? = null,
        val metadata: Map<String, Any> = emptyMap()
    ) {
        var isActive = false
        var currentTime = 0f
        var normalizedTime = 0f

        enum class BlendMode {
            REPLACE,    // Replace previous animation
            ADDITIVE,   // Add to previous animation
            MULTIPLY,   // Multiply with previous
            SUBTRACT    // Subtract from previous
        }

        fun update(deltaTime: Float) {
            if (!isActive) return

            currentTime = currentTime + deltaTime * speed

            // Calculate normalized time
            val totalDuration = animations.maxOfOrNull { it.duration } ?: 0f
            if (totalDuration > 0f) {
                normalizedTime = if (loop) {
                    (currentTime % totalDuration) / totalDuration
                } else {
                    (currentTime / totalDuration).coerceAtMost(1f)
                }
            }

            onUpdate?.invoke(normalizedTime)
        }

        fun reset() {
            currentTime = 0f
            normalizedTime = 0f
        }

        fun enter() {
            isActive = true
            reset()
            onEnter?.invoke()
        }

        fun exit() {
            isActive = false
            onExit?.invoke()
        }
    }

    /**
     * Animation clip reference
     */
    data class AnimationClip(
        val name: String,
        val duration: Float,
        val tracks: List<AnimationTrack> = emptyList()
    )

    /**
     * Animation track (simplified)
     */
    data class AnimationTrack(
        val property: String,
        val keyframes: List<Keyframe>
    )

    /**
     * Keyframe data
     */
    data class Keyframe(
        val time: Float,
        val value: Float,
        val inTangent: Float = 0f,
        val outTangent: Float = 0f
    )

    /**
     * State transition definition
     */
    data class StateTransition(
        val fromState: String,
        val toState: String,
        val conditions: List<TransitionCondition>,
        val duration: Float = 0.25f,
        val curve: TransitionCurve = TransitionCurve.SMOOTH,
        val priority: Int = 0,
        val canInterrupt: Boolean = true,
        val exitTime: Float? = null, // Normalized time to exit
        val onTransitionStart: (() -> Unit)? = null,
        val onTransitionEnd: (() -> Unit)? = null
    ) {
        fun canTransition(parameters: Map<String, Parameter>, currentNormalizedTime: Float): Boolean {
            // Check exit time condition
            exitTime?.let { exitNormTime ->
                if (currentNormalizedTime < exitNormTime) return false
            }

            // Check all conditions
            return conditions.all { condition ->
                condition.evaluate(parameters)
            }
        }
    }

    /**
     * Transition curves for smooth blending
     */
    enum class TransitionCurve {
        LINEAR,
        SMOOTH,
        EASE_IN,
        EASE_OUT,
        EASE_IN_OUT,
        SNAP
    }

    /**
     * Current transition state
     */
    private data class TransitionState(
        val transition: StateTransition,
        val startTime: Float,
        var currentTime: Float = 0f
    ) {
        val progress: Float
            get() = if (transition.duration > 0f) {
                (currentTime / transition.duration).coerceIn(0f, 1f)
            } else 1f

        val isComplete: Boolean
            get() = progress >= 1f

        fun getBlendWeight(curve: TransitionCurve): Float {
            return when (curve) {
                TransitionCurve.LINEAR -> progress
                TransitionCurve.SMOOTH -> progress * progress * (3f - (2f * progress))
                TransitionCurve.EASE_IN -> progress * progress
                TransitionCurve.EASE_OUT -> 1f - (1f - progress) * (1f - progress)
                TransitionCurve.EASE_IN_OUT -> {
                    if (progress < 0.5f) {
                        2f * (progress * progress)
                    } else {
                        1f - 2f * (1f - progress) * (1f - progress)
                    }
                }
                TransitionCurve.SNAP -> if (progress > 0.5f) 1f else 0f
            }
        }
    }

    /**
     * Transition condition types
     */
    sealed class TransitionCondition {
        abstract fun evaluate(parameters: Map<String, Parameter>): Boolean

        data class ParameterEquals(
            val parameterName: String,
            val value: Any
        ) : TransitionCondition() {
            override fun evaluate(parameters: Map<String, Parameter>): Boolean {
                val param = parameters[parameterName] ?: return false
                return when (param.type) {
                    Parameter.Type.BOOL -> param.boolValue == (value as? Boolean ?: false)
                    Parameter.Type.INT -> param.intValue == (value as? Int ?: 0)
                    Parameter.Type.FLOAT -> abs(param.floatValue - (value as? Float ?: 0f)) < 0.001f
                    Parameter.Type.TRIGGER -> param.triggered
                }
            }
        }

        data class ParameterGreater(
            val parameterName: String,
            val threshold: Float
        ) : TransitionCondition() {
            override fun evaluate(parameters: Map<String, Parameter>): Boolean {
                val param = parameters[parameterName] ?: return false
                return when (param.type) {
                    Parameter.Type.FLOAT -> param.floatValue > threshold
                    Parameter.Type.INT -> param.intValue > threshold
                    else -> false
                }
            }
        }

        data class ParameterLess(
            val parameterName: String,
            val threshold: Float
        ) : TransitionCondition() {
            override fun evaluate(parameters: Map<String, Parameter>): Boolean {
                val param = parameters[parameterName] ?: return false
                return when (param.type) {
                    Parameter.Type.FLOAT -> param.floatValue < threshold
                    Parameter.Type.INT -> param.intValue < threshold
                    else -> false
                }
            }
        }

        data class And(
            val conditions: List<TransitionCondition>
        ) : TransitionCondition() {
            override fun evaluate(parameters: Map<String, Parameter>): Boolean {
                return conditions.all { it.evaluate(parameters) }
            }
        }

        data class Or(
            val conditions: List<TransitionCondition>
        ) : TransitionCondition() {
            override fun evaluate(parameters: Map<String, Parameter>): Boolean {
                return conditions.any { it.evaluate(parameters) }
            }
        }

        data class Not(
            val condition: TransitionCondition
        ) : TransitionCondition() {
            override fun evaluate(parameters: Map<String, Parameter>): Boolean {
                return !condition.evaluate(parameters)
            }
        }
    }

    /**
     * State machine parameters
     */
    data class Parameter(
        val name: String,
        val type: Type,
        private var _value: Any = getDefaultValue(type)
    ) {
        enum class Type {
            BOOL,
            INT,
            FLOAT,
            TRIGGER
        }

        val boolValue: Boolean
            get() = _value as? Boolean ?: false

        val intValue: Int
            get() = _value as? Int ?: 0

        val floatValue: Float
            get() = _value as? Float ?: 0f

        var triggered: Boolean = false
            private set

        fun setValue(value: Any) {
            when (type) {
                Type.BOOL -> _value = value as? Boolean ?: false
                Type.INT -> _value = value as? Int ?: 0
                Type.FLOAT -> _value = value as? Float ?: 0f
                Type.TRIGGER -> {
                    triggered = value as? Boolean ?: false
                    if (triggered) {
                        // Auto-reset trigger after one frame
                        // In a real implementation, this would be handled in update()
                    }
                }
            }
        }

        fun resetTrigger() {
            if (type == Type.TRIGGER) {
                triggered = false
            }
        }

        companion object {
            fun getDefaultValue(type: Type): Any {
                return when (type) {
                    Type.BOOL -> false
                    Type.INT -> 0
                    Type.FLOAT -> 0f
                    Type.TRIGGER -> false
                }
            }
        }
    }

    /**
     * State change event for debugging
     */
    data class StateChangeEvent(
        val timestamp: Long,
        val fromState: String?,
        val toState: String,
        val parameters: Map<String, Any>,
        val transitionDuration: Float
    )

    /**
     * State change listener interface
     */
    fun interface StateChangeListener {
        fun onStateChanged(fromState: String?, toState: String, transition: StateTransition?)
    }

    /**
     * Add a state to the state machine
     */
    fun addState(state: AnimationState) {
        states[state.name] = state
        transitions[state.name] = mutableListOf()
    }

    /**
     * Add a transition between states
     */
    fun addTransition(transition: StateTransition) {
        transitions.getOrPut(transition.fromState) { mutableListOf() }.add(transition)
    }

    /**
     * Add parameter
     */
    fun addParameter(parameter: Parameter) {
        parameters[parameter.name] = parameter
    }

    /**
     * Set parameter value
     */
    fun setParameter(name: String, value: Any) {
        parameters[name]?.setValue(value)
    }

    /**
     * Get parameter value
     */
    fun getParameter(name: String): Parameter? = parameters[name]

    /**
     * Trigger a parameter (for trigger type parameters)
     */
    fun trigger(parameterName: String) {
        val param = parameters[parameterName]
        if (param?.type == Parameter.Type.TRIGGER) {
            param.setValue(true)
        }
    }

    /**
     * Set the current state
     */
    fun setState(stateName: String, force: Boolean = false) {
        val state = states[stateName] ?: return

        if (currentState?.name == stateName && !force) return

        val oldState = currentState
        oldState?.exit()

        previousState = oldState
        currentState = state
        state.enter()

        // Notify listeners
        stateListeners.forEach { listener ->
            listener.onStateChanged(oldState?.name, stateName, null)
        }

        // Add to debug history
        if (debugMode) {
            debugHistory.add(StateChangeEvent(
                timestamp = currentTimeMillis(),
                fromState = oldState?.name,
                toState = stateName,
                parameters = parameters.mapValues { (_, param) ->
                    when (param.type) {
                        Parameter.Type.BOOL -> param.boolValue
                        Parameter.Type.INT -> param.intValue
                        Parameter.Type.FLOAT -> param.floatValue
                        Parameter.Type.TRIGGER -> param.triggered
                    }
                },
                transitionDuration = 0f
            ))
        }
    }

    /**
     * Update the state machine
     */
    fun update(deltaTime: Float) {
        this.deltaTime = deltaTime
        val currentTime = currentTimeMillis()

        // Update current state
        currentState?.update(deltaTime)

        // Update transition if active
        transitionState?.let { transition ->
            transition.currentTime = currentTime + deltaTime

            if (transition.isComplete) {
                // Complete transition
                finishTransition()
            }
        }

        // Check for state transitions
        if (transitionState == null) {
            checkForTransitions()
        }

        // Reset triggers
        resetTriggers()

        lastUpdateTime = currentTime
    }

    /**
     * Check for valid transitions from current state
     */
    private fun checkForTransitions() {
        val currentStateName = currentState?.name ?: return
        val availableTransitions = transitions[currentStateName] ?: return

        // Sort by priority (higher priority first)
        val sortedTransitions = availableTransitions.sortedByDescending { it.priority }

        for (transition in sortedTransitions) {
            val currentNormalizedTime = currentState?.normalizedTime ?: 0f

            if (transition.canTransition(parameters, currentNormalizedTime)) {
                startTransition(transition)
                break
            }
        }
    }

    /**
     * Start a state transition
     */
    private fun startTransition(transition: StateTransition) {
        val toState = states[transition.toState] ?: return

        // Check if current transition can be interrupted
        transitionState?.let { currentTransition ->
            if (!currentTransition.transition.canInterrupt) {
                return
            }
        }

        transitionState = TransitionState(
            transition = transition,
            startTime = currentTimeMillis().toFloat()
        )

        transition.onTransitionStart?.invoke()

        // Notify listeners
        stateListeners.forEach { listener ->
            listener.onStateChanged(currentState?.name, transition.toState, transition)
        }

        // Add to debug history
        if (debugMode) {
            debugHistory.add(StateChangeEvent(
                timestamp = currentTimeMillis(),
                fromState = currentState?.name,
                toState = transition.toState,
                parameters = parameters.mapValues { (_, param) ->
                    when (param.type) {
                        Parameter.Type.BOOL -> param.boolValue
                        Parameter.Type.INT -> param.intValue
                        Parameter.Type.FLOAT -> param.floatValue
                        Parameter.Type.TRIGGER -> param.triggered
                    }
                },
                transitionDuration = transition.duration
            ))
        }
    }

    /**
     * Finish the current transition
     */
    private fun finishTransition() {
        val transition = transitionState?.transition ?: return
        val toState = states[transition.toState] ?: return

        // Switch to new state
        currentState?.exit()
        previousState = currentState
        currentState = toState
        toState.enter()

        // Clear transition
        transitionState?.transition?.onTransitionEnd?.invoke()
        transitionState = null
    }

    /**
     * Reset all trigger parameters
     */
    private fun resetTriggers() {
        parameters.values.forEach { param ->
            if (param.type == Parameter.Type.TRIGGER) {
                param.resetTrigger()
            }
        }
    }

    /**
     * Get current blend weights for all active states
     */
    fun getBlendWeights(): Map<String, Float> {
        val weights = mutableMapOf<String, Float>()

        currentState?.let { state ->
            val transitionBlend = transitionState?.let { transition ->
                1f - transition.getBlendWeight(transition.transition.curve)
            } ?: 1f

            weights[state.name] = state.weight * transitionBlend
        }

        // Add transitioning state weight
        transitionState?.let { transition ->
            val toState = states[transition.transition.toState]
            if (toState != null) {
                val blendWeight = transition.getBlendWeight(transition.transition.curve)
                weights[toState.name] = toState.weight * blendWeight
            }
        }

        return weights
    }

    /**
     * Add state change listener
     */
    fun addStateChangeListener(listener: StateChangeListener) {
        stateListeners.add(listener)
    }

    /**
     * Remove state change listener
     */
    fun removeStateChangeListener(listener: StateChangeListener) {
        stateListeners.remove(listener)
    }

    /**
     * Get current state name
     */
    fun getCurrentStateName(): String? = currentState?.name

    /**
     * Get current state
     */
    fun getCurrentState(): AnimationState? = currentState

    /**
     * Check if currently transitioning
     */
    fun isTransitioning(): Boolean = transitionState != null

    /**
     * Get transition progress (0.0 to 1.0)
     */
    fun getTransitionProgress(): Float = transitionState?.progress ?: 0f

    /**
     * Force complete current transition
     */
    fun completeTransition() {
        transitionState?.let { transition ->
            transition.currentTime = transition.transition.duration
            finishTransition()
        }
    }

    /**
     * Get debug information
     */
    fun getDebugInfo(): StateMachineDebugInfo {
        return StateMachineDebugInfo(
            currentState = currentState?.name,
            previousState = previousState?.name,
            isTransitioning = isTransitioning(),
            transitionProgress = getTransitionProgress(),
            parameters = parameters.mapValues { (_, param) ->
                when (param.type) {
                    Parameter.Type.BOOL -> param.boolValue
                    Parameter.Type.INT -> param.intValue
                    Parameter.Type.FLOAT -> param.floatValue
                    Parameter.Type.TRIGGER -> param.triggered
                }
            },
            recentHistory = debugHistory.takeLast(10)
        )
    }

    data class StateMachineDebugInfo(
        val currentState: String?,
        val previousState: String?,
        val isTransitioning: Boolean,
        val transitionProgress: Float,
        val parameters: Map<String, Any>,
        val recentHistory: List<StateChangeEvent>
    )

    /**
     * Clear debug history
     */
    fun clearDebugHistory() {
        debugHistory.clear()
    }

    /**
     * Get all state names
     */
    fun getStateNames(): List<String> = states.keys.toList()

    /**
     * Get all parameter names
     */
    fun getParameterNames(): List<String> = parameters.keys.toList()

    /**
     * Remove state
     */
    fun removeState(stateName: String) {
        states.remove(stateName)
        transitions.remove(stateName)
        // Remove transitions TO this state
        transitions.values.forEach { transitionList ->
            transitionList.removeAll { it.toState == stateName }
        }
    }

    /**
     * Remove parameter
     */
    fun removeParameter(parameterName: String) {
        parameters.remove(parameterName)
    }

    /**
     * Clear all states and transitions
     */
    fun clear() {
        currentState?.exit()
        currentState = null
        previousState = null
        transitionState = null
        states.clear()
        transitions.clear()
        parameters.clear()
        stateListeners.clear()
        debugHistory.clear()
    }

    fun dispose() {
        clear()
    }
}