# MeasureUserActions
Android app designed for measuring time in any application by using an overlay button. It was created to measure time of executing tasks for HCI (Human Computer Interaction) experiment.

Here is an example of doing a task: [https://www.youtube.com/shorts/XF6egrqZ6fw](https://www.youtube.com/shorts/XF6egrqZ6fw)

Application shows shows movable overlay button on any screen, as shown here: 
<p align="center">
<img src="https://github.com/fluffyspace/MeasureUserActions/assets/1338761/3c13c796-a445-42e2-aa40-880ddcff25e3" height="400">
</p>

In further text I will refer to participants in your HCI experiments as victims.

## Usage
When first opening the app the victim has to enter your link to JSON file which contains exercises. After that, exercises are loaded and victim can proceed to do tasks.

Before a victim can measure time, she has to select for which application she wants to measure time. List of allowed applications for each exercise are defined in JSON file.

## Creating exercises
To create exercises for your victims, use following repository: [https://github.com/fluffyspace/hci_web](https://github.com/fluffyspace/hci_web)

You can also manually create JSON file but it may take more time.

With that application you can easily create JSON text which contains following data for each exercise:
  - exercise ID
  - exercise name
  - instructions in HTML (so you can embed images and other fancy website elements in your instructions)
  - approximate duration
  - repetitions needed
  - list of applications for testing

Example of such JSON file: https://pastebin.com/raw/w8C31JNq
