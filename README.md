# cljs-devtools-sample

** DANGER ZONE - WORK IN PROGRESS - EXPERIMENTAL APIs **

cljs-devtools provides additional tools to aid in ClojureScript web development.

  * Better presentation of ClojureScript values in Chrome Devtools Console.

This project is an example of integration of cljs-devtools into an external project.

<img src="https://dl.dropboxusercontent.com/u/559047/cljs-formatter-prototype.png">

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

Optionally you can start browser REPL:

    ./scripts/brepl

Clean project:

    lein clean

## License

[MIT License](http://opensource.org/licenses/MIT)
