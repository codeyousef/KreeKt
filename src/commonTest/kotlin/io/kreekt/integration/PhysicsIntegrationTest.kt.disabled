/**
 * Integration tests for physics simulation system
 * Tests interaction between PhysicsWorld, RigidBody, CollisionShapes, CharacterController, and physics pipeline
 */
package io.kreekt.integration

import io.kreekt.core.math.*
import io.kreekt.physics.*
import io.kreekt.scene.*
import kotlin.test.*

class PhysicsIntegrationTest {

    @Test
    fun testPhysicsWorldSimulation() {
        // This test will fail until we implement the complete physics system
        val physicsWorld = TODO("PhysicsWorld implementation not available yet") as PhysicsWorld
        val scene = TODO("Scene implementation not available yet") as Scene

        // 1. Setup physics world
        physicsWorld.gravity = Vector3(0f, -9.81f, 0f)
        physicsWorld.timeStep = 1f / 60f // 60 Hz
        physicsWorld.maxSubSteps = 10

        // Verify physics world configuration
        assertEquals(Vector3(0f, -9.81f, 0f), physicsWorld.gravity, "Gravity should be set correctly")
        assertEquals(1f / 60f, physicsWorld.timeStep, 0.001f, "Time step should be set correctly")

        // 2. Create ground plane
        val groundShape = BoxShape(Vector3(50f, 1f, 50f))
        val groundBody = RigidBody(
            mass = 0f, // Static body
            shape = groundShape,
            position = Vector3(0f, -1f, 0f),
            rotation = Quaternion.IDENTITY
        )

        physicsWorld.addRigidBody(groundBody)
        assertTrue(physicsWorld.rigidBodies.contains(groundBody), "Ground body should be added to world")

        // 3. Create falling objects with different shapes
        val sphereShape = SphereShape(1f)
        val sphereBody = RigidBody(
            mass = 1f,
            shape = sphereShape,
            position = Vector3(0f, 10f, 0f),
            rotation = Quaternion.IDENTITY
        )

        val boxShape = BoxShape(Vector3(1f, 1f, 1f))
        val boxBody = RigidBody(
            mass = 2f,
            shape = boxShape,
            position = Vector3(3f, 15f, 0f),
            rotation = Quaternion.IDENTITY
        )

        val capsuleShape = CapsuleShape(radius = 0.5f, height = 2f)
        val capsuleBody = RigidBody(
            mass = 1.5f,
            shape = capsuleShape,
            position = Vector3(-3f, 12f, 0f),
            rotation = Quaternion.IDENTITY
        )

        physicsWorld.addRigidBody(sphereBody)
        physicsWorld.addRigidBody(boxBody)
        physicsWorld.addRigidBody(capsuleBody)

        // 4. Simulate physics
        val simulationTime = 3f // 3 seconds
        val steps = (simulationTime / physicsWorld.timeStep).toInt()

        for (step in 0 until steps) {
            physicsWorld.step()

            // Verify bodies are falling due to gravity
            if (step > 10) { // Allow some time for simulation to start
                assertTrue(sphereBody.position.y < 10f, "Sphere should be falling")
                assertTrue(boxBody.position.y < 15f, "Box should be falling")
                assertTrue(capsuleBody.position.y < 12f, "Capsule should be falling")
            }

            // Check for collisions with ground
            if (step > 60) { // After 1 second
                val sphereOnGround = sphereBody.position.y <= 1f
                val boxOnGround = boxBody.position.y <= 2f
                val capsuleOnGround = capsuleBody.position.y <= 2f

                if (sphereOnGround) {
                    assertTrue(abs(sphereBody.linearVelocity.y) < 5f, "Sphere should have reduced velocity on ground")
                }
                if (boxOnGround) {
                    assertTrue(abs(boxBody.linearVelocity.y) < 5f, "Box should have reduced velocity on ground")
                }
                if (capsuleOnGround) {
                    assertTrue(abs(capsuleBody.linearVelocity.y) < 5f, "Capsule should have reduced velocity on ground")
                }
            }
        }

        // Final verification - all objects should have settled
        assertTrue(sphereBody.position.y <= 2f, "Sphere should be on or near ground")
        assertTrue(boxBody.position.y <= 3f, "Box should be on or near ground")
        assertTrue(capsuleBody.position.y <= 3f, "Capsule should be on or near ground")
    }

