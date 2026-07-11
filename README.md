# XML Drawable Preview

An Eclipse ADT Android project that previews XML drawables sent from a development machine.

## Features

* Runs as a classic Eclipse Android project (`.project`, `.classpath`, `project.properties`).
* Starts an in-app HTTP server on port `8765` using Android's built-in legacy `org.apache.http` APIs.
* Exposes `POST /preview`; the request body is read as raw XML bytes and rendered in the preview view.
* Offers a size slider plus common drawable state toggles for pressed, focused, selected, and enabled.
* Supports an extra vector path debugging overlay. Add a non-namespaced attribute such as `debug="true"`, `debugControlPoints="true"`, or `showControlPoints="true"` to a `<path>` element to draw the path outline and its curve control points.

## Usage

Install and launch the app, then post XML from your workstation:

```sh
curl -X POST --data-binary @res/drawable/example.xml http://DEVICE_IP:8765/preview
```

For a debug vector path:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:pathData="M2,12 C8,2 16,22 22,12"
        debugControlPoints="true" />
</vector>
```
