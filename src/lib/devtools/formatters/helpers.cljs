(ns devtools.formatters.helpers
  (:require-macros [devtools.util :refer [oget oset ocall oapply safe-call]])
  (:require [devtools.prefs :as prefs]
            [devtools.munging :as munging]
            [devtools.format :refer [IDevtoolsFormat]]
            [devtools.protocols :refer [ITemplate IGroup ISurrogate IFormat]]))

(defn pref [v]
  (if (keyword? v)
    (recur (prefs/pref v))
    v))

; ---------------------------------------------------------------------------------------------------------------------------

(defn is-prototype? [o]
  (identical? (.-prototype (.-constructor o)) o))

(defn is-js-symbol? [o]
  (= (goog/typeOf o) "symbol"))

(defn cljs-function? [value]
  (and (not (pref :disable-cljs-fn-formatting))
       (not (var? value))                                                                                                     ; HACK: vars have IFn protocol and would act as functions TODO: implement custom rendering for vars
       (munging/cljs-fn? value)))

(defn has-formatting-protocol? [value]
  (or (safe-call satisfies? false IPrintWithWriter value)
      (safe-call satisfies? false IDevtoolsFormat value)                                                                      ; legacy
      (safe-call satisfies? false IFormat value)))

; IRC #clojurescript @ freenode.net on 2015-01-27:
; [13:40:09] darwin_: Hi, what is the best way to test if I'm handled ClojureScript data value or plain javascript object?
; [14:04:34] dnolen: there is a very low level thing you can check
; [14:04:36] dnolen: https://github.com/clojure/clojurescript/blob/c2550c4fdc94178a7957497e2bfde54e5600c457/src/clj/cljs/core.clj#L901
; [14:05:00] dnolen: this property is unlikely to change - still it's probably not something anything anyone should use w/o a really good reason
(defn cljs-type? [f]
  (and (goog/isObject f)                                                                                                      ; see http://stackoverflow.com/a/22482737/84283
       (not (is-prototype? f))
       (oget f "cljs$lang$type")))

(defn cljs-instance? [value]
  (and (goog/isObject value)                                                                                                  ; see http://stackoverflow.com/a/22482737/84283
       (cljs-type? (oget value "constructor"))))

(defn cljs-land-value? [value]
  (or (cljs-instance? value)
      (has-formatting-protocol? value)))                                                                                      ; some raw js types can be extend-protocol to support cljs printing, see issue #21

(defn cljs-value? [value]
  (and
    (or (cljs-land-value? value)
        (cljs-function? value))
    (not (is-prototype? value))
    (not (is-js-symbol? value))))

(defn bool? [value]
  (or (true? value) (false? value)))

(defn instance-of-a-well-known-type? [value]
  (let [well-known-types (pref :well-known-types)
        constructor-fn (oget value "constructor")
        [ns name] (munging/parse-constructor-info constructor-fn)
        fully-qualified-type-name (str ns "/" name)]
    (contains? well-known-types fully-qualified-type-name)))

; ---------------------------------------------------------------------------------------------------------------------------

(defn abbreviate-long-string [string marker prefix-limit postfix-limit]
  (let [prefix (apply str (take prefix-limit string))
        postfix (apply str (take-last postfix-limit string))]
    (str prefix marker postfix)))