    @Test
    fun testCollisionDetectionAndResponse() {
        val physicsWorld = TODO("PhysicsWorld implementation not available yet") as PhysicsWorld
        val collisionDetector = TODO("CollisionDetector implementation not available yet") as CollisionDetector

        // 1. Setup collision world
        physicsWorld.gravity = Vector3.ZERO // No gravity for pure collision testing

        // 2. Create colliding objects
        val boxA = RigidBody(
            mass = 1f,
            shape = BoxShape(Vector3(1f, 1f, 1f)),
            position = Vector3(-2f, 0f, 0f),
            rotation = Quaternion.IDENTITY
        )
        boxA.linearVelocity = Vector3(5f, 0f, 0f) // Moving right

        val boxB = RigidBody(
            mass = 1f,
            shape = BoxShape(Vector3(1f, 1f, 1f)),
            position = Vector3(2f, 0f, 0f),
            rotation = Quaternion.IDENTITY
        )
        boxB.linearVelocity = Vector3(-3f, 0f, 0f) // Moving left

        physicsWorld.addRigidBody(boxA)
        physicsWorld.addRigidBody(boxB)

        // 3. Setup collision callbacks
        var collisionDetected = false
        var collisionData: CollisionData? = null

        val collisionCallback = object : CollisionCallback {
            override fun onCollisionEnter(bodyA: RigidBody, bodyB: RigidBody, data: CollisionData) {
                collisionDetected = true
                collisionData = data
            }

            override fun onCollisionStay(bodyA: RigidBody, bodyB: RigidBody, data: CollisionData) {
                // Handle persistent collision
            }

            override fun onCollisionExit(bodyA: RigidBody, bodyB: RigidBody) {
                // Handle collision end
            }
        }

        physicsWorld.setCollisionCallback(collisionCallback)

        // 4. Simulate until collision
        var collisionOccurred = false
        for (step in 0..120) { // 2 seconds max
            physicsWorld.step()

            if (collisionDetected && !collisionOccurred) {
                collisionOccurred = true

                // Verify collision detection
                assertNotNull(collisionData, "Collision data should be available")
                assertTrue(collisionData!!.penetrationDepth > 0f, "Should have penetration depth")
                assertNotNull(collisionData!!.contactNormal, "Should have contact normal")
                assertTrue(collisionData!!.contactPoints.isNotEmpty(), "Should have contact points")

                // Verify collision response
                val velocityA = boxA.linearVelocity
                val velocityB = boxB.linearVelocity

                // After collision, velocities should change direction (elastic collision)
                assertTrue(velocityA.x < 0f, "Box A should bounce back (velocity should be negative)")
                assertTrue(velocityB.x > 0f, "Box B should bounce back (velocity should be positive)")

                break
            }
        }

        assertTrue(collisionOccurred, "Collision should have been detected")
    }

