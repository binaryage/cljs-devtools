# cljs-devtools-sample

**DANGER ZONE - WORK IN PROGRESS - EXPERIMENTAL APIs**

This project is an example of integration of [cljs-devtools](https://github.com/binaryage/cljs-devtools) into a ClojureScript project.

<img src="https://dl.dropboxusercontent.com/u/559047/cljs-formatter-prototype.png">

## Setup

Initial setup:

    cd some-work-dir
    git clone https://github.com/binaryage/cljs-devtools-sample.git
    cd cljs-devtools-sample
    
Build the project:

    lein cljsbuild auto

Start local server (in another shell session):

    lein ring server

Leiningen should open your default browser with http://localhost:3000. 
Please go to your Chrome Canary with [enabled custom formatters](https://github.com/binaryage/cljs-devtools). 
Open the web development console under devtools and you should see something similar to the screenshot at the top.

#### License [MIT](https://raw.githubusercontent.com/binaryage/cljs-devtools-sample/master/LICENSE.txt)
