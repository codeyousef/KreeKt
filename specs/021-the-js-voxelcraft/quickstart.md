# Quickstart: Verifying the Fix

This guide provides the steps to run the VoxelCraft JS example and verify that the blank screen bug has been fixed.

## Prerequisites

- You have a complete development environment for KreeKt set up.
- You are on the feature branch `021-the-js-voxelcraft`.

## Steps

1.  **Build the Project**

    Run the main Gradle build command from the root of the repository to ensure all modules are compiled:

    ```bash
    ./gradlew build
    ```

2.  **Run the JS Example**

    Execute the Gradle task to run the VoxelCraft example for the JavaScript target. This will start a local development server.

    ```bash
    ./gradlew :examples:voxelcraft:runJs
    ```

3.  **Open in Browser**

    Once the server is running (you will see output like `> Task :examples:voxelcraft:runJs`), open your web browser and navigate to:

    [http://localhost:8080](http://localhost:8080)

4.  **Verify the Fix**

    - On the loaded page, click the "Start Game" button.
    - **Expected Result**: Instead of a black or blank screen, you should see a 3D voxel world rendered.
    - You should be able to move the camera using the mouse and move the player using the `WASD` keys.
    - The HUD at the top of the screen should display your position, FPS, and other stats.