    @Test
    fun testPhysicsConstraints() {
        val physicsWorld = TODO("PhysicsWorld implementation not available yet") as PhysicsWorld

        // 1. Create bodies for constraint testing
        val anchorBody = RigidBody(
            mass = 0f, // Static
            shape = BoxShape(Vector3(0.5f, 0.5f, 0.5f)),
            position = Vector3(0f, 5f, 0f),
            rotation = Quaternion.IDENTITY
        )

        val pendulumBody = RigidBody(
            mass = 1f,
            shape = SphereShape(0.5f),
            position = Vector3(0f, 3f, 0f),
            rotation = Quaternion.IDENTITY
        )

        physicsWorld.addRigidBody(anchorBody)
        physicsWorld.addRigidBody(pendulumBody)

        // 2. Create point-to-point constraint (pendulum)
        val pointConstraint = PointToPointConstraint(
            bodyA = anchorBody,
            bodyB = pendulumBody,
            pivotA = Vector3(0f, -0.5f, 0f),
            pivotB = Vector3(0f, 0.5f, 0f)
        )

        physicsWorld.addConstraint(pointConstraint)
        assertTrue(physicsWorld.constraints.contains(pointConstraint), "Constraint should be added to world")

        // 3. Test pendulum motion
        pendulumBody.linearVelocity = Vector3(3f, 0f, 0f) // Give initial push

        for (step in 0..300) { // 5 seconds
            physicsWorld.step()

            val distance = anchorBody.position.distanceTo(pendulumBody.position)
            val expectedDistance = 2f // Distance between pivot points

            assertEquals(expectedDistance, distance, 0.1f, "Constraint should maintain distance")

            // Verify pendulum swing
            if (step == 150) { // Check at 2.5 seconds
                assertTrue(abs(pendulumBody.position.x) > 0.5f, "Pendulum should swing")
            }
        }

        // 4. Create hinge constraint
        val hingeBodyA = RigidBody(
            mass = 0f, // Static
            shape = BoxShape(Vector3(1f, 0.2f, 0.2f)),
            position = Vector3(-3f, 3f, 0f),
            rotation = Quaternion.IDENTITY
        )

        val hingeBodyB = RigidBody(
            mass = 1f,
            shape = BoxShape(Vector3(1f, 0.2f, 0.2f)),
            position = Vector3(-1f, 3f, 0f),
            rotation = Quaternion.IDENTITY
        )

        val hingeConstraint = HingeConstraint(
            bodyA = hingeBodyA,
            bodyB = hingeBodyB,
            pivotA = Vector3(1f, 0f, 0f),
            pivotB = Vector3(-1f, 0f, 0f),
            axisA = Vector3(0f, 0f, 1f),
            axisB = Vector3(0f, 0f, 1f),
            enableLimits = true,
            lowerLimit = -Math.PI.toFloat() / 2f,
            upperLimit = Math.PI.toFloat() / 2f
        )

        physicsWorld.addRigidBody(hingeBodyA)
        physicsWorld.addRigidBody(hingeBodyB)
        physicsWorld.addConstraint(hingeConstraint)

        // Test hinge rotation
        hingeBodyB.angularVelocity = Vector3(0f, 0f, 2f) // Rotate around Z axis

        for (step in 0..180) { // 3 seconds
            physicsWorld.step()

            val angle = hingeConstraint.getHingeAngle()
            assertTrue(angle >= hingeConstraint.lowerLimit && angle <= hingeConstraint.upperLimit,
                "Hinge angle should respect limits")
        }

        // 5. Create slider constraint
        val sliderBodyA = RigidBody(
            mass = 0f, // Static
            shape = BoxShape(Vector3(0.2f, 2f, 0.2f)),
            position = Vector3(3f, 3f, 0f),
            rotation = Quaternion.IDENTITY
        )

        val sliderBodyB = RigidBody(
            mass = 1f,
            shape = BoxShape(Vector3(0.5f, 0.2f, 0.5f)),
            position = Vector3(3f, 4f, 0f),
            rotation = Quaternion.IDENTITY
        )

        val sliderConstraint = SliderConstraint(
            bodyA = sliderBodyA,
            bodyB = sliderBodyB,
            frameA = Transform.IDENTITY,
            frameB = Transform.IDENTITY,
            lowerLinLimit = -2f,
            upperLinLimit = 2f
        )

        physicsWorld.addRigidBody(sliderBodyA)
        physicsWorld.addRigidBody(sliderBodyB)
        physicsWorld.addConstraint(sliderConstraint)

        // Test slider motion
        sliderBodyB.linearVelocity = Vector3(0f, 3f, 0f) // Move up

        for (step in 0..120) { // 2 seconds
            physicsWorld.step()

            val linearPosition = sliderConstraint.getLinearPosition()
            assertTrue(linearPosition >= sliderConstraint.lowerLinLimit &&
                      linearPosition <= sliderConstraint.upperLinLimit,
                "Slider position should respect limits")
        }
    }

