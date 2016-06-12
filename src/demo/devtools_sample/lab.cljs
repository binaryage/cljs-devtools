(ns devtools-sample.lab
  (:require-macros [devtools-sample.logging :refer [log]])
  (:require [devtools-sample.boot :refer [boot!]]
            [devtools.format :as format]
            [devtools.toolbox :as toolbox]))


(boot! "/src/demo/devtools_sample/lab.cljs")

; --- MEAT STARTS HERE -->
; note: (log ...) function is a shorthand for (.log js/console ...)

(deftype SomeType [some-field])

(def test-interleaved #js {"js" true "nested" {:js false :nested #js {"js2" true "nested2" {:js2 false}}}})

(def v nil)

(def circular1 (atom nil))
(reset! circular1 circular1)

(def circular2 {:k (atom nil)})
(reset! (:k circular2) circular2)

; defrecord with IDevtoolsFormat
(defrecord Language [lang]
  format/IDevtoolsFormat
  (-header [_] (format/template "span" "color:white; background-color:darkgreen; padding: 0px 4px" (str "Language: " lang)))
  (-has-body [_])
  (-body [_]))

(def test-lang (Language. "ClojureScript"))

; reify with IDevtoolsFormat
(def test-reify (reify
                  format/IDevtoolsFormat
                  (-header [_] (format/template "span"
                                 "color:white; background-color:brown; padding: 0px 4px"
                                 "testing reify"))
                  (-has-body [_] false)
                  (-body [_])))

(def long-string
  (str "First line\n"
       "second line\n"
       "third line is really "
       "looooooooooooooooooooooooooooooooooooooooooooooooooooooooong "
       "looooooooooooooooooooooooooooooooooooooooooooooooooooooooong "
       "looooooooooooooooooooooooooooooooooooooooooooooooooooooooong \n"
       "\n"
       "last line"))

(def busy-obj
  (js-obj
    "a" 1
    "s" "string"
    "f" (fn some-fn [] (str "do" "something"))))

(def state (atom []))

(defn simple-fn [count]
  (let [rng (range count)]
    (doseq [item rng]
      (let [s (str "item=" item)]
        (swap! state conj s)))))                                                                                              ; <- set breakpoint HERE and see Scope variables in DevTools

(defn some-fancy-name$%!? [arg1 arg? mo-re]
  (str "hello!"))

(defn fancy-va-arg_fn* [first# & rest!])

(defn multi-arity->>3
  ([a1])
  ([a2_1 a2-2])
  ([a3_1 a3-2 a3-3 a3-4]))

(defn multi-arity-with-var-args
  ([a1])
  ([a2_1 a2-2])
  ([a3_1 a3-2 a3-3 a3-4])
  ([va1 va2 & rest]))

(defn dollar$name$
  "Some docstring ZZZZ"
  [& args])

(deftype ATypeWithIFn []
  Fn
  IFn
  (-invoke [this param1])
  (-invoke [this p1 p2])
  (-invoke [this px---1 px---2 & rest]))

(def instance-with-ifn (ATypeWithIFn.))

(defn sample-cljs-fn [p1 p2 & rest]
  (println p1 p2 rest))

(defn cljs-fn-with-vec-destructuring [[a b c & rest]])
(defn cljs-fn-with-vec-destructuring2 [& [a b c & rest]])
(defn cljs-fn-with-map-destructuring [{:keys [a b c]}])
(defn cljs-fn-with-map-destructuring2 [& {:keys [a b c]}])

(log (SomeType. "some value"))
(log [test-lang test-reify])
(log (.-nested test-interleaved))
(log test-interleaved)
(log [long-string])

(log [::namespaced-keyword])
(log [1 [2 [3 [4 [5 [6 [7 [8 [9]]]]]]]]])
(log circular1)
(log circular2)
(log (var v))
(log (toolbox/envelope (range 20)))
(log (toolbox/envelope js/window "win envelope with a custom header"))
(log (toolbox/envelope "hello!" #(str "envelope with a custom header function, the wrapped value is '" % "'")))
(log (toolbox/envelope busy-obj
                       "busy js-object envelope with a custom style"
                       "color:white;background-color:purple;padding:0px 4px;"
                       "div"))

; see https://github.com/binaryage/cljs-devtools/issues/14#issuecomment-217485305
(.log js/console {:value "b"
                  :map   {:some-key "some-val"}
                  :vec   [{:1 "1-val"
                           :2 "2-val"
                           :3 {:3-val-k "3-val"}
                           ;; :4 (range 0 20)
                           }]})

(log some-fancy-name$%!? fancy-va-arg_fn* multi-arity->>3 dollar$name$ multi-arity-with-var-args (fn ([]) ([a b & c]))
     (fn [p--x p---y p--p---z & p--var]))

(log (fn [a b c]) instance-with-ifn)

(log sample-cljs-fn)

(log cljs-fn-with-vec-destructuring)
(log cljs-fn-with-vec-destructuring2)
(log cljs-fn-with-map-destructuring)
(log cljs-fn-with-map-destructuring2)

; <-- MEAT STOPS HERE ---
