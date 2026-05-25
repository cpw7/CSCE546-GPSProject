# GPS Project App

CSCE 546 Mobile Application Development  
Christopher Wright

## Project description

This is an Android/Kotlin app for the GPS project. The app reads location data from the Android location system and displays:

- Latitude
- Longitude
- Accuracy
- Altitude if the device/emulator provides it
- Speed if available
- Bearing if available
- Location provider and timestamp

The app also includes a simple geofence/boundary feature. After a location is received, the user can set the current location as the boundary center. The app then checks whether the current GPS location is inside or outside the selected radius.

## Main features

- Requests location permission
- Reads current GPS/location values
- Starts live GPS/location updates
- Stops live GPS/location updates
- Sets a boundary center based on the current location
- Checks inside/outside geofence status
- Includes emulator testing directions inside the app

## How to run

1. Open the project folder in Android Studio.
2. Let Gradle sync finish.
3. Create or start an Android emulator.
4. Run the app.
5. Allow location permission when asked.
6. On the emulator, open the three-dot menu, go to Location, enter a latitude and longitude, and click Set Location.
7. Return to the app and press Get Current Location or Start GPS Updates.

## Suggested demo screenshots

Take screenshots of:

1. The app opened on the emulator.
2. Location permission granted.
3. A GPS/location reading showing latitude and longitude.
4. Live updates running.
5. Boundary center set.
6. Geofence status showing inside or outside the boundary.

## Submission

Submit the GitHub repository link and the demo PDF/video in Blackboard.