    @Test
    fun testCharacterController() {
        val physicsWorld = TODO("PhysicsWorld implementation not available yet") as PhysicsWorld
        val characterController = TODO("CharacterController implementation not available yet") as CharacterController

        // 1. Setup character controller
        val characterShape = CapsuleShape(radius = 0.5f, height = 1.8f)

        characterController.setup(
            shape = characterShape,
            mass = 70f, // 70kg character
            position = Vector3(0f, 2f, 0f),
            stepHeight = 0.3f,
            slopeLimit = 45f, // degrees
            skinWidth = 0.08f
        )

        physicsWorld.addCharacterController(characterController)

        // 2. Create test environment
        // Ground
        val ground = RigidBody(
            mass = 0f,
            shape = BoxShape(Vector3(20f, 1f, 20f)),
            position = Vector3(0f, 0f, 0f),
            rotation = Quaternion.IDENTITY
        )

        // Steps
        val step1 = RigidBody(
            mass = 0f,
            shape = BoxShape(Vector3(2f, 0.2f, 2f)),
            position = Vector3(3f, 0.2f, 0f),
            rotation = Quaternion.IDENTITY
        )

        val step2 = RigidBody(
            mass = 0f,
            shape = BoxShape(Vector3(2f, 0.4f, 2f)),
            position = Vector3(6f, 0.4f, 0f),
            rotation = Quaternion.IDENTITY
        )

        // Slope
        val slope = RigidBody(
            mass = 0f,
            shape = BoxShape(Vector3(4f, 0.1f, 2f)),
            position = Vector3(0f, 1f, 6f),
            rotation = Quaternion.fromAxisAngle(Vector3.RIGHT, Math.PI.toFloat() / 6f) // 30 degree slope
        )

        physicsWorld.addRigidBody(ground)
        physicsWorld.addRigidBody(step1)
        physicsWorld.addRigidBody(step2)
        physicsWorld.addRigidBody(slope)

        // 3. Test basic movement
        val deltaTime = 1f / 60f
        var isGrounded = false

        // Move character forward
        val moveDirection = Vector3(1f, 0f, 0f).normalized()
        val moveSpeed = 5f

        for (step in 0..300) { // 5 seconds
            val currentTime = step * deltaTime

            // Apply movement
            characterController.move(moveDirection * moveSpeed * deltaTime)

            // Update physics
            physicsWorld.step()

            // Check if character is grounded
            isGrounded = characterController.isGrounded()

            // Character should be grounded most of the time
            if (step > 60) { // After 1 second
                assertTrue(isGrounded, "Character should be grounded on flat terrain")
            }

            // Check step climbing
            if (currentTime > 1f && currentTime < 2f) {
                // Character should climb step1
                assertTrue(characterController.position.y > 0.5f, "Character should climb first step")
            }

            if (currentTime > 2f && currentTime < 3f) {
                // Character should climb step2
                assertTrue(characterController.position.y > 0.7f, "Character should climb second step")
            }
        }

        // 4. Test slope climbing
        characterController.position = Vector3(0f, 2f, 3f)
        val slopeDirection = Vector3(0f, 0f, 1f).normalized()

        for (step in 0..180) { // 3 seconds
            characterController.move(slopeDirection * moveSpeed * deltaTime)
            physicsWorld.step()

            if (step > 60) { // After 1 second on slope
                assertTrue(characterController.isGrounded(), "Character should stay grounded on walkable slope")
                assertTrue(characterController.position.y > 1f, "Character should climb slope")
            }
        }

        // 5. Test jumping
        characterController.position = Vector3(0f, 2f, 0f)
        characterController.jump(8f) // Jump with 8 m/s initial velocity

        var jumpStarted = false
        var jumpPeakReached = false
        var jumpLanded = false

        for (step in 0..120) { // 2 seconds
            physicsWorld.step()

            val wasGrounded = isGrounded
            isGrounded = characterController.isGrounded()

            if (!wasGrounded && !isGrounded && !jumpStarted) {
                jumpStarted = true
                assertTrue(characterController.position.y > 1.5f, "Character should leave ground when jumping")
            }

            if (jumpStarted && !jumpPeakReached && characterController.velocity.y <= 0f) {
                jumpPeakReached = true
                assertTrue(characterController.position.y > 3f, "Character should reach significant height")
            }

            if (jumpPeakReached && !jumpLanded && isGrounded) {
                jumpLanded = true
                assertTrue(characterController.position.y <= 2f, "Character should land back on ground")
            }
        }

        assertTrue(jumpStarted, "Jump should have started")
        assertTrue(jumpPeakReached, "Jump should have reached peak")
        assertTrue(jumpLanded, "Jump should have landed")
    }

