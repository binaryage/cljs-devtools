(ns devtools.toolbox
  (:require [devtools.protocols :refer [IFormat]]
            [devtools.formatters.templating :refer [make-template make-surrogate]]
            [devtools.formatters.templating :refer [render-markup]]
            [devtools.formatters.markup :refer [<preview> <standard-body-reference>]]))

; TODO: convert envelope to use new markup apis

(defn envelope
  "This is a simple wrapper for logging \"busy\" objects.
  This is especially handy when you happen to be logging javascript objects with many properties.
  Standard javascript console renderer tends to print a lot of infomation in the header in some cases and that can make
  console output pretty busy. By using envelope you can force your own short header and let the details expand on demand
  via disclosure triangle. The header can be styled and you can optionally specify preferred wrapping tag (advanced)."
  ([obj]
   (envelope obj :default-envelope-header))
  ([obj header]
   (envelope obj header :default-envelope-style))
  ([obj header style]
   (envelope obj header style :span))
  ([obj header style tag]
   (reify
     IFormat
     (-header [_] (make-template tag style (if (fn? header) (header obj) header)))
     (-has-body [_] true)
     (-body [_] (make-template :span :body-style (render-markup (<standard-body-reference> obj)))))))

(defn force-format
  "Forces object to be rendered by cljs-devtools during console logging.

  Unfortunately custom formatters subsystem in DevTools is not applied to primitive values like numbers, strings, null, etc.
  This wrapper can be used as a workaround if you really need to force cljs-devtools rendering:

    (.log js/console nil) ; will render 'null'
    (.log js/console (force-format nil)) ; will render 'nil' and not 'null'

  See https://github.com/binaryage/cljs-devtools/issues/17
  "
  [obj]
  (make-surrogate obj (<preview> obj) false))
