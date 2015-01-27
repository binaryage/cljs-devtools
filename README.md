# cljs-devtools-sample

** DANGER ZONE - WORK IN PROGRESS - EXPERIMENTAL APIs **

cljs-devtools provides additional tools to aid in ClojureScript web development.

  * Better presentation of ClojureScript values in Chrome Devtools Console.

This project is an example of integration of cljs-devtools into an external project.

<img src="https://dl.dropboxusercontent.com/u/559047/cljs-formatter-prototype.png">

## Enable Custom formatters in your Chrome (Canary)

For now you must use Chrome Canary (worked for me under 42.0.2289.0 canary (64-bit) @ OS X)

##### First, enable Dev Tools Experiments:

  * Visit chrome://flags
  * Search the page for "devtools".
  * Click "Enable" under "Enable Developer Tools Experiments"
  * Click "Relaunch Now"

##### Then turn on custom formatters:

  * Open Dev Tools.
  * Click the gear in the upper right to go to Settings.
  * Click the "Experiments" tab.
  * Press the shift key six times to show the "hidden experiments".
  * Check "Custom Object Formatters".
  * Close Dev Tools.
  * Open Dev Tools.
  * Click the gear to open settings again.
  * Under "Console", check "Enable custom formatters".

## Setup

cljs-devtools library has not been published yet, you have to install it locally

Initial setup:

    cd some-work-dir
    git clone https://github.com/binaryage/cljs-devtools.git
    git clone https://github.com/binaryage/cljs-devtools-sample.git
    cd cljs-devtools-sample
    mkdir checkouts
    ln -s ../../cljs-devtools checkouts/cljs-devtools

Build the project:

    lein cljsbuild auto

Start local server (in another shell session):

    lein ring server

Leiningen should open your default browser with http://localhost:3000. Please go to your Chrome Canary with enabled custom formatters. Open web development console under devtools and you should see something similar to the screenshot the top.

### Optional commands

Optionally you can start browser REPL:

    ./scripts/brepl

Clean project:

    lein clean

## Integration in your very own project

At some point you have to run `support-devtools!` from `devtools.core` namespace. Ideally run this at launch time of your app.

    (ns your-project.core
      (:require [devtools.core :as dev]))
    
    (dev/support-devtools!)

## License

[MIT License](http://opensource.org/licenses/MIT)