    @Test
    fun testSoftBodyPhysics() {
        val physicsWorld = TODO("PhysicsWorld implementation not available yet") as PhysicsWorld
        val softBodySolver = TODO("SoftBodySolver implementation not available yet") as SoftBodySolver

        // 1. Create soft body cloth
        val clothResolution = 20
        val clothSize = 5f

        val clothMesh = TODO("Mesh creation not available yet") as Mesh
        val softBodyCloth = SoftBody(
            mesh = clothMesh,
            mass = 1f,
            stiffness = 0.8f,
            damping = 0.1f,
            constraints = SoftBodyConstraints(
                structuralStiffness = 1.0f,
                shearStiffness = 0.5f,
                bendingStiffness = 0.2f,
                enableSelfCollision = true
            )
        )

        // Pin top corners
        softBodyCloth.pinVertex(0) // Top-left corner
        softBodyCloth.pinVertex(clothResolution - 1) // Top-right corner

        physicsWorld.addSoftBody(softBodyCloth)

        // 2. Create obstacle for cloth to interact with
        val obstacle = RigidBody(
            mass = 0f,
            shape = SphereShape(1f),
            position = Vector3(0f, 2f, 0f),
            rotation = Quaternion.IDENTITY
        )

        physicsWorld.addRigidBody(obstacle)

        // 3. Apply external forces (wind)
        val windForce = Vector3(2f, 0f, 1f)

        for (step in 0..600) { // 10 seconds
            val currentTime = step * physicsWorld.timeStep

            // Apply wind force periodically
            if (currentTime % 1f < 0.5f) { // Wind blows for 0.5s every second
                softBodyCloth.applyForce(windForce)
            }

            physicsWorld.step()

            // Verify cloth behavior
            if (step > 60) { // After 1 second
                // Cloth should deform and move
                val clothCenter = softBodyCloth.getCenterOfMass()
                assertTrue(clothCenter.y < 2f, "Cloth should sag due to gravity")

                // Check collision with obstacle
                if (softBodyCloth.isCollidingWith(obstacle)) {
                    assertTrue(softBodyCloth.getDeformation() > 0.1f, "Cloth should deform when colliding")
                }
            }
        }

        // 4. Test cloth tearing
        val tearingForce = Vector3(0f, -50f, 0f) // Strong downward force
        softBodyCloth.applyForceAtVertex(clothResolution / 2, tearingForce)

        for (step in 0..60) { // 1 second of strong force
            physicsWorld.step()

            if (softBodyCloth.getMaxStress() > softBodyCloth.tearThreshold) {
                assertTrue(softBodyCloth.hasTears(), "Cloth should tear under excessive stress")
                break
            }
        }
    }

