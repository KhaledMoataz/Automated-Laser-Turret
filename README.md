# Automated Laser Turret

A prototype for a missle defence, where it detects strange objects that enters its range and fire on them.\
The turret has 2 degrees of freedom (rotation around x axis and z axis).

## Modes of Operation
Power Button : Select Mode\
Button 1 : Choose First Mode\
Button 2 : Choose Second Mode\
Button 8 : Save remoteX, remoteY values in EEPROM (Calibration values)

### First Mode (Control using Remote)
  - Fast Forward (>>|) : Move Right
  - Fast Back (|<<) : Move Left
  - Vol+ : Move Up
  - Vol- : Move Down
  - Pause/Play : Fire
### Second Mode (Automated)
- Object detection is done by thresholding on colour values in HSV color space.\
The lower and upper bounds are taken as an input in  the android application.
- The application then sends the center of the largest object it detects to the Arduino serially     by usb cable.
- The Arduino converts the center from pixel space to angles to be given to the motors.
## Main Components
  - Android mobile with good camera for image processing
  - Arduino uno or mega
  - 2 - Servo motors for z-axis rotation (left, right) and x-axis rotation (up and down)
  - Laser or Gun for firing
  - Relay for controlling the firing
  - Wood & Stone for making the base
  - IR Receiver Module & Remote
  - LCD & Potentiometer
  - Adapter & Power Supply Module
## Demo Video
[![Automate Laser Turret](https://img.youtube.com/vi/4U9zLkuj-vE/hqdefault.jpg)](https://www.youtube.com/watch?v=4U9zLkuj-vE)