    @Test
    fun testFluidSimulation() {
        val physicsWorld = TODO("PhysicsWorld implementation not available yet") as PhysicsWorld
        val fluidSolver = TODO("FluidSolver implementation not available yet") as FluidSolver

        // 1. Setup fluid domain
        val fluidDomain = FluidDomain(
            bounds = BoundingBox(
                min = Vector3(-5f, 0f, -5f),
                max = Vector3(5f, 8f, 5f)
            ),
            resolution = Vector3i(50, 80, 50),
            viscosity = 0.1f,
            density = 1000f, // Water density
            surfaceTension = 0.07f
        )

        fluidSolver.setDomain(fluidDomain)

        // 2. Create fluid emitter
        val fluidEmitter = FluidEmitter(
            position = Vector3(0f, 6f, 0f),
            radius = 0.5f,
            velocity = Vector3(0f, -2f, 0f),
            emissionRate = 100f, // particles per second
            particleLifetime = 5f
        )

        fluidSolver.addEmitter(fluidEmitter)

        // 3. Create obstacles
        val fluidObstacle = RigidBody(
            mass = 0f,
            shape = SphereShape(1.5f),
            position = Vector3(0f, 3f, 0f),
            rotation = Quaternion.IDENTITY
        )

        physicsWorld.addRigidBody(fluidObstacle)
        fluidSolver.addObstacle(fluidObstacle)

        // 4. Simulate fluid
        val deltaTime = 1f / 30f // 30 Hz for fluid simulation

        for (step in 0..900) { // 30 seconds
            fluidSolver.step(deltaTime)
            physicsWorld.step()

            val fluidParticles = fluidSolver.getParticles()

            if (step > 90) { // After 3 seconds
                assertTrue(fluidParticles.isNotEmpty(), "Fluid should have particles")

                // Check fluid-obstacle interaction
                val particlesNearObstacle = fluidParticles.count { particle ->
                    particle.position.distanceTo(fluidObstacle.position) < 2f
                }

                if (particlesNearObstacle > 0) {
                    // Verify particles are deflected by obstacle
                    val deflectedParticles = fluidParticles.count { particle ->
                        val directionToObstacle = (fluidObstacle.position - particle.position).normalized()
                        val velocityDot = particle.velocity.normalized().dot(directionToObstacle)
                        velocityDot < 0f // Moving away from obstacle
                    }

                    assertTrue(deflectedParticles > 0, "Some particles should be deflected by obstacle")
                }
            }

            // Check fluid pooling at bottom
            if (step > 300) { // After 10 seconds
                val bottomParticles = fluidParticles.count { it.position.y < 2f }
                assertTrue(bottomParticles > 0, "Fluid should pool at bottom")
            }
        }

        // 5. Test fluid surface reconstruction
        val surfaceResult = fluidSolver.reconstructSurface(
            method = SurfaceReconstructionMethod.MARCHING_CUBES,
            isoValue = 0.5f,
            smoothing = true
        )

        when (surfaceResult) {
            is FluidSurfaceResult.Success -> {
                val surfaceMesh = surfaceResult.value

                assertNotNull(surfaceMesh.vertices, "Surface mesh should have vertices")
                assertNotNull(surfaceMesh.triangles, "Surface mesh should have triangles")
                assertTrue(surfaceMesh.vertices.size > 0, "Surface should have mesh data")
            }
            is FluidSurfaceResult.Error -> {
                // Surface reconstruction might fail if no fluid is present - acceptable
                println("Surface reconstruction failed: ${surfaceResult.exception.message}")
            }
        }
    }

    @Test
    fun testPhysicsPerformanceOptimization() {
        val physicsWorld = TODO("PhysicsWorld implementation not available yet") as PhysicsWorld
        val performanceMonitor = TODO("PerformanceMonitor implementation not available yet") as PerformanceMonitor

        // 1. Create large number of physics objects (stress test)
        val objectCount = 1000
        val rigidBodies = mutableListOf<RigidBody>()

        for (i in 0 until objectCount) {
            val shape = when (i % 3) {
                0 -> SphereShape(0.5f)
                1 -> BoxShape(Vector3(0.5f, 0.5f, 0.5f))
                else -> CapsuleShape(0.3f, 1f)
            }

            val body = RigidBody(
                mass = 1f,
                shape = shape,
                position = Vector3(
                    (Math.random() * 20 - 10).toFloat(),
                    (Math.random() * 20 + 10).toFloat(),
                    (Math.random() * 20 - 10).toFloat()
                ),
                rotation = Quaternion.IDENTITY
            )

            rigidBodies.add(body)
            physicsWorld.addRigidBody(body)
        }

        // 2. Measure baseline performance
        val baselineMetrics = performanceMonitor.startMeasurement("physics_baseline")

        repeat(300) { // 5 seconds at 60Hz
            physicsWorld.step()
        }

        val baselineResult = performanceMonitor.endMeasurement(baselineMetrics)
        val baselineStepTime = baselineResult.averageFrameTime

        // 3. Enable spatial optimization (octree/grid)
        physicsWorld.enableSpatialOptimization(
            type = SpatialOptimizationType.OCTREE,
            maxDepth = 8,
            maxObjectsPerNode = 10
        )

        val spatialMetrics = performanceMonitor.startMeasurement("physics_spatial")

        repeat(300) { // 5 seconds with spatial optimization
            physicsWorld.step()
        }

        val spatialResult = performanceMonitor.endMeasurement(spatialMetrics)
        val spatialStepTime = spatialResult.averageFrameTime

        // 4. Enable sleeping optimization
        physicsWorld.enableSleeping(
            linearThreshold = 0.1f,
            angularThreshold = 0.1f,
            timeThreshold = 1f // 1 second of inactivity
        )

        val sleepingMetrics = performanceMonitor.startMeasurement("physics_sleeping")

        repeat(600) { // 10 seconds to allow objects to settle and sleep
            physicsWorld.step()
        }

        val sleepingResult = performanceMonitor.endMeasurement(sleepingMetrics)
        val sleepingStepTime = sleepingResult.averageFrameTime

        // 5. Verify performance improvements
        val spatialImprovement = (baselineStepTime - spatialStepTime) / baselineStepTime
        val sleepingImprovement = (baselineStepTime - sleepingStepTime) / baselineStepTime

        assertTrue(spatialImprovement > 0.1f, "Spatial optimization should improve performance by at least 10%")
        assertTrue(sleepingImprovement > 0.3f, "Sleeping optimization should improve performance by at least 30%")

        // 6. Check sleeping statistics
        val sleepingStats = physicsWorld.getSleepingStatistics()

        assertTrue(sleepingStats.sleepingBodies > objectCount * 0.5f,
            "More than 50% of settled objects should be sleeping")
        assertTrue(sleepingStats.activeBodies < objectCount * 0.5f,
            "Less than 50% of objects should be active when settled")

        // 7. Test broad phase collision detection optimization
        val broadPhaseMetrics = physicsWorld.getBroadPhaseMetrics()

        assertTrue(broadPhaseMetrics.potentialCollisions < (objectCount * objectCount) / 4,
            "Broad phase should significantly reduce collision pairs")

        val narrowPhaseMetrics = physicsWorld.getNarrowPhaseMetrics()

        assertTrue(narrowPhaseMetrics.actualCollisions <= broadPhaseMetrics.potentialCollisions,
            "Narrow phase collisions should be less than or equal to broad phase")

        // 8. Test multithreading performance
        if (physicsWorld.supportsMultithreading()) {
            physicsWorld.setThreadCount(4)

            val multithreadedMetrics = performanceMonitor.startMeasurement("physics_multithreaded")

            repeat(300) { // 5 seconds with multithreading
                physicsWorld.step()
            }

            val multithreadedResult = performanceMonitor.endMeasurement(multithreadedMetrics)
            val multithreadedStepTime = multithreadedResult.averageFrameTime

            val multithreadingImprovement = (sleepingStepTime - multithreadedStepTime) / sleepingStepTime

            assertTrue(multithreadingImprovement > 0f, "Multithreading should improve performance")
        }

        // Final verification - physics should maintain target frame rate
        assertTrue(sleepingStepTime < 16.67f, "Optimized physics should maintain 60 FPS")
    }
}